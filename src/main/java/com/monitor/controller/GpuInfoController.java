package com.monitor.controller;


import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:50
 * @Version 1.0.0
 */

public class GpuInfoController {
    @FXML
    private Label gpuStatusLabel;

    @FXML
    private void initialize() {
        gpuStatusLabel.setText("GPU信息监控页面\n（功能待实现）");
    }


}
