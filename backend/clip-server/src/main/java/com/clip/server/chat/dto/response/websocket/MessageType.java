package com.clip.server.chat.dto.response.websocket;

public enum MessageType {
    USER_TEXT, // 사용자 음성을 텍스트로 변환한 결과
    PRONUNCIATION, // 발음 평가 결과
    AI_TEXT_CHUNK,
    AI_TEXT_DONE,
    AI_AUDIO, // AI 음성의 URL
    PROGRESS,
    SUGGESTION,
    HINT_OFFER,    // 힌트 제안 팝업 ("힌트 보기 좋은 타이밍이에요!")
    HINT_CARD,     // 힌트 카드 본체 (단어 + 예문)
    COMPLETION,
    ERROR
}
