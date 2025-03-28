package com.devicemonitor;

import com.devicemonitor.controller.MainController;
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
 */

public class MainApp extends Application {
    private MainController controller;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/devicemonitor/main-view.fxml"));
        Parent root = loader.load();
        controller = loader.getController();

        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("手机设备监控器");
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
