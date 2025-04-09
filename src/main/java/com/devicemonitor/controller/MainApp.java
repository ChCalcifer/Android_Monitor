package com.devicemonitor.controller;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Author: CYC
 * Time: 2025/3/27 16:13:11
 * Description:
 * Branch:
 * Version: 1.0
 * @author uu
 */

public class MainApp extends Application {
    private DeviceInfoController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/DeviceInfoPage.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        DeviceInfoController deviceInfoController = loader.getController();
        deviceInfoController.setSplitPaneStable();

        Scene scene = new Scene(root, 950, 600);

        primaryStage.setMinWidth(950);
        primaryStage.setMinHeight(620);
        primaryStage.setTitle("MTK Monitor V1.0");
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        if (controller != null) {
            controller.shutdown();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
