package com.clip.server.admin.dashboard.stats.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
public class UserVideoWordStatResponse {
    private final Long userId;
    private final String userName;
    private final String videoId;
    private final String videoTitle;
    private final Long totalCollected;
    private final Long collectTypeCount;
    private final Long popupTypeCount;
}