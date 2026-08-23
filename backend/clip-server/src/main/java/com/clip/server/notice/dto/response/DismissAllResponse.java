package com.clip.server.notice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DismissAllResponse {

    private Long dismissedCount; // dismiss된 공재 개수
    private LocalDateTime dismissedAt; // 처리 시각

}
