package com.monitor.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:50
 * @Version 1.0.0
 */

public class DisplayInfoController {
    @FXML
    private Label displayStatusLabel;

    @FXML
    private void initialize() {
        displayStatusLabel.setText("显示信息监控页面\n（功能待实现）");
    }
}
