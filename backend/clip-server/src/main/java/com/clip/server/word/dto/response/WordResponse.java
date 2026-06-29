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
    private final String word;
    private final List<String> meanings;
}
