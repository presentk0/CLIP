package com.clip.server.subtitle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
public class SubtitleResponse {

    private String videoId;
    private Long subtitleId;
    private String text; // 자막 내용
    private String translation; // 자막 번역
    private Double startTime; // 영상 시작 시각
    private Double endTime; // 영상 종료 시각
    private Boolean isQuizGenerate; // 퀴즈 생성 가능 여부
    private int section; //  섹션
    private int totalSections; // 전체 퀴즈 세션
}
