package com.monitor.controller;

import com.monitor.utils.DeviceInfoUtil;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.SplitPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Author: CYC
 * Time: 2025/3/27 16:14:15
 * Description:
 * Branch:
 * Version: 1.0
 * @author uu
 */

public class DeviceInfoController implements Initializable{
    @FXML
    private Canvas statusCanvas;
    @FXML
    private HBox cpuFrequenciesBox;

    @FXML
    private Button powerHalButton;

    @FXML
    private SplitPane splitPane;

    @FXML
    private ListView<String> menuListView;

    @FXML
    private TabPane tabPane;

    @FXML
    private Label deviceBuildVersionLabel,
            deviceBuildTypeLabel,
            deviceTabInside,
            deviceStatusLabel,
            deviceModelLabel,
            timerLabel,
            softwareVersionLabel,
            androidVersionLabel;

    private Timeline timerTimeline;
    private long startTime;

    @FXML
    private Tab deviceTab;

    private Tab cpuTab,
            gpuTab,
            displayTab,
            specialFunctionTab,
            deviceUnlockTab,
            spdTab,
            settingsTab;

    @FXML
    private TextArea gatActivityTextArea;

    @FXML
    private ToggleGroup serviceModeGroup;

    private Timeline statusCheckTimeline;


    /**
    状态变量，true表示当前是关闭状态
    */
    private boolean isPowerHalDisabled = true;

    // private void togglePowerHal() {
    //      if (isPowerHalDisabled) {
    //          // 如果当前是关闭状态，执行开启操作
    //          SpecialFunctionUtil.setPowerHalState(1, powerHalStatus);
    //          powerHalButton.setText("开启PowerHal");
    //      } else {
    //          // 如果当前是开启状态，执行关闭操作
    //          SpecialFunctionUtil.setPowerHalState(0, powerHalStatus);
    //          powerHalButton.setText("关闭PowerHal");
    //      }
    //      isPowerHalDisabled = !isPowerHalDisabled;
    //  }


    private final ObservableList<Label> frequencyLabels = FXCollections.observableArrayList();

    private static final int NUM_OF_CORE = 16;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // 设置ListView的固定大小和样式
        menuListView.setPrefWidth(120);
        menuListView.setPrefHeight(430);

        tabPane.setTabMinHeight(0);
        tabPane.setTabMaxHeight(0);
        tabPane.setStyle("-fx-tab-min-height: 0; -fx-tab-max-height: 0;");

        deviceTabInside.setText("设备信息监控页面\n（功能待实现）");

        setupStatusChecker();
        setupListViewAnimation();

        // 设置ListView的单元格工厂，使文本居中
        menuListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER); // 文本居中
                    setStyle("-fx-font-size: 14px; -fx-padding: 10px;"); // 设置字体大小和内边距
                }
            }
        });

        menuListView.setFixedCellSize(53);

        // 填充菜单项
        ObservableList<String> menuItems = FXCollections.observableArrayList(
                "信息",
                "CPU",
                "GPU",
                "Disp",
                "功能",
                "解锁",
                "Spd",
                "设置"
        );
        menuListView.setItems(menuItems);

        menuListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        showTab(newValue, newValue);
                    }
                });

        deviceTab = tabPane.getTabs().get(0);

        // 清空其他预定义的Tab
        tabPane.getTabs().removeIf(tab -> tab != deviceTab);

        menuListView.getSelectionModel().selectFirst();

        initTimer();
    }

    private void setupStatusChecker() {
        statusCheckTimeline = new Timeline(
                new KeyFrame(Duration.seconds(1), e -> updateStatusLight())
        );
        statusCheckTimeline.setCycleCount(Timeline.INDEFINITE);
        statusCheckTimeline.play();
    }

    private void updateStatusLight() {
        boolean isConnected = DeviceInfoUtil.isDeviceConnected();
        GraphicsContext gc = statusCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, statusCanvas.getWidth(), statusCanvas.getHeight());

        // 绘制圆形指示灯
        gc.setFill(isConnected ? Color.LIGHTGREEN : Color.WHITE);
        gc.fillOval(2, 2, 18, 18); // 留2px边距

        if(isConnected){
            deviceStatusLabel.setText("已连接");
            if (!timerTimeline.getStatus().equals(Animation.Status.RUNNING)) {
                startTime = System.currentTimeMillis();
                timerTimeline.play();
            }
        }else if(!isConnected){
            deviceStatusLabel.setText("未连接");
            timerTimeline.stop();
            timerLabel.setText("00:00:00");
        }

        updateDeviceNameAndType();
        updateSoftWare();
        // 可选：添加边框
//        gc.setStroke(Color.BLACK);
//        gc.strokeOval(1, 1, 20, 20);
    }

    private void updateDeviceNameAndType() {
        boolean isConnected = DeviceInfoUtil.isDeviceConnected();
        if(isConnected){
            DeviceInfoUtil.getDeviceModel(deviceModelLabel);
            DeviceInfoUtil.getDeviceBuildVersion(deviceBuildVersionLabel);
            DeviceInfoUtil.getDeviceBuildType(deviceBuildTypeLabel);
        }else {
            deviceModelLabel.setText("");
            deviceBuildVersionLabel.setText("");
            deviceBuildTypeLabel.setText("");
        }
    }

    private void updateSoftWare() {
        boolean isConnected = DeviceInfoUtil.isDeviceConnected();
        if(isConnected){
            DeviceInfoUtil.getAndroidVersion(androidVersionLabel);
            DeviceInfoUtil.getDeviceSoftwareVersion(softwareVersionLabel);
            DeviceInfoUtil.getActivity(gatActivityTextArea);
        }else {
            androidVersionLabel.setText("");
            softwareVersionLabel.setText("");
            gatActivityTextArea.setText("");
        }
    }

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


    // @FXML
    // private void brightnessSetDefault() {
    //     AdbUtil.setScreenBrightness(102, resultLabel);
    // }

    // @Override
    // public void onStatusUpdate(boolean isConnected, List<String> cpuFrequencies) {
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
    // }
    private void setupListViewAnimation() {
        // 自定义单元格工厂
        menuListView.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);

                    // 添加按压动画
                    setOnMousePressed(e -> {
                        // 颜色渐变动画
                        FillTransition ft = new FillTransition(Duration.millis(100), this.getShape());
                        ft.setFromValue(Color.valueOf("#F1F1F1")); // 初始颜色
                        ft.setToValue(Color.valueOf("#e0e0e0"));   // 按压颜色
                        ft.play();

                        // 缩放动画
                        ScaleTransition st = new ScaleTransition(Duration.millis(100), this);
                        st.setFromX(1);
                        st.setFromY(1);
                        st.setToX(0.98);
                        st.setToY(0.98);
                        st.play();
                    });

                    // 添加释放动画
                    setOnMouseReleased(e -> {
                        FillTransition ft = new FillTransition(Duration.millis(100), this.getShape());
                        ft.setFromValue(Color.valueOf("#e0e0e0")); // 按压颜色
                        ft.setToValue(Color.valueOf("#F1F1F1"));   // 恢复颜色
                        ft.play();

                        ScaleTransition st = new ScaleTransition(Duration.millis(100), this);
                        st.setFromX(0.98);
                        st.setFromY(0.98);
                        st.setToX(1);
                        st.setToY(1);
                        st.play();
                    });
                }
            }
        });
    }

    private void showTab(String tabType, String tabTitle) {
        Tab targetTab = null;

        switch (tabType) {
            case "信息":
                if (deviceTab == null) {
                    deviceTab = createTab("信息", "/xml/DeviceInfoPage.fxml");
                }
                targetTab = deviceTab;
                break;
            case "CPU":
                if (cpuTab == null) {
                    cpuTab = createTab("CPU", "/xml/CpuInfoTab.fxml");
                }
                targetTab = cpuTab;
                break;
            case "GPU":
                if (gpuTab == null) {
                    gpuTab = createTab("GPU", "/xml/GpuInfoTab.fxml");
                }
                targetTab = gpuTab;
                break;
            case "Disp":
                if (displayTab == null) {
                    displayTab = createTab("Disp", "/xml/DisplayInfoTab.fxml");
                }
                targetTab = displayTab;
                break;
            case "功能":
                if (specialFunctionTab == null) {
                    specialFunctionTab = createTab("功能", "/xml/SpecialFunctionTab.fxml");
                }
                targetTab = specialFunctionTab;
                break;
            case "解锁":
                if (deviceUnlockTab == null) {
                    deviceUnlockTab = createTab("解锁", "/xml/DeviceUnlockTab.fxml");
                }
                targetTab = deviceUnlockTab;
                break;
            case "Spd":
                if (spdTab == null) {
                    spdTab = createTab("Spd", "/xml/SpdCheckTab.fxml");
                }
                targetTab = spdTab;
                break;
            case "设置":
                if (settingsTab == null) {
                    settingsTab = createTab("设置", "/xml/SettingsTab.fxml");
                }
                targetTab = settingsTab;
                break;
        }

        if (targetTab != null) {
            // 清空所有 Tab，只保留目标 Tab
            tabPane.getTabs().setAll(targetTab);
            tabPane.getSelectionModel().select(targetTab);
        }
    }

    private Tab createTab(String title, String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent content = loader.load();
            Tab newTab = new Tab(title);
            newTab.setContent(content);
            newTab.setClosable(true); // 允许关闭

            // Tab关闭时重置对应变量
            newTab.setOnClosed(event -> {
                switch (title) {
                    case "CPU": cpuTab = null; break;
                    case "GPU": gpuTab = null; break;
                    case "Disp": displayTab = null; break;
                    case "功能": specialFunctionTab = null; break;
                    case "解锁": deviceUnlockTab = null; break;
                    case "Spd": spdTab = null; break;
                    case "设置": settingsTab = null; break;
                }
            });
            return newTab;
        } catch (IOException e) {
            e.printStackTrace();
            return new Tab(title + " (加载失败)");
        }
    }



    // 在 initialize 方法中初始化计时器
    private void initTimer() {
        timerLabel.setText("00:00:00");
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimer()));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    // 更新计时器显示
    private void updateTimer() {
        long elapsedTime = System.currentTimeMillis() - startTime;
        long seconds = (elapsedTime / 1000) % 60;
        long minutes = (elapsedTime / (1000 * 60)) % 60;
        long hours = (elapsedTime / (1000 * 60 * 60));

        timerLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    @FXML
    public void setSplitPaneStable() {
        // 获取FXML文件中的SplitPane
        if (splitPane != null) {
            splitPane.getDividers().get(0).positionProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.doubleValue() != 0.15) {
                    splitPane.setDividerPosition(0, 0.15);
                }
            });
        }
    }

    public void shutdown() {
        if (statusCheckTimeline != null) {
            statusCheckTimeline.stop();
        }
    }
}

