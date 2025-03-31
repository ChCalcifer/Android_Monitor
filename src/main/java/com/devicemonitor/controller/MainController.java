package com.devicemonitor.controller;

import com.devicemonitor.DeviceMonitor;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext; // 新增这个导入
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import javafx.util.Duration;

/**
 * Author: CYC
 * Time: 2025/3/27 16:14:15
 * Description:
 * Branch:
 * Version: 1.0
 */

public class MainController implements Initializable, DeviceMonitor.DeviceStatusListener {
    @FXML
    private Canvas statusCanvas;
    @FXML
    private Label cpuFrequencyLabel;
    @FXML
    private VBox cpuFrequenciesBox;
    @FXML
    private Label statusLabel;

    private DeviceMonitor deviceMonitor;

    private Timeline updateTimeline;

    private final ObservableList<Label> frequencyLabels = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        initFrequencyLabels();

        updateStatusMessage(false);

        // 初始化设备监控器
        deviceMonitor = new DeviceMonitor();
        deviceMonitor.startMonitoring(this); // 关键修复点
    }

    private void initFrequencyLabels() {
        cpuFrequenciesBox.getChildren().clear();
        frequencyLabels.clear();

        // 动态创建标签（最多16核）
        for (int i = 0; i < 16; i++) {
            Label label = new Label();
            label.getStyleClass().add("cpu-frequency-label");
            label.setVisible(false);
            label.setManaged(false); // 优化布局计算
            frequencyLabels.add(label);
        }
        cpuFrequenciesBox.getChildren().addAll(frequencyLabels);
    }

    @Override
    public void onStatusUpdate(boolean isConnected, List<String> cpuFrequencies) {
        Platform.runLater(() -> {
            // 更新设备状态灯
            drawStatusLight(isConnected);

            // 优化：减少频繁的可见性和管理性切换
            for (int i = 0; i < frequencyLabels.size(); i++) {
                Label label = frequencyLabels.get(i);

                // 如果频率数据已改变，更新文本
                if (i < cpuFrequencies.size()) {
                    String newFrequency = cpuFrequencies.get(i);
                    if (!newFrequency.equals(label.getText())) {
                        label.setText(newFrequency);
                    }

                    // 只需要设置可见性一次，避免频繁切换
                    if (!label.isVisible()) {
                        label.setVisible(true);
                        label.setManaged(true);
                    }
                } else {
                    // 没有频率数据时，隐藏标签
                    if (label.isVisible()) {
                        label.setVisible(false);
                        label.setManaged(false);
                    }
                }
            }
        });
    }

    private void drawStatusLight(boolean isConnected) {
        GraphicsContext gc = statusCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, statusCanvas.getWidth(), statusCanvas.getHeight());

        // 绘制外圈
        gc.setStroke(Color.LIGHTGRAY);
        gc.setLineWidth(3);
        gc.strokeOval(10, 10, 80, 80);

        // 绘制状态灯
        gc.setFill(isConnected ? Color.valueOf("#2ecc71") : Color.valueOf("#e74c3c"));
        gc.fillOval(15, 15, 70, 70);

        // 添加光泽效果
        gc.setFill(Color.rgb(255, 255, 255, 0.3));
        gc.fillArc(20, 20, 60, 60, 30, 120, ArcType.ROUND);
    }


    private void updateStatusMessage(boolean isConnected) {
        if (statusLabel == null) return; // 安全防护

        String status = isConnected ? "设备已连接 ✓" : "设备未连接 ×";
        String color = isConnected ? "#2ecc71" : "#e74c3c";

        statusLabel.setText(status);
        statusLabel.setStyle("-fx-text-fill: " + color + ";");
    }

    public void shutdown() {
        if (deviceMonitor != null) {
            deviceMonitor.stopMonitoring();
        }
    }
}

