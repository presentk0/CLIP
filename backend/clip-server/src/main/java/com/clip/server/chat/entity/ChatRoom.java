package com.clip.server.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "word_id")
    private Long wordId;  // 이어하기 시 null 허용

    @Column(name = "selected_scenario", length = 255)
    private String selectedScenario;

    @Column(name = "ai_gender", length = 20)
    @Enumerated(EnumType.STRING)
    private AiGender aiGender; // 채팅 AI 모델의 성별

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ChatRoomStatus status; // 채팅방 상태

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public ChatRoom(Long userId, Long wordId, String selectedScenario, AiGender aiGender) {
        this.userId = userId;
        this.wordId = wordId;
        this.selectedScenario = selectedScenario;
        this.aiGender = aiGender;
        this.status = ChatRoomStatus.IN_PROGRESS;
    }
}

