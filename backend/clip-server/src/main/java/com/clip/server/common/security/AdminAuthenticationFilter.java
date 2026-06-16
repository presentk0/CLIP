package com.clip.server.common.security;

import com.clip.server.auth.jwt.JwtProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 관리자 JWT 인증 필터
 * - /api/admin/** 경로에 한정해서 동작
 * - validateAdminToken으로 type=admin 검증
 * - SecurityContext에 ROLE_ADMIN 권한 부여
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ADMIN_PATH_PREFIX = "/api/admin/";
    private static final String ADMIN_LOGIN_PATH = "/api/admin/auth/login";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 관리자 경로가 아니면 통과 (이 필터는 /api/admin/** 만 처리)
        if (!path.startsWith(ADMIN_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 관리자 로그인 경로는 토큰 검증 없이 통과
        if (path.equals(ADMIN_LOGIN_PATH)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization 헤더에서 토큰 추출
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtProvider.validateAdminToken(token)) {
            Long adminId = jwtProvider.getUserId(token);

            // ROLE_ADMIN 권한으로 SecurityContext에 인증 객체 등록
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            adminId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("관리자 인증 성공. adminId={}", adminId);
        } else {
            log.warn("관리자 인증 실패. path={}", path);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰 추출
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}