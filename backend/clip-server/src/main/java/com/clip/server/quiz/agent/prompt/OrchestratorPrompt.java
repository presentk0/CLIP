package com.clip.server.quiz.agent.prompt;

import com.clip.server.quiz.agent.dto.UserLearningState;

public class OrchestratorPrompt {

    // 프롬프트 생성 방지용 private 생성자
    private OrchestratorPrompt() {}

    /**
     * 사용자 학습 상태를 LLM에게 전달하고 퀴즈 전략을 요청하는 프롬프트
     */
    public static String build(UserLearningState state) {
        return String.format("""
                You are CLIPZY's learning strategy agent for English learners.
                Analyze the user's current learning state and decide the optimal quiz strategy.

                === USER LEARNING STATE ===
                - Absolute Level: %s
                - Learning Goal: %s
                - Recent Accuracy: %.2f (0.0 ~ 1.0)
                - Total Collected Words: %d
                - Weak Words Count: %d
                - Recent Wrong Words Count: %d
                - Unresolved Chat Weaknesses: %d
                - Days Since Last Quiz: %d

                === DECISION RULES ===
                1. If accuracy < 0.5 → quiz_type=OX_HEAVY, difficulty=EASY, focus=WEAK_WORDS
                   (Bottom quartile of real users, confidence recovery needed)
                2. If accuracy between 0.5 and 0.85 → quiz_type=BALANCED, difficulty=MEDIUM
                   (Middle 50% of real users, balanced learning)
                3. If accuracy > 0.85 → quiz_type=BLANK_HEAVY, difficulty=HARD, focus=NEW_WORDS
                   (Top quartile of real users, challenge needed)
                4. If days_since > 7 → prioritize WEAK_WORDS (review needed)
                5. If unresolved_chat_weaknesses >= 3 → focus=CHAT_WEAKNESS
                6. Adjust word_count based on user's state (3-5 range)
                
                === OUTPUT ===
                Respond ONLY with valid JSON (no markdown, no explanation).
                {
                  "quiz_type": "OX_HEAVY" | "BLANK_HEAVY" | "BALANCED",
                  "difficulty": "EASY" | "MEDIUM" | "HARD",
                  "focus": "WEAK_WORDS" | "NEW_WORDS" | "CHAT_WEAKNESS" | "MIXED",
                  "word_count": 3,
                  "reason": "왜 이 전략을 선택했는지 한국어로 설명 (100자 이내)"
                }
                """,
                state.getAbsoluteLevel(),
                state.getLearningGoal(),
                state.getRecentAccuracy() != null ? state.getRecentAccuracy() : 0.0,
                state.getTotalCollectedWords() != null ? state.getTotalCollectedWords() : 0,
                state.getWeakWords() != null ? state.getWeakWords().size() : 0,
                state.getRecentWrongWords() != null ? state.getRecentWrongWords().size() : 0,
                state.getChatWeakExpressions() != null ? state.getChatWeakExpressions().size() : 0,
                state.getDaysSinceLastQuiz() != null ? state.getDaysSinceLastQuiz() : 999
        );
    }
}