package com.clip.server.translation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class WordTranslationResponse {

    private final String word;
    private final List<String> translations;
}
