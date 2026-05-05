package com.clip.server.quiz.dto.request;

import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GeminiRequest {

    private List<Content> contents;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    private static class Content {
        private List<Part> parts;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Part {
        private String text;
    }

    // 편의를 위해 "Hello" 같은 문자열만 넣으면 객체를 만들어주는 static 메서드입니다.
    public static GeminiRequest from(String prompt) {
        Part part = new Part(prompt);
        Content content = new Content(List.of(part));
        return new GeminiRequest(List.of(content));
    }
}
