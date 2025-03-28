package com.devicemonitor.utils;

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

    private static List<String> parseCPUFrequencies(String output) {
        List<String> frequencies = new ArrayList<>();
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(output);
        int coreIndex = 0;

        while (matcher.find()) {
            int kHz = Integer.parseInt(matcher.group());
            double MHz = kHz / 1000.0;
            frequencies.add(String.format("CPU %d: %.1f MHz", coreIndex++, MHz));
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
