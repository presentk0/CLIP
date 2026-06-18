package com.clip.server.admin.login.controller;

import com.clip.server.admin.login.dto.request.AdminLoginRequest;
import com.clip.server.admin.login.dto.response.AdminLoginResponse;
import com.clip.server.admin.login.service.AdminAuthService;
import com.clip.server.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Auth", description = "관리자 인증 API")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(
            summary = "관리자 로그인",
            description = "아이디/비밀번호로 관리자 로그인을 수행하고 JWT 토큰을 발급합니다."
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> login(
            @Valid @RequestBody AdminLoginRequest request
    ) {
        AdminLoginResponse response = adminAuthService.login(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                        .body(ApiResponse.success(response, "관리자 로그인에 성공하였습니다."));
    }

    @Operation(summary = "관리자 계정 로그아웃",
            description = "관리자 로그아웃을 진행합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Long adminId,
            HttpServletRequest request
    ) {

        // 기존에 쓰시던 헬퍼 메서드 구조를 그대로 차용하여 토큰 추출
        String accessToken = extractAccessToken(request);

        // 서비스 레이어를 호출해 블랙리스트 등록
        adminAuthService.logout(adminId, accessToken);

        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃되었습니다."));
    }

    /**
     * Authorization 헤더에서 Access Token 추출 (기존 코드와 싱크 맞춤)
     */
    private String extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}