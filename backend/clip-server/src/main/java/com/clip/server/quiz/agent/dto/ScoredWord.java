package com.clip.server.quiz.agent.dto;

import com.clip.server.quiz.entity.QuizSessionWord;
import com.clip.server.word.entity.WordType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ScoredWord {

    private QuizSessionWord word;    // 원본 QuizSessionWord
    private int score;                // 총점
    private List<String> reasons;     // 선택 근거 (설명 가능성)

    // 편의 메서드
    public String getWordText() {
        return word.getWord();
    }

    public WordType getWordType() {
        return word.getWordType();
    }
}