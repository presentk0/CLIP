package com.clip.server.word.controller;

import com.clip.server.common.response.PaginationResponse;
import com.clip.server.word.contorller.WordController;
import com.clip.server.word.dto.request.CollectedWordRequest;
import com.clip.server.word.dto.response.CollectedWordResponse;
import com.clip.server.word.dto.response.WordListResponse;
import com.clip.server.word.dto.response.WordResponse;
import com.clip.server.word.entity.WordType;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
        CollectedWordRequest request = new CollectedWordRequest("v1", "Title", "apple", "I eat an apple", "0:01", "사과", WordType.COLLECT);
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
                "v1", "Title", "", "I eat an apple", "0:01", "사과", WordType.COLLECT
        );

        // When & Then
        mockMvc.perform(post("/api/words/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("수집된 단어를 목록 요청을 성공하면 200을 반환한다.")
    void get_collectWord_success() throws Exception {
        // 1. Given
        WordResponse words = WordResponse.builder()
                .id(1L)
                .word("apple")
                .collectedAt(LocalDateTime.now())
                .timestamp("1:20")
                .translation("사과")
                .videoTitle("title1")
                .build();

        PaginationResponse paginationResponse = PaginationResponse.builder()
                .totalPage(1)
                .totalCount(1L)
                .currentPage(0)
                .pageSize(5)
                .build();

        WordListResponse wordListResponse = WordListResponse.builder()
                .words(List.of(words))
                .pagination(paginationResponse)
                .build();

        given(wordService.getWords(anyLong(), anyInt(), anyInt(), any()))
                .willReturn(wordListResponse);

        // When & Then
        mockMvc.perform(get("/api/words/my-collection")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.words[0].word").value("apple"))
                .andExpect(jsonPath("$.data.pagination.totalCount").value(1));
    }

    @Test
    @DisplayName("페이지 번호가 음수일 경우 400 에러를 반환한다")
    void get_collectWord_fail_invalidPage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/words/my-collection")
                .param("page", "-1")
                .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.message").value("페이지 번호는 0 이상이어야 합니다. (입력값: -1)"))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT_VALUE"));

    }
}