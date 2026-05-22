package com.clip.server.user.controller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.user.dto.response.UserDashBoardResponse;
import com.clip.server.user.dto.response.UserProfileResponse;
import com.clip.server.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/me")
@Tag(name = "사용자 정보 API", description = "사용자 유저 프로필을 보여주는 API입니다.")
@Validated
public class UserController {

    private final UserService userService;

    @Operation(summary = "사용자 프로필 조회")
    @GetMapping
    public ApiResponse<UserProfileResponse> getUserProfile(
            @AuthenticationPrincipal Long userId
    ) {
        UserProfileResponse response = userService.showProfile(userId);
        return ApiResponse.success(response, "사용자의 프로필이 성공적으로 조회되었습니다.");
    }

    @Operation(summary = "사용자 대시보드 조회(마이페이지 윗 화면)")
    @GetMapping("/dashboard")
    public ApiResponse<UserDashBoardResponse> getUserDashBoard(
            @AuthenticationPrincipal Long userId
    ) {
        UserDashBoardResponse response = userService.showDashBoard(userId);
        return ApiResponse.success(response, "사용자의 대시보드가 성공적으로 조회되었습니다.");
    }


}
