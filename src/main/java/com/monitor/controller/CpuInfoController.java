package com.monitor.controller;

import com.monitor.utils.CpuInfoUtil;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;
import java.awt.Color;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:49
 * @Version 1.0.0
 */

public class CpuInfoController {
    @FXML private ChartViewer combinedChartViewer;
    @FXML private HBox bigCoreCheckBoxContainer;
    @FXML private HBox smallCoreCheckBoxContainer;

    private final Map<Integer, TimeSeries> cpuSeriesMap = new ConcurrentHashMap<>();
    private TimeSeriesCollection dataset;
    private List<Integer> bigCores = new ArrayList<>();
    private List<Integer> smallCores = new ArrayList<>();

    private static final int MAX_DATA_POINTS = 100;
    private Timeline dataPollingTimeline;

    private final Set<Integer> selectedCores = ConcurrentHashMap.newKeySet();



    @FXML
    public void initialize() {
        setupChart();
        detectCoreTypes();
        createCheckBoxes();
        startDataPolling();
    }

    private void setupChart() {
        dataset = new TimeSeriesCollection();
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "CPU频率动态监控",
                "时间（秒）",
                "频率 (MHz)",
                dataset,
                true, true, false
        );

        XYPlot plot = chart.getXYPlot();
        plot.getDomainAxis().setAutoRange(true);
        plot.setBackgroundPaint(java.awt.Color.WHITE);

        combinedChartViewer.setChart(chart);
    }

    // 动态检测大核和小核（示例逻辑，需根据实际设备调整）
    private void detectCoreTypes() {
        // 假设核心0-3为小核，4-7为大核
        int totalCores = CpuInfoUtil.getCpuCoreCount();
        for (int i = 0; i < totalCores; i++) {
            if (i < 4) {
                smallCores.add(i);
            } else {
                bigCores.add(i);
            }
        }
    }

    private void createCheckBoxes() {
        // 创建小核复选框，只显示 CPU0
        smallCores.stream()
                .filter(core -> core == 0)
                .forEach(core -> {
                    CheckBox checkBox = new CheckBox("CPU " + core);
                    checkBox.setSelected(false); // 默认选中
                    checkBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                            toggleSeriesVisibility(core, newVal)
                    );
                    smallCoreCheckBoxContainer.getChildren().add(checkBox);
                });

        // 创建大核复选框，只显示 CPU7
        bigCores.stream()
                .filter(core -> core == 7)
                .forEach(core -> {
                    CheckBox checkBox = new CheckBox("CPU " + core);
                    checkBox.setSelected(false); // 默认选中
                    checkBox.selectedProperty().addListener((obs, oldVal, newVal) ->
                            toggleSeriesVisibility(core, newVal)
                    );
                    bigCoreCheckBoxContainer.getChildren().add(checkBox);
                });
    }

    private void toggleSeriesVisibility(int coreId, boolean visible) {
        TimeSeries series = cpuSeriesMap.get(coreId);
        if (series != null) {
            if (visible) {
                dataset.addSeries(series);
            } else {
                dataset.removeSeries(series);
            }
        }
    }

    private void startDataPolling() {
        dataPollingTimeline = new Timeline(
                new KeyFrame(javafx.util.Duration.seconds(1),
                        e -> CpuInfoUtil.getCpuFrequenciesAsync(this::updateChartData)
                ));
        dataPollingTimeline.setCycleCount(Animation.INDEFINITE);
        dataPollingTimeline.play();
    }

    private void updateChartData(List<String> frequencies) {
        long timestamp = System.currentTimeMillis();
        frequencies.stream()
                .filter(freq -> !freq.startsWith("Error"))
                .forEach(freq -> {
                    String[] parts = freq.split(" ");
                    int coreId = Integer.parseInt(parts[0].substring(3));
                    double mhz = Double.parseDouble(parts[1]);

                    TimeSeries series = cpuSeriesMap.computeIfAbsent(coreId,
                            k -> new TimeSeries("CPU " + coreId)
                    );

                    series.addOrUpdate(new Millisecond(new Date(timestamp)), mhz);
                    if (series.getItemCount() > MAX_DATA_POINTS) {
                        series.delete(0, series.getItemCount() - MAX_DATA_POINTS);
                    }
                });
    }

    @FXML
    public void shutdown() {
        if (dataPollingTimeline != null) {
            dataPollingTimeline.stop();
        }
        // 数据保留不清理
    }
}