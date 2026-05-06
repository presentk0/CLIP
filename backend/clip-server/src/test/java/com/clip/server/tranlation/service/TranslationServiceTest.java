package com.clip.server.tranlation.service;

import com.clip.server.common.config.AppConfig;
import com.clip.server.translation.service.TranslationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.clip.server.translation.client.TranslationClient;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.translation.dto.response.TranslationResponse;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@Import(AppConfig.class)
@ActiveProfiles("test")
class TranslationServiceTest {

    @Mock
    private TranslationClient translationClient;

    @InjectMocks
    private TranslationService translationService;

    @Test
    @DisplayName("빈 텍스트 리스트가 들어오면 빈 응답을 반환한다")
    void translateTexts_EmptyInput() {
        // given
        TranslationRequest request = new TranslationRequest("video123", List.of());

        // when
        TranslationResponse response = translationService.translateTexts(request);

        // then
        assertThat(response.getTranslatedTexts()).isEmpty();
        verify(translationClient, never()).translateBatch(any());
    }
}

