package com.clip.server.video.dto.ai.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AIRecommendRequest {

    private Long userId;
    private String learningGoal;
    private String absoluteLevel;

    /** 시청 이력 (제외 대상) */
    private List<String> excludedVideoIds;

    /** 취약 표현 (선택) */
    private List<WeaknessDto> weaknesses;

    /** 취약 단어 (선택) */
    private List<WeakWordDto> weakWords;

    /** 수집 단어 (선택) */
    private List<CollectedWordDto> collectedWords;

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    public static class WeaknessDto {
        private String weakExpression;
        private String recommendedExpression;
        private Integer reviewCount;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    public static class WeakWordDto {
        private String word;
        private String meaning;
        private Double correctRate;
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    public static class CollectedWordDto {
        private String word;
        private String meaning;
    }
}