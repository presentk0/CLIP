package com.clip.server.word.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectedWordRequest {

    // 수집하려는 단어의 영상 Id
    private String videoId;
    // 수집하려는 영상 제목
    private String title;
    // 수집하려는 단어
    private String word;
    // 수집하려는 단어가 포함된 문장
    private String sentence;
    // 수집하려는 단어의 영상 타임스탬프
    private String timestamp;
    // 수집하려는 단어가 포함된 문장 번역
    private String translation;
}
