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
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:49
 * @Version 1.0.0
 */

public class CpuInfoController {
    @FXML private LineChart<Number, Number> upperChart;
    @FXML private LineChart<Number, Number> lowerChart;
    @FXML private HBox upperCheckBoxContainer;
    @FXML private HBox lowerCheckBoxContainer;
    @FXML private NumberAxis upperXAxis;
    @FXML private NumberAxis upperYAxis;
    @FXML private NumberAxis lowerXAxis;
    @FXML private NumberAxis lowerYAxis;

    private static final int MAX_DATA_POINTS = 30;
    private final Map<Integer, XYChart.Series<Number, Number>> seriesMap = new ConcurrentHashMap<>();
    private final Map<Integer, CheckBox> checkBoxMap = new ConcurrentHashMap<>();
    private final Map<Integer, Deque<XYChart.Data<Number, Number>>> dataQueues = new ConcurrentHashMap<>();
    private Timeline timeline;
    private final AtomicLong startTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong upperStartTime = new AtomicLong();
    private final AtomicLong lowerStartTime = new AtomicLong();
    private Timeline upperTimeline;
    private Timeline lowerTimeline;

    @FXML
    private void initialize() {
        setupCharts();
        initializeCheckBoxes();
        startDataPolling();
    }

    private void setupCharts() {
        // 配置上半部分图表
        upperXAxis.setAutoRanging(false);
        upperXAxis.setLowerBound(0);
        upperXAxis.setUpperBound(30);
        upperYAxis.setAutoRanging(false);
        upperYAxis.setLowerBound(500);
        upperYAxis.setUpperBound(2300);
        upperYAxis.setTickUnit(100);
        upperChart.setAnimated(false);
        upperChart.setCreateSymbols(false);

        // 配置下半部分图表
        lowerXAxis.setAutoRanging(false);
        lowerXAxis.setLowerBound(0);
        lowerXAxis.setUpperBound(30);
        lowerYAxis.setAutoRanging(false);
        lowerYAxis.setLowerBound(500);
        lowerYAxis.setUpperBound(2300);
        lowerYAxis.setTickUnit(100);
        lowerChart.setAnimated(false);
        lowerChart.setCreateSymbols(false);
    }

    private void initializeCheckBoxes() {
        int coreCount = CpuInfoUtil.getCpuCoreCount();
        for (int i = 0; i < coreCount; i++) {
            CheckBox checkBox = new CheckBox("CPU " + i);
            checkBox.setSelected(false);
            dataQueues.put(i, new ArrayDeque<>(MAX_DATA_POINTS));

            HBox targetContainer = i < 4 ? upperCheckBoxContainer : lowerCheckBoxContainer;
            targetContainer.getChildren().add(checkBox);

            int coreId = i;
            checkBox.selectedProperty().addListener((obs, oldVal, newVal) -> {
                XYChart.Series<Number, Number> series = seriesMap.get(coreId);
                if (series != null) {
                    series.getNode().setVisible(newVal);
                    if (newVal) {
                        // 选中时重置时间起点
                        if (coreId < 4) {
                            upperStartTime.set(System.currentTimeMillis());
                        } else {
                            lowerStartTime.set(System.currentTimeMillis());
                        }
                    }
                }
            });

            checkBoxMap.put(coreId, checkBox);
            createSeriesForCore(coreId);
        }
    }

    private void createSeriesForCore(int coreId) {
        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("CPU " + coreId);
        series.getNode().setVisible(false);

        if (coreId < 4) {
            upperChart.getData().add(series);
        } else {
            lowerChart.getData().add(series);
        }
        seriesMap.put(coreId, series);
    }

    private void startDataPolling() {
        upperStartTime.set(System.currentTimeMillis());
        lowerStartTime.set(System.currentTimeMillis());

        // 上半部分图表时间轴
        upperTimeline = new Timeline(new KeyFrame(Duration.millis(300), event -> {
            CpuInfoUtil.getCpuFrequenciesAsync(frequencies -> {
                Platform.runLater(() -> processUpperFrequencies(frequencies));
            });
        }));
        upperTimeline.setCycleCount(Animation.INDEFINITE);
        upperTimeline.play();

        // 下半部分图表时间轴
        lowerTimeline = new Timeline(new KeyFrame(Duration.millis(300), event -> {
            CpuInfoUtil.getCpuFrequenciesAsync(frequencies -> {
                Platform.runLater(() -> processLowerFrequencies(frequencies));
            });
        }));
        lowerTimeline.setCycleCount(Animation.INDEFINITE);
        lowerTimeline.play();
    }

    private void processUpperFrequencies(List<String> frequencies) {
        long currentTime = System.currentTimeMillis() - upperStartTime.get();
        double seconds = currentTime / 1000.0;

        frequencies.stream()
                .filter(freq -> !freq.startsWith("Error"))
                .filter(freq -> {
                    int coreId = Integer.parseInt(freq.split(" ")[0].substring(3));
                    return coreId < 4;
                })
                .forEach(freq -> processFrequency(freq, seconds, true));
    }

    private void processLowerFrequencies(List<String> frequencies) {
        long currentTime = System.currentTimeMillis() - lowerStartTime.get();
        double seconds = currentTime / 1000.0;

        frequencies.stream()
                .filter(freq -> !freq.startsWith("Error"))
                .filter(freq -> {
                    int coreId = Integer.parseInt(freq.split(" ")[0].substring(3));
                    return coreId >= 4;
                })
                .forEach(freq -> processFrequency(freq, seconds, false));
    }

    private void processFrequency(String freq, double seconds, boolean isUpper) {
        String[] parts = freq.split(" ");
        int coreId = Integer.parseInt(parts[0].substring(3));
        double mhz = Double.parseDouble(parts[1]);

        XYChart.Series<Number, Number> series = seriesMap.get(coreId);
        if (series == null || !checkBoxMap.get(coreId).isSelected()) return;

        Deque<XYChart.Data<Number, Number>> queue = dataQueues.get(coreId);
        queue.addLast(new XYChart.Data<>(seconds, mhz));
        if (queue.size() > MAX_DATA_POINTS) {
            queue.removeFirst();
        }

        series.getData().setAll(queue);
        updateXAxisRange(seconds, isUpper);
    }

    private void updateXAxisRange(double currentSeconds, boolean isUpper) {
        NumberAxis xAxis = isUpper ? upperXAxis : lowerXAxis;
        double lowerBound = Math.max(0, currentSeconds - 30);
        double upperBound = Math.max(30, currentSeconds);
        xAxis.setLowerBound(lowerBound);
        xAxis.setUpperBound(upperBound);
    }

    @FXML
    private void shutdown() {
        if (upperTimeline != null) upperTimeline.stop();
        if (lowerTimeline != null) lowerTimeline.stop();
        CpuInfoUtil.shutdownExecutor();
    }
}
