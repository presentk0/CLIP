package com.clip.server.quiz.persistence;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.OpenAIQuizDataResponse;
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
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 퀴즈 DB 저장 전담 서비스
 * - AI 호출이 끝난 데이터를 받아 DB에 저장하는 짧은 트랜잭션만 담당
 * - 트랜잭션 경계를 명확히 분리하여 외부 API 호출 시 DB 커넥션 점유 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizPersistenceService {

    private final QuizResultRepository quizResultRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository;

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

    // ==================== OX 퀴즈 저장 ====================

    @Transactional
    public QuizResult saveOXQuiz(Long sessionId, Long userId,
                                 QuizWordRequest request, OpenAIQuizDataResponse aiData) {
        QuizSession session = findSession(sessionId);
        User user = findUser(userId);

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
                .relatedExpressions(aiData.getRelatedExpressions())
                .videoTimestamp(request.getVideoTimeStamp())
                .correctFeedback(safeFeedback(aiData.getCorrectFeedback(), true))
                .wrongFeedback(safeFeedback(aiData.getWrongFeedback(), false))
                .build();

        return quizResultRepository.save(result);
    }

    // ==================== 빈칸 퀴즈 저장 ====================

    @Transactional
    public QuizResult saveBlankQuiz(Long sessionId, Long userId,
                                    QuizWordRequest request, OpenAIQuizDataResponse aiData) {
        QuizSession session = findSession(sessionId);
        User user = findUser(userId);

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
                .relatedExpressions(aiData.getRelatedExpressions())
                .videoTimestamp(request.getVideoTimeStamp())
                .correctFeedback(safeFeedback(aiData.getCorrectFeedback(), true))
                .wrongFeedback(safeFeedback(aiData.getWrongFeedback(), false))
                .build();

        return quizResultRepository.save(result);
    }

    // ==================== 매칭 퀴즈 저장 (부분 Fallback 포함) ====================

    @Transactional
    public List<QuizResult> saveMatchingQuizzes(Long sessionId, Long userId,
                                                List<QuizWordRequest> requests,
                                                List<OpenAIQuizDataResponse> aiDataList,
                                                int quizLimit) {
        QuizSession session = findSession(sessionId);
        User user = findUser(userId);

        return IntStream.range(0, quizLimit).mapToObj(i -> {
            QuizWordRequest req = requests.get(i);
            OpenAIQuizDataResponse aiData = (i < aiDataList.size()) ? aiDataList.get(i) : null;

            QuizResult result;
            if (aiData != null && isValidMatchingQuiz(aiData)) {
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
                        .relatedExpressions(aiData.getRelatedExpressions())
                        .videoTimestamp(req.getVideoTimeStamp())
                        .correctFeedback(safeFeedback(aiData.getCorrectFeedback(), true))
                        .wrongFeedback(safeFeedback(aiData.getWrongFeedback(), false))
                        .build();
            } else {
                log.warn("매칭 퀴즈 인덱스 {} AI 데이터 무효 - 로컬 보충", i);
                result = createLocalMatchingResult(session, user, req);
            }

            return quizResultRepository.save(result);
        }).collect(Collectors.toList());
    }

    // ==================== 내부 헬퍼 ====================

    private QuizSession findSession(Long sessionId) {
        return quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private boolean isValidMatchingQuiz(OpenAIQuizDataResponse aiData) {
        return aiData.getWord() != null && !aiData.getWord().isBlank()
                && aiData.getQuestion() != null && !aiData.getQuestion().isBlank()
                && aiData.getAnswer() != null && !aiData.getAnswer().isBlank();
    }

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