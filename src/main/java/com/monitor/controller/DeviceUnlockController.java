package com.monitor.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:51
 * @Version 1.0.0
 */

public class DeviceUnlockController {
    @FXML
    private Label deviceStatusLabel;

    @FXML
    private void initialize() {
        deviceStatusLabel.setText("设备解锁页面\n（功能待实现）");
    }
}
