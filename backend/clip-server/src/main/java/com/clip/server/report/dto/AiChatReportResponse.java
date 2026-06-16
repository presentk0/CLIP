package com.clip.server.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AiChatReportResponse {

    private Long reportId;
    private String reportType;
    private LocalDateTime createdAt;
}
