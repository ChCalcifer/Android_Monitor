package com.monitor.controller;

import com.monitor.utils.DeviceInfoUtil;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.SplitPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    private SplitPane splitPane;

    @FXML
    private ListView<String> menuListView;

    @FXML
    private TabPane tabPane;

    @FXML
    private Label deviceStatusLabel,
            timerLabel;

    @FXML
    private TextArea getActivityTextArea,
            getAndroidVersionTextArea,
            getDeviceModelTextArea,
            getDeviceBuildTypeTextArea,
            getDeviceBuildDateTextArea,
            getDeviceBuildVersionTextArea,
            getDeviceDpiTextArea,
            getDeviceDisplaySizeTextArea,
            getDeviceRamTextArea,
            getDeviceRomTextArea,
            getBatteryCapacityTextArea,
            getBatterySizeTextArea,
            getCurrentNowTextArea,
            getCurrentAvgTextArea,
            getVoltageNowTextArea,
            getVoltageAvgTextArea,
            getBatteryHealthTextArea,
            getBatteryStatusTextArea,
            getBatteryTempTextArea,
            getBatteryTempAmbientTextArea,
            getBatteryTechnologyTextArea,
            getBatteryTimeFullToNowTextArea,
            getBatteryUsbTypeTextArea;

    @FXML
    private Tab deviceTab;

    @FXML
    private ImageView statusIcon;

    private Timeline timerTimeline;

    private long startTime;

    private Tab cpuTab,
            gpuTab,
            memoryTab,
            specialFunctionTab,
            deviceUnlockTab,
            spdTab,
            settingsTab;

    private Timeline statusCheckTimeline;

    private static final double DEFAULT_DIVIDER_POSITION = 0.15;

    private boolean previousConnected = false;

    private final Image connectedImage = new Image(getClass().getResourceAsStream("/images/connected_2048x2048.png"));
    private final Image disconnectedImage = new Image(getClass().getResourceAsStream("/images/disconnected.png"));

    /**
     * 用于记录设备信息控制器中的日志信息。
     * <p>
     * 该日志记录器用于捕捉和记录异常、错误以及其他重要的运行时信息，
     * 有助于调试和监控应用程序的运行状态。
     * </p>
     */
    private static final Logger logger = Logger.getLogger(DeviceInfoController.class.getName());

    @Override
    public void initialize(URL url, ResourceBundle rb) {

        // 设置ListView的固定大小和样式
        menuListView.setPrefWidth(120);
        menuListView.setPrefHeight(430);

        tabPane.setTabMinHeight(0);
        tabPane.setTabMaxHeight(0);
        tabPane.setStyle("-fx-tab-min-height: 0; -fx-tab-max-height: 0;");

        setupStatusChecker();
        setupListViewAnimation();

        // 设置ListView的单元格工厂，使文本居中
        menuListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setAlignment(Pos.CENTER); // 文本居中
                    setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
                }
            }
        });

        menuListView.setFixedCellSize(53);

        // 填充菜单项
        ObservableList<String> menuItems = FXCollections.observableArrayList(
                "信息",
                "CPU",
                "GPU",
                "Mem",
                "功能",
                "解锁",
                "Spd",
                "设置"
        );
        menuListView.setItems(menuItems);

        menuListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        showTab(newValue);
                    }
                });

        deviceTab = tabPane.getTabs().get(0);

        // 清空其他预定义的Tab
        tabPane.getTabs().removeIf(tab -> tab != deviceTab);

        menuListView.getSelectionModel().selectFirst();

        initTimer();

        if (DeviceInfoUtil.isDeviceConnected()) {
            DeviceInfoUtil.preloadBatteryPath();
        }
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

        // 绘制指示灯
        gc.setFill(isConnected ? Color.LIGHTGREEN : Color.WHITE);
        gc.fillOval(2, 2, 18, 18);

        Platform.runLater(() -> {
            statusIcon.setImage(isConnected ? connectedImage : disconnectedImage);
            statusIcon.setOpacity(isConnected ? 1.0 : 0.6);
            deviceStatusLabel.setText(isConnected ? "已连接" : "未连接");
        });

        // 仅在连接状态变化时执行更新
        if (isConnected != previousConnected) {
            updateDeviceNameAndType();
            updateSoftWare();
            previousConnected = isConnected;
        }

        // 计时器逻辑（保持不变）
        if (isConnected) {
            if (!timerTimeline.getStatus().equals(Animation.Status.RUNNING)) {
                startTime = System.currentTimeMillis();
                timerTimeline.play();
            }
        } else {
            timerTimeline.stop();
            timerLabel.setText("00:00:00");
        }

        updateBatteryInfo();
    }

    private void updateDeviceNameAndType() {
        boolean isConnected = DeviceInfoUtil.isDeviceConnected();
        if(isConnected){
            DeviceInfoUtil.getDeviceModel(getDeviceModelTextArea);
            DeviceInfoUtil.getDeviceBuildVersion(getDeviceBuildDateTextArea);
            DeviceInfoUtil.getDeviceBuildType(getDeviceBuildTypeTextArea);
            DeviceInfoUtil.getDisplaySize(getDeviceDisplaySizeTextArea);
        }else {
            getDeviceModelTextArea.setText("");
            getDeviceBuildDateTextArea.setText("");
            getDeviceBuildTypeTextArea.setText("");
            getDeviceDisplaySizeTextArea.setText("");
        }
    }

    private void updateSoftWare() {
        boolean isConnected = DeviceInfoUtil.isDeviceConnected();
        if(isConnected){
            DeviceInfoUtil.getAndroidVersion(getAndroidVersionTextArea);
            DeviceInfoUtil.getDeviceSoftwareVersion(getDeviceBuildVersionTextArea);
            DeviceInfoUtil.getActivity(getActivityTextArea);
            DeviceInfoUtil.getDpi(getDeviceDpiTextArea);
            DeviceInfoUtil.getDeviceRam(getDeviceRamTextArea);
            DeviceInfoUtil.getDeviceRom(getDeviceRomTextArea);
            DeviceInfoUtil.getBatterySize(getBatterySizeTextArea);
            DeviceInfoUtil.getBatteryHealth(getBatteryHealthTextArea);
            DeviceInfoUtil.getBatteryUsbType(getBatteryUsbTypeTextArea);
        }else {
            getAndroidVersionTextArea.setText("");
            getDeviceBuildVersionTextArea.setText("");
            getActivityTextArea.setText("");
            getDeviceDpiTextArea.setText("");
            getDeviceRamTextArea.setText("");
            getDeviceRomTextArea.setText("");
            getBatterySizeTextArea.setText("");
            getBatteryHealthTextArea.setText("");
            getBatteryUsbTypeTextArea.setText("");
            }
    }

    private void updateBatteryInfo() {
        boolean isConnected = DeviceInfoUtil.isDeviceConnected();
        if(isConnected){
            DeviceInfoUtil.getBatteryCapacity(getBatteryCapacityTextArea);
            DeviceInfoUtil.getBatterySize(getBatterySizeTextArea);
            DeviceInfoUtil.getBatteryCurrentNow(getCurrentNowTextArea);
            DeviceInfoUtil.getBatteryCurrentAvg(getCurrentAvgTextArea);
            DeviceInfoUtil.getBatteryVoltageNow(getVoltageNowTextArea);
            DeviceInfoUtil.getBatteryVoltageAvg(getVoltageAvgTextArea);
            DeviceInfoUtil.getBatteryStatus(getBatteryStatusTextArea);
            DeviceInfoUtil.getBatteryTemp(getBatteryTempTextArea);
            DeviceInfoUtil.getBatteryTempAmbient(getBatteryTempAmbientTextArea);
            DeviceInfoUtil.getBatteryTechnology(getBatteryTechnologyTextArea);
            DeviceInfoUtil.getBatteryTimeFullToNow(getBatteryTimeFullToNowTextArea);
        }else {
            getBatteryCapacityTextArea.setText("");
            getBatterySizeTextArea.setText("");
            getCurrentNowTextArea.setText("");
            getCurrentAvgTextArea.setText("");
            getVoltageNowTextArea.setText("");
            getVoltageAvgTextArea.setText("");
            getBatteryStatusTextArea.setText("");
            getBatteryTempTextArea.setText("");
            getBatteryTempAmbientTextArea.setText("");
            getBatteryTechnologyTextArea.setText("");
            getBatteryTimeFullToNowTextArea.setText("");
        }
    }

    private void setupListViewAnimation() {
        // 自定义单元格工厂
        menuListView.setCellFactory(lv -> new ListCell<>() {
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

    private void showTab(String tabType) {
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
            case "Mem":
                if (memoryTab == null) {
                    memoryTab = createTab("Mem", "/xml/MemoryInfoTab.fxml");
                }
                targetTab = memoryTab;
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
            default:
                // 这里可以添加默认的处理逻辑
                System.out.println("Unexpected tab title:");
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
            newTab.setClosable(true);

            // Tab关闭时重置对应变量
            newTab.setOnClosed(event -> {
                switch (title) {
                    case "CPU": cpuTab = null; break;
                    case "GPU": gpuTab = null; break;
                    case "Mem": memoryTab = null; break;
                    case "功能": specialFunctionTab = null; break;
                    case "解锁": deviceUnlockTab = null; break;
                    case "Spd": spdTab = null; break;
                    case "设置": settingsTab = null; break;
                    default:
                        // 这里可以添加默认的处理逻辑
                        System.out.println("Unexpected tab title: " + title);
                        break;
                }
            });
            return newTab;
        } catch (IOException e) {
            // 通过 logger 记录异常
            logger.log(Level.SEVERE, "Failed to load FXML for tab: " + title, e);
            return new Tab(title + " (加载失败)");
        }
    }

    private void initTimer() {
        timerLabel.setText("00:00:00");
        timerTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateTimer()));
        timerTimeline.setCycleCount(Timeline.INDEFINITE);
    }

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
                if (newVal.doubleValue() != DEFAULT_DIVIDER_POSITION) {
                    splitPane.setDividerPosition(0, 0.15);
                }
            });
        }
    }

    public void shutdown() {
        if (statusCheckTimeline != null) {
            statusCheckTimeline.stop();
        }

        DeviceInfoUtil.onDeviceDisconnected();

        DeviceInfoUtil.shutdownExecutor();
    }
}

