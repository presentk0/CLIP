package com.clip.server.chat.client;

import com.clip.server.ai.client.OpenAiClient;
import com.clip.server.chat.dto.ai.AIScenarioResult;
import com.clip.server.chat.prompt.ScenarioPromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatScenarioAiClient {

    private final OpenAiClient openAiClient;
    private final ScenarioPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    public AIScenarioResult requestScenarios(String targetWord) {
        String systemPrompt = promptBuilder.getSystemPrompt();
        String userPrompt = promptBuilder.buildUserPrompt(targetWord);

        // 1. AI 호출
        String content = openAiClient.chat(systemPrompt, userPrompt);
        log.debug("AI 시나리오 응답: {}", content);

        // 2. JSON 파싱
        AIScenarioResult result;
        try {
            result = objectMapper.readValue(content, AIScenarioResult.class);
        } catch (Exception e) {
            log.error("AI 응답 파싱 실패. content={}", content, e);
            throw new RuntimeException("AI 응답을 처리하는 중 오류가 발생했습니다.");
        }

        // 3. 응답 검증
        validate(result);

        return result;
    }

    private void validate(AIScenarioResult result) {
        if (result == null || result.getScenarios() == null || result.getScenarios().isEmpty()) {
            throw new RuntimeException("AI 응답이 비어있습니다.");
        }

        // goal + situation
        boolean hasInvalidItem = result.getScenarios().stream()
                .anyMatch(item ->
                        isBlank(item.getTitle()) ||
                                isBlank(item.getGoal()) ||
                                isBlank(item.getSituation())
                );

        if (hasInvalidItem) {
            throw new RuntimeException("AI 응답 형식이 올바르지 않습니다.");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}