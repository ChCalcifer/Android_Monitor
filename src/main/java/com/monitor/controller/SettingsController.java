package com.monitor.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:51
 * @Version 1.0.0
 */

public class SettingsController {
    @FXML
    private Label settingsStatusLabel;

    @FXML
    private void initialize() {
        settingsStatusLabel.setText("设置页面\n（功能待实现）");
    }
}
