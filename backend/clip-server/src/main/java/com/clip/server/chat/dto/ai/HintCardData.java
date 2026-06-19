package com.clip.server.chat.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class HintCardData {
    private String contextMessage;        // "친구가 "다르게 보고 싶다"라고 했어요"
    private String guideMessage;          // "perspective를 써서 위로해볼 수 있어요!"
    private String exampleSentence;       // "Glad you're seeing it from a new perspective."
    private String exampleTranslation;    // "새로운 시각으로 보게 돼서 다행이야."
}