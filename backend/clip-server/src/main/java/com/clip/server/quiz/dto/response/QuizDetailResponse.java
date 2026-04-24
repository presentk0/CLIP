package com.clip.server.quiz.dto.response;

import com.clip.server.quiz.entity.QuizType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizDetailResponse {

    private Long quizId; // quiz_result id
    private QuizType quizType;
    private String question;
    private String videoTimeStamp; // 다시 듣기용 비디오 타임스탬프

}
