package com.clip.server.word.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class WordResponse {

    private Long id;
    private String word;
    private String timestamp;
    private String translation;
    private String videoId;
    private String videoTitle;
    private LocalDateTime collectedAt;
}
