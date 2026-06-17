package com.clip.server.feedback.controller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.feedback.dto.request.FeedbackRequest;
import com.clip.server.feedback.dto.response.FeedbackCheckResponse;
import com.clip.server.feedback.dto.response.FeedbackResponse;
import com.clip.server.feedback.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "피드백 API", description = "피드백 제출 API입니다.")
@RequestMapping("/api/feedback")
@Validated
public class FeedbackController {

    private final FeedbackService feedbackService;

    @Operation(summary = "사용자 피드백 제출")
    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> postUserFeedBack(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody FeedbackRequest request
    ) {
        FeedbackResponse response = feedbackService.postFeedBack(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "소중한 의견 감사합니다."));
    }

    @Operation(summary = "사용자 피드백 제출 여부 확인")
    @GetMapping("/check")
    public ApiResponse<FeedbackCheckResponse> getFeedbackCheck(
            @AuthenticationPrincipal Long userId
    ) {
        FeedbackCheckResponse response = feedbackService.getFeedBackCheck(userId);
        return ApiResponse.success(response, "설문조사 제출 여부 조회를 성공하였습니다");
    }

}
