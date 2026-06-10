package com.clip.server.chat.entity;

import com.clip.server.user.entity.User;
import com.clip.server.word.entity.CollectedWord;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_room",
        indexes = {
                @Index(name = "idx_user_status", columnList = "user_id, status"),
                @Index(name = "idx_user_created", columnList = "user_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id")
    private CollectedWord word;  // 이어하기 시 null 가능

    @Column(name = "selected_scenario", length = 255)
    private String selectedScenario;

    @Column(name = "ai_gender", length = 20)
    @Enumerated(EnumType.STRING)
    private AiGender aiGender; // 채팅 AI 모델의 성별

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ChatRoomStatus status; // 채팅방 상태

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public ChatRoom(User user, CollectedWord word, String selectedScenario, AiGender aiGender) {
        this.user = user;
        this.word = word;
        this.selectedScenario = selectedScenario;
        this.aiGender = aiGender;
        this.status = ChatRoomStatus.IN_PROGRESS;
    }

    public void complete() {
        this.status = ChatRoomStatus.COMPLETED;
    }
}

