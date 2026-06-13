package com.clip.server.chat.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "chat_message",
        indexes = {
                @Index(name = "idx_room_created", columnList = "chat_room_id, created_at"),
                @Index(name = "idx_room_sender", columnList = "chat_room_id, sender_type")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom; // 채팅방 ID

    @Column(name = "sender_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private SenderType senderType; // 발신자 유형(User, AI)

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content; // 메시지 내용(User: STT로 변환된 사용자 발화 또는 직접 입력 테스트, AI: LLM이 생성한 AI 응답 테스트)

    @Column(name = "audio_url", columnDefinition = "TEXT")
    private String audioUrl; // 음성파일 URL (User 사용자가 녹음한 음성 (음성 모드일 때만, 텍스트 모드는 null),  AI: TTS로 생성된 AI 음성 파일)

    @Column(name = "pronunciation_score", precision = 5, scale = 2)
    private BigDecimal pronunciationScore; // 발음 점수(텍스트 모드 또는 AI 메시지는 null)

    /**
     * LLM이 추천한 자연스러운 대체 표현
     * - isNatural = false 일 때만 값 존재
     * - 예: "I lost my path" → "I'm lost"
     */
    @Column(name = "is_natural")
    private Boolean isNatural; // 자연스러움 여부(User 메시지 평가)

    @Column(name = "recommended_alternative", columnDefinition = "TEXT")
    private String recommendedAlternative; // 어색한 표현 대체안(AI 추천)

    @Column(name = "turn_number")
    private Integer turnNumber;  // 대회 마디

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatMessage(
            ChatRoom chatRoom,
            SenderType senderType,
            String content,
            String audioUrl,
            BigDecimal pronunciationScore,
            Boolean isNatural,
            String recommendedAlternative,
            Integer turnNumber
    ) {
        this.chatRoom = chatRoom;
        this.senderType = senderType;
        this.content = content;
        this.audioUrl = audioUrl;
        this.pronunciationScore = pronunciationScore;
        this.isNatural = isNatural;
        this.recommendedAlternative = recommendedAlternative;
        this.turnNumber = turnNumber;
    }

    public void updateFeedback(Boolean isNatural, String recommendedAlternative) {
        this.isNatural = isNatural;
        this.recommendedAlternative = recommendedAlternative;
    }
}

