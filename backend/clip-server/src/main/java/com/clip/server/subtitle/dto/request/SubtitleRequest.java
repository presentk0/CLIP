package com.clip.server.subtitle.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class SubtitleRequest {

    @NotBlank(message = "영상 제목은 필수입니다.")
    private String title; // 영상 제목

    @NotBlank(message = "자막 내용은 필수입니다.")
    private String text; // 자막 내용

    private String translation; // 자막 번역

    private Double startTime = 0.0; // 영상 시작 시각

    private Double endTime = 0.0; // 영상 종료 시각

    @NotNull(message = "영상 전체 길이는 필수입니다.")
    @Min(value = 1, message = "영상 길이는 최소 1초 이상이어야 합니다.")
    private Integer duration; // 영상 전체 길이

}
