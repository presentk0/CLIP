package com.clip.server.user.entity.preference;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DifficultyLevel {

    CURRENT("지금이 좋아요"),
    RELAXED("여유롭게 갈게요"),
    EASIER("지금보다 가볍게 갈게요"),
    HARDER("한 발 더 내딛어볼게요"),
    MAX("전력으로 해볼게요");

    private final String description;
}
