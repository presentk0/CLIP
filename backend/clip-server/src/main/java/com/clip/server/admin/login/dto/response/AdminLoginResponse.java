package com.clip.server.admin.login.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminLoginResponse {
    private String accessToken;
    private String tokenType;  // "Bearer"
    private long expiresIn;    // 초 단위
}