package com.devicemonitor;

import com.devicemonitor.utils.ADBUtil;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
/**
 * Author: CYC
 * Time: 2025/3/27 16:13:34
 * Description:
 * Branch:
 * Version: 1.0
 */

public class DeviceMonitor {
    private final ScheduledExecutorService scheduler;
    private DeviceStatusListener listener;

    public interface DeviceStatusListener {
        void onStatusUpdate(boolean isConnected, List<String> cpuFrequencies);
    }

    public DeviceMonitor() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startMonitoring(DeviceStatusListener listener) {
        this.listener = listener;
        scheduler.scheduleAtFixedRate(this::checkDeviceStatus, 0, 100, TimeUnit.MILLISECONDS);
    }

    public void stopMonitoring() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                    System.err.println("监控线程未正常终止");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void checkDeviceStatus() {
        try {
            boolean isConnected = ADBUtil.isDeviceConnected();
            List<String> cpuFrequencies = ADBUtil.getCPUFrequencies();

            if (listener != null) {
                listener.onStatusUpdate(isConnected, cpuFrequencies);
            }
        } catch (Exception e) {

        }
    }
}
