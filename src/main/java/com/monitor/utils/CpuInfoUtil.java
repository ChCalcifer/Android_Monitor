package com.monitor.utils;

import com.monitor.thread.CustomThreadFactory;
import javafx.application.Platform;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Author: CYC
 * Time: 2025/4/9 20:12:33
 * Description:
 * Branch:
 * Version: 1.0
 * @author CYC
 */

public class CpuInfoUtil {

    private static final String ADB_PATH = "adb";

    /**
     * 空格。
     */
    private static final Pattern CPU_FREQUENCY_PATTERN = Pattern.compile("\\d+");


    /**
     * 异步线程池。
     */
    private static final ExecutorService executorService = new ThreadPoolExecutor(
            2, 10, 60, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),new CustomThreadFactory("CpuInfoUtil-Pool")
    );

    /**
     * CPU频率 获取并更新cpu频率。
     */
    public static List<String> getCpuFrequencies() {
        try {
            String output = executeCommand(ADB_PATH + " shell \"for i in $(seq 0 3); do cat /sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq; done\"");
            return parseCpuFrequencies(output);
        } catch (Exception e) {
            return Collections.singletonList("Error: " + e.getMessage());
        }
    }

    // CpuInfoUtil.java 修改getCpuFrequencies为异步

    public static void getCpuFrequenciesAsync(Consumer<List<String>> callback) {
        executorService.submit(() -> {
            try {
                // 获取所有核心（假设最多8核）
                String output = executeCommand(ADB_PATH + " shell \"for i in $(seq 0 7); do cat /sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq 2>/dev/null || echo 0; done\"");
                callback.accept(parseCpuFrequencies(output));
            } catch (Exception e) {
                callback.accept(Collections.singletonList("Error: " + e.getMessage()));
            }
        });
    }

    public static void getCpuFrequencyAsync(int coreId, Consumer<Double> callback) {
        executorService.submit(() -> {
            try {
                String cmd = String.format("adb shell cat /sys/devices/system/cpu/cpu%d/cpufreq/scaling_cur_freq", coreId);
                String output = executeCommand(cmd);
                double mhz = Double.parseDouble(output.trim()) / 1000.0;
                Platform.runLater(() -> callback.accept(mhz));
            } catch (Exception e) {
                Platform.runLater(() -> callback.accept(0.0));
            }
        });
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

    public static int getCpuCoreCount() {
        try {
            String output = executeCommand(ADB_PATH + " shell ls /sys/devices/system/cpu/ | grep 'cpu[0-9]'");
            return output.split("\n").length;
        } catch (Exception e) {
            return 4;
        }
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
