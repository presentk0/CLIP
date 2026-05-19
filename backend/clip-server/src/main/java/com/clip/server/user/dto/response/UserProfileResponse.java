package com.clip.server.user.dto.response;

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
    private String email;
    private String profileImageUrl;
    private Integer level;
    private Integer exp;
    private Integer nextLevelExp;
    private Double progressPercentage;
    private LocalDateTime createdAt;
}