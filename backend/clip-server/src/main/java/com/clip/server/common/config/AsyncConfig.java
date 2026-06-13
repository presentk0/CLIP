package com.clip.server.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "chatTaskExecutor")
    public Executor chatTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);          // 기본 스레드 수
        executor.setMaxPoolSize(20);          // 최대 스레드 수
        executor.setQueueCapacity(100);       // 대기 큐 크기
        executor.setThreadNamePrefix("Chat-");
        executor.initialize();
        return executor;
    }
}