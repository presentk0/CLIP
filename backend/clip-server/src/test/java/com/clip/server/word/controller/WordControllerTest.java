package com.clip.server.word.controller;

import com.clip.server.auth.jwt.JwtProvider;
import com.clip.server.common.response.PaginationResponse;
import com.clip.server.common.security.JwtAuthenticationFilter;
import com.clip.server.common.security.SessionActivityFilter;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WordController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
public class WordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WordService wordService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private SessionActivityFilter sessionActivityFilter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ============================================
    // 단어 수집 (POST /api/words/collect)
    // ============================================

    @Test
    @DisplayName("단어 수집 요청 시 성공하면 201 Created를 반환한다")
    void collectWord_success() throws Exception {
        CollectedWordRequest.MeaningByPos meaning = new CollectedWordRequest.MeaningByPos(
                "명사", List.of("사과", "사과나무")
        );

        CollectedWordRequest request = new CollectedWordRequest(
                "v1", "Title", "apple",
                List.of(meaning),
                "I eat an apple", "0:01",
                "나는 사과를 먹는다.",
                WordType.COLLECT
        );

        CollectedWordResponse mockResponse = CollectedWordResponse.builder()
                .wordId(1L)
                .collectedAt(LocalDateTime.now())
                .totalCollectedWords(5L)
                .build();

        given(wordService.save(any(), any(CollectedWordRequest.class)))
                .willReturn(mockResponse);

        ResultActions result = mockMvc.perform(post("/api/words/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print());

        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("단어가 성공적으로 수집되었습니다."))
                .andExpect(jsonPath("$.data.wordId").value(1));
    }

    @Test
    @DisplayName("필수값(word) 누락시 400 Bad Request를 반환한다.")
    void collectWord_fail_validation() throws Exception {
        CollectedWordRequest.MeaningByPos meaning = new CollectedWordRequest.MeaningByPos(
                "명사", List.of("사과")
        );

        CollectedWordRequest request = new CollectedWordRequest(
                "v1", "Title", "",  // word 빈 값
                List.of(meaning),
                "I eat an apple", "0:01",
                "나는 사과를 먹는다.",
                WordType.COLLECT
        );

        mockMvc.perform(post("/api/words/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    @Test
    @DisplayName("meaningsByPos 빈 배열일 경우 400 Bad Request를 반환한다")
    void collectWord_fail_emptyMeaningsByPos() throws Exception {
        CollectedWordRequest request = new CollectedWordRequest(
                "v1", "Title", "apple",
                List.of(),  // 빈 배열
                "I eat an apple", "0:01",
                "나는 사과를 먹는다.",
                WordType.COLLECT
        );

        mockMvc.perform(post("/api/words/collect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andDo(print());
    }

    // ============================================
    // 단어장 조회 (GET /api/words/my-collection)
    // ============================================

    @Test
    @DisplayName("수집된 단어 목록 조회 성공 시 200을 반환한다.")
    void get_collectWord_success() throws Exception {
        // 1. Given
        WordResponse words = WordResponse.builder()
                .id(1L)
                .word("apple")
                .meanings(List.of("사과", "사과나무"))
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

        given(wordService.getWords(any(), anyInt(), anyInt(), anyString(), anyString(), any()))
                .willReturn(wordListResponse);

        // 2. When & Then
        mockMvc.perform(get("/api/words/my-collection")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.words[0].id").value(1))
                .andExpect(jsonPath("$.data.words[0].word").value("apple"))
                .andExpect(jsonPath("$.data.words[0].meanings[0]").value("사과"))
                .andExpect(jsonPath("$.data.words[0].meanings[1]").value("사과나무"))
                .andExpect(jsonPath("$.data.pagination.totalCount").value(1));
    }

    @Test
    @DisplayName("정렬/필터/검색 파라미터로 단어장 조회 성공 시 200을 반환한다.")
    void get_collectWord_with_filters_success() throws Exception {
        WordResponse words = WordResponse.builder()
                .id(1L)
                .word("appreciate")
                .meanings(List.of("감사하다", "고마워하다"))
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

        given(wordService.getWords(any(), anyInt(), anyInt(), anyString(), anyString(), any()))
                .willReturn(wordListResponse);

        mockMvc.perform(get("/api/words/my-collection")
                        .param("page", "0")
                        .param("size", "5")
                        .param("sort", "alphabet-asc")
                        .param("filter", "today")
                        .param("keyword", "appre")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.words[0].word").value("appreciate"))
                .andExpect(jsonPath("$.data.words[0].meanings[0]").value("감사하다"));
    }

    @Test
    @DisplayName("검색어 없이 조회해도 정상 응답한다.")
    void get_collectWord_without_keyword_success() throws Exception {
        WordListResponse wordListResponse = WordListResponse.builder()
                .words(List.of())
                .pagination(PaginationResponse.builder()
                        .totalPage(0)
                        .totalCount(0L)
                        .currentPage(0)
                        .pageSize(10)
                        .build())
                .build();

        given(wordService.getWords(any(), anyInt(), anyInt(), anyString(), anyString(), any()))
                .willReturn(wordListResponse);

        mockMvc.perform(get("/api/words/my-collection")
                        .param("page", "0")
                        .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pagination.totalCount").value(0));
    }
}