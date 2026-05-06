package com.clip.server.translation.service;

import com.clip.server.translation.client.TranslationClient;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.translation.dto.response.TranslationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TranslationService {
    private final TranslationClient translationClient;

    public TranslationResponse translateTexts(TranslationRequest request) {
        if (request.getTexts() == null || request.getTexts().isEmpty()) {
            return TranslationResponse.builder()
                    .translatedTexts(Collections.emptyList())
                    .build();
        }

        // 번역 수행
        List<String> translatedList = translationClient.translateBatch(request.getTexts());

        return TranslationResponse.builder()
                .translatedTexts(translatedList)
                .build();
    }
}
