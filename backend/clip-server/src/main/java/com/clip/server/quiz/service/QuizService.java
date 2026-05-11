package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizSubmitRequest;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.OpenAIQuizDataResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizSubmitResponse;
import com.clip.server.quiz.entity.QuizResult;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizType;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final OpenAIService openAIService;
    private final QuizFallbackService quizFallbackService;
    private final QuizResultRepository quizResultRepository;
    private final UserRepository userRepository;
    private final QuizSessionRepository quizSessionRepository;

    private static final int CORRECT_EXP = 100;
    private static final int MATCHING_QUIZ_COUNT = 5;

    private static final List<String> CORRECT_FEEDBACKS = List.of(
            "맞았어요.",
            "정답이에요. 다음도 볼까요?",
            "문맥 잘 보셨어요.",
            "좋아요. 이어서 가볼까요?",
            "잘 찾으셨어요."
    );

    private static final List<String> WRONG_SOFT = List.of(
            "괜찮아요. 다시 보면 보여요.",
            "조금 헷갈릴 수 있어요.",
            "이 부분이 까다로워요.",
            "괜찮아요. 낯설 수 있어요."
    );

    private static final List<String> WRONG_GUIDE = List.of(
            "다시 한 번 볼까요?",
            "같이 다시 살펴볼까요?",
            "문맥을 다시 보면 힌트가 있어요.",
            "다른 선택지와 쓰임이 달라요."
    );

    // ==================== OX 퀴즈 생성 ====================

    @Transactional
    public QuizDetailResponse createOXQuiz(Long sessionId, Long userId, QuizWordRequest request) {

        // 1. AI 시도
        OpenAIQuizDataResponse aiData = null;
        try {
            aiData = openAIService.generateOXQuiz(request.getWord(), request.getMeaning());
        } catch (Exception e) {
            log.warn("OX 퀴즈 AI 호출 예외 - word: {}", request.getWord(), e);
        }

        // 2. AI 실패 또는 유효하지 않으면 Fallback
        if (aiData == null || !isValidOXQuiz(aiData)) {
            log.warn("OX 퀴즈 AI 실패, Fallback 사용 - word: {}", request.getWord());
            return quizFallbackService.createLocalOXQuiz(sessionId, userId, request);
        }

        // 3. AI 성공 시 저장
        return saveOXQuiz(sessionId, userId, request, aiData);
    }

    /**
     * OX 퀴즈 유효성 검사
     */
    private boolean isValidOXQuiz(OpenAIQuizDataResponse aiData) {
        return aiData.getContent() != null && !aiData.getContent().isBlank()
                && aiData.getAnswer() != null && !aiData.getAnswer().isBlank()
                && aiData.getTranslation() != null && !aiData.getTranslation().isBlank()
                && !aiData.getContent().matches(".*[가-힣].*");
    }

    /**
     * OX 퀴즈 저장 (AI 성공 시)
     */
    private QuizDetailResponse saveOXQuiz(Long sessionId, Long userId,
                                          QuizWordRequest request, OpenAIQuizDataResponse aiData) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        QuizResult result = QuizResult.builder()
                .quizSession(session)
                .user(user)
                .word(aiData.getWord() != null ? aiData.getWord() : request.getWord())
                .quizType(QuizType.OX)
                .content(safe(aiData.getContent()))
                .translation(safe(aiData.getTranslation()))
                .question(safe(aiData.getQuestion()))
                .correctAnswer(safe(aiData.getAnswer()))
                .explanation(safe(aiData.getExplanation()))
                .videoTimestamp(request.getVideoTimeStamp())
                .correctFeedback(safeFeedback(aiData.getCorrectFeedback(), true))
                .wrongFeedback(safeFeedback(aiData.getWrongFeedback(), false))
                .build();

        QuizResult saved = quizResultRepository.save(result);
        return mapToQuizDetailResponse(saved, null);
    }

    // ==================== 빈칸 퀴즈 생성 ====================

    @Transactional
    public QuizDetailResponse createBlankQuiz(Long sessionId, Long userId, QuizWordRequest request) {

        // 1. AI 시도
        OpenAIQuizDataResponse aiData = null;
        try {
            aiData = openAIService.generateBlankQuiz(request.getWord(), request.getMeaning());
        } catch (Exception e) {
            log.warn("빈칸 퀴즈 AI 호출 예외 - word: {}", request.getWord(), e);
        }

        // 2. AI 실패 또는 유효하지 않으면 Fallback
        if (aiData == null || !isValidBlankQuiz(aiData)) {
            log.warn("빈칸 퀴즈 AI 실패, Fallback 사용 - word: {}", request.getWord());
            return quizFallbackService.createLocalBlankQuiz(sessionId, userId, request);
        }

        // 3. AI 성공 시 저장
        return saveBlankQuiz(sessionId, userId, request, aiData);
    }

    /**
     * 빈칸 퀴즈 유효성 검사
     */
    private boolean isValidBlankQuiz(OpenAIQuizDataResponse aiData) {
        return aiData.getContent() != null && aiData.getContent().contains("[ ]")
                && aiData.getAnswer() != null && !aiData.getAnswer().isBlank()
                && aiData.getOptions() != null && aiData.getOptions().size() == 4
                && aiData.getTranslation() != null && !aiData.getTranslation().isBlank()
                // content에 한글이 포함되면 무효
                && !aiData.getContent().matches(".*[가-힣].*");
    }

    /**
     * 빈칸 퀴즈 저장 (AI 성공 시)
     */
    private QuizDetailResponse saveBlankQuiz(Long sessionId, Long userId,
                                             QuizWordRequest request, OpenAIQuizDataResponse aiData) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        QuizResult result = QuizResult.builder()
                .quizSession(session)
                .user(user)
                .word(aiData.getWord() != null ? aiData.getWord() : request.getWord())
                .quizType(QuizType.BLANK)
                .content(safe(aiData.getContent()))
                .translation(safe(aiData.getTranslation()))
                .question(safe(aiData.getQuestion()))
                .correctAnswer(safe(aiData.getAnswer()))
                .explanation(safe(aiData.getExplanation()))
                .videoTimestamp(request.getVideoTimeStamp())
                .correctFeedback(safeFeedback(aiData.getCorrectFeedback(), true))
                .wrongFeedback(safeFeedback(aiData.getWrongFeedback(), false))
                .build();

        QuizResult saved = quizResultRepository.save(result);
        return mapToQuizDetailResponse(saved, aiData.getOptions());
    }

    // ==================== 매칭 퀴즈 생성 ====================

    @Transactional
    public List<QuizDetailResponse> createMatchingQuiz(Long sessionId, Long userId, List<QuizWordRequest> requests) {

        // 1. AI 시도
        List<Map<String, String>> wordList = requests.stream()
                .map(r -> Map.of("word", r.getWord(), "meaning", r.getMeaning()))
                .collect(Collectors.toList());

        List<OpenAIQuizDataResponse> aiDataList = null;
        try {
            aiDataList = openAIService.generateMatchingQuiz(wordList);
        } catch (Exception e) {
            log.warn("매칭 퀴즈 AI 호출 예외", e);
        }

        // 2. AI 완전 실패 시 전체 Fallback
        if (aiDataList == null || aiDataList.isEmpty()) {
            log.warn("매칭 퀴즈 AI 전체 실패, Fallback 사용 - requestSize: {}", requests.size());
            return quizFallbackService.createLocalMatchingQuiz(sessionId, userId, requests);
        }

        log.info("매칭 퀴즈 생성 완료 - requestSize={}, aiResponseSize={}",
                requests.size(), aiDataList.size());

        // 3. 개별 항목 검증 및 부분 Fallback
        return saveMatchingQuizWithFallback(sessionId, userId, requests, aiDataList);
    }

    /**
     * 매칭 퀴즈 저장 (부분 Fallback 포함)
     */
    private List<QuizDetailResponse> saveMatchingQuizWithFallback(
            Long sessionId, Long userId,
            List<QuizWordRequest> requests, List<OpenAIQuizDataResponse> aiDataList) {

        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int quizLimit = Math.min(requests.size(), MATCHING_QUIZ_COUNT);

        return IntStream.range(0, quizLimit).mapToObj(i -> {
            QuizWordRequest req = requests.get(i);
            OpenAIQuizDataResponse aiData = (i < aiDataList.size()) ? aiDataList.get(i) : null;

            QuizResult result;

            // AI 데이터 유효성 검사
            if (aiData != null && isValidMatchingQuiz(aiData)) {
                // AI 성공
                result = QuizResult.builder()
                        .quizSession(session)
                        .user(user)
                        .word(aiData.getWord())
                        .quizType(QuizType.MATCHING)
                        .content(safe(aiData.getContent()))
                        .translation(safe(aiData.getTranslation()))
                        .question(safe(aiData.getQuestion()))
                        .correctAnswer(safe(aiData.getAnswer()))
                        .explanation(safe(aiData.getExplanation()))
                        .videoTimestamp(req.getVideoTimeStamp())
                        .correctFeedback(safeFeedback(aiData.getCorrectFeedback(), true))
                        .wrongFeedback(safeFeedback(aiData.getWrongFeedback(), false))
                        .build();
            } else {
                // 부분 Fallback (해당 인덱스만)
                log.warn("매칭 퀴즈 인덱스 {} AI 데이터 무효 - 로컬 보충", i);
                result = createLocalMatchingResult(session, user, req);
            }

            QuizResult saved = quizResultRepository.save(result);

            return QuizDetailResponse.builder()
                    .quizId(saved.getId())
                    .quizType(saved.getQuizType())
                    .content(saved.getContent())
                    .translation(saved.getTranslation())
                    .question(saved.getQuestion())
                    .answer(saved.getCorrectAnswer())
                    .videoTimeStamp(saved.getVideoTimestamp())
                    .build();

        }).collect(Collectors.toList());
    }

    /**
     * 매칭 퀴즈 유효성 검사
     */
    private boolean isValidMatchingQuiz(OpenAIQuizDataResponse aiData) {
        return aiData.getWord() != null && !aiData.getWord().isBlank()
                && aiData.getQuestion() != null && !aiData.getQuestion().isBlank()
                && aiData.getAnswer() != null && !aiData.getAnswer().isBlank();
    }

    /**
     * 매칭 퀴즈 로컬 생성 (부분 Fallback용)
     */
    private QuizResult createLocalMatchingResult(QuizSession session, User user, QuizWordRequest req) {
        String meaning = (req.getMeaning() != null && !req.getMeaning().isBlank())
                ? req.getMeaning() : "뜻";

        return QuizResult.builder()
                .quizSession(session)
                .user(user)
                .word(req.getWord())
                .quizType(QuizType.MATCHING)
                .content("")
                .translation("")
                .question(req.getWord())
                .correctAnswer(shortenMeaning(meaning))
                .explanation(String.format("'%s'는 '%s'라는 뜻이에요.", req.getWord(), meaning))
                .videoTimestamp(req.getVideoTimeStamp())
                .correctFeedback(getRandom(CORRECT_FEEDBACKS))
                .wrongFeedback(generateWrongFallback())
                .build();
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

    private String safe(String s) {
        return (s == null || s.isBlank()) ? "" : s;
    }

    private String safeFeedback(String feedback, boolean isCorrect) {
        if (feedback != null && !feedback.isBlank()) {
            return feedback.trim();
        }
        return isCorrect ? getRandom(CORRECT_FEEDBACKS) : generateWrongFallback();
    }

    private String generateWrongFallback() {
        return getRandom(WRONG_SOFT) + " " + getRandom(WRONG_GUIDE);
    }

    private String getRandom(List<String> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }

    private String shortenMeaning(String meaning) {
        if (meaning == null || meaning.isBlank()) return "뜻";
        if (meaning.length() <= 5) return meaning;
        String shortened = meaning.split("[,\\(]")[0].trim();
        return shortened.length() > 5 ? shortened.substring(0, 5) : shortened;
    }
}