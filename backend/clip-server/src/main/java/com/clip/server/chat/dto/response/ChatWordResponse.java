package com.clip.server.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null 필드는 JSON 응답에서 제외
public class ChatWordResponse {

    private final Boolean hasWords; // 학습 단어 존재 여부(신규 회원 여부 차단용, false면 신규 유저)
    private final List<Word> todayWords; // 오늘 학습한 단어(1순위)
    private final List<Word> collectedWords; // 수집 단어(2순위)
    private final List<Word> recommendedWords; // AI 추천 단어(수집 단어와 1:1 비율)
    private final Summary summary; // 후보 단어 요약

    // 1. 단어 정보
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Word {

        /**
         * 단어 ID (AI 추천 단어는 null 가능)
         */
        private final Long wordId;

        /**
         * 단어 텍스트
         */
        private final String word;

        /**
         * 품사별 의미 목록
         */
        private final List<Meaning> meaningsByPos;

        /**
         * 단어 출처
         */
        private final WordSource source;

        /**
         * AI 추천 단어 강조 표시용
         */
        private final boolean isRecommended;

        /**
         * 학습 시각 (오늘 학습 단어만)
         */
        private final LocalDateTime learnedAt;

        /**
         * 관계 유형 (AI 추천 단어만)
         */
        private final RelationType relationType;

        /**
         * 추천 단어의 원본 (AI 추천 단어만)
         */
        private final Related relatedTo;
    }

    // 2. 품사별 의미
    @Getter
    @Builder
    public static class Meaning {

        /**
         * 품사 (명사, 형용사, 동사 등)
         */
        private final String partOfSpeech;

        /**
         * 의미
         */
        private final String meaning;
    }


    // 3. 관련 단어 (AI 추천의 원본 단어)
    @Getter
    @Builder
    public static class Related {

        /**
         * 원본 단어 ID
         */
        private final Long wordId;

        /**
         * 원본 단어 텍스트
         */
        private final String word;
    }

    // 4. 요약 정보
    @Getter
    @Builder
    public static class Summary {

        /**
         * 수집 단어 개수
         */
        private final int totalCollected;

        /**
         * AI 추천 단어 개수
         */
        private final int totalRecommended;

        /**
         * 최대 후보 단어 수
         */
        private final int maxCandidates;
    }

    // 5. Enum: 단어 출처
    public enum WordSource {
        TODAY_LEARNED,    // 오늘 학습한 단어
        COLLECTED,        // 수집 단어
        AI_RECOMMENDED    // AI 추천 단어
    }

    // 6. Enum: 단어 관계 유형
    public enum RelationType {
        SYNONYM,    // 유의어
        ANTONYM,    // 반의어
        RELATED     // 관련어
    }
}
