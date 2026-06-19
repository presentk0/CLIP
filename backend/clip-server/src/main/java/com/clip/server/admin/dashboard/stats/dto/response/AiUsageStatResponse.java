package com.clip.server.admin.dashboard.stats.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AiUsageStatResponse {
    private final Long totalCallCount;       // 총 AI 호출 횟수
    private final Long chatBotCallCount;     // AI 채팅 봇 발화 횟수
    private final Long quizExplanationCount; // 퀴즈 AI 해설 생성 횟수
}