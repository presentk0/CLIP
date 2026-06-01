package com.clip.server.video.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoMetadata {
    private String videoId;
    private String title;
    private String thumbnailUrl;
    private Integer duration;
    private String channelName;
    private String channelProfileImageUrl;
}
