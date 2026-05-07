package com.clip.server.user.dto.response;

import com.clip.server.user.entity.badge.BadgeType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class BadgeInfoResponse {

    private String videoId;
    private String videoTitle;
    private BadgeType currentBadge;
    private BadgeType nextBadge;
    private List<BadgeDetail> earnedBadges;

    @Getter
    @Builder
    public static class BadgeDetail {
        private BadgeType badgeType;
        private LocalDateTime earnedAt;
    }
}
