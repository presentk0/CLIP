package com.clip.server.notice.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PopupResponse {

    // 공지가 있을 경우: true, 공지를 확인한 경우: false
    private Boolean hasPopup;
    private NoticeDetail notice;

    @Getter
    @Builder
    public static class NoticeDetail {
        private Long id;
        private String title; // 공지 제목
        private String summary; // 공지 요약
        private String detailUrl; // 공지 URL
        private LocalDateTime createdAt; // 공지가 만들어진 시각
    }
}
