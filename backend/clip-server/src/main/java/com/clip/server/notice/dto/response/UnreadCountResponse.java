package com.clip.server.notice.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnreadCountResponse {

    // 미확인 공지 수
    private Long unreadCount;

    // 새로운 공지 존재 여부 true: 존재, false: X
    private Boolean hasNew;
}
