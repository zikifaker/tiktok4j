package com.github.zikifaker.tiktok4j.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
public class ThreadPoolConfig {
    private static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    private static final int VIDEO_TASK_QUEUE_SIZE = 600;

    @Bean("videoTaskExecutor")
    public Executor videoTaskExecutor() {
        return new ThreadPoolExecutor(
                CPU_CORES * 2,
                CPU_CORES * 3,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(VIDEO_TASK_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean("commentTaskExecutor")
    public Executor commentTaskExecutor() {
        return new ThreadPoolExecutor(
                CPU_CORES * 2,
                CPU_CORES * 3,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(VIDEO_TASK_QUEUE_SIZE),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
