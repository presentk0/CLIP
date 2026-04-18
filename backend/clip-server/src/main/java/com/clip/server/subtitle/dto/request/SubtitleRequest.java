package com.clip.server.subtitle.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubtitleRequest {

    private String title; // 영상 제목
    private String text; // 자막 내용
    private String translation; // 자막 번역
    private BigDecimal startTime; // 영상 시작 시각
    private BigDecimal endTime; // 영상 종료 시각

}
