package com.clip.server.chat.entity;

import com.clip.server.user.entity.User;
import com.clip.server.word.entity.CollectedWord;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    @Column(name = "scenario_title", length = 100)
    private String scenarioTitle; // 시나리오 제목 (예: "병원 모금 참여")

    @Column(name = "scenario_goal", length = 500)
    private String scenarioGoal; // 학습자에게 주는 미션 ("~해주세요" 형태)

    @Column(name = "scenario_situation", length = 1000)
    private String scenarioSituation; // 학습자가 처한 상황 묘사 (3~5문장)

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
    public ChatRoom(User user, CollectedWord word,
                    String scenarioTitle,
                    String scenarioGoal,
                    String scenarioSituation,
                    AiGender aiGender) {
        this.user = user;
        this.word = word;
        this.scenarioTitle = scenarioTitle;
        this.scenarioGoal = scenarioGoal;
        this.scenarioSituation = scenarioSituation;
        this.aiGender = aiGender;
        this.status = ChatRoomStatus.IN_PROGRESS;
    }


    public void complete() {
        this.status = ChatRoomStatus.COMPLETED;
    }
}

