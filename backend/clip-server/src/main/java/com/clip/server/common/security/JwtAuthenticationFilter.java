package com.clip.server.common.security;

import com.clip.server.auth.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT 인증 필터
 * - 매 요청마다 Authorization 헤더의 토큰 검증
 * - 블랙리스트(로그아웃) 체크
 * - SecurityContext에 인증 정보 저장
 */
@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final StringRedisTemplate redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);

            if (token != null && jwtProvider.validateAccessToken(token)) {

                // 블랙리스트 체크
                if (isBlacklisted(token)) {
                    log.warn("블랙리스트 토큰 접근 시도");
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                // 인증 정보 설정
                setAuthentication(token, request);
            }
        } catch (Exception e) {
            log.error("인증 필터 처리 중 오류 발생: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 인증 제외 경로 (로그인, 헬스체크 등)
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/") // 로그인 관련 기능
                || path.equals("/health") // 헬스체크
                || path.startsWith("/swagger-ui/") // Swagger 명세서
                || path.startsWith("/v3/api-docs");
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }

    /**
     * 블랙리스트 등록 여부 확인
     */
    private boolean isBlacklisted(String token) {
        String value = redisTemplate.opsForValue().get(BLACKLIST_PREFIX + token);
        return StringUtils.hasText(value);
    }

    /**
     * SecurityContext에 인증 정보 저장
     */
    private void setAuthentication(String token, HttpServletRequest request) {
        Long userId = jwtProvider.getUserId(token);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}