package com.clip.server.chat.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRoomInitResponse {

    private Long chatRoomId; // 채팅 방 번호
    private boolean hasPreviousMessages; // 이 방에 과거 대화 내역이 남아 있는지 여부(새로운 채팅방:false, 이어하기:true)
    private String scenarioTitle; // 시나리오 제목
    private String scenarioGoal; // 학습자 미션
    private String scenarioSituation; // 시나리오 상황

    // 이어하기 팝업에 사용
    private String word;
    private List<String> meanings;
}
