package com.clip.server.tranlation.service;

import com.clip.server.subtitle.service.SubtitleService;
import com.clip.server.translation.client.TranslationClient;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.translation.service.TranslationService;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private TranslationClient translationClient;

    @Mock
    private SubtitleService subtitleService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TranslationService translationService;

    private final Long tempUserId = 1L;
    private User user;

    @BeforeEach
    void setUp() {
        // 테스트용 유저 객체 생성
        user = User.builder()
                .email("test@clip.com")
                .name("테스터")
                .build();
        ReflectionTestUtils.setField(user, "id", tempUserId);
    }

    @Test
    @DisplayName("자막 리스트가 들어오면 번역 후 저장을 요청한다 (userId 포함)")
    void translateAndSave_Success() {
        // given
        TranslationRequest.SubtitleDetail detail = new TranslationRequest.SubtitleDetail("Hello", 0.0, 1.0);
        TranslationRequest request = new TranslationRequest("v1", "Title", 100, List.of(detail));
        List<String> mockTranslated = List.of("안녕");

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(translationClient.translateBatch(anyList())).willReturn(mockTranslated);

        // when
        translationService.translateAndSave(tempUserId, request);

        // then
        verify(userRepository).findById(tempUserId); // 유저 조회 확인
        verify(translationClient).translateBatch(anyList());
        verify(subtitleService).bulkSaveSubtitles(
                eq("v1"),
                eq("Title"),
                eq(100),
                anyList(),
                eq(mockTranslated)
        );
    }

    @Test
    @DisplayName("빈 리스트 요청 시 아무것도 수행하지 않는다")
    void translateAndSave_Empty() {
        // given
        TranslationRequest request = new TranslationRequest("v1", "T", 10, List.of());
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

        // when
        translationService.translateAndSave(tempUserId, request);

        // then
        verify(translationClient, never()).translateBatch(any());
        verify(subtitleService, never()).bulkSaveSubtitles(any(), any(), any(), any(), any());
    }
}

