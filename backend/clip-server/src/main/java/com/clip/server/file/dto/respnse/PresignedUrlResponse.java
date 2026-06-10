package com.clip.server.file.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedUrlResponse {

    private String presignedUrl; // S3 직접 업로드용 URL (5분 유효)

    private String audioUrl; // 업로드 후 접근 가능한 최종 URL

    private long expiresIn; // URL 유효 시간 (초)
}