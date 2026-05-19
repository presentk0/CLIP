package com.clip.server.quiz.controller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.quiz.dto.request.QuizSubmitRequest;
import com.clip.server.quiz.dto.response.QuizSubmitResponse;
import com.clip.server.quiz.service.QuizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "퀴즈 제출 API", description = "퀴즈 문제 제출 API입니다.")
@RequestMapping("/api/quiz")
public class QuizController {

    private final QuizService quizService;

    @Operation(summary = "퀴즈 제출")
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<QuizSubmitResponse>> submitQuiz(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody QuizSubmitRequest request
    ) {
        QuizSubmitResponse response = quizService.submitQuiz(userId, request);
        return ResponseEntity.ok(ApiResponse.success(response,"퀴즈 제출 및 채점이 완료되었습니다."));
    }

}
