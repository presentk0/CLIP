package com.clip.server.admin.dashboard.stats.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageStatRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate; // 검색 시작일 (선택)

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;   // 검색 종료일 (선택)
}