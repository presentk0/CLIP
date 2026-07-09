package com.clip.server.quiz.agent;

import com.clip.server.quiz.agent.dto.ScoredWord;
import com.clip.server.quiz.agent.dto.UserLearningState;
import com.clip.server.quiz.entity.QuizSessionWord;
import com.clip.server.word.entity.WordType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * [CLIPZY Agent - Layer 2: Quiz Word Selector]
 * 스코어링 철학:
 *  기존 우선순위 체계(COLLECT, POPUP, SYSTEM)는 타입 기본 점수로 유지
 *  그 위에 사용자 학습 상태를 반영한 개인화 점수를 가산
 *  모든 선택에는 근거(reasons)가 함께 반환됨 → 설명 가능성 확보
 *
 * 스코어링 기준:
 *
 *   [타입 기본 점수]              [개인화 점수]
 *   COLLECT : +100               취약 단어(정답률↓)  : +10
 *   POPUP   : +50                최근 7일 오답        : +7
 *   SYSTEM  : +10                채팅 약점 관련 ⭐    : +8
 *                                미출제 수집 단어     : +5
 *                                복습 시점(3일↑)      : +3
 *
 * @see UserStateAnalyzer
 * @see ScoredWord
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizWordSelector {

    // 타입 기본 점수 (기존 우선순위 유지)
    /** COLLECT 타입 기본 점수 - 사용자가 직접 수집한 단어 (최우선) */
    private static final int SCORE_TYPE_COLLECT = 100;
    /** POPUP 타입 기본 점수 - 사용자가 조회만 한 단어 */
    private static final int SCORE_TYPE_POPUP = 50;
    /** SYSTEM 타입 기본 점수 - 자막에서 자동 추출된 단어 */
    private static final int SCORE_TYPE_SYSTEM = 10;

    // 개인화 점수 (사용자 학습 상태 반영)
    /** 취약 단어 가산점 (정답률 50% 미만) */
    private static final int SCORE_WEAK_WORD = 10;
    /** 채팅 약점 관련 단어 가산점 ⭐ 시그니처 */
    private static final int SCORE_CHAT_WEAKNESS = 8;
    /** 최근 7일 내 오답 단어 가산점 */
    private static final int SCORE_RECENT_WRONG = 7;
    /** 수집했지만 퀴즈 미출제 단어 가산점 */
    private static final int SCORE_NEVER_TESTED = 5;
    /** 마지막 학습 후 오래됨 (복습 필요 시점) 가산점 */
    private static final int SCORE_NEEDS_REVIEW = 3;
    /** 복습 필요로 판단하는 경과일 기준 */
    private static final int DAYS_THRESHOLD_REVIEW = 3;

    /**
     * 후보 단어들을 스코어링하여 정렬된 리스트로 반환합니다.
     *
     * <p>기존 {@code Collections.shuffle() + sort(getPriority)} 로직을 대체합니다.</p>
     *
     * @param candidates 후보 단어 리스트 (QuizSessionWord)
     * @param state 사용자 학습 상태
     * @return 점수 내림차순으로 정렬된 ScoredWord 리스트 (근거 포함)
     */
    public List<ScoredWord> scoreAndSort(
            List<QuizSessionWord> candidates,
            UserLearningState state) {

        if (candidates == null || candidates.isEmpty()) {
            log.warn("[Agent] No candidate words to score");
            return List.of();
        }

        log.debug("[Agent] Scoring start - candidateSize: {}, userId: {}",
                candidates.size(), state.getUserId());

        // 각 후보 단어에 점수 부여 → 내림차순 정렬
        List<ScoredWord> scored = candidates.stream()
                .map(word -> scoreWord(word, state))
                .sorted(Comparator.comparingInt(ScoredWord::getScore).reversed())
                .toList();

        // 상위 몇 개 로그 (디버깅 및 발표 자료용)
        logTopScores(scored);

        return scored;
    }

    /**
     * 단일 단어에 대한 스코어링 로직.
     *
     * @param word 대상 단어
     * @param state 사용자 학습 상태
     * @return 점수와 선택 근거가 담긴 ScoredWord
     */
    private ScoredWord scoreWord(QuizSessionWord word, UserLearningState state) {
        int score = 0;
        List<String> reasons = new ArrayList<>();

        String wordText = word.getWord();

        // 1. 타입 기본 점수 (기존 우선순위 유지)
        // COLLECT > POPUP > SYSTEM 순서는 유지되도록 큰 점수 차이 부여
        int typeScore = getTypeScore(word.getWordType());
        score += typeScore;
        reasons.add(getTypeReason(word.getWordType()));

        // 2. 개인화 점수 (학습 상태 반영)
        // [2-1] 취약 단어 - 정답률이 낮은 단어 우선 복습
        if (state.getWeakWords() != null && state.getWeakWords().contains(wordText)) {
            score += SCORE_WEAK_WORD;
            reasons.add("취약 단어 (정답률 낮음)");
        }

        // [2-2] 최근 오답 - 최근 7일 내 틀린 단어
        if (state.getRecentWrongWords() != null && state.getRecentWrongWords().contains(wordText)) {
            score += SCORE_RECENT_WRONG;
            reasons.add("최근 오답");
        }

        // [2-3] ⭐ 채팅 약점 관련 (CLIPZY 시그니처)
        // AI 채팅에서 어색하게 사용한 표현과 연관된 단어라면 가산점
        // 예: "very like" 약점 → "like", "really" 등이 관련 단어
        if (isRelatedToChatWeakness(wordText, state.getChatWeakExpressions())) {
            score += SCORE_CHAT_WEAKNESS;
            reasons.add("회화에서 어려워한 표현");
        }

        // [2-4] 미출제 단어 - 수집만 하고 퀴즈에 안 나온 단어
        if (state.getNeverTestedWords() != null && state.getNeverTestedWords().contains(wordText)) {
            score += SCORE_NEVER_TESTED;
            reasons.add("첫 학습");
        }

        // [2-5] 복습 필요 시점 - 마지막 학습 후 오래된 경우
        if (state.getDaysSinceLastQuiz() != null
                && state.getDaysSinceLastQuiz() >= DAYS_THRESHOLD_REVIEW) {
            score += SCORE_NEEDS_REVIEW;
            reasons.add("복습 필요 시점");
        }

        return ScoredWord.builder()
                .word(word)
                .score(score)
                .reasons(reasons)
                .build();
    }

    /**
     * 단어 타입에 따른 기본 점수를 반환
     * 기존 getPriority() 로직의 대체 (반대 방향: 큰 값이 우선).
     */
    private int getTypeScore(WordType wordType) {
        return switch (wordType) {
            case COLLECT -> SCORE_TYPE_COLLECT;   // 100
            case POPUP -> SCORE_TYPE_POPUP;       // 50
            case SYSTEM -> SCORE_TYPE_SYSTEM;     // 10
        };
    }

    /**
     * 단어 타입에 대한 사람이 읽을 수 있는 근거 문자열.
     */
    private String getTypeReason(WordType wordType) {
        return switch (wordType) {
            case COLLECT -> "사용자 수집 단어";
            case POPUP -> "관심 표시 단어 (조회)";
            case SYSTEM -> "영상 핵심 단어";
        };
    }

    /**
     * 이 단어가 채팅 약점 표현과 관련이 있는지 판단합니다.
     *
     * 예: 채팅 약점이 "very like coffee"라면,
     * 단어 "like", "very" 등이 관련 단어로 매칭
     *
     * @param word 현재 단어
     * @param chatWeakExpressions 사용자의 극복 못한 채팅 약점 표현들
     * @return 관련 있으면 true
     */
    private boolean isRelatedToChatWeakness(String word, List<String> chatWeakExpressions) {
        if (word == null || chatWeakExpressions == null || chatWeakExpressions.isEmpty()) {
            return false;
        }

        String lowerWord = word.toLowerCase();

        // 채팅 약점 표현들 중 하나라도 이 단어를 포함하고 있으면 관련 있음
        return chatWeakExpressions.stream()
                .filter(expr -> expr != null && !expr.isBlank())
                .anyMatch(expr -> expr.toLowerCase().contains(lowerWord));
    }

    /**
     * 상위 스코어 로그 출력 (디버깅 및 발표 자료용).
     */
    private void logTopScores(List<ScoredWord> scored) {
        int topN = Math.min(scored.size(), 5);
        for (int i = 0; i < topN; i++) {
            ScoredWord sw = scored.get(i);
            log.info("[Agent] #{} '{}' score={} reasons={}",
                    i + 1,
                    sw.getWord().getWord(),
                    sw.getScore(),
                    sw.getReasons());
        }
    }
}