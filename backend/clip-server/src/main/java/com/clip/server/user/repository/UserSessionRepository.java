package com.clip.server.user.repository;

import com.clip.server.user.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    /**
     * 유저의 가장 최근 세션 조회
     * - 활동 시각 갱신 시 사용
     */
    Optional<UserSession> findFirstByUserIdOrderByLoginAtDesc(Long userId);

    /**
     * 기간 내 전체 세션 수
     */
    Long countByLoginAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 평균 체류 시간 (분)
     * - 0분 초과 세션만 집계 (의미 있는 세션만)
     */
    @Query("""
        SELECT AVG(s.durationMinutes)
        FROM UserSession s
        WHERE s.durationMinutes IS NOT NULL
          AND s.durationMinutes > 0
          AND s.loginAt >= :startDate
          AND s.loginAt < :endDate
    """)
    Double findAverageDurationMinutes(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 최대 체류 시간 (분)
     */
    @Query("""
        SELECT MAX(s.durationMinutes)
        FROM UserSession s
        WHERE s.loginAt >= :startDate
          AND s.loginAt < :endDate
    """)
    Integer findMaxDurationMinutes(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 활성 유저 수 (기간 내 로그인한 유니크 유저)
     */
    @Query("""
        SELECT COUNT(DISTINCT s.userId)
        FROM UserSession s
        WHERE s.loginAt >= :startDate
          AND s.loginAt < :endDate
    """)
    Long countDistinctActiveUsers(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 시간대별 로그인 분포 (0시 ~ 23시)
     * 결과: [hour, count]
     */
    @Query("""
        SELECT HOUR(s.loginAt) AS hour, COUNT(s) AS count
        FROM UserSession s
        WHERE s.loginAt >= :startDate
          AND s.loginAt < :endDate
        GROUP BY HOUR(s.loginAt)
        ORDER BY HOUR(s.loginAt)
    """)
    List<Object[]> findHourlyDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 요일별 로그인 분포 (1=일요일, 2=월요일, ..., 7=토요일)
     */
    @Query("""
        SELECT FUNCTION('DAYOFWEEK', s.loginAt) AS dayOfWeek, COUNT(s) AS count
        FROM UserSession s
        WHERE s.loginAt >= :startDate
          AND s.loginAt < :endDate
        GROUP BY FUNCTION('DAYOFWEEK', s.loginAt)
        ORDER BY FUNCTION('DAYOFWEEK', s.loginAt)
    """)
    List<Object[]> findDayOfWeekDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 일별 로그인 추이
     * - 날짜별 세션 수
     */
    @Query(value = """
    SELECT 
        DATE(s.login_at) AS date,
        COUNT(*) AS count
    FROM user_session s
    WHERE s.login_at >= :startDate
      AND s.login_at < :endDate
    GROUP BY DATE(s.login_at)
    ORDER BY DATE(s.login_at)
    """, nativeQuery = true)
    List<Object[]> findDailyTrend(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 체류 시간 구간별 분포
     * - 5분 미만, 5-15분, 15-30분, 30분 이상
     */
    @Query(value = """
    SELECT 
        CASE 
            WHEN duration_minutes < 5 THEN 'under5'
            WHEN duration_minutes >= 5 AND duration_minutes < 15 THEN '5to15'
            WHEN duration_minutes >= 15 AND duration_minutes < 30 THEN '15to30'
            ELSE 'over30'
        END AS bucket,
        COUNT(*) AS count
    FROM user_session
    WHERE login_at >= :startDate
      AND login_at < :endDate
      AND duration_minutes IS NOT NULL
    GROUP BY bucket
    """, nativeQuery = true)
    List<Object[]> findDurationDistribution(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    /**
     * 요일별 로그인 패턴
     * - 1=일, 2=월, ..., 7=토 (MySQL DAYOFWEEK)
     */
    @Query(value = """
    SELECT 
        DAYOFWEEK(login_at) AS dayOfWeek,
        COUNT(*) AS count
    FROM user_session
    WHERE login_at >= :startDate
      AND login_at < :endDate
    GROUP BY DAYOFWEEK(login_at)
    ORDER BY DAYOFWEEK(login_at)
    """, nativeQuery = true)
    List<Object[]> findDayOfWeekPattern(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );
}
