package com.clip.server.chat.dto.response.websocket;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WebSocketMessage<T> {
    private MessageType type;
    private T data;

    public static <T> WebSocketMessage<T> of(MessageType type, T data) {
        return WebSocketMessage.<T>builder()
                .type(type)
                .data(data)
                .build();
    }
}