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
import java.util.Objects;
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
     * 定位屏幕尺寸。
     */
    private static final Pattern DISPLAY_SIZE_PATTERN = Pattern.compile("(\\d+)x(\\d+)");
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

    /**
     * 软件版本 获取软件版本。
     */
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

    /**
     * 安卓版本 获取安卓版本。
     */
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

    /**
     * build版本 获取build版本。
     */
    public static void getBuildType(Label buildTypeLabel) {
        executorService.submit(() -> {
            try {
                // 执行命令获取设备buildType
                String output = executeCommand(ADB_PATH + " shell getprop ro.build.type");
                if (output != null && !output.isEmpty()) {
                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> buildTypeLabel.setText(output.trim()));
                } else {
                    Platform.runLater(() -> buildTypeLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> buildTypeLabel.setText("None"));
            }
        });
    }

    /**
     * dpi 获取dpi。
     */
    public static void getDpi(Label buildTypeLabel) {
        executorService.submit(() -> {
            try {
                // 执行命令获取设备dpi
                String output = executeCommand(ADB_PATH + " shell wm density");
                if (output != null && !output.isEmpty()) {
                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> buildTypeLabel.setText(output.trim()));
                } else {
                    Platform.runLater(() -> buildTypeLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> buildTypeLabel.setText("None"));
            }
        });
    }


    /**
     * FPS 获取并更新帧数。
     */
    public static void getFrameRate(Label fpsLabel) {
        executorService.submit(() -> {
            try {
                if (!isDeviceConnected()){
                    Platform.runLater(() -> fpsLabel.setText("设备未连接"));
                    return;
                }

                // 添加超时和错误流处理
                String output = executeCommand(ADB_PATH + " shell \"cat /sys/kernel/fpsgo/fstb/fpsgo_status\"");
                if (output == null) {
                    Platform.runLater(() -> fpsLabel.setText("no data"));
                    return;
                }

                String frameRate = parseFrameRate(output);
                Platform.runLater(() -> {
                    if (isValidFps(frameRate)) {
                        fpsLabel.setText(frameRate + " FPS");
                    } else {
                        fpsLabel.setText("0 FPS");
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> fpsLabel.setText("需Root"));
                e.printStackTrace();
            }
        });
    }

    /**
     * FPS adb中获取FPS帧数。
     */
    private static String parseFrameRate(String output) {
        return output.lines()
                .skip(1)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(FPS_PATTERN::matcher)
                .filter(Matcher::find)
                .map(m -> m.group(1))
                .filter(fps -> !"-1".equals(fps))
                .findFirst()
                .orElse(null);
    }

    /**
     * FPS。
     */
    private static boolean isValidFps(String fps) {
        return fps != null && !fps.isEmpty() && !"-1".equals(fps);
    }

    /**
     * Activity 获取并更新Activity。
     */
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

    /**
     * Soc温度 获取并更新Soc温度。
     */
    public static void getSocTemp(Label activityLabel) {
        executorService.submit(() -> {
            try {
                // 执行命令获取soc温度
                String output = executeCommand(ADB_PATH + " shell \"cat /sys/class/thermal/thermal_zone0/temp\"");
                if (output != null && !output.isEmpty()) {
                    // 将五位数转换为温度（千分之一度 -> 度）
                    int temp = Integer.parseInt(output.trim());
                    double temperature = temp / 1000.0;

                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> activityLabel.setText("Soc: " + String.format("%.2f", temperature) + "°C"));
                } else {
                    Platform.runLater(() -> activityLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> activityLabel.setText("None"));
            }
        });
    }

    /**
     * 小核温度 获取并更新小核温度。
     */
    public static void getSmCoreTemp(Label cpuSmallCoreTempLabel) {
        executorService.submit(() -> {
            try {
                // 执行命令获取小核温度
                String output = executeCommand(ADB_PATH + " shell \"cat /sys/class/thermal/thermal_zone1/temp\"");
                if (output != null && !output.isEmpty()) {
                    // 将五位数转换为温度（千分之一度 -> 度）
                    int temp = Integer.parseInt(output.trim());
                    double temperature = temp / 1000.0;

                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> cpuSmallCoreTempLabel.setText("SmallCore: " + String.format("%.2f", temperature) + "°C"));
                } else {
                    Platform.runLater(() -> cpuSmallCoreTempLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> cpuSmallCoreTempLabel.setText("None"));
            }
        });
    }

    /**
     * 大核温度 获取并更新大核温度。
     */
    public static void getBigCoreTemp(Label cpuBigCoreTempLabel) {
        executorService.submit(() -> {
            try {
                // 执行命令获取大核温度
                String output = executeCommand(ADB_PATH + " shell \"cat /sys/class/thermal/thermal_zone5/temp\"");
                if (output != null && !output.isEmpty()) {
                    // 将五位数转换为温度（千分之一度 -> 度）
                    int temp = Integer.parseInt(output.trim());
                    double temperature = temp / 1000.0;

                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> cpuBigCoreTempLabel.setText("BigCore: " + String.format("%.2f", temperature) + "°C"));
                } else {
                    Platform.runLater(() -> cpuBigCoreTempLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> cpuBigCoreTempLabel.setText("None"));
            }
        });
    }

    /**
     * modem温度 获取并更新modem温度。
     */
    public static void getModemTemp(Label modemTempLabel) {
        executorService.submit(() -> {
            try {
                // 执行命令获取modem温度
                String output = executeCommand(ADB_PATH + " shell \"cat /sys/class/thermal/thermal_zone13/temp\"");
                if (output != null && !output.isEmpty()) {
                    // 将五位数转换为温度（千分之一度 -> 度）
                    int temp = Integer.parseInt(output.trim());
                    double temperature = temp / 1000.0;

                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> modemTempLabel.setText("modem: " + String.format("%.2f", temperature) + "°C"));
                } else {
                    Platform.runLater(() -> modemTempLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> modemTempLabel.setText("None"));
            }
        });
    }

    /**
     * PMIC温度 获取并更新PMIC温度。
     */
    public static void getPmicTemp(Label pmicTempLabel) {
        executorService.submit(() -> {
            try {
                // 执行命令获取PMIC温度
                String output = executeCommand(ADB_PATH + " shell \"cat /sys/class/thermal/thermal_zone16/temp\"");
                if (output != null && !output.isEmpty()) {
                    // 将五位数转换为温度（千分之一度 -> 度）
                    int temp = Integer.parseInt(output.trim());
                    double temperature = temp / 1000.0;

                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> pmicTempLabel.setText("PMIC: " + String.format("%.2f", temperature) + "°C"));
                } else {
                    Platform.runLater(() -> pmicTempLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> pmicTempLabel.setText("None"));
            }
        });
    }

    /**
     * PMIC温度 获取并更新Camera温度。
     */
    public static void getCameraTemp(Label cameraTempLabel) {
        executorService.submit(() -> {
            try {
                // 执行命令获取Camera温度
                String output = executeCommand(ADB_PATH + " shell \"cat /sys/class/thermal/thermal_zone21/temp\"");
                if (output != null && !output.isEmpty()) {
                    // 将五位数转换为温度（千分之一度 -> 度）
                    int temp = Integer.parseInt(output.trim());
                    double temperature = temp / 1000.0;

                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> cameraTempLabel.setText("Camera: " + String.format("%.2f", temperature) + "°C"));
                } else {
                    Platform.runLater(() -> cameraTempLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> cameraTempLabel.setText("None"));
            }
        });
    }

    /**
     * PMIC温度 获取并更新GPU温度。
     */
    public static void getGpuTemp(Label cameraTempLabel) {
        executorService.submit(() -> {
            try {
                // 执行命令获取GPU温度
                String output = executeCommand(ADB_PATH + " shell \"cat /sys/class/thermal/thermal_zone10/temp\"");
                if (output != null && !output.isEmpty()) {
                    // 将五位数转换为温度（千分之一度 -> 度）
                    int temp = Integer.parseInt(output.trim());
                    double temperature = temp / 1000.0;

                    // 在 JavaFX 应用程序线程中更新 UI 标签
                    Platform.runLater(() -> cameraTempLabel.setText("GPU: " + String.format("%.2f", temperature) + "°C"));
                } else {
                    Platform.runLater(() -> cameraTempLabel.setText("Unknown"));
                }
            } catch (Exception e) {
                Platform.runLater(() -> cameraTempLabel.setText("None"));
            }
        });
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
     * CPU频率 获取并更新cpu频率。
     */
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
     * CPU频率 从adb中提取cpu频率。
     */
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



    /**
     * 屏幕尺寸 获取屏幕尺寸。
     */
    public static void getDisplaySize(Label displaySizeLabel) {
        executorService.submit(() -> {
            try {
                // 执行命令获取设备型号
                String output = executeCommand(ADB_PATH + " shell wm size");

                String deviceDisplaySize = parseDisplaySize(output);

                Platform.runLater(() -> {
                    // 使用 Objects.requireNonNullElse 来替代 if 语句
                    displaySizeLabel.setText(Objects.requireNonNullElse(deviceDisplaySize, "Unknown"));
                });
            } catch (Exception e) {
                Platform.runLater(() -> displaySizeLabel.setText("error"));
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
