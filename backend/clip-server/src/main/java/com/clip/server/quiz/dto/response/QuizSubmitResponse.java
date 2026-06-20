package com.clip.server.quiz.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizSubmitResponse {

    private boolean isCorrect;
    private Integer earnedExp;     // 이번 문제로 얻은 경험치
    private Integer currentExp;    // 유저의 현재 총 경험치
    private String feedback;       // 정오답 여부에 따라 캐릭터가 맞는 반응 피드백을 제공
    private String explanation;    // 퀴즈 상세 해설
    private String relatedExpressions; // 함께 알아두면 좋은 표현
    private String correctAnswer;
}
