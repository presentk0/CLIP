package com.clip.server.subtitle.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.subtitle.dto.request.SubtitleRequest;
import com.clip.server.subtitle.dto.response.SubtitleListResponse;
import com.clip.server.subtitle.dto.response.SubtitleResponse;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.video.entity.Video;
import com.clip.server.video.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubtitleServiceTest {

    @Mock
    private VideoService videoService;

    @Mock
    private SubtitleProcessor subtitleProcessor;

    @InjectMocks
    private SubtitleService subtitleService;

    private final Long userId = 1L;
    private final String videoId = "v12345";
    private Video video;
    private SubtitleRequest request;

    @BeforeEach
    void setUp() {
        video = Video.builder()
                .videoId(videoId)
                .title("테스트 영상")
                .duration(600)
                .build();

        request = new SubtitleRequest("title1", "The quick brown fox", "빠른 갈색 여우", 0.0, 1.5, 600);
    }

    @Test
    @DisplayName("자막 저장 시 VideoService와 SubtitleProcessor가 순서대로 호출된다")
    void saveSubtitle_callsServicesInOrder() {
        // given
        SubtitleResponse expectedResponse = SubtitleResponse.builder()
                .videoId(videoId)
                .subtitleId(100L)
                .text("The quick brown fox")
                .translation("빠른 갈색 여우")
                .startTime(0.0)
                .endTime(1.5)
                .isQuizGenerate(false)
                .section(0)
                .totalSections(1)
                .build();

        given(videoService.getOrCreateVideo(
                anyString(), anyString(), anyInt()
        )).willReturn(video);
        given(subtitleProcessor.saveSubtitleInternal(anyLong(), anyString(), any(SubtitleRequest.class)))
                .willReturn(expectedResponse);

        // when
        SubtitleResponse response = subtitleService.saveSubtitle(userId, videoId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getText()).isEqualTo("The quick brown fox");

        var inOrder = inOrder(videoService, subtitleProcessor);
        inOrder.verify(videoService).getOrCreateVideo(videoId, "title1", 600);
                inOrder.verify(subtitleProcessor).saveSubtitleInternal(userId, videoId, request);
    }

    @Test
    @DisplayName("벌크 자막 저장 시 VideoService와 SubtitleProcessor가 순서대로 호출된다")
    void bulkSaveSubtitles_callsServicesInOrder() {
        // given
        TranslationRequest.SubtitleDetail detail1 = new TranslationRequest.SubtitleDetail("Text1", 0.0, 1.0);
        TranslationRequest.SubtitleDetail detail2 = new TranslationRequest.SubtitleDetail("Text2", 1.0, 2.0);
        List<TranslationRequest.SubtitleDetail> requests = List.of(detail1, detail2);
        List<String> translations = List.of("번역1", "번역2");

        String channelName = "Test Channel";
        String thumbnailUrl = "https://i.ytimg.com/vi/v12345/hqdefault.jpg";

        given(videoService.getOrCreateVideo(
                anyString(), anyString(), anyInt(), anyString(), anyString()))
                .willReturn(video);
        doNothing().when(subtitleProcessor).bulkSaveSubtitlesInternal(anyString(), anyList(), anyList());

        // when
        subtitleService.bulkSaveSubtitles(
                videoId, "Title", 600, channelName, thumbnailUrl, requests, translations);

        // then
        var inOrder = inOrder(videoService, subtitleProcessor);
        inOrder.verify(videoService).getOrCreateVideo(
                videoId, "Title", 600, channelName, thumbnailUrl);
        inOrder.verify(subtitleProcessor).bulkSaveSubtitlesInternal(videoId, requests, translations);
    }

    @Test
    @DisplayName("자막 목록 조회 시 SubtitleProcessor가 호출된다")
    void getSubtitles_callsProcessor() {
        // given
        SubtitleListResponse expectedResponse = SubtitleListResponse.builder()
                .videoId(videoId)
                .subtitles(List.of())
                .build();

        given(subtitleProcessor.getSubtitles(anyString(), anyLong())).willReturn(expectedResponse);

        // when
        SubtitleListResponse response = subtitleService.getSubtitles(videoId, userId);

        // then
        assertThat(response.getVideoId()).isEqualTo(videoId);
        verify(subtitleProcessor).getSubtitles(videoId, userId);
    }
}