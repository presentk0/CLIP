package com.clip.server.analytics.entity;

import com.clip.server.user.entity.User;
import com.clip.server.user.entity.UserSession;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_session_id")
    private UserSession userSession;

    @Column(length = 50)
    private String eventName;

    @Column(length = 200)
    private String pagePath;

    @Column(length = 10)
    private String httpMethod;

    private Integer statusCode;
    private Long durationMs;

    @Column(columnDefinition = "TEXT")
    private String properties; // JSON String

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public UserEvent(User user,
                     UserSession userSession,
                     String eventName,
                     String pagePath,
                     String httpMethod,
                     Integer statusCode,
                     Long durationMs,
                     String properties) {
        this.user = user;
        this.userSession = userSession;
        this.eventName = eventName;
        this.pagePath = pagePath;
        this.httpMethod = httpMethod;
        this.statusCode = statusCode;
        this.durationMs = durationMs;
        this.properties = properties;
    }
}
