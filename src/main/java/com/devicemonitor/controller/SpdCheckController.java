package com.devicemonitor.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:51
 * @Version 1.0.0
 */

public class SpdCheckController {
    @FXML
    private Label spdStatusLabel;

    @FXML
    private void initialize() {
        spdStatusLabel.setText("spd文档检验页面\n（功能待实现）");
    }
}
