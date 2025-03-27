package com.devicemonitor.controller;

import com.devicemonitor.DeviceMonitor;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext; // 新增这个导入
import javafx.scene.control.Label;
import javafx.scene.paint.Color;

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

    private DeviceMonitor deviceMonitor;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 初始化并启动设备监控
        deviceMonitor = new DeviceMonitor();
        deviceMonitor.startMonitoring(this); // 注册监听器
    }

    @Override
    public void onStatusUpdate(boolean isConnected, String cpuFrequency) {
        Platform.runLater(() -> {
            drawStatusLight(isConnected);
            cpuFrequencyLabel.setText(cpuFrequency);
        });
    }

    private void drawStatusLight(boolean isConnected) {
        GraphicsContext gc = statusCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, statusCanvas.getWidth(), statusCanvas.getHeight());

        gc.setFill(isConnected ? Color.GREEN : Color.GRAY);
        gc.fillOval(10, 10, 30, 30);
    }

    public void shutdown() {
        if (deviceMonitor != null) {
            deviceMonitor.stopMonitoring();
        }
    }
}

