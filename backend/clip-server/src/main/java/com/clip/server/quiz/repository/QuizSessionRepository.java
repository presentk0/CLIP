package com.clip.server.quiz.repository;

import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.SessionStatus;
import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    Optional<QuizSession> findByUserAndVideo(User user, Video video);

    // 정복한 영상 종류 수 (중복 제거) ← 대시보드용
    @Query("SELECT COUNT(DISTINCT qs.video.videoId) " +
            "FROM QuizSession qs " +
            "WHERE qs.user.id = :userId " +
            "AND qs.status = :status")
    long countDistinctVideoByUserIdAndStatus(
            @Param("userId") Long userId,
            @Param("status") SessionStatus status
    );

    // 당일 영상 학습 여부
    @Query("SELECT COUNT(qs) > 0 FROM QuizSession qs " +
            "WHERE qs.user.id = :userId " +
            "AND qs.status = com.clip.server.quiz.entity.SessionStatus.COMPLETED " +
            "AND qs.completedAt >= :startOfDay " +
            "AND qs.completedAt < :endOfDay")
    boolean existsCompletedQuizToday(@Param("userId") Long userId,
                                     @Param("startOfDay") LocalDateTime startOfDay,
                                     @Param("endOfDay") LocalDateTime endOfDay);

    // 전체 완료된 퀴즈 세션 조회 - 평균 정확도용
    @Query("SELECT COALESCE(SUM(qs.totalQuizCount), 0) FROM QuizSession qs " +
            "WHERE qs.user.id = :userId " +
            "AND qs.status = com.clip.server.quiz.entity.SessionStatus.COMPLETED")
    long sumTotalQuizCount(@Param("userId") Long userId);

    @Query("SELECT COALESCE(SUM(qs.correctCount), 0) FROM QuizSession qs " +
            "WHERE qs.user.id = :userId " +
            "AND qs.status = com.clip.server.quiz.entity.SessionStatus.COMPLETED")
    long sumCorrectCount(@Param("userId") Long userId);

    // 특정 기간 내 완료된 퀴즈 세션 조회 - 정확도 계산용
    @Query("SELECT qs FROM QuizSession qs " +
            "WHERE qs.user.id = :userId " +
            "AND qs.status = com.clip.server.quiz.entity.SessionStatus.COMPLETED " +
            "AND qs.completedAt >= :startDate " +
            "AND qs.completedAt < :endDate " +
            "AND qs.totalQuizCount > 0")
    List<QuizSession> findCompletedSessionsBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );


    // 특정 기간 내 총 퀴즈 수 합계
    @Query("SELECT COALESCE(SUM(qs.totalQuizCount), 0) FROM QuizSession qs " +
            "WHERE qs.user.id = :userId " +
            "AND qs.status = com.clip.server.quiz.entity.SessionStatus.COMPLETED " +
            "AND qs.completedAt >= :startDate " +
            "AND qs.completedAt < :endDate")
    Long sumTotalQuizCountBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

     // 특정 기간 내 정답 수 합계
    @Query("SELECT COALESCE(SUM(qs.correctCount), 0) FROM QuizSession qs " +
            "WHERE qs.user.id = :userId " +
            "AND qs.status = com.clip.server.quiz.entity.SessionStatus.COMPLETED " +
            "AND qs.completedAt >= :startDate " +
            "AND qs.completedAt < :endDate")
    Long sumCorrectCountBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

}
