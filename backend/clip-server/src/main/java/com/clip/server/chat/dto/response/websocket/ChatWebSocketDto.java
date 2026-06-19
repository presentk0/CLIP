package com.clip.server.chat.dto.response.websocket;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

public class ChatWebSocketDto {

    // AI_TEXT_DONE: AI 답변 + 자연스러움 평가 정보
    @Getter
    @Builder
    public static class AiTextDone {
        private Long messageId;
        private String content;
        private String highlightWord;
        private Boolean isNatural;
        private String recommendedAlternative;
        private Boolean wordUsedNaturally;
    }

    // PROGRESS: 진행 상황
    @Getter
    @Builder
    public static class Progress {
        private int currentTurn;
        private int remainingTurn;
        private int maxTurn;
        private boolean completed;
    }

    // COMPLETION: 자동 종료
    @Getter
    @Builder
    public static class Completion {
        private Long chatRoomId;
        private String message;
    }

    // ERROR: 에러 발생
    @Getter
    @Builder
    public static class Error {
        private String code;
        private String message;
    }

     // USER_TEXT: 사용자 음성 → 텍스트 변환 결과
    @Getter
    @Builder
    public static class UserText {
        private Long messageId;
        private String content;          // STT 변환된 텍스트
        private String audioUrl;         // 사용자 음성 URL
    }

    // AI_AUDIO: AI 응답의 음성 URL
    @Getter
    @Builder
    public static class AiAudio {
        private Long messageId;          // 어떤 메시지의 음성인지
        private String audioUrl;         // AI 음성 URL (S3)
    }


    // PRONUNCIATION: 발음 평가 결과
    @Getter
    @Builder
    public static class Pronunciation {
        private Long messageId;
        private Integer pronunciationScore;
        private String feedback;
    }

    // HINT_OFFER: 힌트 제안 팝업 ("힌트 보기 좋은 타이밍이에요!")
    @Getter
    @Builder
    public static class HintOffer {
        private String message;        // "지금이 힌트 보기 좋은 타이밍이에요!"
        private String subMessage;     // "한 번 같이 볼까요?"
        private boolean hintAvailable; // 항상 true (팝업 표시 신호)
    }

    // HINT_CARD: 실제 힌트 카드 데이터 (단어 + 예문)
    @Getter
    @Builder
    public static class HintCard {
        private String word;                 // "perspective"
        private List<String> meanings;       // ["관점", "시각"]
        private String contextMessage;       // "친구가 \"다르게 보고 싶다\"라고 했어요"
        private String guideMessage;         // "perspective를 써서 위로해볼 수 있어요!"
        private String exampleSentence;      // "Glad you're seeing it from a new perspective."
        private String exampleTranslation;   // "새로운 시각으로 보게 돼서 다행이야."
        private String highlightWord;        // "perspective" (강조 표시)
    }
}