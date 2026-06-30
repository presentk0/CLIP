package com.clip.server.common.config;

import com.clip.server.analytics.interceptor.EventTrackingInterceptor;
import com.clip.server.analytics.mapper.EventNameMapper;
import com.clip.server.analytics.service.UserEventService;
import com.clip.server.auth.jwt.JwtProvider;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정
 * - 유저 행동 이벤트 추적 인터셉터 등록
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final UserEventService userEventService;
    private final EventNameMapper eventNameMapper;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 인터셉터 수동 생성 (테스트 슬라이스 영향 방지)
        EventTrackingInterceptor interceptor = new EventTrackingInterceptor(
                userEventService,
                eventNameMapper,
                userRepository,
                jwtProvider
        );

        registry.addInterceptor(interceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/refresh",
                        "/api/users/me",
                        "/api/translate/**",
                        "/api/files/**",
                        "/api/feedback/check",
                        "/api/videos/*/subtitles"
                );
    }
}