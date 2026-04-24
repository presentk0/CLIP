package com.clip.server.subtitle.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubtitleListResponse {

    private String videoId;
    private List<SubtitleDetailResponse> subtitles;
}
