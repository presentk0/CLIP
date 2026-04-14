package com.clip.server.word.controller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.word.contorller.WordController;
import com.clip.server.word.dto.request.CollectedWordRequest;
import com.clip.server.word.dto.response.CollectedWordResponse;
import com.clip.server.word.service.WordService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;


import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WordController.class)
public class WordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WordService wordService;

    // DTO를 JSON으로 바꿀 도구
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("단어 수집 요청 시 성공하면 201 Created를 반환한다")
    void collectWord_success() throws Exception {
        // 1. Given
        CollectedWordRequest request = new CollectedWordRequest("v1", "Title", "apple", "I eat an apple", "0:01", "사과");
        CollectedWordResponse mockResponse = CollectedWordResponse.builder()
                .wordId(1L)
                .collectedAt(LocalDateTime.now())
                .totalCollectedWords(5L)
                .build();
        given(wordService.save(eq(1L), any(CollectedWordRequest.class)))
                .willReturn(mockResponse);

        // 2. When
        ResultActions result = mockMvc.perform(post("/api/words/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        // 3. Then
        result.andExpect(status().isCreated()) // 201 Created 확인
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.wordId").value(1))
                .andExpect(jsonPath("$.message").value("단어가 성공적으로 수집되었습니다."));
    }

    @Test
    @DisplayName("필수값 누락시 400 Bad Request를 반환한다.")
    void collectWord_fail_validation() throws Exception {
        // Given - word가 빈 값인 경우
        CollectedWordRequest request = new CollectedWordRequest(
                "v1", "Title", "", "I eat an apple", "0:01", "사과"
        );

        // When & Then
        mockMvc.perform(post("/api/words/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }
}