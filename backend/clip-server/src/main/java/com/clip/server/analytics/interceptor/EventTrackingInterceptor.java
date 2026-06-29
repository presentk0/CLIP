package com.clip.server.analytics.interceptor;

import com.clip.server.analytics.entity.UserEvent;
import com.clip.server.analytics.mapper.EventNameMapper;
import com.clip.server.analytics.service.UserEventService;
import com.clip.server.auth.jwt.JwtProvider;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventTrackingInterceptor implements HandlerInterceptor {

    private final UserEventService userEventService;
    private final EventNameMapper eventNameMapper;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    private static final String START_TIME_ATTR = "eventStartTime";

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        req.setAttribute(START_TIME_ATTR, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest req, HttpServletResponse res,
                                Object handler, Exception ex) {
        try {
            String method = req.getMethod();
            String path = req.getRequestURI();

            // 1. 추적 대상 이벤트인지 확인
            Optional<String> eventNameOpt = eventNameMapper.resolveEventName(method, path);
            if (eventNameOpt.isEmpty()) return;

            // 2. JWT에서 userId 추출
            Long userId = extractUserId(req);
            if (userId == null) return; // 비로그인 요청은 스킵

            // 3. User 조회
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) return;

            // 4. 소요 시간 계산
            Long startTime = (Long) req.getAttribute(START_TIME_ATTR);
            long durationMs = startTime != null ? System.currentTimeMillis() - startTime : 0L;

            // 5. 이벤트 생성
            UserEvent event = UserEvent.builder()
                    .user(user)
                    .eventName(eventNameOpt.get())
                    .pagePath(path)
                    .httpMethod(method)
                    .statusCode(res.getStatus())
                    .durationMs(durationMs)
                    .build();

            // 6. 비동기 저장 (응답 속도 영향 없음)
            userEventService.saveAsync(event);

        } catch (Exception e) {
            log.warn("Event tracking failed: {}", e.getMessage());
        }
    }

    /**
     * Authorization 헤더에서 Access Token 파싱 → userId 추출
     */
    private Long extractUserId(HttpServletRequest req) {
        try {
            String authHeader = req.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }

            String token = authHeader.substring(7);

            // 토큰 유효성 검증 (만료/위조 토큰 제외)
            if (!jwtProvider.validateAccessToken(token)) {
                return null;
            }

            return jwtProvider.getUserId(token);
        } catch (Exception e) {
            return null;
        }
    }
}