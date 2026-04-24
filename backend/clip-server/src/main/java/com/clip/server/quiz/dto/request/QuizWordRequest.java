package com.clip.server.quiz.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizWordRequest {

    private String word;
    private String meaning;
    private String videoTimeStamp;
}
