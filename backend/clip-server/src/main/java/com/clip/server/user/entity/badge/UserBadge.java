package com.clip.server.user.entity.badge;

import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="user_badge")
@EntityListeners(AuditingEntityListener.class)
public class UserBadge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Enumerated(EnumType.STRING)
    @Column(name = "badge_type", nullable = false)
    private BadgeType badgeType;

    @Column(name = "earned_at", nullable = false)
    private LocalDateTime earnedAt; // 배지 획득 시각

    @Builder
    public UserBadge(User user, Video video, BadgeType badgeType) {
        this.user = user;
        this.video = video;
        this.badgeType = badgeType;
        this.earnedAt = LocalDateTime.now();
    }

    public void upgrade(BadgeType newType) {
        this.badgeType =  newType;
        this.earnedAt = LocalDateTime.now(); // 업그레이드 갱신 시점
    }
}
