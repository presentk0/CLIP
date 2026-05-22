package com.clip.server.user.entity.exp;

import com.clip.server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(
        name = "exp_log",
        indexes = {
                @Index(name = "idx_user_created", columnList = "user_id, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private ExpSourceType sourceType; // Exp 변동의 카테고리

    @Column(name = "source_id")
    private Long sourceId; // Exp 변동 발생의 원인 엔티티 ID

    /**
     * 표시용 제목 (예: "퀴즈 정답 (8문제)")
     * null인 경우 sourceType.defaultTitle 사용
     */
    @Column(length = 100)
    private String description; // 대시보드에 표시할 텍스트

    @Column(nullable = false)
    private int amount; // Exp 변동량

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // Exp 변동 발생 시각

    @Builder
    private ExpLog(User user, ExpSourceType sourceType, Long sourceId, String description, int amount) {
        this.user = user;
        this.sourceType = sourceType;
        this.sourceId = sourceId;
        this.description = description;
        this.amount = amount;
    }

    /**
     * 표시용 제목 반환
     * description이 있으면 description, 없으면 sourceType의 defaultTitle 사용
     */
    public String getTitle() {
        return (description != null && !description.isBlank())
                ? description
                : sourceType.getDefaultTitle();
    }
}