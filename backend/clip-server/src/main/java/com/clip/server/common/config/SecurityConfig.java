package com.clip.server.common.config;

import com.clip.server.common.security.AdminAuthenticationFilter;  // 🆕
import com.clip.server.common.security.JwtAuthenticationFilter;
import com.clip.server.common.security.SessionActivityFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AdminAuthenticationFilter adminAuthenticationFilter;
    private final CorsProperties corsProperties;
    private final SessionActivityFilter sessionActivityFilter;

    // 현재 구동 중인 환경(Profile) 정보
    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> {
                    // 1. 공통 허용 경로
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .requestMatchers(
                                    "/api/auth/**",
                                    "/api/admin/auth/login",
                                    "/health",
                                    "/ws-chat/**"
                            ).permitAll();

                    // 2. Swagger 프로필별 조건부 분기
                    if ("prod".equals(activeProfile)) {
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").hasRole("ADMIN");
                        log.info("운영 환경(prod): Swagger UI 접근이 ADMIN으로 제한됩니다.");
                    } else {
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll();
                        log.info("개발 환경({}): Swagger UI가 공개 상태입니다.", activeProfile);
                    }

                    // 3. 관리자 전용 및 나머지 경로
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(adminAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(sessionActivityFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        log.info("=== CORS Allowed Origins: {} ===", corsProperties.getAllowedOrigins());

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}