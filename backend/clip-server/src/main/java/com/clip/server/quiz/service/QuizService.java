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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.ThreadLocalRandom;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final OpenAIService openAIService;
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

    /**
     * OX 퀴즈 생성 및 저장
     */
    @Transactional
    public QuizDetailResponse createOXQuiz(Long sessionId, Long userId, QuizWordRequest request) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. OpenAI를 통한 OX 퀴즈 데이터 생성
        OpenAIQuizDataResponse aiData;

        try {
            aiData = openAIService.generateOXQuiz(
                    request.getWord(),
                    request.getMeaning()
            );

        } catch (Exception e) {

            log.error("### OX 퀴즈 생성 실패", e);

            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        if (aiData == null) {
            log.error("### OX 퀴즈 응답 NULL");
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 2. 결과 엔티티 빌드 (AI 응답 단어가 누락된 경우 요청 단어 사용)
        QuizResult result = QuizResult.builder()
                .quizSession(session)
                .user(user)
                .word(aiData.getWord() != null ? aiData.getWord() : request.getWord())
                .quizType(QuizType.OX)
                .content(safe(aiData.getContent()))       // 영어 예문
                .translation(safe(aiData.getTranslation())) // 한글 해석
                .question(safe(aiData.getQuestion()))     // 캐릭터 가이드 질문
                .correctAnswer(safe(aiData.getAnswer()))
                .explanation(safe(aiData.getExplanation())) // 친절한 뉘앙스 해설
                .videoTimestamp(request.getVideoTimeStamp())
                .correctFeedback(safeFeedback(aiData.getCorrectFeedback(), true))
                .wrongFeedback(safeFeedback(aiData.getWrongFeedback(), false))
                .build();

        QuizResult saved = quizResultRepository.save(result);

        // 3. 프론트엔드용 DTO 매핑
        return mapToQuizDetailResponse(saved, null);
    }

    /**
     * 빈칸 채우기 퀴즈 생성 및 저장 (4지선다형)
     */
    @Transactional
    public QuizDetailResponse createBlankQuiz(Long sessionId, Long userId, QuizWordRequest request) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. OpenAI를 통한 빈칸 퀴즈 데이터 생성 (options 포함)
        OpenAIQuizDataResponse aiData = openAIService.generateBlankQuiz(request.getWord(), request.getMeaning());
        if (aiData == null) throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);

        // 2. 결과 엔티티 빌드
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

        // 3. 프론트엔드용 DTO 매핑 (options 리스트 포함)
        return mapToQuizDetailResponse(saved, aiData.getOptions());
    }

    /**
     * 매칭 퀴즈 세트 생성 및 저장
     */
    @Transactional
    public List<QuizDetailResponse> createMatchingQuiz(Long sessionId, Long userId, List<QuizWordRequest> requests) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. OpenAI 요청용 리스트 구성
        List<Map<String, String>> wordList = requests.stream()
                .map(r -> Map.of("word", r.getWord(), "meaning", r.getMeaning()))
                .collect(Collectors.toList());

        // 2. OpenAI를 통한 매칭 데이터 세트 생성
        List<OpenAIQuizDataResponse> aiDataList = openAIService.generateMatchingQuiz(wordList);

        log.info(
                "매칭 퀴즈 생성 완료 - requestSize={}, aiResponseSize={}",
                requests.size(),
                aiDataList != null ? aiDataList.size() : 0
        );

        if (aiDataList == null || aiDataList.isEmpty()) {
            log.error(
                    "매칭 퀴즈 AI 생성 실패 - sessionId={}, userId={}, requestSize={}",
                    sessionId,
                    userId,
                    requests.size()
            );
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        int quizLimit = Math.min(requests.size(), MATCHING_QUIZ_COUNT);

        return IntStream.range(0, quizLimit).mapToObj(i -> {
            QuizWordRequest req = requests.get(i);

            // AI가 해당 인덱스의 데이터를 줬는지 확인
            OpenAIQuizDataResponse aiData = (i < aiDataList.size()) ? aiDataList.get(i) : null;

            QuizResult result;
            if (aiData != null
                    && aiData.getWord() != null
                    && !aiData.getWord().isBlank()) {
                // AI 데이터가 존재할 때
                result = QuizResult.builder()
                        .quizSession(session).user(user).word(aiData.getWord())
                        .quizType(QuizType.MATCHING).question(safe(aiData.getQuestion()))
                        .content(safe(aiData.getContent())).translation(safe(aiData.getTranslation()))
                        .correctAnswer(safe(aiData.getAnswer())).explanation(safe(aiData.getExplanation()))
                        .videoTimestamp(req.getVideoTimeStamp())
                        .correctFeedback(safeFeedback(aiData.getCorrectFeedback(), true))
                        .wrongFeedback(safeFeedback(aiData.getWrongFeedback(), false)).build();
            } else {

                // AI 데이터가 누락되었을 때 (로컬 데이터 보충)
                log.warn("매칭 퀴즈 인덱스 {} 데이터 누락 - 로컬 보충 가동", i);
                result = QuizResult.builder()
                        .quizSession(session).user(user).word(req.getWord())
                        .quizType(QuizType.MATCHING).question(req.getWord())
                        .correctAnswer(req.getMeaning()).explanation(req.getWord() + "의 뜻은 '" + req.getMeaning() + "'입니다.")
                        .videoTimestamp(req.getVideoTimeStamp())
                        .correctFeedback(getRandom(CORRECT_FEEDBACKS))
                        .wrongFeedback(generateWrongFallback()).build();
            }

            QuizResult saved = quizResultRepository.save(result);
            return QuizDetailResponse.builder()
                    .quizId(saved.getId()).quizType(saved.getQuizType())
                    .content(saved.getContent()).translation(saved.getTranslation())
                    .question(saved.getQuestion()).answer(saved.getCorrectAnswer())
                    .videoTimeStamp(saved.getVideoTimestamp()).build();
        }).collect(Collectors.toList());
    }

    // 퀴즈 제출 로직
    @Transactional
    public QuizSubmitResponse submitQuiz(Long userId, QuizSubmitRequest request) {

        // 퀴즈 세션, 사용자 정보 확인
        QuizResult quizResult = quizResultRepository.findById(request.getQuizId())
                .orElseThrow(()-> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean isCorrect = checkAnswer(quizResult.getCorrectAnswer(), request.getUserAnswer());
        int earnedExp = isCorrect? CORRECT_EXP : 0; // 정답이면 exp 오답이면 0

        // 사용자 exp 업데이트
        if(isCorrect) {
            user.addExp(earnedExp);
        }

        // 사용자 답, 정답 여부, 퀴즈를 통해 얻은 exp 저장
        quizResult.submitAnswer(request.getUserAnswer(), isCorrect, earnedExp);

        return mapToQuizSubmitResponse(quizResult, user.getExp());
    }

    // 퀴즈 정답 체크
    private boolean checkAnswer(String correctAnswer, String userAnswer) {
        if (correctAnswer == null || userAnswer == null) {
            return false;
        }
        return correctAnswer.trim()
                .equalsIgnoreCase(userAnswer.trim());
    }

    /**
     *  엔티티 -> 응답 DTO 변환 유틸리티
     */
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
                .feedback(quizResult.getIsCorrect()? quizResult.getCorrectFeedback() : quizResult.getWrongFeedback()) // 정오답 여부에 맞는 피드백 제공
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

        if (isCorrect) {
            return getRandom(CORRECT_FEEDBACKS);
        }

        return generateWrongFallback();
    }

    private String generateWrongFallback() {
        return getRandom(WRONG_SOFT) + " " + getRandom(WRONG_GUIDE);
    }

    private String getRandom(List<String> list) {
        return list.get(
                ThreadLocalRandom.current().nextInt(list.size())
        );

    }

}