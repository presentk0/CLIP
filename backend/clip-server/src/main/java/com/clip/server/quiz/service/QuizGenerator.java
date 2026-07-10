package com.clip.server.quiz.service;

import com.clip.server.quiz.ai.OpenAIService;
import com.clip.server.quiz.dto.response.OpenAIQuizDataResponse;
import com.clip.server.quiz.ai.validator.QuizValidator;
import com.clip.server.quiz.ai.validator.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Function;

/**
 * AI 퀴즈 생성 + 검증 + 재시도 오케스트레이터
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizGenerator {

    private final OpenAIService openAIService;
    private final QuizValidator quizValidator;

    private static final int MAX_ATTEMPTS = 3;

    // ==================== OX 퀴즈 ====================

    public OpenAIQuizDataResponse generateOXQuiz(String word, String meaning, String difficulty){
        return generateWithRetry(
                "OX",
                word,
                (hint) -> openAIService.generateOXQuiz(word, meaning, hint, difficulty),
                quizValidator::validateOXQuiz
        );
    }

    // ==================== 빈칸 퀴즈 ====================

    public OpenAIQuizDataResponse generateBlankQuiz(String word, String meaning, String difficulty) {
        return generateWithRetry(
                "BLANK",
                word,
                (hint) -> openAIService.generateBlankQuiz(word, meaning, hint, difficulty),
                quizValidator::validateBlankQuiz
        );
    }

    // ==================== 공통 재시도 로직 ====================

    /**
     * AI 생성 + 검증 + 재시도 패턴
     *
     * @param quizType 로그용 퀴즈 타입
     * @param word 로그용 단어
     * @param generator AI 호출 함수 (이전 실패 사유를 힌트로 받음)
     * @param validator 검증 함수
     * @return 유효한 퀴즈 또는 null (최종 실패 시)
     */
    private OpenAIQuizDataResponse generateWithRetry(
            String quizType,
            String word,
            Function<String, OpenAIQuizDataResponse> generator,
            Function<OpenAIQuizDataResponse, ValidationResult> validator) {

        String previousFailReason = null;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                log.debug("[{}] AI 퀴즈 생성 시도 {}/{} - word: {}", quizType, attempt, MAX_ATTEMPTS, word);

                // 1. AI 호출 (이전 실패 사유를 힌트로 전달)
                OpenAIQuizDataResponse quiz = generator.apply(previousFailReason);

                // 2. 검증
                ValidationResult result = validator.apply(quiz);

                if (result.isValid()) {
                    log.info("[{}] AI 퀴즈 생성 성공 (시도 {}/{}) - word: {}",
                            quizType, attempt, MAX_ATTEMPTS, word);
                    return quiz;
                }

                // 3. 실패 사유 기록 → 다음 시도 시 힌트로 활용
                previousFailReason = result.getReason();
                log.warn("[{}] 검증 실패 (시도 {}/{}) - word: {}, 사유: {}",
                        quizType, attempt, MAX_ATTEMPTS, word, previousFailReason);

            } catch (Exception e) {
                log.warn("[{}] AI 호출 예외 (시도 {}/{}) - word: {}", quizType, attempt, MAX_ATTEMPTS, word, e);
                previousFailReason = "AI 호출 중 예외 발생: " + e.getMessage();
            }
        }

        log.error("[{}] AI 퀴즈 생성 최종 실패 ({}회 시도) - word: {}", quizType, MAX_ATTEMPTS, word);
        return null;
    }
}
