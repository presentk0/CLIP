package com.clip.server.quiz.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizSubmitRequest {

    private Long sessionId;
    private Long quizId; // QuizResult id
    private String userAnswer; // 사용자가 선택한 O/X 또는 입력한 단어, 매칭된 뜻

}
