package com.devicemonitor;

import com.devicemonitor.utils.ADBUtil;

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
        void onStatusUpdate(boolean isConnected, String cpuFrequency);
    }

    public DeviceMonitor() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void startMonitoring(DeviceStatusListener listener) {
        this.listener = listener;
        scheduler.scheduleAtFixedRate(this::checkDeviceStatus, 0, 1, TimeUnit.SECONDS);
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
        boolean isConnected = ADBUtil.isDeviceConnected();
        String cpuFrequency = ADBUtil.getCPUFrequency();
        if (listener != null) {
            listener.onStatusUpdate(isConnected, cpuFrequency);
        }
    }
}
