package com.clip.server.user.dto.response;

import com.clip.server.user.entity.badge.BadgeType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BadgeAwardResponse {

    private BadgeType badgeType;
    private int bonusExp;
}
