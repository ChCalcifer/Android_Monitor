package com.monitor.utils;

import com.monitor.thread.CustomThreadFactory;
import javafx.application.Platform;
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
     * 电池信息基础路径
     */
    private static String batteryBasePath = null;

    private static final Object BATTERYPATHLOCK = new Object();

    /**
     * 候选电池路径列表（按优先级排序）
     */
    private static final String[] BATTERY_PATH_CANDIDATES = {
            "/sys/class/power_supply/battery",
            "/sys/devices/platform/charger-manager/power_supply/battery",
            "/sys/devices/qpnp-charger-*/power_supply/battery"
    };

    /**
     * 定位屏幕尺寸。
     */
    private static final Pattern DISPLAY_SIZE_PATTERN = Pattern.compile("(\\d+)x(\\d+)");

    private static final Pattern DENSITY_PATTERN = Pattern.compile("Physical density: (\\d+)");

    private static final Pattern RAM_PATTERN = Pattern.compile(
            "MemTotal:\\s+(\\d+)\\s+kB.*MemAvailable:\\s+(\\d+)\\s+kB",
            Pattern.DOTALL
    );

    private static final Pattern ROM_PATTERN = Pattern.compile("^(\\S+)\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)\\s+\\d+%\\s+(/data|/storage/emulated/0).*");


    /**
     * 异步线程池。
     */
    private static final ExecutorService executorService = new ThreadPoolExecutor(
            5, 30, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new CustomThreadFactory("DeviceInfoUtil-Pool")
    );

    /**
     * 设备连接 获取并更新设备连接状态。
     */
    public static boolean isDeviceConnected() {
        try {
            String output = executeCommand(ADB_PATH + " devices");
            boolean connected = output.contains("\tdevice");

            // 新增逻辑：连接状态变化时触发预加载
            if (connected && batteryBasePath == null) {
                preloadBatteryPath(); // 设备首次连接时触发
            } else if (!connected) {
                onDeviceDisconnected(); // 设备断开时重置路径
            }

            return connected;
        } catch (Exception e) {
            onDeviceDisconnected(); // 异常时也重置路径
            return false;
        }
    }

    /**
     * 设备型号 获取设备型号并显示。
     */
    public static void getDeviceModel(TextArea getDeviceModelTextArea) {
        // 使用线程池来执行任务
        executeAdbCommandAndUpdate(ADB_PATH + " shell getprop ro.product.model",
                getDeviceModelTextArea, "");
    }

    /**
     * 设备型号 获取设备生产版本型号。
     */
    public static void getDeviceBuildVersion(TextArea getDeviceBuildDateTextArea) {
        // 使用线程池来执行任务
        executeAdbCommandAndUpdate(ADB_PATH + " shell getprop ro.build.version.incremental",
                getDeviceBuildDateTextArea, "");
    }

    /**
     * 设备型号 获取设备生产版本型号。
     */
    public static void getDeviceBuildType(TextArea getDeviceBuildTypeTextArea) {
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
                Platform.runLater(() -> getDeviceBuildTypeTextArea.setText(""));
            }
            final String finalResult = result; // 创建最终的变量
            Platform.runLater(() -> getDeviceBuildTypeTextArea.setText(finalResult));
        });
    }

    /**
     * 软件版本 获取软件版本。
     */
    public static void getDeviceSoftwareVersion(TextArea getDeviceBuildVersionTextArea) {
        executeAdbCommandAndUpdate(ADB_PATH + " shell getprop ro.build.display.id",
                getDeviceBuildVersionTextArea, "Unknown");
    }

    /**
     * 安卓版本 获取安卓版本。
     */
    public static void getAndroidVersion(TextArea getAndroidVersionTextArea) {
        executorService.submit(() -> {
            try {
                // 执行命令获取设备型号
                String output = executeCommand(ADB_PATH + " shell getprop ro.build.version.release");
                if (output != null && !output.isEmpty()) {
                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> getAndroidVersionTextArea.setText(output.trim()));
                } else {
                    Platform.runLater(() -> getAndroidVersionTextArea.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> getAndroidVersionTextArea.setText("None"));
            }
        });
    }

    /**
     * dpi 获取dpi。
     */
    public static void getDpi(TextArea getDeviceDpiTextArea) {
        executorService.submit(() -> {
            try {
                // 执行命令获取设备DPI
                String output = executeCommand(ADB_PATH + " shell wm density");

                String deviceDensity = parseDensity(output);

                Platform.runLater(() -> {
                    getDeviceDpiTextArea.setText(Objects.requireNonNullElse(deviceDensity, "Unknown"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> getDeviceDpiTextArea.setText("error"));
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
    public static void getActivity(TextArea getActivityTextArea) {
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
                            String currentText = getActivityTextArea.getText();
                            // 仅在新值不同时更新
                            if (!newActivity.equals(currentText)) {
                                getActivityTextArea.setText(newActivity);
                            }
                        });
                    } else {
                        Platform.runLater(() -> {
                            String currentText = getActivityTextArea.getText();
                            if (!"Unknown".equals(currentText)) {
                                getActivityTextArea.setText("Unknown");
                            }
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        String currentText = getActivityTextArea.getText();
                        if (!"Unknown".equals(currentText)) {
                            getActivityTextArea.setText("Unknown");
                        }
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    String currentText = getActivityTextArea.getText();
                    if (!"None".equals(currentText)) {
                        getActivityTextArea.setText("None");
                    }
                });
            }
        });
    }

    /**
     * 屏幕尺寸 获取屏幕尺寸。
     */
    public static void getDisplaySize(TextArea getDeviceDisplaySizeTextArea) {
        executorService.submit(() -> {
            try {
                // 执行命令获取设备型号
                String output = executeCommand(ADB_PATH + " shell wm size");

                String deviceDisplaySize = parseDisplaySize(output);

                Platform.runLater(() -> {
                    // 使用 Objects.requireNonNullElse 来替代 if 语句
                    getDeviceDisplaySizeTextArea.setText(Objects.requireNonNullElse(deviceDisplaySize, "Unknown"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> getDeviceDisplaySizeTextArea.setText("error"));
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
     * 获取设备 ROM 信息
     */
    public static void getDeviceRom(TextArea getDeviceRomTextArea) {
        executorService.submit(() -> {
            try {
                String output = executeCommand(ADB_PATH + " shell df /data");
                String romInfo = parseRomInfo(output);
                Platform.runLater(() -> {
                    getDeviceRomTextArea.setText(Objects.requireNonNullElse(romInfo, "Unknown"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> getDeviceRomTextArea.setText("error"));
            }
        });
    }

    /**
     * 解析 ROM 信息（总空间、已用、可用）
     */
    public static String parseRomInfo(String output) {
        String[] lines = output.split("\n");
        for (String line : lines) {
            Matcher matcher = ROM_PATTERN.matcher(line);
            if (matcher.find()) {
                long totalBlocks = Long.parseLong(matcher.group(2));
                long usedBlocks = Long.parseLong(matcher.group(3));
                long freeBlocks = Long.parseLong(matcher.group(4));
                // 转换为 GB，并取整
                long totalGb = (long) Math.floor(totalBlocks / 1024.0 / 1024.0);
                long usedGb = (long) Math.floor(usedBlocks / 1024.0 / 1024.0);
                long freeGb = (long) Math.floor(freeBlocks / 1024.0 / 1024.0);
                return "总:" + totalGb + "GB   " + "已用:" + usedGb +  "GB   " + "可用:" + freeGb + "GB";
            }
        }
        return "Unknown";
    }

    /**
     * 获取设备 RAM 信息
     */
    public static void getDeviceRam(TextArea getDeviceRamTextArea) {
        executorService.submit(() -> {
            try {
                String output = executeCommand(ADB_PATH + " shell cat /proc/meminfo");
                String ramInfo = parseRamInfo(output);
                Platform.runLater(() -> {
                    getDeviceRamTextArea.setText(Objects.requireNonNullElse(ramInfo, "Unknown"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> getDeviceRamTextArea.setText("error"));
            }
        });
    }

    /**
     * 解析 RAM 信息（总内存和可用内存）
     */
    public static String parseRamInfo(String output) {
        Matcher matcher = RAM_PATTERN.matcher(output);

        if (matcher.find()) {
            // 获取总内存和可用内存（单位：KB）
            long totalKb = Long.parseLong(matcher.group(1));
            long availableKb = Long.parseLong(matcher.group(2));

            // 将内存从 KB 转换为 GB，使用 Math.round() 进行四舍五入
            long totalGb = Math.round(totalKb / 1024.0 / 1024.0);
            long availableGb = Math.round(availableKb / 1024.0 / 1024.0);

            // 返回结果（总内存和可用内存）
            return "总: " + totalGb + " GB   可用: " + availableGb + " GB";
        }

        return "Unknown";
    }

    /**
     * 电池电量 获取电池电量。
     */
    public static void getBatteryCapacity(TextArea getBatteryCapacityTextArea) {
        getBatteryInfo("capacity", getBatteryCapacityTextArea, "Unknown");
    }

    /**
     * 电池容量 获取电池容量。
     */
    public static void getBatterySize(TextArea getBatterySizeTextArea) {
        getBatteryInfo("charge_full_design", getBatterySizeTextArea, "Unknown");
    }

    /**
     * 当前电流 获取当前电流。
     */
    public static void getBatteryCurrentNow(TextArea getCurrentNowTextArea) {
        getBatteryInfo("current_now", getCurrentNowTextArea, "Unknown");
    }

    /**
     * 平均电流 获取平均电流。
     */
    public static void getBatteryCurrentAvg(TextArea getCurrentAvgTextArea) {
        getBatteryInfo("current_avg", getCurrentAvgTextArea, "Unknown");
    }

    /**
     * 当前电压 获取当前电压。
     */
    public static void getBatteryVoltageNow(TextArea getVoltageNowTextArea) {
        getBatteryInfo("voltage_now", getVoltageNowTextArea, "Unknown");
    }

    /**
     * 平均电压 获取平均电压。
     */
    public static void getBatteryVoltageAvg(TextArea getVoltageAvgTextArea) {
        getBatteryInfo("voltage_avg", getVoltageAvgTextArea, "Unknown");
    }

    /**
     * 电池健康 获取电池健康。
     */
    public static void getBatteryHealth(TextArea getBatteryHealthTextArea) {
        getBatteryInfo("health", getBatteryHealthTextArea, "Unknown");
    }

    /**
     * 充电状态 获取充电状态。
     */
    public static void getBatteryStatus(TextArea getBatteryStatusTextArea) {
        getBatteryInfo("status", getBatteryStatusTextArea, "Unknown");
    }

    /**
     * 充电温度 获取充电温度。
     */
    public static void getBatteryTemp(TextArea getBatteryTempTextArea) {
        getBatteryInfo("temp", getBatteryTempTextArea, "Unknown");
    }

    /**
     * 环境温度 获取环境温度。
     */
    public static void getBatteryTempAmbient(TextArea getBatteryTempAmbientTextArea) {
        getBatteryInfo("temp_ambient", getBatteryTempAmbientTextArea, "Unknown");
    }

    /**
     * 电池材质 获取电池材质。
     */
    public static void getBatteryTechnology(TextArea getBatteryTechnologyTextArea) {
        getBatteryInfo("technology", getBatteryTechnologyTextArea, "Unknown");
    }

    /**
     * 充满时间 获取充满时间。
     */
    public static void getBatteryTimeFullToNow(TextArea getBatteryTimeFullToNowTextArea) {
        getBatteryInfo("time_to_full_now", getBatteryTimeFullToNowTextArea, "Unknown");
    }

    /**
     * 连接类型 获取连接类型。
     */
    public static void getBatteryUsbType(TextArea getBatteryUsbTypeTextArea) {
        getBatteryInfo("usb_type", getBatteryUsbTypeTextArea, "Unknown");
    }

    /**
     * 获取设备XDPI
     */
    public static void getDeviceXdpi(TextArea getDeviceXdpiTextArea) {
        executorService.submit(() -> {
            try {
                String output = executeCommand(ADB_PATH + " shell dumpsys display");
                String xDpi = parseXdpi(output);
                Platform.runLater(() -> getDeviceXdpiTextArea.setText(Objects.requireNonNullElse(xDpi, "Unknown")));
            } catch (Exception e) {
                Platform.runLater(() -> getDeviceXdpiTextArea.setText("error"));
            }
        });
    }

    /**
     * 获取设备YDPI
     */
    public static void getDeviceYdpi(TextArea getDeviceYdpiTextArea) {
        executorService.submit(() -> {
            try {
                String output = executeCommand(ADB_PATH + " shell dumpsys display");
                String yDpi = parseYdpi(output);
                Platform.runLater(() -> getDeviceYdpiTextArea.setText(Objects.requireNonNullElse(yDpi, "Unknown")));
            } catch (Exception e) {
                Platform.runLater(() -> getDeviceYdpiTextArea.setText("error"));
            }
        });
    }

    /**
     * 解析XDPI
     */
    private static String parseXdpi(String output) {
        Pattern pattern = Pattern.compile("mActiveSfDisplayMode=.*?xDpi=([\\d.]+)");
        Matcher matcher = pattern.matcher(output);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 解析YDPI
     */
    private static String parseYdpi(String output) {
        Pattern pattern = Pattern.compile("mActiveSfDisplayMode=.*?yDpi=([\\d.]+)");
        Matcher matcher = pattern.matcher(output);
        return matcher.find() ? matcher.group(1) : null;
    }

    /**
     * 获取纯白最高尼特值
     */
    public static void getScreenWhiteNit(TextArea getScreenWhiteNitTextArea) {
        executeAdbCommandAndUpdateLabel(
                ADB_PATH + " shell dumpsys display | grep mMaxLuminance",
                getScreenWhiteNitTextArea,
                "Unknown",
                "mMaxLuminance=([\\d.]+)"
        );
    }

    /**
     * 获取常规最高尼特值
     */
    public static void getScreenNormalNit(TextArea getScreenNormalNitTextArea) {
        executorService.submit(() -> {
            try {
                String output = executeCommand(ADB_PATH + " shell dumpsys display | grep mBrightnessLevelsNits");
                String[] nits = output.split("[,\\s\\[\\]]+");
                String lastNit = nits.length > 0 ? nits[nits.length - 1] : "Unknown";
                Platform.runLater(() -> getScreenNormalNitTextArea.setText(lastNit));
            } catch (Exception e) {
                Platform.runLater(() -> getScreenNormalNitTextArea.setText("error"));
            }
        });
    }

    /**
     * 获取亮度级别Lux和Nits数组
     */
    public static void getScreenLuxLevel(TextArea getScreenLuxLevelTextArea) {
        executeAdbCommandAndUpdateLabel(
                ADB_PATH + " shell dumpsys display | grep mBrightnessLevelsLux",
                getScreenLuxLevelTextArea,
                "Unknown",
                "mBrightnessLevelsLux=\\s*\\[([^]]+)\\]"
        );
    }
    public static void getScreenNitLevel(TextArea getScreenNitLevelTextArea) {
        executeAdbCommandAndUpdateLabel(
                ADB_PATH + " shell dumpsys display | grep mBrightnessLevelsNits",
                getScreenNitLevelTextArea,
                "Unknown",
                "mBrightnessLevelsNits=\\s*\\[([^]]+)\\]"
        );
    }

    /**
     * 获取所有DisplayMode挡位及刷新率
     */
    public static void getAllScreenRefreshRateLevels(TextArea getScreenBrightnessTextArea) {
        executorService.submit(() -> {
            try {
                String output = executeCommand(ADB_PATH + " shell dumpsys display");
                Pattern pattern = Pattern.compile("DisplayMode\\{id=(\\d+).*?refreshRate=([\\d.]+)");
                Matcher matcher = pattern.matcher(output);
                StringBuilder result = new StringBuilder();
                while (matcher.find()) {
                    result.append("挡位").append(matcher.group(1)).append(": ").append(matcher.group(2)).append("Hz\n");
                }
                Platform.runLater(() -> getScreenBrightnessTextArea.setText(result.toString().trim()));
            } catch (Exception e) {
                Platform.runLater(() -> getScreenBrightnessTextArea.setText("error"));
            }
        });
    }

    /**
     * 获取当前DisplayMode挡位及刷新率
     */
    public static void getCurrentDisplayMode(TextArea getScreenRefreshRateTextArea) {
        executorService.submit(() -> {
            try {
                String output = executeCommand(ADB_PATH + " shell dumpsys display");
                Pattern pattern = Pattern.compile("mActiveSfDisplayMode=.*?id=(\\d+).*?refreshRate=([\\d.]+)");
                Matcher matcher = pattern.matcher(output);
                if (matcher.find()) {
                    String mode = "挡位" + matcher.group(1) + ": " + matcher.group(2) + "Hz";
                    Platform.runLater(() -> getScreenRefreshRateTextArea.setText(mode));
                } else {
                    Platform.runLater(() -> getScreenRefreshRateTextArea.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> getScreenRefreshRateTextArea.setText("error"));
            }
        });
    }

    /**
     * 通用带正则匹配的命令执行方法
     */
    private static void executeAdbCommandAndUpdateLabel(String command, TextArea textArea, String defaultValue, String regex) {
        executorService.submit(() -> {
            try {
                String output = executeCommand(command);
                Pattern pattern = Pattern.compile(regex);
                Matcher matcher = pattern.matcher(output);
                String result = matcher.find() ? matcher.group(1) : defaultValue;
                Platform.runLater(() -> textArea.setText(result));
            } catch (Exception e) {
                Platform.runLater(() -> textArea.setText("error"));
            }
        });
    }

    /**
     * 动态检测电池路径（线程安全）
     */
    private static synchronized void detectBatteryPath() throws IOException {
        if (batteryBasePath != null) {
            return;
        }

        for (String path : BATTERY_PATH_CANDIDATES) {
            // 检查路径是否存在且包含关键文件
            String checkCmd = String.format(
                    "shell \"if [ -d %s ] && [ -f %s/capacity ]; then echo exists; fi\"",
                    path, path
            );

            try {
                String output = executeCommand(ADB_PATH + " " + checkCmd);
                if (output != null && output.contains("exists")) {
                    batteryBasePath = path;
                    return;
                }
            } catch (IOException e) {
                // 忽略当前路径的检测异常
                continue;
            }
        }

        throw new IOException("无法检测到电池路径");
    }

    /**
     * 当检测到设备断开或重新连接时
     */
    public static void onDeviceDisconnected() {
        synchronized (BATTERYPATHLOCK) {
            batteryBasePath = null;
        }
    }

    /**
     * 获取电池信息通用方法
     */
    private static void getBatteryInfo(String file, TextArea textArea, String defaultValue) {
        executorService.submit(() -> {
            try {
                ensureBatteryPathInitialized();
                String command = String.format("%s shell cat %s/%s",
                        ADB_PATH, batteryBasePath, file);
                String output = executeCommand(command);
                String result = (output != null && !output.isEmpty()) ? output.trim() : defaultValue;
                Platform.runLater(() -> textArea.setText(result));
            } catch (Exception e) {
                Platform.runLater(() -> textArea.setText("无"));
            }
        });
    }

    /**
     * 异步预加载路径（减少首次延迟）
     */
    public static void preloadBatteryPath() {
        executorService.submit(() -> {
            try {
                ensureBatteryPathInitialized();
            } catch (IOException e) {
                // 记录日志或处理异常
            }
        });
    }

    /**
     * 确保电池路径已初始化
     */
    private static synchronized void ensureBatteryPathInitialized() throws IOException {
        if (batteryBasePath == null) {
            synchronized (BATTERYPATHLOCK) {
                // 双重检查锁定
                if (batteryBasePath == null) {
                    detectBatteryPath();
                }
            }
        }
    }

    /**
     * 通用方法：执行ADB命令并更新Label
     */
    private static void executeAdbCommandAndUpdate(String command, TextArea textArea, String defaultValue) {
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