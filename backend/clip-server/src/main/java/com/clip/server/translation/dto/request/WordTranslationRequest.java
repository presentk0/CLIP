package com.clip.server.translation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class WordTranslationRequest {

    @NotBlank
    private String word; // 번역하고자 하는 단어
    @NotEmpty
    private List<String> meanings; // 단어 뜻
 }
