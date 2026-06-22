package com.clip.server.quiz.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor  // Jackson 역직렬화용
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenAIQuizDataResponse {

    private String word;        // 퀴즈의 핵심 단어
    private String quizType;    // OX, BLANK, MATCHING (문자열로 받아 유연하게 처리)
    private String content;     // 영어 예문 (I want to [ ] around...)
    private String translation; // 예문에 대한 한글 해석
    private String question;    // 캐릭터가 던지는 질문 메시지 (가이드 문구)
    private String answer;      // 정답
    private String explanation; // 뉘앙스 차이를 포함한 친절한 해설
    private List<String> options; // 빈칸 채우기용 4지선다 보기 리스트

    @JsonProperty("correctFeedback")
    private String correctFeedback; // 정답시 피드백

    @JsonProperty("wrongFeedback")
    private String wrongFeedback; // 오답시 피드백

    @JsonProperty("relatedExpressions")
    private String relatedExpressions; // 함께 알아두면 좋은 표현
}
