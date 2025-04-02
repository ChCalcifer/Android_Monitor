package com.devicemonitor.utils;

import javafx.application.Platform;
import javafx.scene.control.Label;
import org.apache.commons.exec.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: CYC
 * Time: 2025/3/27 16:15:08
 * Description:
 * Branch:
 * Version: 1.0
 * @author uu
 */

public class AdbUtil {

    /**
     * ADB 命令的路径。
     */
    private static final String ADB_PATH = "adb";

    /**
     * 空格。
     */
    private static final Pattern CPU_FREQUENCY_PATTERN = Pattern.compile("\\d+");
    /**
     * 定位batteryTemp。
     */
    private static final Pattern BATTERY_TEMP_PATTERN = Pattern.compile("temperature:\\s*(\\d+)");
    /**
     * 定位currentFps。
     */
    private static final Pattern FPS_PATTERN = Pattern.compile("^\\s*\\d+\\s+\\S+\\s+\\S+\\s+(-?\\d+)\\b");
    /**
     * 定时线程池。
     */
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(10);

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

    public static boolean isDeviceConnected() {
        try {
            String output = executeCommand(ADB_PATH + " devices");
            // 检查输出中是否包含设备 ID，判断设备是否连接
            return output.contains("\tdevice") && !output.contains("List of devices attached");
        } catch (Exception e) {
            // 捕获异常时返回 false，表示设备未连接
            return false;
        }
    }


    /**
     * 获取设备型号并显示。
     */
    public static void getDeviceModel(Label phoneModelLabel) {
        // 使用线程池来执行任务
        executorService.submit(() -> {
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
        });
    }

    public static void getDeviceSoftwareVersion(Label softwareVersionLabel) {
        executorService.submit(() -> {
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
        });
    }

    public static void getAndroidVersion(Label androidVersionLabel) {
        executorService.submit(() -> {
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
        });
    }

    public static void getFrameRate(Label fpsLabel) {
        executorService.submit(() -> {
            try {
                // 添加超时和错误流处理
                String output = executeCommand(ADB_PATH + " shell \"cat /sys/kernel/fpsgo/fstb/fpsgo_status 2>/dev/null\"");
                if (output == null) {
                    Platform.runLater(() -> fpsLabel.setText("no data"));
                    return;
                }

                String frameRate = parseFrameRate(output);
                Platform.runLater(() -> {
                    if (isValidFPS(frameRate)) {
                        fpsLabel.setText(frameRate + " FPS");
                    } else {
                        fpsLabel.setText("0");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> fpsLabel.setText("需Root"));
                e.printStackTrace();
            }
        });
    }

    private static String parseFrameRate(String output) {
        return output.lines()
                .skip(1) // 跳过标题行
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(FPS_PATTERN::matcher)
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .filter(fps -> !fps.equals("0") && !fps.equals("-1"))
                .findFirst()
                .orElse(null);
    }

    private static boolean isValidFPS(String fps) {
        return fps != null && !fps.isEmpty() && !fps.equals("0") && !fps.equals("-1");
    }

    public static void getActivity(Label activityLabel) {
        executorService.submit(() -> {
            try {
                // 执行命令获取设备型号
                String output = executeCommand(ADB_PATH + " shell \"dumpsys activity top | grep ACTIVITY | tail -n 1\"");
                if (output != null && !output.isEmpty()) {
                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> activityLabel.setText("CurrentActivity: " + output.trim()));
                } else {
                    Platform.runLater(() -> activityLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> activityLabel.setText("None"));
            }
        });
    }

    public static List<String> getCpuFrequencies() {
        try {
            // 增加超时和错误处理
            String output = executeCommand(ADB_PATH + " shell \"cat /sys/devices/system/cpu/cpu*/cpufreq/scaling_cur_freq 2> /dev/null\"");
            List<String> result = parseCpuFrequencies(output);
            return result.isEmpty() ?
                    Collections.singletonList("No frequency data") : result;
        } catch (Exception e) {
            return Collections.singletonList("ADB Error: " + e.getMessage());
        }
    }

    /**
     * 获取并更新电池温度。
     */
    public static void getBatteryTemperature(Label batteryTemperatureLabel) {
        // 定时任务，初始延迟 0，周期 20 秒
        SCHEDULER.scheduleAtFixedRate(() -> {
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
            } catch (Exception e) {
                Platform.runLater(() -> batteryTemperatureLabel.setText("error"));
            }
            // 从0秒开始，每20秒执行一次
        }, 0, 20, TimeUnit.SECONDS);
    }

    /**
     * 从adb输出中提取温度。
     */
    private static String parseBatteryTemperature(String output) {
        // 正则表达式匹配温度
        Matcher matcher = BATTERY_TEMP_PATTERN.matcher(output);

        if (matcher.find()) {
            // 获取温度值并转换为摄氏度
            int temperatureInDeciCelsius = Integer.parseInt(matcher.group(1));
            // 转换为摄氏度并保留一位小数
            return String.format("%.1f", temperatureInDeciCelsius / 10.0);
        }
        return null;
    }

    private static List<String> parseCpuFrequencies(String output) {
        List<String> frequencies = new ArrayList<>();
        Matcher matcher = CPU_FREQUENCY_PATTERN.matcher(output);
        int coreIndex = 0;

        while (matcher.find()) {
            int kHz = Integer.parseInt(matcher.group());
            double mhz = kHz / 1000.0;
            frequencies.add(String.format("CPU%d %.1f ", coreIndex++, mhz));
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
                throw new IOException("Exit code " + exitValue + ": " + errorStream.toString(StandardCharsets.UTF_8));
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
