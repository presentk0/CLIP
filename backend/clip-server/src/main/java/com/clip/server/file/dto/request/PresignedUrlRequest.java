package com.clip.server.file.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PresignedUrlRequest {

    @NotBlank(message = "fileName은 필수입니다.")
    private String fileName; // 파일명 ex) user_audio.wav

    @NotBlank(message = "contentType은 필수입니다.")
    private String contentType;
}