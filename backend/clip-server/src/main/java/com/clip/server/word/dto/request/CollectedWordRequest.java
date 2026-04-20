package com.clip.server.word.dto.request;

import com.clip.server.word.entity.WordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CollectedWordRequest {

    @NotBlank(message = "영상 Id는 필수입니다.")
    private String videoId; // 수집하려는 단어의 영상 Id
    @NotBlank(message = "영상 제목은 필수입니다.")
    private String title; // 수집하려는 영상 제목
    @NotBlank(message = "단어는 필수입니다.")
    private String word; // 수집하려는 단어
    @NotBlank(message = "문장은 필수입니다.")
    private String sentence; // 수집하려는 단어가 포함된 문장
    @NotBlank(message = "영상 타임스탬프는 필수입니다.")
    private String timestamp; // 수집하려는 단어의 영상 타임스탬프
    @NotBlank(message = "문장 번역은 필수입니다.")
    private String translation; // 수집하려는 단어가 포함된 문장 번역
    @NotBlank(message = "수집 타입은 필수입니다.")
    private WordType wordType; // COLLECT 또는 POPUP

}
