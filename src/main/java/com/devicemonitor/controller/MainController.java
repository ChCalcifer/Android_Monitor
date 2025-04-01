package com.devicemonitor.controller;

import com.devicemonitor.DeviceMonitor;
import com.devicemonitor.utils.ADBUtil;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.Glow;
import javafx.scene.layout.HBox;
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
    private HBox cpuFrequenciesBox;
    @FXML
    private Label statusLabel;
    @FXML
    private Label phoneModelLabel;
    @FXML
    private Label softwareVersionLabel;
    @FXML
    private Label androidVersionLabel;
    @FXML
    private Label batteryTempLabel;

    private DeviceMonitor deviceMonitor;

    private Timeline updateTimeline;

    private final ObservableList<Label> frequencyLabels = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 设置Canvas固定大小
        statusCanvas.setWidth(40);
        statusCanvas.setHeight(40);

        // 初始化横向标签容器
        initFrequencyLabels();
        updateStatusMessage(false);
        deviceMonitor = new DeviceMonitor();
        deviceMonitor.startMonitoring(this);

        updateDeviceInfo();
    }

    private void initFrequencyLabels() {
        cpuFrequenciesBox.getChildren().clear();
        cpuFrequenciesBox.setSpacing(2); // 横向间距

        for (int i = 0; i < 16; i++) {
            Label label = new Label();
            label.setStyle("-fx-font-family: monospace;"); // 简化样式
            label.setVisible(false);
            frequencyLabels.add(label);
        }
        cpuFrequenciesBox.getChildren().addAll(frequencyLabels);
    }

    public void updateDeviceInfo() {
        // 调用 ADBUtil 中的 displayDeviceModel 方法
        ADBUtil.getDeviceModel(phoneModelLabel);
        ADBUtil.getDeviceSoftwareVersion(softwareVersionLabel);
        ADBUtil.getAndroidVersion(androidVersionLabel);
        ADBUtil.getBatteryTemperature(batteryTempLabel);
    }

    @Override
    public void onStatusUpdate(boolean isConnected, List<String> cpuFrequencies) {
        Platform.runLater(() -> {
            // 更新设备状态灯
            drawStatusLight(isConnected);

            updateStatusMessage(isConnected);

            updateDeviceInfo();

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

        // 绘制状态灯
        gc.setFill(isConnected ? Color.valueOf("#2ecc71") : Color.valueOf("#e74c3c"));
        gc.fillOval(0, 1, 38, 38);

    }


    private void updateStatusMessage(boolean isConnected) {
        if (statusLabel == null) return; // 安全防护

        String status = isConnected ? "已连接 ✓" : "未连接 ×";
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

