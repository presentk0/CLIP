package com.clip.server.quiz.dto.request;

import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class QuizWordRequest {

    private String word;
    private String meaning;
    private String videoTimeStamp;
}
