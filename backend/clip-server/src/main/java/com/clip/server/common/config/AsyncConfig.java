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

    /**
     * 이벤트 로깅 전용 Executor
     * - 분석용 데이터라 우선순위 낮음
     * - 스레드 적게, 큐는 크게
     */
    @Bean(name = "eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);          // 기본 2개
        executor.setMaxPoolSize(5);           // 최대 5개
        executor.setQueueCapacity(500);       // 큐 크게 (이벤트 폭주 대비)
        executor.setThreadNamePrefix("Event-");
        executor.initialize();
        return executor;
    }
}