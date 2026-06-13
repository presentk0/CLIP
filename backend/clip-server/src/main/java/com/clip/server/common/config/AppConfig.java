package com.clip.server.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    /**
     * OpenAI, Azure 등 외부 API 호출용 RestTemplate
     * - Connection Pool 사용 (성능 ↑)
     * - 죽은 연결 자동 감지 (Connection reset 방지)
     * - idle 연결 자동 정리 (장시간 미사용 후 호출 안정성 ↑)
     */
    @Bean
    public RestTemplate restTemplate() {
        // 1. 연결 풀 설정
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(50);              // 전체 최대 연결 수
        connectionManager.setDefaultMaxPerRoute(20);    // 호스트당 최대 연결 수
        connectionManager.setDefaultConnectionConfig(
                ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(10))    // 연결 타임아웃 10초
                        .setSocketTimeout(Timeout.ofSeconds(60))     // 응답 대기 60초 (OpenAI 응답이 길 수 있음)
                        // 30초 이상 idle된 연결은 사용 전 검증 (Connection reset 방지)
                        .setValidateAfterInactivity(TimeValue.ofSeconds(30))
                        .build()
        );

        // 2. 요청 설정
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(5))   // 풀에서 연결 받기까지 5초
                .build();

        // 3. HttpClient 생성
        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                // 60초 이상 idle된 연결은 자동 폐기
                .evictIdleConnections(TimeValue.ofSeconds(60))
                // 만료된 연결 자동 제거
                .evictExpiredConnections()
                .build();

        // 4. RestTemplate 생성
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory(httpClient);

        return new RestTemplate(factory);
    }
}
