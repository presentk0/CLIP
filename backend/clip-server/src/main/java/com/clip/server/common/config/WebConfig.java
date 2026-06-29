package com.clip.server.common.config;

import com.clip.server.analytics.interceptor.EventTrackingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final EventTrackingInterceptor eventTrackingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(eventTrackingInterceptor)
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