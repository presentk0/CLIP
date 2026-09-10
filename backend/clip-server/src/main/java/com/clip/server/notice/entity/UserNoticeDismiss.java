package com.clip.server.notice.entity;

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
        name = "user_notice_dismiss",
        indexes = {
                @Index(name = "uk_user_notice", columnList = "user_id, notice_id", unique = true),
                @Index(name = "idx_user", columnList = "user_id"),
                @Index(name = "idx_notice", columnList = "notice_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class UserNoticeDismiss {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 공지 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    /**
    * 다시 보지 않기 여부
     * false(기존): 공지 수정 시 재노출 가능
     * true: 영구 차단
     */
    @Column(name = "dont_show_again", nullable = false)
    private Boolean dontShowAgain;

    // dismiss 처리 시각
    @Column(name = "dismissed_at", nullable = false)
    @CreatedDate
    private LocalDateTime dismissedAt;

    @Builder
    private UserNoticeDismiss(User user, Notice notice, Boolean dontShowAgain) {
        this.user = user;
        this.notice = notice;
        this.dontShowAgain = dontShowAgain!= null? dontShowAgain: false;
    }
}
