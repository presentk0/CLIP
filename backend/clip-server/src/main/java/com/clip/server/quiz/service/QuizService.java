package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.ai.OpenAIService;
import com.clip.server.quiz.ai.QuizFallbackService;
import com.clip.server.quiz.dto.request.QuizSubmitRequest;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.OpenAIQuizDataResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizSubmitResponse;
import com.clip.server.quiz.entity.QuizResult;
import com.clip.server.quiz.persistence.QuizPersistenceService;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 퀴즈 비즈니스 로직 진입점
 * - OX, 빈칸: QuizGenerator (검증+재시도)
 * - 매칭: OpenAIService 직접 호출 (1회 시도 + Fallback)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizGenerator quizGenerator;                  // ✅ OX, 빈칸용
    private final OpenAIService openAIService;                  // ✅ 매칭용 (직접 호출)
    private final QuizFallbackService quizFallbackService;
    private final QuizPersistenceService quizPersistenceService;
    private final QuizResultRepository quizResultRepository;
    private final UserRepository userRepository;

    private static final int CORRECT_EXP = 100;
    private static final int MATCHING_QUIZ_COUNT = 5;

    // ==================== OX 퀴즈 생성 ====================

    public QuizDetailResponse createOXQuiz(Long sessionId, Long userId, QuizWordRequest request) {

        // 1. AI 생성 (검증+재시도 포함)
        OpenAIQuizDataResponse aiData = quizGenerator.generateOXQuiz(
                request.getWord(), request.getMeaning());

        // 2. 최종 실패 시 Fallback
        if (aiData == null) {
            log.warn("OX 퀴즈 AI 최종 실패, Fallback 사용 - word: {}", request.getWord());
            return quizFallbackService.createLocalOXQuiz(sessionId, userId, request);
        }

        // 3. 저장
        QuizResult saved = quizPersistenceService.saveOXQuiz(sessionId, userId, request, aiData);
        return mapToQuizDetailResponse(saved, null);
    }

    // ==================== 빈칸 퀴즈 생성 ====================

    public QuizDetailResponse createBlankQuiz(Long sessionId, Long userId, QuizWordRequest request) {

        // 1. AI 생성 (검증+재시도 포함)
        OpenAIQuizDataResponse aiData = quizGenerator.generateBlankQuiz(
                request.getWord(), request.getMeaning());

        // 2. 최종 실패 시 Fallback
        if (aiData == null) {
            log.warn("빈칸 퀴즈 AI 최종 실패, Fallback 사용 - word: {}", request.getWord());
            return quizFallbackService.createLocalBlankQuiz(sessionId, userId, request);
        }

        // 3. 저장
        QuizResult saved = quizPersistenceService.saveBlankQuiz(sessionId, userId, request, aiData);
        return mapToQuizDetailResponse(saved, aiData.getOptions());
    }

    // ==================== 매칭 퀴즈 생성 (재시도 X, 1회 시도) ====================

    public List<QuizDetailResponse> createMatchingQuiz(Long sessionId, Long userId, List<QuizWordRequest> requests) {

        // 1. AI 호출 (1회 시도, 재시도 없음)
        List<Map<String, String>> wordList = requests.stream()
                .map(r -> Map.of("word", r.getWord(), "meaning", r.getMeaning()))
                .collect(Collectors.toList());

        List<OpenAIQuizDataResponse> aiDataList = null;
        try {
            aiDataList = openAIService.generateMatchingQuiz(wordList, null);  // failHint = null
        } catch (Exception e) {
            log.warn("매칭 퀴즈 AI 호출 예외", e);
        }

        // 2. AI 완전 실패 시 전체 Fallback
        if (aiDataList == null || aiDataList.isEmpty()) {
            log.warn("매칭 퀴즈 AI 실패, Fallback 사용 - requestSize: {}", requests.size());
            return quizFallbackService.createLocalMatchingQuiz(sessionId, userId, requests);
        }

        log.info("매칭 퀴즈 생성 완료 - requestSize={}, aiResponseSize={}",
                requests.size(), aiDataList.size());

        // 3. 저장 (부분 Fallback은 Persistence에서 처리)
        int quizLimit = Math.min(requests.size(), MATCHING_QUIZ_COUNT);
        List<QuizResult> savedList = quizPersistenceService.saveMatchingQuizzes(
                sessionId, userId, requests, aiDataList, quizLimit);

        // 4. 응답 DTO 변환
        return savedList.stream()
                .map(saved -> QuizDetailResponse.builder()
                        .quizId(saved.getId())
                        .quizType(saved.getQuizType())
                        .content(saved.getContent())
                        .translation(saved.getTranslation())
                        .question(saved.getQuestion())
                        .answer(saved.getCorrectAnswer())
                        .videoTimeStamp(saved.getVideoTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================== 퀴즈 제출 ====================

    @Transactional
    public QuizSubmitResponse submitQuiz(Long userId, QuizSubmitRequest request) {

        QuizResult quizResult = quizResultRepository.findById(request.getQuizId())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean isCorrect = checkAnswer(quizResult.getCorrectAnswer(), request.getUserAnswer());
        int earnedExp = isCorrect ? CORRECT_EXP : 0;

        if (isCorrect) {
            user.addExp(earnedExp);
        }

        quizResult.submitAnswer(request.getUserAnswer(), isCorrect, earnedExp);

        return mapToQuizSubmitResponse(quizResult, user.getExp());
    }

    // ==================== 유틸리티 메서드 ====================

    private boolean checkAnswer(String correctAnswer, String userAnswer) {
        if (correctAnswer == null || userAnswer == null) {
            return false;
        }
        return correctAnswer.trim().equalsIgnoreCase(userAnswer.trim());
    }

    private QuizDetailResponse mapToQuizDetailResponse(QuizResult result, List<String> options) {
        return QuizDetailResponse.builder()
                .quizId(result.getId())
                .quizType(result.getQuizType())
                .content(result.getContent())
                .translation(result.getTranslation())
                .question(result.getQuestion())
                .options(options)
                .videoTimeStamp(result.getVideoTimestamp())
                .build();
    }

    private QuizSubmitResponse mapToQuizSubmitResponse(QuizResult quizResult, Integer currentExp) {
        return QuizSubmitResponse.builder()
                .isCorrect(quizResult.getIsCorrect())
                .correctAnswer(quizResult.getCorrectAnswer())
                .earnedExp(quizResult.getEarnedExp())
                .feedback(quizResult.getIsCorrect()
                        ? quizResult.getCorrectFeedback()
                        : quizResult.getWrongFeedback())
                .explanation(quizResult.getExplanation())
                .currentExp(currentExp)
                .build();
    }
}