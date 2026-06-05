package com.clip.server.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ChatRoomInitResponse {

    private Long chatRoomId; // 채팅 방 번호
    private boolean hasPreviousMessages; // 이 방에 과거 대화 내역이 남아 있는지 여부(새로운 채팅방:false, 이어하기:true)
}
