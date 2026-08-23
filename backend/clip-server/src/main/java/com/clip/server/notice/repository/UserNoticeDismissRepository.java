package com.clip.server.notice.repository;

import com.clip.server.notice.entity.UserNoticeDismiss;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserNoticeDismissRepository extends JpaRepository<UserNoticeDismiss, Long> {

    /**
     * 특정 사용자의 특정 공지 dismiss 이력 조회
     *
     * 사용처: POST /api/notices/{id}/dismiss
     *  - 이미 dismiss한 이력이 있으면 UPDATE
     *  - 없으면 신규 INSERT
     */
    @Query("""
        SELECT und FROM UserNoticeDismiss und
        WHERE und.user.id = :userId
            AND und.notice.id = :noticeId
        """)
    Optional<UserNoticeDismiss> findByUserIdAndNoticeId(
            @Param("userId") Long userId,
            @Param("noticeId") Long noticeId
    );

    /**
     * 특정 사용자의 dismiss한 공지 ID 목록 조회
     * (dismiss-all 시 중복 방지용)
     *
     * 사용처: POST /api/notices/dismiss-all
     */
    @Query("""
        SELECT und.notice.id FROM UserNoticeDismiss und
        WHERE und.user.id = :userId
        """)
    List<Long> findDismissedNoticeIdsByUserId(@Param("userId") Long userId);

    /**
     * 특정 공지의 dismiss 총 개수 (관리자 통계용)
     *
     * 사용처: 관리자 공지 상세 조회 시
     */
    long countByNoticeId(Long noticeId);
}
