package com.clip.server.video.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum VideoDifficulty {
    // 영상의 절대적 난이도
    BEGINNER("초급",1),
    INTERMEDIATE("중급",2),
    ADVANCED("고급",3);

    private final String description;
    private final int levelCode;

    /**
     * 현재 난이도 이하의 모든 난이도 리스트를 반환 (추천 범위 확장용)
     */
    public List<VideoDifficulty> getRecommendationRange() {
        return switch (this) {
            case BEGINNER -> List.of(BEGINNER, INTERMEDIATE);
            case INTERMEDIATE -> List.of(BEGINNER, INTERMEDIATE, ADVANCED);
            case ADVANCED -> List.of(INTERMEDIATE, ADVANCED);
        };
    }
}
