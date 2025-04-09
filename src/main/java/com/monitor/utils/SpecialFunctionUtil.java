package com.monitor.utils;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.monitor.utils.DeviceInfoUtil.isDeviceConnected;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:32
 * @Version 1.0.0
 */

public class SpecialFunctionUtil {

    /**
     * ADB 命令的路径。
     */
    private static final String ADB_PATH = "adb";

    /**
     * 异步线程池。
     */
    private static final ExecutorService executorService = new ThreadPoolExecutor(
            // 核心线程数
            10,
            // 最大线程数
            50,
            // 线程空闲时的最大存活时间（秒）
            60,
            // 时间单位
            TimeUnit.SECONDS,
            // 任务队列
            new LinkedBlockingQueue<>()
    );

    /**
     * 默认亮度 将屏幕亮度设置为默认。
     */
    public static void setScreenBrightness(int value, Label resultLabel) {
        String exception = "Exception", error = "error";
        executorService.submit(() -> {
            try {
                if (!isDeviceConnected()) {
                    Platform.runLater(() -> resultLabel.setText("设备未连接"));
                    return;
                }

                String command = ADB_PATH + " shell settings put system screen_brightness " + value;
                String output = executeCommand(command);

                Platform.runLater(() -> {
                    if (output.contains(exception) || output.contains(error)) {
                        resultLabel.setText("设置失败: 需要ADB权限");
                    } else {
                        resultLabel.setText("亮度已设置为默认");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> resultLabel.setText("错误: " + e.getMessage()));
            }
        });
    }

    /**
     * PowerHal 关闭Powerhal。
     */
    public static void setPowerHalState(int value, Label statusLabel) {
        String exception = "Exception", error = "error";

        executorService.submit(() -> {
            try {
                if (!isDeviceConnected()) {
                    Platform.runLater(() -> statusLabel.setText("设备未连接"));
                    return;
                }

                // 修复命令拼接增加空格
                String command = ADB_PATH + " shell setprop persist.vendor.powerhal.enable " + value;
                String output = executeCommand(command);

                Platform.runLater(() -> {
                    if (output.contains(exception) || output.contains(error)) {
                        statusLabel.setText("设置失败: 需要ADB权限");
                    } else {
                        String state = value == 1 ? "已开启" : "已关闭";
                        statusLabel.setText("PowerHal状态: " + state);
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> statusLabel.setText("错误: " + e.getMessage()));
            }
        });
    }

    /**
     * 执行线程。
     */
    private static String executeCommand(String command) throws IOException {

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ByteArrayOutputStream errorStream = new ByteArrayOutputStream()) {

            CommandLine cmdLine = CommandLine.parse(command);
            DefaultExecutor executor = new DefaultExecutor();
            executor.setStreamHandler(new PumpStreamHandler(outputStream, errorStream));

            // 设置超时防止阻塞（例如5秒）
            ExecuteWatchdog watchdog = new ExecuteWatchdog(5000);
            executor.setWatchdog(watchdog);

            int exitValue = executor.execute(cmdLine);
            if (exitValue != 0) {
                String errorMessage = "Exit code " + exitValue + ": " + errorStream.toString(StandardCharsets.UTF_8);
                throw new IOException(errorMessage);
            }
            return outputStream.toString(StandardCharsets.UTF_8);

        }
    }

    /**
     *在应用关闭时，确保正确关闭线程池
     */
    public static void shutdownExecutor() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }

}
