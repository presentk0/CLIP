package com.clip.server.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.clip.server.chat.entity.SenderType;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatMessageListResponse {

    private List<MessageDto> messages;

    @Getter
    @Builder
    public static class MessageDto {

        private Long messageId;

        private SenderType senderType;

        private String content;

        private String audioUrl;

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;
    }
}
