package com.clip.server.chat.controller;

import com.clip.server.chat.dto.request.ChatMessageSendRequest;
import com.clip.server.chat.service.ChatMessageOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    //private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageOrchestrator chatMessageOrchestrator;


//    /**
//     * 테스트용 Echo
//     * 클라이언트가 /app/chat/echo로 보내면, /user/queue/chat으로 응답
//     */
//    @MessageMapping("/chat/echo")
//    public void echo(@Payload Map<String, Object> payload, Principal principal) {
//        Long userId = Long.parseLong(principal.getName());
//        log.info("Echo 수신. userId={}, payload={}", userId, payload);
//
//        // 사용자에게 1:1 응답
//        messagingTemplate.convertAndSendToUser(
//                userId.toString(),
//                "/queue/chat",
//                Map.of(
//                        "type", "ECHO",
//                        "data", Map.of(
//                                "message", "Hello! Your message was received.",
//                                "received", payload
//                        )
//                )
//        );
//    }

    /**
     * 채팅 메시지 전송
     * Destination: /app/chat/send
     */
    @MessageMapping("/chat/send")
    public void sendMessage(
            @Payload ChatMessageSendRequest request,
            Principal principal
    ) {
        Long userId = Long.parseLong(principal.getName());
        log.info("메시지 수신. userId={}, chatRoomId={}, inputMode={}",
                userId, request.getChatRoomId(), request.getInputMode());

        // Orchestrator로 위임 (비동기 처리)
        chatMessageOrchestrator.processMessage(userId, request);
    }
}
