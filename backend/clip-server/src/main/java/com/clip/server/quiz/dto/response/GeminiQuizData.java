package com.clip.server.quiz.dto.response;

import com.clip.server.quiz.entity.QuizType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiQuizData {

    private String word; // 퀴즈의 핵심 단어
    private QuizType quizType; // OX, BLANK, MATCHING
    private String question; // 질문
    private String answer; // 정답
    private String explanation; // 퀴즈 해설
}
