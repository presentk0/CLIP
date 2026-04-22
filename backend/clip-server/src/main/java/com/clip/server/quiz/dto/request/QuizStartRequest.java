package com.clip.server.quiz.dto.request;

import com.clip.server.quiz.entity.SessionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizStartRequest {

    private Long userId;
    private String videoId;
    private SessionType sessionType;
}
