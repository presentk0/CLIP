package com.clip.server.file.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Presigner s3Presigner;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.audio-prefix}")
    private String audioPrefix;

    @Value("${aws.s3.presigned-url-expiration}")
    private long presignedUrlExpiration;

    @Value("${aws.region}")
    private String region;

    /**
     * 사용자 음성 업로드용 Presigned URL 발급
     * - 클라이언트가 이 URL로 직접 S3에 PUT
     */
    public PresignedUrlResult generateUserAudioUploadUrl(String fileName, String contentType) {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String objectKey = String.format(
                "%suser-voice/%s/%s_%s",
                audioPrefix, yearMonth, UUID.randomUUID(), fileName
        );

        return generatePresignedUrl(objectKey, contentType);
    }

    /**
     * AI TTS 음성 업로드 (서버에서 직접)
     * - byte[] 데이터를 S3에 저장
     * - 업로드 후 publicUrl 반환
     */
    public String uploadAiAudio(byte[] audioData, String contentType) {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String objectKey = String.format(
                "%sai-voice/%s/%s.mp3",
                audioPrefix, yearMonth, UUID.randomUUID()
        );

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength((long) audioData.length)
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(audioData));
        log.info("AI 음성 업로드 완료. key={}, size={}KB", objectKey, audioData.length / 1024);

        return buildPublicUrl(objectKey);
    }

    /**
     * Presigned URL 발급 (공통)
     */
    private PresignedUrlResult generatePresignedUrl(String objectKey, String contentType) {
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(presignedUrlExpiration))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        String presignedUrl = presignedRequest.url().toString();
        String publicUrl = buildPublicUrl(objectKey);

        log.info("Presigned URL 발급. key={}", objectKey);

        return new PresignedUrlResult(presignedUrl, publicUrl);
    }

    /**
     * S3 객체 키 → 공개 URL
     */
    private String buildPublicUrl(String objectKey) {
        return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, objectKey);
    }

    /**
     * Presigned URL 결과 (record 사용)
     */
    public record PresignedUrlResult(String presignedUrl, String publicUrl) {
    }

    /**
     * AI TTS 음성 업로드 + 재생용 Presigned URL 반환
     */
    public String uploadAiAudioAndGetUrl(byte[] audioData, String contentType) {
        String yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        String objectKey = String.format(
                "%sai-voice/%s/%s.mp3",
                audioPrefix, yearMonth, UUID.randomUUID()
        );

        // 1. 업로드
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .contentLength((long) audioData.length)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(audioData));
        log.info("AI 음성 업로드 완료. key={}", objectKey);

        // 2. 다운로드용 Presigned URL 발급 (1시간 유효)
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofHours(1))
                .getObjectRequest(getRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        String downloadUrl = presignedRequest.url().toString();

        log.info("AI 음성 재생 URL 발급. duration=1h");
        return downloadUrl;
    }

    /**
     * S3에서 파일 다운로드 (URL → 임시 파일)
     */
    public Path downloadToTempFile(String audioUrl) throws IOException {
        // 1. URL에서 S3 객체 키 추출
        String objectKey = extractObjectKey(audioUrl);
        log.info("S3 다운로드 시작. key={}", objectKey);

        // 2. S3에서 다운로드
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        Path tempFile = Files.createTempFile("audio_", ".tmp");

        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(request)) {
            Files.copy(response, tempFile, StandardCopyOption.REPLACE_EXISTING);
        }

        log.info("S3 다운로드 완료. path={}, size={}KB",
                tempFile, Files.size(tempFile) / 1024);

        return tempFile;
    }

    /**
     * S3 URL에서 객체 키 추출
     * https://bucket.s3.region.amazonaws.com/path/to/file.webm
     *   → path/to/file.webm
     */
    private String extractObjectKey(String url) {
        // 두 번째 / 이후 부분
        int domainEnd = url.indexOf('/', 8);  // https:// 이후
        if (domainEnd == -1) {
            throw new IllegalArgumentException("Invalid S3 URL: " + url);
        }

        String path = url.substring(domainEnd + 1);

        // 쿼리스트링 제거 (Presigned URL인 경우)
        int queryStart = path.indexOf('?');
        if (queryStart != -1) {
            path = path.substring(0, queryStart);
        }

        return path;
    }
}
