package com.clip.server.auth.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "구글 ID 토큰은 필수입니다.")
    @JsonAlias({"id_token", "idToken"})
    private String idToken;
}
