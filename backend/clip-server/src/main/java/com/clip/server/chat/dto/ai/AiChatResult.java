package com.clip.server.chat.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiChatResult {

    private String aiResponse;              // AI 답변 텍스트
    private String highlightWord;           // 강조 단어 (없으면 null)
    private Boolean isNatural;              // 사용자 표현이 자연스러운지
    private String recommendedAlternative;  // 자연스럽지 않으면 추천 표현
    private Boolean wordUsedNaturally;      // 타겟 단어를 자연스럽게 썼는지
}