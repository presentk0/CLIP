package com.clip.server.admin.dashboard.stats.controller;


import com.clip.server.admin.dashboard.stats.dto.request.AiUsageStatRequest;
import com.clip.server.admin.dashboard.stats.dto.request.ChatPatternStatRequest;
import com.clip.server.admin.dashboard.stats.dto.request.UserVideoStatRequest;
import com.clip.server.admin.dashboard.stats.dto.response.AiUsageStatResponse;
import com.clip.server.admin.dashboard.stats.dto.response.ChatPatternStatResponse;
import com.clip.server.admin.dashboard.stats.dto.response.UserVideoWordStatResponse;
import com.clip.server.admin.dashboard.stats.service.AdminStatsService;
import com.clip.server.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/stats")
@Tag(name = "관리자 대시보드 통계 API", description = "어드민 지표 분석 조회를 위한 API 세트입니다.")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    /**
     * 유저-영상별 단어 수집 통계 목록 조회
     * 예시 URL: /api/admin/stats/user-video/words?startDate=2026-06-01&limit=30
     */
    @Operation(summary = "유저 및 영상별 단어 수집 패턴 통계 조회",
            description = "유저가 특정 영상에서 단어를 몇 개나 수집(COLLECT/POPUP)했는지 행동 분포를 랭킹순으로 조회합니다.")
    @GetMapping("/user-video/words")
    public ResponseEntity<ApiResponse<List<UserVideoWordStatResponse>>> getUserVideoWordStats(
            @ModelAttribute UserVideoStatRequest request
    ) {
        List<UserVideoWordStatResponse> response = adminStatsService.getUserVideoWordStatistics(request);
        return ResponseEntity.ok(ApiResponse.success(response, "유저-영상별 단어 수집 통계 조회가 완료되었습니다."));
    }

    /**
     * AI 호출 횟수 비용 통계 조회
     * 예시 URL: /api/admin/stats/ai-usage?startDate=2026-06-01&endDate=2026-06-17
     */
    @Operation(summary = "AI 서비스 사용량(호출 횟수) 통계 조회",
            description = "지정된 기간 동안 대화봇 발화 및 퀴즈 해설 생성에 소모된 AI API 총 호출 건수를 조회합니다.")
    @GetMapping("/ai-usage")
    public ResponseEntity<ApiResponse<AiUsageStatResponse>> getAiUsageStats(
            @ModelAttribute AiUsageStatRequest request
    ) {
        AiUsageStatResponse response = adminStatsService.getAiUsageStatistics(request);
        return ResponseEntity.ok(ApiResponse.success(response, "AI 서비스 호출 통계 조회가 완료되었습니다."));
    }

    /**
     * AI 채팅 세션 사용 패턴 통계 조회
     * 예시 URL: /api/admin/stats/chat-patterns?startDate=2026-06-01&endDate=2026-06-17
     */
    @Operation(summary = "AI 채팅 세션 사용 패턴 및 성과 통계 조회",
            description = "유저들이 시나리오방을 완료한 비율, 평균 대화 마디 수(Turn), 평균 발음 점수 추이를 모니터링합니다.")
    @GetMapping("/chat-patterns")
    public ResponseEntity<ApiResponse<ChatPatternStatResponse>> getChatPatternStats(
            @ModelAttribute ChatPatternStatRequest request
    ) {
        ChatPatternStatResponse response = adminStatsService.getChatPatternStatistics(request);

        return ResponseEntity.ok(ApiResponse.success(response, "채팅 세션 패턴 통계 조회가 완료되었습니다."));
    }
}
