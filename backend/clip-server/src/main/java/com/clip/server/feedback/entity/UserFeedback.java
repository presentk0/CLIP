package com.clip.server.feedback.entity;

import com.clip.server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_feedback",
        indexes = {
                @Index(name = "idx_user", columnList = "user_id"),
                @Index(name = "idx_created", columnList = "created_at"),
                @Index(name = "idx_score_created", columnList = "satisfaction_score, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "satisfaction_score", nullable = false)
    private Integer satisfactionScore;  // 1~5(서비스 만족 점수)

    @Column(name = "good_point", columnDefinition = "TEXT")
    private String goodPoint; // 좋았던 점

    @Column(name = "improve_point", columnDefinition = "TEXT")
    private String improvePoint; // 개선할 점

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 피드백 제출 시각

    @Builder
    public UserFeedback(User user, Integer satisfactionScore, String goodPoint, String improvePoint) {
        this.user = user;
        this.satisfactionScore = satisfactionScore;
        this.goodPoint = goodPoint;
        this.improvePoint = improvePoint;
    }


}