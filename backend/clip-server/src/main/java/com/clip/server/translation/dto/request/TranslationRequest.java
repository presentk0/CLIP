package com.clip.server.translation.dto.request;

import com.clip.server.subtitle.dto.request.SubtitleRequest;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class TranslationRequest {

    private String videoId;
    private String title;
    private Integer duration; // 영상 시간
    private String channelName; // 채널명
    private String thumbnailUrl; // 썸네일 URL
    private List<SubtitleDetail> subtitleRequests;

    /**
     * 자막의 개별 정보를 담는 내부 클래스
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubtitleDetail {
        private String text; // 원문
        private Double startTime;
        private Double endTime;
    }
}
