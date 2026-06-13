package com.clip.server.chat.entity;
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
        name = "user_weakness",
        indexes = {
                @Index(name = "idx_user_created", columnList = "user_id, created_at"),
                @Index(name = "idx_chat_room", columnList = "chat_room_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserWeakness {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @Column(name = "weak_expression", nullable = false, columnDefinition = "TEXT")
    private String weakExpression;

    @Column(name = "recommended_expression", nullable = false, columnDefinition = "TEXT")
    private String recommendedExpression;

    @Column(name = "review_count")
    private Integer reviewCount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserWeakness(
            User user,
            ChatRoom chatRoom,
            String weakExpression,
            String recommendedExpression
    ) {
        this.user = user;
        this.chatRoom = chatRoom;
        this.weakExpression = weakExpression;
        this.recommendedExpression = recommendedExpression;
        this.reviewCount = 0;
    }
}

