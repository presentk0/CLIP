package com.clip.server.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * [JwtProvider]
 * Access Token과 Refresh Token을 분리 관리하는 JWT 유틸 클래스
 * - 토큰 타입 구분으로 오용 방지
 * - 검증 메서드 분리
 */
@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secretKeyPlain;

    @Value("${jwt.access-expiration}")
    private long accessTokenExpirationTime;

    @Value("${jwt.refresh-expiration}")
    private long refreshTokenExpirationTime;

    private SecretKey key;

    // 토큰 타입 상수 (오타 방지)
    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKeyPlain.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Access Token 생성 (API 인증용, 짧은 수명)
     */
    public String createAccessToken(Long userId) {
        return buildToken(userId, TYPE_ACCESS, accessTokenExpirationTime);
    }

    /**
     * Refresh Token 생성 (재발급용, 긴 수명)
     */
    public String createRefreshToken(Long userId) {
        return buildToken(userId, TYPE_REFRESH, refreshTokenExpirationTime);
    }

    /**
     * 토큰 생성 공통 로직
     */
    private String buildToken(Long userId, String type, long expirationTime) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 Claims 추출 (공통 파싱 로직)
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 토큰에서 유저 ID(Subject) 추출
     */
    public Long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    /**
     * Access Token 검증
     */
    public boolean validateAccessToken(String token) {
        return validateToken(token, TYPE_ACCESS);
    }

    /**
     * Refresh Token 검증
     */
    public boolean validateRefreshToken(String token) {
        return validateToken(token, TYPE_REFRESH);
    }

    /**
     * 토큰 유효성 검증 (서명 + 만료 + 타입 체크)
     */
    private boolean validateToken(String token, String expectedType) {
        try {
            Claims claims = parseClaims(token);

            // 토큰 타입 일치 여부 확인 (Access/Refresh 오용 방지)
            String tokenType = claims.get(CLAIM_TYPE, String.class);
            if (!expectedType.equals(tokenType)) {
                log.error("토큰 타입 불일치. expected: {}, actual: {}", expectedType, tokenType);
                return false;
            }

            return true;
        } catch (SecurityException | MalformedJwtException | SignatureException e) {
            log.error("잘못된 JWT 서명입니다.");
        } catch (ExpiredJwtException e) {
            log.error("만료된 JWT 토큰입니다.");
        } catch (UnsupportedJwtException e) {
            log.error("지원되지 않는 JWT 토큰입니다.");
        } catch (IllegalArgumentException e) {
            log.error("JWT 토큰이 비어있거나 잘못되었습니다.");
        }
        return false;
    }

    /**
     * Refresh Token 만료 시간(ms) 반환
     * - Redis TTL 설정 시 사용
     */
    public long getRefreshTokenExpirationTime() {
        return refreshTokenExpirationTime;
    }
}