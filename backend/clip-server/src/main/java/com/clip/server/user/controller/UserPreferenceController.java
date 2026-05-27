package com.clip.server.user.controller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.user.dto.request.InitialPreferenceRequest;
import com.clip.server.user.dto.request.UpdatePreferenceRequest;
import com.clip.server.user.dto.response.PreferenceResponse;
import com.clip.server.user.service.UserPreferenceService;
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
@RequestMapping("/api")
@Tag(name = "사용자 온보딩 API", description = "사용자 온보딩 설정 관련 API입니다.")
@Validated
public class UserPreferenceController {

    private final UserPreferenceService userPreferenceService;

    @Operation(summary = "초기 학습 목표 및 난이도 설정 API")
    @PostMapping("/preferences/onboarding")
    public ResponseEntity<ApiResponse<PreferenceResponse>> postUserPreference(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody InitialPreferenceRequest request
    ) {
        PreferenceResponse response = userPreferenceService.postUserPreference(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "학습 목표 및 난이도가 성공적으로 설정되었습니다."));
    }

    @Operation(summary = "학습 목표 및 난이도 수정 API")
    @PatchMapping("/users/me/preferences")
    public ResponseEntity<ApiResponse<PreferenceResponse>> updateUserPreference(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdatePreferenceRequest request
            ) {
        PreferenceResponse response = userPreferenceService.updateUserPreference(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "학습 목표 및 난이도가 성공적으로 수정되었습니다."));
    }

    @Operation(summary = "학습 목표 및 난이도 조회 API")
    @GetMapping("/users/me/preferences")
    public ApiResponse<PreferenceResponse> getUserPreference(
            @AuthenticationPrincipal Long userId
    ) {
        PreferenceResponse response = userPreferenceService.getUserPreference(userId);
        return ApiResponse.success(response, "학습 목표 및 난이도가 성공적으로 조회되었습니다.");
    }

}
