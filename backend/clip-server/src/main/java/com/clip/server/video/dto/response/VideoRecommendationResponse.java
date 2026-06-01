package com.clip.server.video.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class VideoRecommendationResponse {

    private final List<Recommendation> recommendations;

    @Getter
    @Builder
    public static class Recommendation {
        private final String videoId; // 영상 고유 ID
        private final String title; // 영상 제목
        private final String thumbnailUrl; //영상 썸네일 URL
        private final int duration; // 영상 길이(초)
        private final String channelName;  // 채널명
        private final String channelProfileUrl; // 채널 프로필 이미지
        private final String recommendationReason; // 추천 이유
        private final String estimatedDifficulty;
    }
}