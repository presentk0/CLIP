package com.clip.server.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ChatScenarioResponse {
    private String targetWord;
    private List<ScenarioDto> scenarios;
}
