package com.clip.server.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSubmitResponse {

    private boolean isCorrect;
    private Integer earnedExp;     // 이번 문제로 얻은 경험치
    private Integer currentExp;    // 유저의 현재 총 경험치
    private String feedback;       // "정답이야! ~이야" 형태의 상세 해설 (explanation)
    private String correctAnswer;
}
