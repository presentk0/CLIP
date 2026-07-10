package com.clip.server.quiz.agent;

import com.clip.server.chat.entity.UserWeakness;
import com.clip.server.chat.repository.UserWeaknessRepository;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.agent.dto.UserLearningState;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.preference.UserPreference;
import com.clip.server.user.repository.UserPreferenceRepository;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.clip.server.word.entity.WordType;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * [Agent Layer 1] User State Analyzer
 * 사용자의 학습 상태를 통합 분석하여 UserLearningState로 반환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserStateAnalyzer {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final QuizResultRepository quizResultRepository;
    private final CollectedWordRepository collectedWordRepository;
    private final UserWeaknessRepository userWeaknessRepository;

    @Transactional(readOnly = true)
    public UserLearningState analyze(Long userId) {
        log.debug("[Agent] User state analysis start - userId: {}", userId);

        // 1. 사용자 프로필 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        UserPreference userPreference = userPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        // 2. 퀴즈 이력
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        // 최근 30개 퀴즈 정답률 (0.0 ~ 1.0), 이력이 없으면 0.0으로 초기화
        Double recentAccuracy = Optional.ofNullable(
                quizResultRepository.findRecentAccuracy(userId)
        ).orElse(0.0);

        // 취약 단어: 정답률 50% 미만 & 최소 2회 이상 출제된 단어
        List<String> weakWords = quizResultRepository.findWeakWords(userId);

        // 최근 7일 내 오답 단어 (중복 제거)
        List<String> recentWrongWords = quizResultRepository.findRecentWrongWords(userId, weekAgo);

        // 마지막 퀴즈 이후 경과일
        // 한 번도 퀴즈를 안 풀었다면 매우 오래된 것으로 처리 (999)
        Integer daysSince = Optional.ofNullable(
                quizResultRepository.findDaysSinceLastQuiz(userId)
        ).orElse(999);

        // 3. 수집 활동
        // 사용자가 수집(COLLECT)은 했지만 퀴즈에는 한 번도 안 나온 단어
        // → 첫 학습이 필요한 단어 후보
        List<String> neverTestedWords = collectedWordRepository.findNeverTestedWords(userId, WordType.COLLECT);

        // 총 수집 단어 수 (통계용, 학습 활동성 지표)
        Long totalCollected = collectedWordRepository.countByUserIdAndWordType(userId, WordType.COLLECT);

        // 4. 채팅 약점
        // AI 채팅에서 사용자가 어색한 표현을 사용한 이력 조회
        // review_count < 3 인 것만 = 아직 극복 못한 표현
        // → 이후 퀴즈에 자연스럽게 녹여넣어 크로스 도메인 학습 루프 구현
        List<UserWeakness> weaknesses = userWeaknessRepository.findUnresolvedWeaknesses(userId);
        List<String> chatWeakExpressions = weaknesses.stream()
                .map(UserWeakness::getWeakExpression)
                .toList();

        // 5. State 조립
        UserLearningState state = UserLearningState.builder()
                .userId(userId) // 사용자 프로필
                .userLevel(user.getLevel())
                .absoluteLevel(userPreference.getUserAbsoluteLevel())
                .learningGoal(userPreference.getLearningGoal())
                .recentAccuracy(recentAccuracy) // 퀴즈 이력
                .weakWords(weakWords)
                .recentWrongWords(recentWrongWords)
                .neverTestedWords(neverTestedWords) // 수집 활동
                .totalCollectedWords(totalCollected != null ? totalCollected.intValue() : 0)
                .chatWeakExpressions(chatWeakExpressions) // 채팅 약점
                .daysSinceLastQuiz(daysSince)
                .build();

        log.info("[Agent] User state analyzed - userId: {}, accuracy: {}, weak: {}, chatWeak: {}",
                userId, recentAccuracy, weakWords.size(), chatWeakExpressions.size());

        return state;
    }
}