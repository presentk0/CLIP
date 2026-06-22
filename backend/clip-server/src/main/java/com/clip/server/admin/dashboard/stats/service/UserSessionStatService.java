package com.clip.server.admin.dashboard.stats.service;

import com.clip.server.admin.dashboard.stats.dto.response.UserSessionStatResponse;
import com.clip.server.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserSessionStatService {

    private final UserSessionRepository userSessionRepository;

    private static final String[] DAY_NAMES = {"", "일", "월", "화", "수", "목", "금", "토"};

    public UserSessionStatResponse getUserSessionStats(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = (startDate != null)
                ? startDate.atStartOfDay()
                : LocalDateTime.now().minusDays(30);
        LocalDateTime end = (endDate != null)
                ? endDate.atTime(23, 59, 59)
                : LocalDateTime.now();

        log.info("유저 세션 통계 조회: {} ~ {}", start, end);

        // 기본 통계
        Long totalSessions = userSessionRepository.countByLoginAtBetween(start, end);
        Long activeUsers = userSessionRepository.countDistinctActiveUsers(start, end);
        Double avgDuration = userSessionRepository.findAverageDurationMinutes(start, end);
        Integer maxDuration = userSessionRepository.findMaxDurationMinutes(start, end);

        // 시간대별 분포
        List<UserSessionStatResponse.HourlyLogin> hourlyDistribution =
                buildHourlyDistribution(start, end);


        List<UserSessionStatResponse.DailyTrend> dailyTrend = buildDailyTrend(start, end);
        UserSessionStatResponse.DurationDistribution durationDistribution = buildDurationDistribution(start, end);
        List<UserSessionStatResponse.DayOfWeekPattern> dayOfWeekPattern = buildDayOfWeekPattern(start, end);

        return UserSessionStatResponse.builder()
                .totalSessions(totalSessions != null ? totalSessions : 0L)
                .activeUsers(activeUsers != null ? activeUsers : 0L)
                .averageDurationMinutes(avgDuration != null ? avgDuration : 0.0)
                .maxDurationMinutes(maxDuration != null ? maxDuration : 0)
                .hourlyDistribution(hourlyDistribution)
                .dailyTrend(dailyTrend)
                .durationDistribution(durationDistribution)
                .dayOfWeekPattern(dayOfWeekPattern)
                .build();
    }

    /**
     * 시간대별 분포 (0~23시 모두 채움)
     */
    private List<UserSessionStatResponse.HourlyLogin> buildHourlyDistribution(
            LocalDateTime start, LocalDateTime end
    ) {
        List<Object[]> rawData = userSessionRepository.findHourlyDistribution(start, end);
        Map<Integer, Long> hourCountMap = new HashMap<>();

        for (Object[] row : rawData) {
            Integer hour = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            hourCountMap.put(hour, count);
        }

        List<UserSessionStatResponse.HourlyLogin> result = new ArrayList<>();
        for (int hour = 0; hour < 24; hour++) {
            result.add(UserSessionStatResponse.HourlyLogin.builder()
                    .hour(hour)
                    .count(hourCountMap.getOrDefault(hour, 0L))
                    .build());
        }

        return result;
    }

    /**
     * 일별 추이
     */
    private List<UserSessionStatResponse.DailyTrend> buildDailyTrend(
            LocalDateTime start, LocalDateTime end
    ) {
        List<Object[]> rawData = userSessionRepository.findDailyTrend(start, end);
        List<UserSessionStatResponse.DailyTrend> result = new ArrayList<>();

        for (Object[] row : rawData) {
            String date = row[0].toString();  // java.sql.Date → String
            Long count = ((Number) row[1]).longValue();

            result.add(UserSessionStatResponse.DailyTrend.builder()
                    .date(date)
                    .count(count)
                    .build());
        }

        return result;
    }

    /**
     * 체류 시간 분포
     */
    private UserSessionStatResponse.DurationDistribution buildDurationDistribution(
            LocalDateTime start, LocalDateTime end
    ) {
        List<Object[]> rawData = userSessionRepository.findDurationDistribution(start, end);
        Map<String, Long> bucketMap = new HashMap<>();

        for (Object[] row : rawData) {
            String bucket = (String) row[0];
            Long count = ((Number) row[1]).longValue();
            bucketMap.put(bucket, count);
        }

        return UserSessionStatResponse.DurationDistribution.builder()
                .under5min(bucketMap.getOrDefault("under5", 0L))
                .min5to15(bucketMap.getOrDefault("5to15", 0L))
                .min15to30(bucketMap.getOrDefault("15to30", 0L))
                .over30min(bucketMap.getOrDefault("over30", 0L))
                .build();
    }

    /**
     * 요일별 패턴
     */
    private List<UserSessionStatResponse.DayOfWeekPattern> buildDayOfWeekPattern(
            LocalDateTime start, LocalDateTime end
    ) {
        List<Object[]> rawData = userSessionRepository.findDayOfWeekPattern(start, end);
        Map<Integer, Long> dayMap = new HashMap<>();

        for (Object[] row : rawData) {
            Integer dayOfWeek = ((Number) row[0]).intValue();
            Long count = ((Number) row[1]).longValue();
            dayMap.put(dayOfWeek, count);
        }

        // 1(일) ~ 7(토) 모두 채움 - 데이터 없는 요일도 0으로
        List<UserSessionStatResponse.DayOfWeekPattern> result = new ArrayList<>();
        for (int day = 1; day <= 7; day++) {
            result.add(UserSessionStatResponse.DayOfWeekPattern.builder()
                    .dayOfWeek(day)
                    .dayName(DAY_NAMES[day])
                    .count(dayMap.getOrDefault(day, 0L))
                    .build());
        }

        return result;
    }
}