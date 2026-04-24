package com.clip.server.subtitle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubtitleDetailResponse {

    private Long subtitleId;
    private String text; // 자막 내용
    private String translation; // 자막 번역
    private Double startTime; // 영상 시작 시각
    private Double endTime; // 영상 종료 시각
}
