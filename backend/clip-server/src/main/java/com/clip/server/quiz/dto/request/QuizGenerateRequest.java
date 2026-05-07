package com.clip.server.quiz.dto.request;

import com.clip.server.quiz.entity.SessionType;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuizGenerateRequest {

    private String videoId;
    private Integer sectionNumber;
    private SessionType sessionType;
}
