package com.clip.server.subtitle.controller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.subtitle.dto.request.SubtitleRequest;
import com.clip.server.subtitle.dto.response.SubtitleListResponse;
import com.clip.server.subtitle.dto.response.SubtitleResponse;
import com.clip.server.subtitle.service.SubtitleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos/{videoId}/subtitles")
@Tag(name = "자막 API", description = "영상 자막 관련 API입니다.")
public class SubtitleController {

    private final SubtitleService subtitleService;

    @Operation(summary = "영상 자막 저장 API(자막 1개)")
    @PostMapping
    ResponseEntity<ApiResponse<SubtitleResponse>> postSubtitle(
            @AuthenticationPrincipal Long userId,
            @PathVariable String videoId,
            @Valid @RequestBody SubtitleRequest subtitleRequest
    ) {
        SubtitleResponse subtitleResponse = subtitleService.saveSubtitle(userId, videoId, subtitleRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(subtitleResponse,"자막이 성공적으로 저장되었습니다."));
    }

    @Operation(summary = "해당 영상의 전체 자막 조회 API")
    @GetMapping
    ApiResponse<SubtitleListResponse> getVideoSubtitles(
            @PathVariable String videoId,
            @AuthenticationPrincipal Long userId
    ) {
        SubtitleListResponse subtitleListResponse = subtitleService.getSubtitles(videoId, userId);
        return ApiResponse.success(subtitleListResponse, "해당 영상의 자막이 성공적으로 조회되었습니다.");
    }

}
