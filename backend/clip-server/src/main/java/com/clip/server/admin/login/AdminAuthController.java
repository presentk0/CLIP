package com.clip.server.admin.login;

import com.clip.server.admin.login.dto.request.AdminLoginRequest;
import com.clip.server.admin.login.dto.response.AdminLoginResponse;
import com.clip.server.admin.login.service.AdminAuthService;
import com.clip.server.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}