package com.clip.server.tranlation.service;

import com.clip.server.subtitle.repository.SubtitleRepository;
import com.clip.server.subtitle.service.SubtitleService;
import com.clip.server.translation.client.TranslationClient;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.translation.service.TranslationCacheService;
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
import com.clip.server.translation.dto.response.TranslationResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TranslationServiceTest {

    @Mock
    private TranslationClient translationClient;

    @Mock
    private SubtitleService subtitleService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubtitleRepository subtitleRepository;

    @InjectMocks
    private TranslationService translationService;
    @Mock
    private TranslationCacheService translationCacheService;

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
        TranslationRequest.SubtitleDetail detail =
                new TranslationRequest.SubtitleDetail("Hello", 0.0, 1.0);
        TranslationRequest request = new TranslationRequest(
                "v1", "Title", 100, "channel1", "www.youtube.com", List.of(detail)
        );
        List<String> mockTranslated = List.of("안녕");

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

        //  L1 캐시 미스 시뮬레이션
        given(translationCacheService.getVideoTranslations("v1")).willReturn(null);

        //  L2 캐시 미스 시뮬레이션 (빈 리스트)
        given(subtitleRepository.findTranslationsByVideoId("v1"))
                .willReturn(Collections.emptyList());

        given(translationClient.translateBatch(anyList())).willReturn(mockTranslated);

        // when
        translationService.translateAndSave(tempUserId, request);

        // then
        verify(userRepository).findById(tempUserId);
        verify(translationCacheService).getVideoTranslations("v1");      //  L1 조회 확인
        verify(subtitleRepository).findTranslationsByVideoId("v1");      //  L2 조회 확인
        verify(translationClient).translateBatch(anyList());
        verify(subtitleService).bulkSaveSubtitles(
                eq("v1"),
                eq("Title"),
                eq(100),
                eq("channel1"),
                eq("www.youtube.com"),
                anyList(),
                eq(mockTranslated)
        );
        verify(translationCacheService).putVideoTranslations("v1", mockTranslated); // 캐시 저장 확인
    }

    @Test
    @DisplayName("빈 리스트 요청 시 아무것도 수행하지 않는다")
    void translateAndSave_Empty() {
        // given
        TranslationRequest request = new TranslationRequest("v1", "T", 10, "channel1", "www.youtube.com", List.of());
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));

        // when
        translationService.translateAndSave(tempUserId, request);

        // then
        verify(translationClient, never()).translateBatch(any());
        verify(subtitleService, never()).bulkSaveSubtitles(any(), any(), any(), any(),any(), any(), any());
    }

    @Test
    @DisplayName("L1 캐시 히트 시 DeepL 호출 없이 캐시된 번역을 반환한다")
    void translateAndSave_L1CacheHit() {
        // given
        TranslationRequest.SubtitleDetail detail =
                new TranslationRequest.SubtitleDetail("Hello", 0.0, 1.0);
        TranslationRequest request = new TranslationRequest(
                "v1", "Title", 100, "channel1", "www.youtube.com", List.of(detail)
        );
        List<String> cachedTranslations = List.of("안녕(캐시)");

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(translationCacheService.getVideoTranslations("v1"))
                .willReturn(cachedTranslations);  // L1 히트!

        // when
        TranslationResponse response = translationService.translateAndSave(tempUserId, request);

        // then
        assertThat(response.getTranslatedTexts()).containsExactly("안녕(캐시)");

        // L2 조회 X, DeepL 호출 X, 저장 X
        verify(subtitleRepository, never()).findTranslationsByVideoId(anyString());
        verify(translationClient, never()).translateBatch(anyList());
        verify(subtitleService, never()).bulkSaveSubtitles(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("L2 캐시 히트 시 L1으로 승격된다")
    void translateAndSave_L2Hit_PromotesToL1() {
        // given
        TranslationRequest.SubtitleDetail detail =
                new TranslationRequest.SubtitleDetail("Hello", 0.0, 1.0);
        TranslationRequest request = new TranslationRequest(
                "v1", "Title", 100, "channel1", "www.youtube.com", List.of(detail)
        );
        List<String> dbTranslations = List.of("안녕(DB)");

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(translationCacheService.getVideoTranslations("v1")).willReturn(null);
        given(subtitleRepository.findTranslationsByVideoId("v1")).willReturn(dbTranslations);

        // when
        translationService.translateAndSave(tempUserId, request);

        // then
        // L1 승격 확인
        verify(translationCacheService).putVideoTranslations("v1", dbTranslations);

        // DeepL 호출 X
        verify(translationClient, never()).translateBatch(anyList());
    }

    @Test
    @DisplayName("L1 미스, L2 히트 시 DB 결과를 반환하고 L1으로 승격시킨다")
    void translateAndSave_L2CacheHit() {
        // given
        TranslationRequest.SubtitleDetail detail =
                new TranslationRequest.SubtitleDetail("Hello", 0.0, 1.0);
        TranslationRequest request = new TranslationRequest(
                "v1", "Title", 100, "channel1", "www.youtube.com", List.of(detail)
        );
        List<String> dbTranslations = List.of("안녕(DB)");

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(translationCacheService.getVideoTranslations("v1")).willReturn(null);
        given(subtitleRepository.findTranslationsByVideoId("v1")).willReturn(dbTranslations);

        // when
        TranslationResponse response = translationService.translateAndSave(tempUserId, request);

        // then
        assertThat(response.getTranslatedTexts()).containsExactly("안녕(DB)");

        // L1 승격 호출 확인
        verify(translationCacheService).putVideoTranslations("v1", dbTranslations);

        // DeepL 호출 X
        verify(translationClient, never()).translateBatch(anyList());
    }

    @Test
    @DisplayName("L1, L2 모두 미스 시 DeepL 호출 후 DB와 Redis에 저장한다")
    void translateAndSave_AllCacheMiss() {
        // given
        TranslationRequest.SubtitleDetail detail =
                new TranslationRequest.SubtitleDetail("Hello", 0.0, 1.0);
        TranslationRequest request = new TranslationRequest(
                "v1", "Title", 100, "channel1", "www.youtube.com", List.of(detail)
        );
        List<String> mockTranslated = List.of("안녕");

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(translationCacheService.getVideoTranslations("v1")).willReturn(null);
        given(subtitleRepository.findTranslationsByVideoId("v1"))
                .willReturn(Collections.emptyList());
        given(translationClient.translateBatch(anyList())).willReturn(mockTranslated);

        // when
        TranslationResponse response = translationService.translateAndSave(tempUserId, request);

        // then
        assertThat(response.getTranslatedTexts()).containsExactly("안녕");

        // 전체 흐름 검증
        verify(translationCacheService).getVideoTranslations("v1");
        verify(subtitleRepository).findTranslationsByVideoId("v1");
        verify(translationClient).translateBatch(anyList());
        verify(subtitleService).bulkSaveSubtitles(
                eq("v1"), eq("Title"), eq(100),
                eq("channel1"), eq("www.youtube.com"),
                anyList(), eq(mockTranslated)
        );
        verify(translationCacheService).putVideoTranslations("v1", mockTranslated);
    }

}

