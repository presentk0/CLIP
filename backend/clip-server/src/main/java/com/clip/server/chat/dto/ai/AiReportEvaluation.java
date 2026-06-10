package com.clip.server.chat.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiReportEvaluation {

    private Integer overallScore;     // 0~100
    private String goodPoints;        // 잘한 점
    private String improvePoints;     // 개선할 점
    private String wordUsageFeedback; // 타겟 단어 사용 피드백
    private String expressionFeedback; // 표현 자연스러움 피드백
}