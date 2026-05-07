package com.clip.server.quiz.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuizType {
    OX("OX 퀴즈"),
    BLANK("빈칸 채우기"),
    MATCHING("매칭 퀴즈");

    private final String description;
}
