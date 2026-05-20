package com.clip.server.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RefreshResponse {

    private String accessToken;
    private Long expiresIn;  // Access Token 만료 시간 (초 단위)
}
