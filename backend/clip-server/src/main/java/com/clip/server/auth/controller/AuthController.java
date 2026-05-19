package com.clip.server.auth.controller;

import com.clip.server.auth.dto.response.LoginResult;
import com.clip.server.auth.dto.request.GoogleLoginRequest;
import com.clip.server.auth.dto.response.LoginResponse;
import com.clip.server.auth.dto.response.RefreshResponse;
import com.clip.server.auth.service.AuthService;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.common.response.ApiResponse;
import com.clip.server.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.clip.server.common.exception.BusinessException;

import java.time.Duration;

/**
 * [AuthController]
 * 인증 관련 API 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@Tag(name = "로그인 관련 API", description = "oauth 인증 API입니다.")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Value("${jwt.access-expiration}")
    private long accessTokenExpirationMs;

    private static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String COOKIE_PATH = "/api/auth";

    /**
     * 구글 로그인
     */
    @Operation(summary = "구글 계정 로그인")
    @PostMapping("/google/login")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request,
            HttpServletResponse response) {

        log.info("구글 로그인 요청");
        LoginResult result = authService.googleLogin(request.getIdToken());
        addRefreshTokenCookie(response, result.getRefreshToken());

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken(result.getAccessToken())
                .user(UserResponse.from(result.getUser(), result.isNewUser()))
                .build();

        String message = result.isNewUser()
                ? "환영합니다! 🎉"
                : "다시 만나서 반가워요!";

        return ResponseEntity.ok(ApiResponse.success(loginResponse, message));
    }

    /**
     * 토큰 갱신
     */
    @Operation(summary = "토큰 갱신")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @CookieValue(name = REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        LoginResult result = authService.refresh(refreshToken);

        addRefreshTokenCookie(response, result.getRefreshToken());

        RefreshResponse refreshResponse = RefreshResponse.builder()
                .accessToken(result.getAccessToken())
                .expiresIn(accessTokenExpirationMs / 1000)
                .build();

        return ResponseEntity.ok(ApiResponse.success(refreshResponse, "토큰이 갱신되었습니다."));
    }

    /**
     * 로그아웃
     */
    @Operation(summary = "계정 로그아웃")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal Long userId,
            HttpServletRequest request,
            HttpServletResponse response) {

        log.info("로그아웃 요청: userId={}", userId);

        String accessToken = extractAccessToken(request);
        authService.logout(userId, accessToken);
        deleteRefreshTokenCookie(response);

        return ResponseEntity.ok(ApiResponse.success(null, "로그아웃되었습니다."));
    }

    /**
     * Refresh Token 쿠키 추가
     */
    private void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path(COOKIE_PATH)
                .maxAge(Duration.ofMillis(refreshExpirationMs))
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Refresh Token 쿠키 삭제
     */
    private void deleteRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * Authorization 헤더에서 Access Token 추출
     */
    private String extractAccessToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}