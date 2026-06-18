package com.clip.server.admin.dashboard.stats.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor // 테스트 코드 유연성을 위해 전체 생성자 열어둠
public class UserVideoStatRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate; // 검색 시작일 (선택)

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;   // 검색 종료일 (선택)

    private Integer limit = 50;  // 대시보드 과부하 방지용 조회 제한 수 (기본값 50개)
}
