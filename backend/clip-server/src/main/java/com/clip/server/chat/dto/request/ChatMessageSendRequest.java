package com.clip.server.chat.dto.request;

import com.clip.server.chat.entity.InputMode;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageSendRequest {

    @NotNull(message = "chatRoomId는 필수입니다.")
    private Long chatRoomId;

    @NotNull(message = "inputMode는 필수입니다.")
    private InputMode inputMode;

    private String audioUrl;   // VOICE 모드 시 필수
    private String content;    // TEXT 모드 시 필수
}
