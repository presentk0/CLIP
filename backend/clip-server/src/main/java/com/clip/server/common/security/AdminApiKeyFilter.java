package com.clip.server.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-Admin-Api-Key";
    private static final String ADMIN_PATH_PREFIX = "/api/admin";

    @Value("${admin.api-key}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // /api/admin 경로가 아니면 그냥 통과
        if (!request.getRequestURI().startsWith(ADMIN_PATH_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || !apiKey.equals(adminApiKey)) {
            log.warn("관리자 API 인증 실패: uri={}, ip={}",
                    request.getRequestURI(), request.getRemoteAddr());

            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                    "{\"error\": \"Unauthorized\", \"message\": \"유효하지 않은 관리자 API Key입니다.\"}"
            );
            return;
        }

        // 인증 성공 → SecurityContext에 ADMIN 권한 부여
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "ADMIN",
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.info("관리자 API 인증 성공: uri={}", request.getRequestURI());
        filterChain.doFilter(request, response);
    }
}