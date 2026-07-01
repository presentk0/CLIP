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
        private SenderType senderType; // 메시지 주체
        private String content; // 메시지 내용
        private String audioUrl; // 오디오 URL
        private Integer turnNumber; // 대화 턴
        private Integer remainingTurn; // 남은 턴

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime createdAt;
    }
}
