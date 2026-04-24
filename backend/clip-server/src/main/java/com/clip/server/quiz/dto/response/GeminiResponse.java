package com.clip.server.quiz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GeminiResponse {

    private List<Candidate> candidates;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Candidate {
        private Content content;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Content {
        private List<Part> parts;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    // AI의 핵심 답변(Text)만 쏙 뽑아주는 헬퍼 메서드
    public String getAnswer() {
        if (candidates != null && !candidates.isEmpty()) {
            return candidates.get(0).getContent().getParts().get(0).getText();
        }
        return null;
    }
}
