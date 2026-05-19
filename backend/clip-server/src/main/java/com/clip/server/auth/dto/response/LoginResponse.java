package com.clip.server.auth.dto.response;

import com.clip.server.user.dto.response.UserResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LoginResponse {

    private String accessToken;
    private UserResponse user;
}
