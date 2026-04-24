package com.clip.server.word.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class CollectedWordResponse {

    private Long wordId;
    // 단어 수집 시간
    private LocalDateTime collectedAt;
    // 총 수집 단어
    private Long totalCollectedWords;
}
