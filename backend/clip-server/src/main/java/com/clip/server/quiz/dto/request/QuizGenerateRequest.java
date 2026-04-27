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
public class QuizGenerateRequest {

    private String videoId;
    private Integer sectionNumber;
    private SessionType sessionType;
}
