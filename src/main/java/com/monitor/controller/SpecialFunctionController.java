package com.monitor.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:50
 * @Version 1.0.0
 */

public class SpecialFunctionController {
    @FXML
    private Label specialFunctionStatusLabel;

    @FXML
    private void initialize() {
        specialFunctionStatusLabel.setText("特色功能页面\n（功能待实现）");
    }
}
