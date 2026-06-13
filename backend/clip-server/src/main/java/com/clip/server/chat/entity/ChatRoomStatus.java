package com.clip.server.chat.entity;

public enum ChatRoomStatus {
    IN_PROGRESS,   // 진행 중
    COMPLETED,     // 완료 (8마디 끝, 리포트 생성)
    ABANDONED      // 중도 포기 (새로 시작으로 버려진 방)
}
