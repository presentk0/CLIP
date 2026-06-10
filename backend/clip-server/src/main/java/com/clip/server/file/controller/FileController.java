package com.clip.server.file.controller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.file.dto.request.PresignedUrlRequest;
import com.clip.server.file.dto.response.PresignedUrlResponse;
import com.clip.server.file.service.S3Service;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File", description = "파일 업로드 API")
public class FileController {

    private final S3Service s3Service;

    @Value("${aws.s3.presigned-url-expiration}")
    private long presignedUrlExpiration;

    @Operation(summary = "음성 업로드 Presigned URL 발급",
            description = "클라이언트가 S3에 직접 음성 파일을 업로드할 수 있는 임시 URL을 발급합니다. (5분 유효)")
    @PostMapping("/audio/presigned-url")
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> getAudioPresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        S3Service.PresignedUrlResult result = s3Service.generateUserAudioUploadUrl(
                request.getFileName(),
                request.getContentType()
        );

        PresignedUrlResponse response = PresignedUrlResponse.builder()
                .presignedUrl(result.presignedUrl())
                .audioUrl(result.publicUrl())
                .expiresIn(presignedUrlExpiration)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, "Presigned URL이 발급되었습니다."));
    }
}