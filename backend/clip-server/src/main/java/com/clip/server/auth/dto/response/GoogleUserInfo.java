package com.clip.server.auth.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * [GoogleUserInfo]
 * 구글 ID Token에서 추출한 사용자 정보 (서비스 내부 전달용 DTO)
 */
@Getter
@Builder
public class GoogleUserInfo {

    private String oauthId; // 구글 고유 ID
    private String email;
    private String name;
    private String profileImageUrl;
}
