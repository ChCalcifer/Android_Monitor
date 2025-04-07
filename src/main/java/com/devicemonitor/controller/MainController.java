package com.devicemonitor.controller;

import com.devicemonitor.DeviceMonitor;
import com.devicemonitor.utils.AdbUtil;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import javafx.scene.canvas.Canvas;
import javafx.util.Duration;

/**
 * Author: CYC
 * Time: 2025/3/27 16:14:15
 * Description:
 * Branch:
 * Version: 1.0
 * @author uu
 */

public class MainController implements Initializable, DeviceMonitor.DeviceStatusListener {
    @FXML
    private Canvas statusCanvas;
    @FXML
    private HBox cpuFrequenciesBox;
    @FXML
    private Label statusLabel,
            phoneModelLabel,
            softwareVersionLabel,
            androidVersionLabel,
            batteryTempLabel,
            activityLabel,
            fpsLabel,
            displaySizeLabel,
            socTempLabel,
            cpuSmallCoreTempLabel,
            cpuBigCoreTempLabel,
            modemTempLabel,
            pmicTempLabel,
            cameraTempLabel,
            gpuTempLabel,
            resultLabel,
            powerHalStatus,
            buildTypeLabel,
            localTimeLabel,
            dpiLabel;
    @FXML
    private Button powerHalButton;


    @FXML
    private ListView<String> menuListView;

    @FXML
    private TabPane tabPane;

    @FXML
    private Tab deviceTab,
            cpuTab,
            gpuTab,
            displayTab,
            specialFunctionTab,
            deviceRootTab,
            spdTab,
            settingsTab;

    @FXML
    private Pane deviceInfoPane,
            cpuPane,
            gpuPane,
            displayPane,
            specialFunctionPane,
            deviceUnlockPane,
            spdPane,
            settingsPane;
    @FXML
    private ToggleGroup serviceModeGroup;


    /**
    状态变量，true表示当前是关闭状态
    */
    private boolean isPowerHalDisabled = true;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Timeline timeline;




    // @FXML
    // private void togglePowerHal() {
    //     if (isPowerHalDisabled) {
    //         // 如果当前是关闭状态，执行开启操作
    //         AdbUtil.setPowerHalState(1, powerHalStatus);
    //         powerHalButton.setText("开启PowerHal");
    //     } else {
    //         // 如果当前是开启状态，执行关闭操作
    //         AdbUtil.setPowerHalState(0, powerHalStatus);
    //         powerHalButton.setText("关闭PowerHal");
    //     }
    //     isPowerHalDisabled = !isPowerHalDisabled;
    // }

    private DeviceMonitor deviceMonitor;

    private final ObservableList<Label> frequencyLabels = FXCollections.observableArrayList();

    private static final int NUM_OF_CORE = 16;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // // 设置Canvas固定大小
        // statusCanvas.setWidth(38);
        // statusCanvas.setHeight(40);

        // 初始化横向标签容器
        // initFrequencyLabels();
        // updateStatusMessage(false);
        deviceMonitor = new DeviceMonitor();
        deviceMonitor.startMonitoring(this);

        // updateDeviceInfo();
        // setupTimeUpdater();
        // updateLocalTime();
//        disableTabTitleClicks();
    }

    // 禁用 Tab 的标题点击
//    private void disableTabTitleClicks() {
//        deviceTab.setDisable(true);
//        cpuTab.setDisable(true);
//        gpuTab.setDisable(true);
//        displayTab.setDisable(true);
//        specialFunctionTab.setDisable(true);
//        deviceRootTab.setDisable(true);
//        spdTab.setDisable(true);
//        settingsTab.setDisable(true);
//
//        // 如果希望 Tab 不能被关闭，可以禁用关闭按钮
//        deviceTab.setClosable(false);
//        cpuTab.setClosable(false);
//        gpuTab.setClosable(false);
//        displayTab.setClosable(false);
//        specialFunctionTab.setClosable(false);
//        deviceRootTab.setClosable(false);
//        spdTab.setClosable(false);
//        settingsTab.setClosable(false);
//    }
    // private void initFrequencyLabels() {
    //     // 横向间距
    //     cpuFrequenciesBox.getChildren().clear();
    //     cpuFrequenciesBox.setSpacing(2);
    //
    //     for (int i = 0; i < NUM_OF_CORE; i++) {
    //         Label label = new Label();
    //         // 简化样式
    //         label.setStyle("-fx-font-family: monospace;");
    //         label.setVisible(false);
    //         frequencyLabels.add(label);
    //     }
    //     cpuFrequenciesBox.getChildren().addAll(frequencyLabels);
    // }

    // public void updateDeviceInfo() {
    //     // 调用 ADBUtil 中的 displayDeviceModel 方法
    //     AdbUtil.getDeviceModel(phoneModelLabel);
    //     AdbUtil.getDeviceSoftwareVersion(softwareVersionLabel);
    //     AdbUtil.getAndroidVersion(androidVersionLabel);
    //     AdbUtil.getBatteryTemperature(batteryTempLabel);
    //     AdbUtil.getActivity(activityLabel);
    //     AdbUtil.getFrameRate(fpsLabel);
    //     AdbUtil.getDisplaySize(displaySizeLabel);
    //     AdbUtil.getSocTemp(socTempLabel);
    //     AdbUtil.getSmCoreTemp(cpuSmallCoreTempLabel);
    //     AdbUtil.getBigCoreTemp(cpuBigCoreTempLabel);
    //     AdbUtil.getModemTemp(modemTempLabel);
    //     AdbUtil.getPmicTemp(pmicTempLabel);
    //     AdbUtil.getCameraTemp(cameraTempLabel);
    //     AdbUtil.getGpuTemp(gpuTempLabel);
    //     AdbUtil.getBuildType(buildTypeLabel);
    //     AdbUtil.getDpi(dpiLabel);
    // }
    // @FXML
    // private void brightnessSetDefault() {
    //     AdbUtil.setScreenBrightness(102, resultLabel);
    // }
    @Override
    public void onStatusUpdate(boolean isConnected, List<String> cpuFrequencies) {
        // Platform.runLater(() -> {
        //     // 更新设备状态灯
        //     drawStatusLight(isConnected);
        //
        //     updateStatusMessage(isConnected);
        //
        //     updateDeviceInfo();
        //
        //     // 优化：减少频繁的可见性和管理性切换
        //     for (int i = 0; i < frequencyLabels.size(); i++) {
        //         Label label = frequencyLabels.get(i);
        //
        //         // 如果频率数据已改变，更新文本
        //         if (i < cpuFrequencies.size()) {
        //             String newFrequency = cpuFrequencies.get(i);
        //             if (!newFrequency.equals(label.getText())) {
        //                 label.setText(newFrequency);
        //             }
        //
        //             // 只需要设置可见性一次，避免频繁切换
        //             if (!label.isVisible()) {
        //                 label.setVisible(true);
        //                 label.setManaged(true);
        //             }
        //         } else {
        //             // 没有频率数据时，隐藏标签
        //             if (label.isVisible()) {
        //                 label.setVisible(false);
        //                 label.setManaged(false);
        //             }
        //         }
        //     }
        // });
    }
    // private void drawStatusLight(boolean isConnected) {
    //     GraphicsContext gc = statusCanvas.getGraphicsContext2D();
    //     gc.clearRect(0, 0, statusCanvas.getWidth(), statusCanvas.getHeight());
    //
    //     // 绘制状态灯
    //     gc.setFill(isConnected ? Color.valueOf("#2ecc71") : Color.valueOf("#e74c3c"));
    //     gc.fillOval(0, 1, 38, 38);
    //
    // }
    // private void updateStatusMessage(boolean isConnected) {
    //     // 安全防护
    //     if(statusLabel == null){
    //         return;
    //     }

    //     String status = isConnected ? "已连接 ✓" : "未连接 ×";
    //     String color = isConnected ? "#2ecc71" : "#e74c3c";
    //
    //     statusLabel.setText(status);
    //     statusLabel.setStyle("-fx-text-fill: " + color + ";");
    // }
    /**
     * 新增时间更新方法
     * */
    // private void setupTimeUpdater() {
    //     timeline = new Timeline(
    //             new KeyFrame(Duration.seconds(1), e -> {
    //                 updateLocalTime();
    //             })
    //     );
    //     timeline.setCycleCount(Timeline.INDEFINITE);
    //     timeline.play();
    // }
    // private void updateLocalTime() {
    //     LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
    //     String formattedTime = now.format(formatter);
    //     localTimeLabel.setText(formattedTime);
    // }

    @FXML
    private void handleMenuClick() {
        deviceInfoPane.setVisible(false);
        cpuPane.setVisible(false);
        gpuPane.setVisible(false);
        displayPane.setVisible(false);
        specialFunctionPane.setVisible(false);
        deviceUnlockPane.setVisible(false);
        spdPane.setVisible(false);
        settingsPane.setVisible(false);

        String selectedItem = menuListView.getSelectionModel().getSelectedItem();

        if (selectedItem != null) {
            switch (selectedItem) {
                case "设备信息":
                    deviceInfoPane.setVisible(true);
                    break;
                case "CPU":
                    cpuPane.setVisible(true);
                    break;
                case "GPU":
                    gpuPane.setVisible(true);
                    break;
                case "Display":
                    displayPane.setVisible(true);
                    break;
                case "特色功能":
                    specialFunctionPane.setVisible(true);
                    break;
                case "设备解锁":
                    deviceUnlockPane.setVisible(true);
                    break;
                case "Spd":
                    spdPane.setVisible(true);
                    break;
                case "设置":
                    settingsPane.setVisible(true);
                    break;
                default:
                    break;
            }
        }
    }

    public void shutdown() {
        if (deviceMonitor != null) {
            deviceMonitor.stopMonitoring();
        }

        // 新增停止时间更新
        if (timeline != null) {
            timeline.stop();
        }
    }
}

