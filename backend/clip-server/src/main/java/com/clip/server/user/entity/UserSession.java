package com.clip.server.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "user_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt; // 로그인 시각

    @Column(name = "last_activity_at", nullable = false)
    private LocalDateTime lastActivityAt; // 마지막 활동 시각

    @Column(name = "duration_minutes")
    private Integer durationMinutes; // 서비스 체류 시간

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 테이블 생성 시각

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 테이블 수정 시각

    @Builder
    public UserSession(User user, LocalDateTime loginAt) {
        this.user = user;
        this.loginAt = loginAt;
        this.lastActivityAt = loginAt;
        this.durationMinutes = 0;
    }

    // 마지막 활동 시각 업데이트
    public void updateLastActivity() {
        this.lastActivityAt = LocalDateTime.now();
        this.durationMinutes = (int) ChronoUnit.MINUTES.between(this.loginAt, this.lastActivityAt);
    }
}