package com.clip.server.report.entity;

import com.clip.server.chat.entity.ChatMessage;
import com.clip.server.chat.entity.ChatRoom;
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
        name = "chat_report",
        indexes = {
                @Index(name = "idx_type_created", columnList = "report_type, created_at"),
                @Index(name = "idx_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ChatReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    // 신고 타입에 따라 둘 중 하나만 채워짐
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private ChatMessage message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ChatReport(User user, ReportType reportType,
                      ChatMessage message, ChatRoom chatRoom, String content) {
        this.user = user;
        this.reportType = reportType;
        this.message = message;
        this.chatRoom = chatRoom;
        this.content = content;
    }

    /**
     * 메시지 신고 생성 헬퍼
     */
    public static ChatReport ofMessage(User user, ChatMessage message, String content) {
        return ChatReport.builder()
                .user(user)
                .reportType(ReportType.MESSAGE)
                .message(message)
                .content(content)
                .build();
    }

    /**
     * 시나리오 신고 생성 헬퍼
     */
    public static ChatReport ofScenario(User user, ChatRoom chatRoom, String content) {
        return ChatReport.builder()
                .user(user)
                .reportType(ReportType.SCENARIO)
                .chatRoom(chatRoom)
                .content(content)
                .build();
    }
}