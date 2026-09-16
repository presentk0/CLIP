package com.clip.server.video.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.video.dto.ai.request.AIRecommendRequest;
import com.clip.server.video.dto.ai.response.AIRecommendResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class AIRecommendationClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    private static final String RECOMMEND_ENDPOINT = "/api/v1/discovery/recommend-full";

    public AIRecommendResponse getFullRecommendations(AIRecommendRequest request) {
        String url = aiServerUrl + RECOMMEND_ENDPOINT;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<AIRecommendRequest> entity = new HttpEntity<>(request, headers);

        try {
            log.info("AI 서버 호출 시작: userId={}, excludedCount={}",
                    request.getUserId(),
                    request.getExcludedVideoIds() != null ? request.getExcludedVideoIds().size() : 0);

            // 실제 전송 JSON 로그 (디버깅용)
            try {
                String jsonBody = objectMapper.writeValueAsString(request);
                log.info("전송 JSON: {}", jsonBody);
            } catch (JsonProcessingException e) {
                log.warn("JSON 직렬화 실패: {}", e.getMessage());
            }

            ResponseEntity<AIRecommendResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AIRecommendResponse.class
            );

            AIRecommendResponse body = response.getBody();
            if (body == null || body.recommendations() == null) {
                log.error("AI 서버 응답이 비어있음");
                throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
            }

            log.info("AI 서버 응답 성공: recommendations={}, qualityScore={}, retryCount={}",
                    body.recommendations().size(),
                    body.qualityScore(),
                    body.retryCount());

            return body;

        } catch (RestClientException e) {
            log.error("AI 서버 호출 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        } catch (Exception e) {
            log.error("AI 서버 통신 중 오류: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.AI_SERVER_ERROR);
        }
    }
}