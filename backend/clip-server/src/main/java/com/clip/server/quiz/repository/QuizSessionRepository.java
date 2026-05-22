package com.clip.server.quiz.repository;

import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.SessionStatus;
import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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
            "AND qs.status = 'COMPLETED' " +
            "AND qs.completedAt >= :startOfDay " +
            "AND qs.completedAt < :endOfDay")
    boolean existsCompletedQuizToday(@Param("userId") Long userId,
                                     @Param("startOfDay") LocalDateTime startOfDay,
                                     @Param("endOfDay") LocalDateTime endOfDay);

}
