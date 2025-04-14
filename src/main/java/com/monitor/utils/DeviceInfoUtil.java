package com.monitor.utils;

import com.monitor.thread.CustomThreadFactory;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:30
 * @Version 1.0.0
 */

public class DeviceInfoUtil {

    /**
     * ADB 命令的路径。
     */
    private static final String ADB_PATH = "adb";

    /**
     * 定位屏幕尺寸。
     */
    private static final Pattern DISPLAY_SIZE_PATTERN = Pattern.compile("(\\d+)x(\\d+)");

    private static final Pattern DENSITY_PATTERN = Pattern.compile("Physical density: (\\d+)");

    /**
     * 异步线程池。
     */
    private static final ExecutorService executorService = new ThreadPoolExecutor(
            5, 15, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new CustomThreadFactory("DeviceInfoUtil-Pool")
    );

    /**
     * 设备连接 获取并更新设备连接状态。
     */
    public static boolean isDeviceConnected() {
        try {
            String output = executeCommand(ADB_PATH + " devices");
            // 检查输出中是否包含设备 ID，判断设备是否连接
            return output.contains("\tdevice");
        } catch (Exception e) {
            // 捕获异常时返回 false，表示设备未连接
            return false;
        }
    }

    /**
     * 设备型号 获取设备型号并显示。
     */
    public static void getDeviceModel(TextArea gatDeviceModelTextArea) {
        // 使用线程池来执行任务
        executeAdbCommandAndUpdateLabel(ADB_PATH + " shell getprop ro.product.model",
                gatDeviceModelTextArea, "");
    }

    /**
     * 设备型号 获取设备生产版本型号。
     */
    public static void getDeviceBuildVersion(TextArea gatDeviceBuildDateTextArea) {
        // 使用线程池来执行任务
        executeAdbCommandAndUpdateLabel(ADB_PATH + " shell getprop ro.build.version.incremental",
                gatDeviceBuildDateTextArea, "");
    }

    /**
     * 设备型号 获取设备生产版本型号。
     */
    public static void getDeviceBuildType(TextArea gatDeviceBuildTypeTextArea) {
        // 使用线程池来执行任务
        executorService.submit(() -> {
            String result = "";
            try {
                // 执行命令获取设备型号
                String output = executeCommand(ADB_PATH + " shell getprop ro.build.type");
                if (output != null && !output.isEmpty()) {
                    // 根据返回值进行处理
                    switch (output.trim()) {
                        case "userdebug":
                            result = "UD";
                            break;
                        case "userroot":
                            result = "ROOT";
                            break;
                        case "user":
                            result = "USER";
                            break;
                        default:
                            result = "";
                            break;
                    }
                }
            } catch (Exception e) {
                Platform.runLater(() -> gatDeviceBuildTypeTextArea.setText(""));
            }
            final String finalResult = result; // 创建最终的变量
            Platform.runLater(() -> gatDeviceBuildTypeTextArea.setText(finalResult));
        });
    }

    /**
     * 软件版本 获取软件版本。
     */
    public static void getDeviceSoftwareVersion(TextArea gatDeviceBuildVersionTextArea) {
        executeAdbCommandAndUpdateLabel(ADB_PATH + " shell getprop ro.build.display.id",
                gatDeviceBuildVersionTextArea, "Unknown");
    }

    /**
     * 安卓版本 获取安卓版本。
     */
    public static void getAndroidVersion(TextArea gatAndroidVersionTextArea) {
        executorService.submit(() -> {
            try {
                // 执行命令获取设备型号
                String output = executeCommand(ADB_PATH + " shell getprop ro.build.version.release");
                if (output != null && !output.isEmpty()) {
                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> gatAndroidVersionTextArea.setText(output.trim()));
                } else {
                    Platform.runLater(() -> gatAndroidVersionTextArea.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> gatAndroidVersionTextArea.setText("None"));
            }
        });
    }

    /**
     * dpi 获取dpi。
     */
    public static void getDpi(TextArea gatDeviceDpiTextArea) {
        executorService.submit(() -> {
            try {
                // 执行命令获取设备DPI
                String output = executeCommand(ADB_PATH + " shell wm density");

                String deviceDensity = parseDensity(output);

                Platform.runLater(() -> {
                    gatDeviceDpiTextArea.setText(Objects.requireNonNullElse(deviceDensity, "Unknown"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> gatDeviceDpiTextArea.setText("error"));
            }
        });
    }

    /**
     * 从adb输出中提取DPI值
     */
    private static String parseDensity(String output) {
        Matcher matcher = DENSITY_PATTERN.matcher(output);

        if (matcher.find()) {
            // 返回匹配到的DPI值
            return matcher.group(1);
        } else {
            // 如果没有匹配到，返回null
            return null;
        }
    }

    /**
     * Activity 获取并更新Activity。
     */
    public static void getActivity(TextArea gatActivityTextArea) {
        executorService.submit(() -> {
            try {
                String output = executeCommand(ADB_PATH + " shell \"dumpsys activity top | grep ACTIVITY | tail -n 1\"");
                if (output != null && !output.isEmpty()) {
                    // 使用正则提取 "ACTIVITY 包名/类名" 部分
                    Pattern pattern = Pattern.compile("ACTIVITY\\s+([^\\s]+)");
                    Matcher matcher = pattern.matcher(output);
                    if (matcher.find()) {
                        String newActivity = matcher.group(1); // 提取匹配的包名/类名
                        Platform.runLater(() -> {
                            String currentText = gatActivityTextArea.getText();
                            // 仅在新值不同时更新
                            if (!newActivity.equals(currentText)) {
                                gatActivityTextArea.setText(newActivity);
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            String currentText = gatActivityTextArea.getText();
                            if (!"Unknown".equals(currentText)) {
                                gatActivityTextArea.setText("Unknown");
                            }
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        String currentText = gatActivityTextArea.getText();
                        if (!"Unknown".equals(currentText)) {
                            gatActivityTextArea.setText("Unknown");
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    String currentText = gatActivityTextArea.getText();
                    if (!"None".equals(currentText)) {
                        gatActivityTextArea.setText("None");
                    }
                });
            }
        });
    }

    /**
     * 屏幕尺寸 获取屏幕尺寸。
     */
    public static void getDisplaySize(TextArea gatDeviceDisplaySizeTextArea) {
        executorService.submit(() -> {
            try {
                // 执行命令获取设备型号
                String output = executeCommand(ADB_PATH + " shell wm size");

                String deviceDisplaySize = parseDisplaySize(output);

                Platform.runLater(() -> {
                    // 使用 Objects.requireNonNullElse 来替代 if 语句
                    gatDeviceDisplaySizeTextArea.setText(Objects.requireNonNullElse(deviceDisplaySize, "Unknown"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> gatDeviceDisplaySizeTextArea.setText("error"));
            }
        });
    }

    /**
     * 屏幕尺寸 从adb输出中提取屏幕尺寸。
     */
    private static String parseDisplaySize(String output) {
        // 正则表达式匹配温度
        Matcher matcher = DISPLAY_SIZE_PATTERN.matcher(output);

        // 检查是否匹配成功
        if (matcher.find()) {
            // 返回匹配到的屏幕尺寸
            return matcher.group(0);
        } else {
            // 如果没有匹配到，返回null或"Unknown"
            return null;
        }

    }

    /**
     * 通用方法：执行ADB命令并更新Label
     */
    private static void executeAdbCommandAndUpdateLabel(String command, TextArea textArea, String defaultValue) {
        executorService.submit(() -> {
            try {
                String output = executeCommand(command);
                String result = (output != null && !output.isEmpty()) ? output.trim() : defaultValue;
                Platform.runLater(() -> textArea.setText(result));
            } catch (Exception e) {
                Platform.runLater(() -> textArea.setText(defaultValue));
            }
        });
    }

    /**
     * 执行命令。
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
     * 在应用关闭时，确保正确关闭线程池
     */
    public static void shutdownExecutor() {
        if (!executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}