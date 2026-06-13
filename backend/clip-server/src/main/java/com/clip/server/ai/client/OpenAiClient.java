package com.clip.server.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.api.model}")
    private String model;

    /**
     * GPT에게 프롬프트 전송하고 응답 받기
     * - 네트워크 일시 장애(Connection reset 등) 발생 시 최대 3번 재시도
     * - 1초 → 2초 → 4초 간격으로 재시도
     */
    @Retryable(
            value = { ResourceAccessException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public String chat(String systemPrompt, String userPrompt) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    ),
                    "temperature", 0.7,
                    "response_format", Map.of("type", "json_object")
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            log.info("OpenAI 호출 시작");
            String response = restTemplate.postForObject(apiUrl, request, String.class);
            log.info("OpenAI 호출 성공");

            // 응답에서 content 추출
            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();

        } catch (ResourceAccessException e) {
            // 네트워크 에러는 그대로 던짐 → @Retryable이 캐치하고 재시도
            log.warn("OpenAI 네트워크 에러 (재시도 예정)", e);
            throw e;
        } catch (Exception e) {
            // 그 외 에러 (파싱 실패, API 키 오류 등)는 재시도 안 함
            log.error("OpenAI 호출 실패", e);
            throw new RuntimeException("AI 추천 실패", e);
        }
    }

    /**
     * 모든 재시도(3회) 실패 시 호출되는 fallback 메서드
     * - 시그니처는 원래 메서드와 동일하되, 첫 파라미터로 발생한 예외를 받음
     */
    @Recover
    public String recoverChat(ResourceAccessException e, String systemPrompt, String userPrompt) {
        log.error("OpenAI 재시도 3회 모두 실패. AI 호출 포기.", e);
        throw new RuntimeException("AI 서비스가 일시적으로 불안정합니다. 잠시 후 다시 시도해주세요.", e);
    }
}