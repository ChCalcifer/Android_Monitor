package com.devicemonitor.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:49
 * @Version 1.0.0
 */

public class CpuInfoController {
    @FXML
    private Label cpuStatusLabel;

    @FXML
    private void initialize() {
        cpuStatusLabel.setText("CPU信息监控页面\n（功能待实现）");
    }
}
