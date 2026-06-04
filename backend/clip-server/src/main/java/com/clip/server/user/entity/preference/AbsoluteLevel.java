package com.clip.server.user.entity.preference;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AbsoluteLevel {
    BEGINNER("초보자", 1),
    INTERMEDIATE("중급자", 2),
    ADVANCED("상급자", 3);

    private final String description;
    private final int score;
}
