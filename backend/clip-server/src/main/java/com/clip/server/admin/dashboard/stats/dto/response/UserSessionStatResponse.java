package com.clip.server.admin.dashboard.stats.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class UserSessionStatResponse {

    // 기본 통계
    private final Long totalSessions;
    private final Long activeUsers;
    private final Double averageDurationMinutes;
    private final Integer maxDurationMinutes;

    // 시간대별
    private final List<HourlyLogin> hourlyDistribution;

    // 일별 추이
    private final List<DailyTrend> dailyTrend;

    // 체류 시간 분포
    private final DurationDistribution durationDistribution;

    // 요일별 패턴
    private final List<DayOfWeekPattern> dayOfWeekPattern;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class HourlyLogin {
        private final Integer hour;
        private final Long count;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DailyTrend {
        private final String date;       // "2024-12-01"
        private final Long count;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DurationDistribution {
        private final Long under5min;     // 5분 미만
        private final Long min5to15;      // 5-15분
        private final Long min15to30;     // 15-30분
        private final Long over30min;     // 30분 이상
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class DayOfWeekPattern {
        private final Integer dayOfWeek;  // 1(일) ~ 7(토)
        private final String dayName;     // "월", "화", ...
        private final Long count;
    }
}