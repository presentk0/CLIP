package com.clip.server.quiz.entity;

import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quiz_session")
@EntityListeners(AuditingEntityListener.class)
public class QuizSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id",nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id",nullable = false, columnDefinition = "VARCHAR(20)")
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SessionType sessionType;

    @Column(name = "total_quiz_count", nullable = false)
    private int totalQuizCount;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "wrong_count", nullable = false)
    private int wrongCount;

    @Column(name = "earned_exp", nullable = false)
    private int earnedExp;

    @CreatedDate
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startAt; // 퀴즈 시작 시각

    @Column(name = "completed_at")
    private LocalDateTime completedAt; // 퀴즈 종료 시각

    @Builder
    public QuizSession(User user, Video video, SessionType sessionType, int totalQuizCount) {
        this.user = user;
        this.video = video;
        this.sessionType = sessionType;
        this.totalQuizCount = totalQuizCount;
        this.correctCount = 0;
        this.wrongCount = 0;
        this.earnedExp = 0;
        this.startAt = LocalDateTime.now();
    }

    public void completeSession(int correctCount, int wrongCount, int earnedExp) {
        this.correctCount = correctCount;
        this.wrongCount = wrongCount;
        this.earnedExp = earnedExp;
        this.completedAt = LocalDateTime.now();
    }
}
