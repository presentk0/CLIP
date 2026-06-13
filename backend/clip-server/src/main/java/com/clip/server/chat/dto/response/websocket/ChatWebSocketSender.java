package com.clip.server.chat.dto.response.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatWebSocketSender {

    private static final String DESTINATION = "/queue/chat";

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 사용자에게 메시지 푸시
     */
    public <T> void send(Long userId, MessageType type, T data) {
        try {
            WebSocketMessage<T> message = WebSocketMessage.of(type, data);
            messagingTemplate.convertAndSendToUser(
                    userId.toString(),
                    DESTINATION,
                    message
            );
            log.debug("WebSocket 푸시. userId={}, type={}", userId, type);
        } catch (Exception e) {
            log.error("WebSocket 푸시 실패. userId={}, type={}", userId, type, e);
        }
    }
}