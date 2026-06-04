package com.clip.server.user.entity.preference;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LearningGoal {

    TRAVEL("여행"),
    BUSINESS("비즈니스"),
    SELF_DEVELOPMENT("자기계발"),
    EXAM("영어시험"),
    DAILY("일상"),
    NONE("없음");

    private final String description;
}
