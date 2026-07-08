package com.clip.server.user.controller;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.common.response.ApiResponse;
import com.clip.server.user.dto.request.UpdateUiStateRequest;
import com.clip.server.user.dto.response.UserUiStateResponse;
import com.clip.server.user.service.UserUiStateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
@Tag(name = "사용자 UI 팝업 API", description = "사용자 팝업 확인 관련 API입니다.")
@Validated
public class UserUIStateController {

    private final UserUiStateService userUiStateService;

    @Operation(summary = "팝업 동의 여부 조회")
    @GetMapping("/ui-state")
    public ApiResponse<UserUiStateResponse> getUserUiState(
            @AuthenticationPrincipal Long userId
    ) {
        UserUiStateResponse response = userUiStateService.getUserUiState(userId);
        return ApiResponse.success(response, "사용자 UI 상태 조회가 완료되었습니다.");
    }

    @Operation(summary = "사용자 UI 팝업 상태 수정")
    @PatchMapping("/ui-state")
    public ResponseEntity<ApiResponse<Void>> updateUserUiState(
            @AuthenticationPrincipal Long userId,
            @RequestBody UpdateUiStateRequest request
            ) {
        // 최소 하나 필드는 필수
        if(request.getVoiceConsentRead() == null && request.getTutorialCompleted() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        userUiStateService.updateUserUiState(userId, request);
        return ResponseEntity.ok(
                ApiResponse.success(null, "UI 상태가 저장되었습니다.")
        );
    }

}
