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
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author CYC
 */
public class GetTemperatureUtil {
    /**
     * ADB 命令的路径。
     */
    private static final String ADB_PATH = "adb";

    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(5);

    private static final Pattern BATTERY_TEMP_PATTERN = Pattern.compile("temperature:\\s*(\\d+)");

    /**
     * 小核温度 获取并更新小核温度。
     */
    public static void getSmCoreTemp(Label cpuSmallCoreTempLabel) {
        scheduleTemperatureUpdate("/sys/class/thermal/thermal_zone1/temp", cpuSmallCoreTempLabel, "SmallCore");
    }

    /**
     * 大核温度 获取并更新大核温度。
     */
    public static void getBigCoreTemp(Label cpuBigCoreTempLabel) {
        scheduleTemperatureUpdate("/sys/class/thermal/thermal_zone5/temp", cpuBigCoreTempLabel, "BigCore");
    }

    /**
     * Soc温度 获取并更新Soc温度。
     */
    public static void getSocTemp(Label socTempLabel) {
        scheduleTemperatureUpdate("/sys/class/thermal/thermal_zone0/temp", socTempLabel, "Soc");
    }

    /**
     * modem温度 获取并更新modem温度。
     */
    public static void getModemTemp(Label modemTempLabel) {
        scheduleTemperatureUpdate("/sys/class/thermal/thermal_zone0/temp13", modemTempLabel, "Modem");
    }

    /**
     * PMIC温度 获取并更新PMIC温度。
     */
    public static void getPmicTemp(Label pmicTempLabel) {
        scheduleTemperatureUpdate("/sys/class/thermal/thermal_zone0/temp16", pmicTempLabel, "PMIC");

    }

    /**
     * Camera温度 获取并更新Camera温度。
     */
    public static void getCameraTemp(Label cameraTempLabel) {
        scheduleTemperatureUpdate("/sys/class/thermal/thermal_zone0/temp16", cameraTempLabel, "Camera");

    }

    /**
     * PMIC温度 获取并更新GPU温度。
     */
    public static void getGpuTemp(Label gpuTempLabel) {
        scheduleTemperatureUpdate("/sys/class/thermal/thermal_zone0/temp10", gpuTempLabel, "Gpu");

    }

    /**
     * 电池温度 获取并更新电池温度。
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
                Platform.runLater(() -> batteryTemperatureLabel.setText("请插入设备"));
            }
            // 从0秒开始，每20秒执行一次
        }, 0, 20, TimeUnit.SECONDS);
    }

    /**
     * 电池温度 从adb输出中提取温度。
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

    /**
     * 定时更新温度方法。
     */
    public static void scheduleTemperatureUpdate(String thermalZonePath, Label label, String labelPrefix) {
        // 初始延迟0秒，之后每10秒执行一次
        SCHEDULER.scheduleAtFixedRate(() -> {
            try {
                // 执行命令获取温度
                String output = executeCommand(ADB_PATH + " shell \"cat " + thermalZonePath + "\"");
                if (output != null && !output.isEmpty()) {
                    // 将五位数转换为温度（千分之一度 -> 度）
                    int temp = Integer.parseInt(output.trim());
                    double temperature = temp / 1000.0;

                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> label.setText(labelPrefix + ": " + String.format("%.2f", temperature) + "°C"));
                } else {
                    Platform.runLater(() -> label.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> label.setText("None"));
            }
        }, 0, 10, TimeUnit.SECONDS);
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
     *在应用关闭时，确保正确关闭线程池
     */
    public static void shutdownExecutor() {
        if (!SCHEDULER.isShutdown()) {
            SCHEDULER.shutdown();
        }
    }
}
