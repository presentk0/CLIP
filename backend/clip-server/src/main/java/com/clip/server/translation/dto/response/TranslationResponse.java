package com.clip.server.translation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class TranslationResponse {

    private List<String> translatedTexts; // 자막 번역본
}
