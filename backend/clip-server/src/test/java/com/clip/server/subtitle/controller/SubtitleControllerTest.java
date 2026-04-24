package com.clip.server.subtitle.controller;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.subtitle.dto.request.SubtitleRequest;
import com.clip.server.subtitle.dto.response.SubtitleDetailResponse;
import com.clip.server.subtitle.dto.response.SubtitleListResponse;
import com.clip.server.subtitle.dto.response.SubtitleResponse;
import com.clip.server.subtitle.service.SubtitleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;


import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@WebMvcTest(SubtitleController.class)
public class SubtitleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubtitleService subtitleService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("자막 저장에 성공하면 201 Created를 반환한다")
    void postSubtitle_success() throws Exception {
        // given
        String videoId = "video123";
        SubtitleRequest request = new SubtitleRequest("title1","I'm happy", "나는 행복해",
                6.00, 170.00, 600);

        SubtitleResponse response = SubtitleResponse.builder()
                .videoId(videoId)
                .subtitleId(1L)
                .text("title1")
                .translation("나는 행복해")
                .startTime(6.00)
                .endTime(170.00)
                .isQuizGenerate(true)
                .section(1)
                .totalSections(3)
                .build();

        given(subtitleService.saveSubtitle(eq(1L), anyString(), any(SubtitleRequest.class)))
                .willReturn(response);

        String json = objectMapper.writeValueAsString(request);

        // when & then
        mockMvc.perform(post("/api/videos/{videoId}/subtitles", videoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.text").value("title1"))
                .andExpect(jsonPath("$.message").value("자막이 성공적으로 저장되었습니다."));
    }

    @Test
    @DisplayName("자막 내용이 비어있으면 400 에러를 반환한다.")
    void postSubtitle_invalidRequest() throws Exception {
        // given
        String videoId = "video123";
        SubtitleRequest invalidRequest = new SubtitleRequest("title1","", "나는 행복해",
                1.00, 1.10, 600);
        String json = objectMapper.writeValueAsString(invalidRequest);

        // when & then
        mockMvc.perform(post("/api/videos/{videoId}/subtitles", videoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("자막 조회에 성공하면 200을 반환한다")
    void getSubtitles_success() throws Exception {
        // given
        String videoId = "video123";
        Long tempUserID = 1L;

        SubtitleDetailResponse subtitleDetailResponse = SubtitleDetailResponse.builder()
                .text("I'm happy")
                .translation("나는 행복해")
                .startTime(1.00)
                .endTime(1.01)
                .build();

        SubtitleListResponse subtitleListResponse = SubtitleListResponse.builder()
                .videoId(videoId)
                .subtitles(List.of(subtitleDetailResponse))
                .build();

        given(subtitleService.getSubtitles(eq(videoId), eq(tempUserID)))
                .willReturn(subtitleListResponse);
        // when
        mockMvc.perform(get("/api/videos/{videoId}/subtitles", videoId)
                .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.subtitles[0].text").value("I'm happy"));
    }

    @Test
    @DisplayName("존재하지 않는 비디오 ID로 조회하면 404 에러를 반환한다")
    void getSubtitles_notFound() throws Exception {
        // given
        String videoId = "invalid_id";

        // 서비스에서 예외를 던지도록 설정
        given(subtitleService.getSubtitles(eq(videoId), anyLong()))
                .willThrow(new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/videos/{videoId}/subtitles", videoId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound()) // 404 확인
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("영상을 찾을 수 없습니다."))
                .andExpect(jsonPath("$.error.code").value("VIDEO_NOT_FOUND"));
    }
}