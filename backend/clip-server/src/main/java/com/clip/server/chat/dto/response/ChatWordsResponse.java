package com.clip.server.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatWordsResponse {

    private final boolean hasWords;
    private final List<Word> todayWords; // 오늘 학습한 단어
    private final List<Word> collectedWords; // 기존 수집 단어
    private final List<Word> recommendedWords; // AI 추천 단어
    private final Summary summary;

    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Word {
        private final Long wordId;
        private final String word;
        private final List<MeaningByPosDto> meaningsByPos; // 단어 품사와 뜻
        private final WordSource source;
        private final boolean isRecommended;
        private final LocalDateTime learnedAt;
        private final RelationType relationType;
        private final Related relatedTo;
    }

    @Getter
    @Builder
    public static class MeaningByPosDto {
        private final String partOfSpeech;
        private final List<String> meanings;
    }

    @Getter
    @Builder
    public static class Related {
        private final Long wordId;
        private final String word;
    }

    @Getter
    @Builder
    public static class Summary {
        private final int totalCollected;
        private final int totalRecommended;
        private final int maxCandidates;
    }

    public enum WordSource {
        TODAY_LEARNED,
        COLLECTED,
        AI_RECOMMENDED
    }

    public enum RelationType {
        SYNONYM, ANTONYM, RELATED
    }
}