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
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WordController.class)
public class WordControllerTest {

    @Autowired
    private MockMvc mockMvc; // MockMvc: 가짜 요청을 보낼 도구 주입

    @MockitoBean
    private WordService wordService; // 가짜 서비스 객체

    private final ObjectMapper objectMapper = new ObjectMapper(); // DTO를 JSON으로 바꿀 도구

    @Test
    @DisplayName("단어 수집 요청 시 성공하면 200 OK를 반환한다")
    void collectWord_success() throws Exception {
        CollectedWordRequest request = new CollectedWordRequest("v1", "Title", "apple", "I eat an apple", "0:01", "사과");

        // 1. Given
        CollectedWordResponse mockResponse = new CollectedWordResponse(1L, LocalDateTime.now(), 5L);
        given(wordService.save(any(CollectedWordRequest.class))).willReturn(mockResponse);

        // 2. When
        ResultActions result = mockMvc.perform(post("/api/words/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        // 3. Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.wordId").value(1))
                .andExpect(jsonPath("$.message").value("단어가 성공적으로 수집되었습니다."));
    }

    @Test
    @DisplayName("필수값 누락시 400을 반환한다.")
    void collectWord_fail_validation() throws Exception {
        // Given - word가 빈 값
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