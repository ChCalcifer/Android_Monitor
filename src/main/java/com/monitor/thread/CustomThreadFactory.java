package com.monitor.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Author: CYC
 * Time: 2025/4/11 10:08:17
 * Description: 线程池类
 * Branch:
 * Version: 1.0
 * @author CYC
 */

public class CustomThreadFactory implements ThreadFactory {
    /**
    *线程名前缀
     */
    private final String namePrefix;

    private final AtomicInteger threadCounter = new AtomicInteger(1);

    public CustomThreadFactory(String poolName) {
        this.namePrefix = poolName + "-thread-";
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, namePrefix + threadCounter.getAndIncrement());
        //线程名前缀
        thread.setDaemon(false);
        //设置优先级
        thread.setPriority(Thread.NORM_PRIORITY);
        return thread;
    }
}
