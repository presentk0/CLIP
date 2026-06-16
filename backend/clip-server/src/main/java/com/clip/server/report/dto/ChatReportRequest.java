package com.clip.server.report.dto;

import com.clip.server.report.entity.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatReportRequest {

    @NotNull(message = "신고 타입은 필수입니다.")
    private ReportType reportType;  // MESSAGE | SCENARIO

    /**
     * 신고 대상 ID
     * - reportType이 MESSAGE면 메시지 ID
     * - reportType이 SCENARIO면 채팅방 ID
     */
    @NotNull(message = "신고 대상 ID는 필수입니다.")
    private Long targetId;

    @NotBlank(message = "신고 내용은 필수입니다.")
    @Size(max = 1000, message = "신고 내용은 1000자 이내여야 합니다.")
    private String content;
}
