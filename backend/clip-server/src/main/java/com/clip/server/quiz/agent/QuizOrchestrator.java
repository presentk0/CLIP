package com.clip.server.quiz.agent;

import com.clip.server.ai.client.OpenAiClient;
import com.clip.server.quiz.agent.dto.QuizStrategy;
import com.clip.server.quiz.agent.dto.UserLearningState;
import com.clip.server.quiz.agent.prompt.OrchestratorPrompt;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * [CLIPZY Agent - Layer 3: Quiz Orchestrator]
 * LLM이 사용자 학습 상태를 판단하여 오늘의 퀴즈 전략을 결정한다.
 *
 * 결정 항목:
 * - quiz_type: OX_HEAVY | BLANK_HEAVY | BALANCED
 * - difficulty: EASY | MEDIUM | HARD
 * - focus: WEAK_WORDS | NEW_WORDS | CHAT_WEAKNESS | MIXED
 * - word_count: 3~5
 * - reason: 한국어 판단 근거
 *
 * LLM 실패 시 규칙 기반 폴백으로 안정성 확보.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizOrchestrator {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

    // 시스템 프롬프트 (역할 정의)
    private static final String SYSTEM_PROMPT = """
            You are CLIPZY's learning strategy agent for English learners.
            You analyze user learning state and decide optimal quiz strategy.
            You MUST respond with valid JSON only, no markdown, no explanation.
            """;

    /**
     * 사용자 학습 상태를 기반으로 퀴즈 전략을 결정한다.
     */
    public QuizStrategy decideStrategy(UserLearningState state) {
        log.debug("[Agent] Orchestrator start - userId: {}", state.getUserId());

        try {
            // LLM 호출 (system + user 프롬프트)
            String userPrompt = OrchestratorPrompt.build(state);
            String jsonResponse = openAiClient.chat(SYSTEM_PROMPT, userPrompt);

            // JSON 파싱
            QuizStrategy strategy = objectMapper.readValue(jsonResponse, QuizStrategy.class);

            log.info("[Agent] Strategy decided - type: {}, difficulty: {}, focus: {}, count: {}, reason: {}",
                    strategy.getQuizType(),
                    strategy.getDifficulty(),
                    strategy.getFocus(),
                    strategy.getWordCount(),
                    strategy.getReason());

            return strategy;

        } catch (Exception e) {
            log.warn("[Agent] Orchestrator LLM failed, using fallback - reason: {}", e.getMessage());
            return fallbackStrategy(state);
        }
    }

    /**
     * LLM 실패 시 규칙 기반 폴백
     */
    private QuizStrategy fallbackStrategy(UserLearningState state) {
        Double accuracy = state.getRecentAccuracy();
        int weakCount = state.getWeakWords() != null ? state.getWeakWords().size() : 0;

        QuizStrategy fallback = QuizStrategy.fallback(accuracy, weakCount);
        log.info("[Agent] Fallback strategy - type: {}, focus: {}, reason: {}",
                fallback.getQuizType(), fallback.getFocus(), fallback.getReason());
        return fallback;
    }
}