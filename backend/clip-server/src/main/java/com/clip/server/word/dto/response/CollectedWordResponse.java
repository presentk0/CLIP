package com.clip.server.word.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class CollectedWordResponse {

    private Long wordId;
    // 단어 수집 시간
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectedAt;
    // 총 수집 단어
    private Long totalCollectedWords;
}
