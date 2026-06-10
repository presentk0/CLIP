package com.clip.server.common.websocket;

import com.clip.server.auth.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor
                .getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        // CONNECT 시점에만 인증 처리
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("WebSocket 연결 거부: Authorization 헤더 누락");
                throw new IllegalArgumentException("인증 토큰이 필요합니다.");
            }

            String token = authHeader.substring(7);

            try {
                if (!jwtProvider.validateAccessToken(token)) {
                    throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
                }

                // 토큰에서 userId 추출
                Long userId = jwtProvider.getUserId(token);

                // STOMP 세션에 사용자 정보 저장
                Principal principal = () -> userId.toString();
                accessor.setUser(principal);

                log.info("WebSocket 연결 성공. userId={}", userId);

            } catch (Exception e) {
                log.warn("WebSocket 인증 실패: {}", e.getMessage());
                throw new IllegalArgumentException("인증 실패: " + e.getMessage());
            }
        }

        return message;
    }
}