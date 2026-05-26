package com.clip.server.user.dto.response;

import com.clip.server.user.entity.badge.BadgeType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserProfileResponse {
    /*
    * user 프로필용 DTO
     */
    private Long id;
    private String name;
    private String email;
    private String profileImageUrl; // 사용자 프로필 URL
    private Integer level; // 현재 레벨
    private Integer exp; // 현재 exp
    private Integer nextLevelExp; // 다음 레벨까지 필요한 exp
    private Double progressPercentage; // 다음 레벨대비 진행 퍼센드
    private String createdAt;
    private OngoingMastery ongoingMastery;

    @Getter
    @Builder
    public static class OngoingMastery {
        private String videoId;
        private String videoTitle;
        private String videoDuration;
        private String channelName;
        private String channelProfileImageUrl; // 채널 프로필 Url
        private String thumbnailUrl; // 채널 썸네일 Url
        private String currentBadge;
    }
}