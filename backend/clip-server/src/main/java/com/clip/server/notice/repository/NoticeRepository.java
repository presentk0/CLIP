package com.clip.server.notice.repository;

import com.clip.server.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import com.clip.server.notice.entity.NoticeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 팝업 노출 대상 공지 조회 (1개)
     * - 게시 상태(PUBLISHED)
     * - 팝업 대상(is_popup = true)
     * - 노출 기간 내
     * - 해당 사용자가 아직 dismiss 안 한 것
     * - 우선순위 높은 순 → 최신순
     *
     * 사용처: GET /api/notices/popup
     */
    @Query("""
        SELECT n FROM Notice n
        WHERE n.status = com.clip.server.notice.entity.NoticeStatus.PUBLISHED
            AND n.isPopup = true
            AND n.startAt <= :now
            AND (n.endAt IS NULL OR n.endAt >= :now)
            AND NOT EXISTS (
                SELECT 1 FROM UserNoticeDismiss und
                WHERE und.notice.id = n.id
                    AND und.user.id = :userId
            )
        ORDER BY n.priority DESC, n.createdAt DESC
        """)
    Optional<Notice> findPopupTarget(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    /**
     * 미확인 공지 개수 조회 (배지용)
     * - 게시 상태
     * - 노출 기간 내
     * - 해당 사용자가 아직 dismiss 안 한 것
     * - is_popup 조건 없음 (팝업이 아니어도 배지에는 포함)
     *
     * 사용처: GET /api/notices/unread-count
     */
    @Query("""
        SELECT COUNT(n) FROM Notice n
        WHERE n.status = com.clip.server.notice.entity.NoticeStatus.PUBLISHED
            AND n.startAt <= :now
            AND (n.endAt IS NULL OR n.endAt >= :now)
            AND NOT EXISTS (
                SELECT 1 FROM UserNoticeDismiss und
                WHERE und.notice.id = n.id
                    AND und.user.id = :userId
            )
        """)
    long countUnreadByUserId(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    /**
     * dismiss-all 대상 공지 조회
     * - 게시 상태
     * - 노출 기간 내
     * - 해당 사용자가 아직 dismiss 안 한 것
     *
     * 사용처: POST /api/notices/dismiss-all
     */
    @Query("""
        SELECT n FROM Notice n
        WHERE n.status = com.clip.server.notice.entity.NoticeStatus.PUBLISHED
            AND n.startAt <= :now
            AND (n.endAt IS NULL OR n.endAt >= :now)
            AND NOT EXISTS (
                SELECT 1 FROM UserNoticeDismiss und
                WHERE und.notice.id = n.id
                    AND und.user.id = :userId
            )
        """)
    java.util.List<Notice> findActiveNotDismissed(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now
    );

    // ============================================
    // 관리자용 쿼리
    // ============================================

    /**
     * 관리자 공지 목록 조회 (상태 필터)
     * 사용처: GET /api/admin/notices
     */
    Page<Notice> findAllByStatus(NoticeStatus status, Pageable pageable);

    /**
     * slug 중복 체크
     * 사용처: 관리자 공지 등록 시
     */
    boolean existsBySlug(String slug);
}
