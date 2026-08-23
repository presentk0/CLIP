package com.clip.server.notice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DismissResponse {

    // 공지 ID
    private Long noticeId;

    // 공지 읽음 처리 시각
    private LocalDateTime dismissedAt;
}
