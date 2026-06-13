package com.clip.server.word.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WordMeaning {

    /** 품사 (한글: 명사, 동사, 형용사 등) */
    private String partOfSpeech;

    /** 해당 품사의 의미들 */
    private List<String> meanings;

    @JsonCreator
    public static WordMeaning of(
            @JsonProperty("partOfSpeech") String partOfSpeech,
            @JsonProperty("meanings") List<String> meanings
    ) {
        return new WordMeaning(partOfSpeech, meanings);
    }
}