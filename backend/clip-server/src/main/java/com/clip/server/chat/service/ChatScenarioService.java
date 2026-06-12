package com.clip.server.chat.service;

import com.clip.server.chat.client.ChatScenarioAiClient;
import com.clip.server.chat.dto.ai.AIScenarioResult;
import com.clip.server.chat.dto.response.ChatScenarioResponse;
import com.clip.server.chat.dto.response.ScenarioDto;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatScenarioService {

    private final ChatScenarioAiClient chatScenarioAiClient;

    public ChatScenarioResponse getScenarios(Long userId, String word) {

        log.info("시나리오 조회 요청. userId={}, word={}", userId, word);

        // 1. 단어 정규화
        String normalizedWord = word.trim().toLowerCase();

        // 2. AI 호출
        AIScenarioResult aiResult = chatScenarioAiClient.requestScenarios(normalizedWord);

        // 3. DTO 변환 (scenarioId는 1부터 순차 부여)
        List<AIScenarioResult.AIScenarioItem> items = aiResult.getScenarios();
        List<ScenarioDto> scenarios = IntStream.range(0, items.size())
                .mapToObj(i -> ScenarioDto.builder()
                        .scenarioId((long) (i + 1))
                        .title(items.get(i).getTitle())
                        .goal(items.get(i).getGoal())
                        .situation(items.get(i).getSituation())
                        .build())
                .toList();

        return ChatScenarioResponse.builder()
                .targetWord(normalizedWord)
                .scenarios(scenarios)
                .build();
    }
}