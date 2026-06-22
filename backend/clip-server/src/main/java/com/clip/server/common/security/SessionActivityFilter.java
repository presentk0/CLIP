package com.clip.server.common.security;

import com.clip.server.user.service.UserSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 사용자 세션 활동 갱신 필터
 * - 인증된 일반 유저의 마지막 활동 시각 업데이트
 * - 체류 시간(duration_minutes) 자동 계산
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionActivityFilter extends OncePerRequestFilter {

    private final UserSessionService userSessionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // 인증된 일반 유저만 처리
            if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof Long userId) {
                userSessionService.updateUserActivity(userId);
            }
        } catch (Exception e) {
            log.warn("세션 활동 갱신 실패: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/auth/")
                || path.startsWith("/api/admin/")
                || path.equals("/health")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/ws-");
    }

}