package com.clip.server.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * [RefreshTokenService]
 * Redis를 활용한 Refresh Token 관리 서비스
 * - 화이트리스트 방식으로 저장
 * - TTL 자동 만료
 * - 재사용 감지를 위한 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redisTemplate;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    private static final String RT_PREFIX = "RT:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Refresh Token 저장
     */
    public void save(Long userId, String refreshToken) {
        String key = RT_PREFIX + userId;
        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                Duration.ofMillis(refreshExpirationMs)
        );
        log.info("Refresh Token 저장 완료: userId={}", userId);
    }

    /**
     * 저장된 Refresh Token 조회
     */
    public String findByUserId(Long userId) {
        String key = RT_PREFIX + userId;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Refresh Token 일치 여부 확인
     */
    public boolean isValid(Long userId, String refreshToken) {
        String saved = findByUserId(userId);
        return saved != null && saved.equals(refreshToken);
    }

    /**
     * Refresh Token 삭제 (로그아웃)
     */
    public void delete(Long userId) {
        String key = RT_PREFIX + userId;
        redisTemplate.delete(key);
        log.info("Refresh Token 삭제 완료: userId={}", userId);
    }

    /**
     * Access Token 블랙리스트 추가 (로그아웃)
     */
    public void addToBlacklist(String accessToken, long remainingMs) {
        String key = BLACKLIST_PREFIX + accessToken;
        redisTemplate.opsForValue().set(
                key,
                "logout",
                Duration.ofMillis(remainingMs)
        );
        log.info("Access Token 블랙리스트 등록");
    }
}