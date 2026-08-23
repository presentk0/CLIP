package com.clip.server.notice.entity;

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
        name = "notice",
        indexes = {
                @Index(name = "uk_notice_slug", columnList = "slug", unique = true),
                @Index(name = "idx_notice_active", columnList = "status, is_popup, start_at, end_at"),
                @Index(name = "idx_notice_status_start", columnList = "status, start_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Slash 사이트 URL 경로
     * → https://clipzy.com/notices/{slug} 형태로 조립
     */
    @Column(name = "slug", nullable = false, length = 100, unique = true)
    private String slug;

    /**
     * 공지 제목 (팝업/목록 표시용)
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 공지 요약 (팝업 본문용, 1~2줄)
     */
    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    /**
     * 팝업 노출 여부
     * - true: 로그인 시 팝업 표시
     * - false: 배지 카운트에만 포함
     */
    @Column(name = "is_popup", nullable = false)
    private Boolean isPopup;

    /**
     * 우선순위 (높을수록 먼저 노출)
     */
    @Column(name = "priority", nullable = false)
    private Integer priority;

    /**
     * 노출 시작 시각
     */
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    /**
     * 노출 종료 시각 (NULL = 무기한)
     */
    @Column(name = "end_at")
    private LocalDateTime endAt;

    /**
     * 게시 상태 (DRAFT, PUBLISHED, ARCHIVED)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NoticeStatus status;

    /**
     * 작성 관리자 ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    // 등록 시각
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 수정 시각
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    private Notice(String slug, String title, String summary, Boolean isPopup, Integer priority, LocalDateTime startAt, LocalDateTime endAt, NoticeStatus status, Long createdBy) {
        this.slug = slug;
        this.title = title;
        this.summary = summary;
        this.isPopup = isPopup != null ? isPopup : false;
        this.priority = priority != null ? priority : 0;
        this.startAt = startAt;
        this.endAt = endAt;
        this.status = status != null ? status : NoticeStatus.PUBLISHED;
        this.createdBy = createdBy;
    }

    // ============================================
    // 비즈니스 메서드
    // ============================================

    /**
     * 공지 정보 업데이트 메서드
     */
    public void update(String title,
                       String summary,
                       Boolean isPopup,
                       Integer priority,
                       LocalDateTime startAt,
                       LocalDateTime endAt,
                       NoticeStatus status) {
        if (title != null) this.title = title;
        if (summary != null) this.summary = summary;
        if (isPopup != null) this.isPopup = isPopup;
        if (priority != null) this.priority = priority;
        if (startAt != null) this.startAt = startAt;
        if (endAt != null) this.endAt = endAt;
        if (status != null) this.status = status;
    }

    /**
     * Soft Delete (ARCHIVED 상태로 변경)
     */
    public void archive() {
        this.status = NoticeStatus.ARCHIVED;
    }

    /**
     * 현재 활성 상태 여부
     */
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == NoticeStatus.PUBLISHED
                && startAt.isBefore(now)
                && (endAt == null || endAt.isAfter(now));
    }
}
