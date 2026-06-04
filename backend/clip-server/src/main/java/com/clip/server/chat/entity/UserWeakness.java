package com.clip.server.chat.entity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_weakness",
        indexes = {
                @Index(name = "idx_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_chat_room", columnList = "chat_room_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserWeakness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "chat_room_id", nullable = false)
    private Long chatRoomId;

    @Column(name = "weak_expression", nullable = false, columnDefinition = "TEXT")
    private String weakExpression;

    @Column(name = "recommended_expression", nullable = false, columnDefinition = "TEXT")
    private String recommendedExpression;

    @Column(name = "review_count")
    private Integer reviewCount;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserWeakness(
            Long userId,
            Long chatRoomId,
            String weakExpression,
            String recommendedExpression
    ) {
        this.userId = userId;
        this.chatRoomId = chatRoomId;
        this.weakExpression = weakExpression;
        this.recommendedExpression = recommendedExpression;
        this.reviewCount = 0;
    }
}

