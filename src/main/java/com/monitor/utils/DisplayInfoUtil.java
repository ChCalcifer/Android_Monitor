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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.monitor.utils.DeviceInfoUtil.isDeviceConnected;

/**
 * @Author CYC
 * @Date 9/4/2025 上午12:32
 * @Version 1.0.0
 */

public class DisplayInfoUtil {

    private static final String ADB_PATH = "adb";


    private static final Pattern FPS_PATTERN = Pattern.compile("^\\s*\\d+\\s+\\S+\\s+\\S+\\s+(-?\\d+)\\b");

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
