package com.clip.server.video.analyzer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CefrLevel {
    A1(1),
    A2(2),
    B1(3),
    B2(4),
    C1(5),
    C2(6);

    private final int score;
}
