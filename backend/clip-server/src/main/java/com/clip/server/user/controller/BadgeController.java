package com.clip.server.user.controller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.user.dto.response.BadgeInfoResponse;
import com.clip.server.user.service.BadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping
@Tag(name = "영상 배지 API", description = "영상 배지 관련 API입니다.")
@Validated
public class BadgeController {

    private final BadgeService badgeService;

    @Operation(summary = "영상 배지 등급을 확인하는 API")
    @GetMapping("/api/badges/video/{videoId}")
    public ApiResponse<BadgeInfoResponse> getVideoBadge(
            @AuthenticationPrincipal Long userId,
            @PathVariable String videoId
            ) {
        BadgeInfoResponse response = badgeService.getBadge(userId, videoId);
        return ApiResponse.success(response, "영상 배지 등급이 성공적으로 조회되었습니다.");
    }
}
