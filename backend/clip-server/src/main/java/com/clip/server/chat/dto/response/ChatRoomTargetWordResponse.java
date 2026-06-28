package com.clip.server.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRoomTargetWordResponse {

    private Long wordId;
    private String word; // AI 채팅 선택 단어
    private List<String> meanings; // 단어 뜻

}
