package com.clip.server.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class ChatReportResponse {

    private Long chatRoomId;
    private Summary summary;
    private WordUsage wordUsage;
    private PronunciationScore pronunciationScore;
    private ExpressionNaturalness expressionNaturalness;
    private Overall overall;
    private WeaknessAnalysis weaknessAnalysis;

    /**
     * ① 요약 정보
     */
    @Getter
    @Builder
    public static class Summary {
        private String targetWord;
        private int totalTurns;
        private String duration;        // "00:04:32" 형식
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        private LocalDateTime completedAt;
        private String inputMode;       // "voice" or "text"
    }

    /**
     * ② 단어 사용 체크
     */
    @Getter
    @Builder
    public static class WordUsage {
        private String targetWord;
        private boolean used;
        private int usageCount;
        private List<UsageContext> usageContext;
        private String feedback;

        @Getter
        @Builder
        public static class UsageContext {
            private Long messageId;
            private String userSentence;
            private boolean natural;
        }
    }

    /**
     * ③ 발음 정확도 (음성 모드 한정)
     */
    @Getter
    @Builder
    public static class PronunciationScore {
        private boolean available;
        private Integer overallScore;
        private String feedback;
        private List<WeakSentence> weakSentences;

        @Getter
        @Builder
        public static class WeakSentence {
            private Long messageId;
            private String original;
            private Integer score;
            private String issue;
            private String modelAudioUrl;
        }
    }

    /**
     * ④ 표현 자연스러움
     */
    @Getter
    @Builder
    public static class ExpressionNaturalness {
        private Integer overallScore;
        private String feedback;
        private List<Improvement> improvements;

        @Getter
        @Builder
        public static class Improvement {
            private Long messageId;
            private String original;
            private String suggested;
            private String explanation;
            private String modelAudioUrl;
        }
    }

    /**
     * ⑤ 종합 평가
     */
    @Getter
    @Builder
    public static class Overall {
        private Integer score;
        private String goodPoints;
        private String improvePoints;
    }

    /**
     * ⑥ 약점 분석
     */
    @Getter
    @Builder
    public static class WeaknessAnalysis {
        private List<String> awkwardExpressions;
        private List<String> avoidedSituations;
        private boolean willBeUsedInNextScenario;
    }
}