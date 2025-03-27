package com.devicemonitor.utils;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

    public static String getCPUFrequency() {
        try {
            String output = executeCommand(ADB_PATH + " shell cat /proc/cpuinfo");
            return parseCPUFrequency(output);
        } catch (Exception e) {
            return "N/A";
        }
    }

    private static String parseCPUFrequency(String output) {
        Pattern pattern = Pattern.compile("cpu MHz\\s+:\\s+(\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(output);
        if(matcher.find()) {
            return String.format("%.2f MHz", Float.parseFloat(matcher.group(1)));
        }
        return "N/A";
    }

    private static String executeCommand(String command) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

        CommandLine cmdLine = CommandLine.parse(command);
        DefaultExecutor executor = new DefaultExecutor();

        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, errorStream);
        executor.setStreamHandler(streamHandler);

        try {
            int exitValue = executor.execute(cmdLine);
            if(exitValue != 0) {
                throw new IOException("命令执行失败: " + errorStream.toString());
            }
            return outputStream.toString();
        } catch (ExecuteException e) {
            throw new IOException("执行异常: " + errorStream.toString(), e);
        }
    }
}
