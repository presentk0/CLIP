package com.clip.server.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.profile}")
    private String profile;

    /**
     * SSO 프로파일 기반 자격 증명
     * - ~/.aws/credentials 파일에서 자동 로드
     * - 토큰 만료 시 재로그인 필요 (aws sso login --profile clipzy-sso)
     */
    private ProfileCredentialsProvider credentialsProvider() {
        return ProfileCredentialsProvider.create(profile);
    }

    /**
     * S3 클라이언트 (파일 업로드/다운로드)
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }

    /**
     * S3 Presigner (Presigned URL 발급)
     */
    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider())
                .build();
    }
}