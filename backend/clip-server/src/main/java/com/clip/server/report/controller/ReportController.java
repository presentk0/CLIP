package com.clip.server.report.controller;

import com.clip.server.chat.dto.response.ChatReportResponse;
import com.clip.server.common.response.ApiResponse;
import com.clip.server.report.dto.AiChatReportResponse;
import com.clip.server.report.dto.ChatReportRequest;
import com.clip.server.report.service.AiChatReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "AI 채팅 신고 API", description = "AI 채팅기능 신고 관련 API입니다.")
@Validated
public class ReportController {

    private final AiChatReportService aiChatReportService;

    @PostMapping("/chats/reports")
    public ResponseEntity<ApiResponse<AiChatReportResponse>> postAiChatReport(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatReportRequest request
            ) {
        AiChatReportResponse response = aiChatReportService.postAiChatReport(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "신고가 정상 접수되었습니다"));
    }
}
