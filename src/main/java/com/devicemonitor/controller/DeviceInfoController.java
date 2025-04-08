package com.devicemonitor.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:49
 * @Version 1.0.0
 */

public class DeviceInfoController {
    @FXML
    private Label deviceInfoLabel;

    @FXML
    private void initialize() {
        deviceInfoLabel.setText("CPU信息监控页面\n（功能待实现）");
    }
}
