package com.clip.server.admin.dashboard.stats.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ChatPatternStatResponse {
    private final Long activeRoomsCount;       // 현재 진행 중인 채팅방 수 (IN_PROGRESS)
    private final Long completedRoomsCount;    // 완료된 채팅방 수 (COMPLETED)
    private final Double averageTurnsPerRoom;  // 방 하나당 평균 대화 주고받은 횟수 (Turn)
    private final Double averagePronunciationScore; // 유저들의 평균 발음 정확도 점수
}