package com.clip.server.chat.dto.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// AI 추천 결과
@Getter
@NoArgsConstructor
public class AiRecommendation {

    private String word;                          // 추천 단어
    private List<MeaningByPos> meaningsByPos;     // 품사별 의미
    private String relationType;                  // SYNONYM | ANTONYM | RELATED
    private String relatedToWord;                 // 연관 사용자 단어

    @JsonCreator
    public AiRecommendation(
            @JsonProperty("word") String word,
            @JsonProperty("meaningsByPos") List<MeaningByPos> meaningsByPos,
            @JsonProperty("relationType") String relationType,
            @JsonProperty("relatedToWord") String relatedToWord
    ) {
        this.word = word;
        this.meaningsByPos = meaningsByPos;
        this.relationType = relationType;
        this.relatedToWord = relatedToWord;
    }

    @Getter
    @NoArgsConstructor
    public static class MeaningByPos {
        private String partOfSpeech;
        private List<String> meanings;

        @JsonCreator
        public MeaningByPos(
                @JsonProperty("partOfSpeech") String partOfSpeech,
                @JsonProperty("meanings") List<String> meanings
        ) {
            this.partOfSpeech = partOfSpeech;
            this.meanings = meanings;
        }
    }
}
