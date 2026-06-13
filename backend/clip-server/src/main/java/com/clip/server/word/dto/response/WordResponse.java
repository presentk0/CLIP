package com.clip.server.word.dto.response;

import com.clip.server.word.entity.WordType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class WordResponse {

    private final Long id;
    private final WordType wordType;
    private final String word;
    private final List<MeaningByPosDto> meaningsByPos;
    private final String timestamp;
    private final String sentence;
    private final String translation;
    private final String videoId;
    private final String videoTitle;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private final LocalDateTime collectedAt;

    @Getter
    @Builder
    public static class MeaningByPosDto {
        private final String partOfSpeech;
        private final List<String> meanings;
    }
}
