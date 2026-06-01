package com.clip.server.video.controller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.video.dto.response.VideoRecommendationResponse;
import com.clip.server.video.service.VideoRecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos")
@Tag(name = "영상 API", description = "영상 기능 관련 API입니다.")
@Validated
public class VideoController {

    private final VideoRecommendationService videoRecommendationService;

    @Operation(summary = "영상 추천")
    @GetMapping("/recommended")
    public ApiResponse<VideoRecommendationResponse> getVideoReRecommendation(
            @AuthenticationPrincipal Long userId
    ) {
        VideoRecommendationResponse response = videoRecommendationService.getRecommendedVideos(userId);
        return ApiResponse.success(response, "추천 영상이 성공적으로 조회되었습니다.");
    }
}