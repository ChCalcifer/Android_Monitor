package com.devicemonitor.utils;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.apache.commons.exec.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: CYC
 * Time: 2025/3/27 16:15:08
 * Description:
 * Branch:
 * Version: 1.0
 */

public class ADBUtil {
    private static final String ADB_PATH = "adb"; // 需要配置环境变量或绝对路径

    public static boolean isDeviceConnected() {
        try {
            String output = executeCommand(ADB_PATH + " devices");
            return output.contains("\tdevice");
        } catch (Exception e) {
            return false;
        }
    }

    // 获取设备型号并显示
    public static void getDeviceModel(Label phoneModelLabel) {
        new Thread(() -> {
            try {
                // 执行命令获取设备型号
                String output = executeCommand(ADB_PATH + " shell getprop ro.product.model");
                if (output != null && !output.isEmpty()) {
                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> phoneModelLabel.setText(output.trim()));
                } else {
                    Platform.runLater(() -> phoneModelLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> phoneModelLabel.setText("None"));
            }
        }).start(); // 启动新线程执行 ADB 命令
    }

    public static void getDeviceSoftwareVersion(Label softwareVersionLabel) {
        new Thread(() -> {
            try {
                // 执行命令获取设备型号
                String output = executeCommand(ADB_PATH + " shell getprop ro.build.display.id");
                if (output != null && !output.isEmpty()) {
                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> softwareVersionLabel.setText(output.trim()));
                } else {
                    Platform.runLater(() -> softwareVersionLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> softwareVersionLabel.setText("None"));
            }
        }).start(); // 启动新线程执行 ADB 命令
    }

    public static void getAndroidVersion(Label androidVersionLabel) {
        new Thread(() -> {
            try {
                // 执行命令获取设备型号
                String output = executeCommand(ADB_PATH + " shell getprop ro.build.version.release");
                if (output != null && !output.isEmpty()) {
                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> androidVersionLabel.setText("Android " + output.trim()));
                } else {
                    Platform.runLater(() -> androidVersionLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> androidVersionLabel.setText("None"));
            }
        }).start(); // 启动新线程执行 ADB 命令
    }

    public static List<String> getCPUFrequencies() {
        try {
            // 增加超时和错误处理
            String output = executeCommand(ADB_PATH + " shell \"cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq 2> /dev/null\"");
            List<String> result = parseCPUFrequencies(output);
            return result.isEmpty() ?
                    Collections.singletonList("No frequency data") : result;
        } catch (Exception e) {
            return Collections.singletonList("ADB Error: " + e.getMessage());
        }
    }

    // 获取并更新电池温度
    public static void getBatteryTemperature(Label batteryTemperatureLabel) {
        new Thread(() -> {
            while (true) {
                try {
                    // 执行命令获取电池信息
                    String output = executeCommand(ADB_PATH + " shell dumpsys battery");

                    // 从输出中提取温度信息
                    String temperature = parseBatteryTemperature(output);

                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> {
                        if (temperature != null) {
                            batteryTemperatureLabel.setText(temperature + "°C");
                        } else {
                            batteryTemperatureLabel.setText("Unknown");
                        }
                    });

                    // 每20秒更新一次
                    Thread.sleep(20000);
                } catch (Exception e) {
                    Platform.runLater(() -> batteryTemperatureLabel.setText("Error"));
                }
            }
        }).start(); // 启动新线程执行 ADB 命令
    }

    // 从adb输出中提取温度
    private static String parseBatteryTemperature(String output) {
        // 正则表达式匹配温度
        Pattern pattern = Pattern.compile("temperature:\\s*(\\d+)");
        Matcher matcher = pattern.matcher(output);

        if (matcher.find()) {
            // 获取温度值并转换为摄氏度
            int temperatureInDeciCelsius = Integer.parseInt(matcher.group(1));
            return String.format("%.1f", temperatureInDeciCelsius / 10.0); // 转换为摄氏度并保留一位小数
        }
        return null;
    }

    private static List<String> parseCPUFrequencies(String output) {
        List<String> frequencies = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(output);
        int coreIndex = 0;

        while (matcher.find()) {
            int kHz = Integer.parseInt(matcher.group());
            double MHz = kHz / 1000.0;
            frequencies.add(String.format("CPU%d %.1f ", coreIndex++, MHz));
        }
        return frequencies.isEmpty() ? Collections.singletonList("N/A") : frequencies;
    }

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
                throw new IOException("Exit code " + exitValue + ": " + errorStream.toString());
            }
            return outputStream.toString();
        }
    }
}
