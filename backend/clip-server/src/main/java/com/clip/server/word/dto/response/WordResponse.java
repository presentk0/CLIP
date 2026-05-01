package com.clip.server.word.dto.response;

import com.clip.server.word.entity.WordType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
public class WordResponse {

    private Long id;
    private WordType wordType;
    private String word;
    private String meaning;
    private String timestamp;
    private String sentence;
    private String translation;
    private String videoId;
    private String videoTitle;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime collectedAt;
}
