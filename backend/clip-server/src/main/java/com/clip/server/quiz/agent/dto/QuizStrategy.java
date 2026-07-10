package com.clip.server.quiz.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizStrategy {

    @JsonProperty("quiz_type")
    private String quizType;      // "OX_HEAVY" | "BLANK_HEAVY" | "BALANCED"

    @JsonProperty("difficulty")
    private String difficulty;    // "EASY" | "MEDIUM" | "HARD"

    @JsonProperty("focus")
    private String focus;         // "WEAK_WORDS" | "NEW_WORDS" | "CHAT_WEAKNESS" | "MIXED"

    @JsonProperty("word_count")
    private Integer wordCount;

    @JsonProperty("reason")
    private String reason;

    // 규칙 기반 폴백
    public static QuizStrategy fallback(Double accuracy, int weakCount) {
        String difficulty;
        String focus;

        if (accuracy == null || accuracy < 0.5) {
            difficulty = "EASY";
            focus = "WEAK_WORDS";
        } else if (accuracy > 0.8) {
            difficulty = "HARD";
            focus = "NEW_WORDS";
        } else {
            difficulty = "MEDIUM";
            focus = "MIXED";
        }

        return QuizStrategy.builder()
                .quizType("BALANCED")
                .difficulty(difficulty)
                .focus(focus)
                .wordCount(3)
                .reason("규칙 기반 폴백 전략")
                .build();
    }
}