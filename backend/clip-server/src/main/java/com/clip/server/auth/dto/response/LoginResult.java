package com.clip.server.auth.dto.response;

import com.clip.server.user.entity.User;
import lombok.Builder;
import lombok.Getter;

/**
 * [LoginResult]
 * AuthService에서 Controller로 전달하는 내부 DTO
 */
@Getter
@Builder
public class LoginResult {

    private String accessToken;
    private String refreshToken;
    private User user;
    private boolean isNewUser;
}