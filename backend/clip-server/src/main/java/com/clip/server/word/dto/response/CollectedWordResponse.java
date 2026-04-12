package com.clip.server.word.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectedWordResponse {

    private Long wordId;
    // 단어 수집 시간
    private LocalDateTime collectedAt;
    // 총 수집 단어
    private Long totalCollectedWords;
}
