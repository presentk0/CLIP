package com.clip.server.quiz.controller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.quiz.dto.request.QuizGenerateRequest;
import com.clip.server.quiz.dto.response.QuizCompleteResponse;
import com.clip.server.quiz.dto.response.QuizGenerateResponse;
import com.clip.server.quiz.service.QuizSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz/sessions")
@Tag(name = "퀴즈 세션 API", description = "퀴즈 생성 API입니다.")
@Validated
public class QuizSessionController {

    private final QuizSessionService quizSessionService;
    @Operation(summary = "퀴즈 섹션 요청(OX, 빈칸")
    @PostMapping("/generate/section")
    public ResponseEntity<ApiResponse<QuizGenerateResponse>> createSectionQuiz(
            @Valid @RequestBody QuizGenerateRequest request) {
        Long tempUserId = 1L; // TODO: 나중에 시큐리티 적용 시 토큰에서 추출
        QuizGenerateResponse response = quizSessionService.generateSectionQuiz(tempUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "퀴즈 섹션(OX, 빈칸) 요청에 성공하였습니다."));
    }

    @Operation(summary = "매칭 퀴즈 요청")
    @PostMapping("/generate/matching")
    public ResponseEntity<ApiResponse<QuizGenerateResponse>> createMatchingQuiz(
            @Valid @RequestBody QuizGenerateRequest request) {
        Long tempUserId = 1L; // TODO: 나중에 시큐리티 적용 시 토큰에서 추출
        QuizGenerateResponse response = quizSessionService.generateMatchingQuiz(tempUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "매칭 퀴즈 요청에 성공하였습니다."));
    }

    @Operation(summary = "퀴즈 세션 종료")
    @PostMapping("/{sessionId}/complete")
    public ResponseEntity<ApiResponse<QuizCompleteResponse>> completeSession(
            @PathVariable Long sessionId) {
        Long tempUserId = 1L; // TODO: 나중에 시큐리티 적용 시 토큰에서 추출
        QuizCompleteResponse response = quizSessionService.completeQuizSession(tempUserId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(response, "퀴즈 세션이 성공적으로 만료되었습니다."));
    }

}
