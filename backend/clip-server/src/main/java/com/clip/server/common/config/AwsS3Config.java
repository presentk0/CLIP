package com.clip.server.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.profile:}")  //  빈 문자열 기본값!
    private String profile;

    /**
     * 환경별 AWS 자격증명 자동 분기
     *
     * - 로컬: aws.profile=clipzy-sso → SSO Profile 사용
     *   (~/.aws/credentials 파일 사용, 토큰 만료 시 재로그인)
     *
     * - 운영(EC2): aws.profile 비어있음 → DefaultCredentialsProvider
     *   (EC2 IAM Instance Profile 자동 인식)
     */
    private AwsCredentialsProvider credentialsProvider() {
        if (profile != null && !profile.isBlank()) {
            // 로컬 개발: SSO Profile
            return ProfileCredentialsProvider.create(profile);
        } else {
            // 운영: EC2 IAM Role / 환경변수 / Java System Property 자동 시도
            return DefaultCredentialsProvider.create();
        }
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