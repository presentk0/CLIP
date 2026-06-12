package com.clip.server.chat.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AIScenarioResult {

    private List<AIScenarioItem> scenarios;

    @Getter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AIScenarioItem {
        private String title;
        private String goal;        // 학습자에게 주는 미션
        private String situation;   // 학습자가 처한 상황 묘사
    }
}
