package com.clip.server.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ScenarioDto {
    private Long scenarioId;
    private String title;
    private String description;
}
