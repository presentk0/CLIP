package com.clip.server.video.dto.ai.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * FastAPI /recommend-full 응답 DTO
 * ️ 외부 API 응답 수신용:
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AIRecommendResponse(
        List<RecommendedVideo> recommendations,
        String strategy,

        // 메타 정보
        Integer totalCandidates,
        Integer excludedCount,
        Integer processingTimeMs,
        Integer tokensUsed,
        Double qualityScore,
        Integer retryCount
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RecommendedVideo(
            String videoId,
            Integer rank,
            String reason,
            Double relevanceScore,

            // 영상 상세 정보 (Video 테이블 자동 등록용)
            String title,
            String channelId,
            String channelName,
            String thumbnailUrl,
            String description,
            String publishedAt
    ) {}
}