package com.clip.server.chat.repository;

import com.clip.server.chat.entity.UserWeakness;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserWeaknessRepository extends JpaRepository<UserWeakness, Long> {

    /**
     * 특정 채팅방의 약점 목록
     * - chatRoom.id로 조회
     */
    List<UserWeakness> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    /**
     * 사용자의 모든 약점 (최신순)
     * - user.id로 조회
     */
    List<UserWeakness> findByUserIdOrderByCreatedAtDesc(Long userId);

    // ==================== Agent용 쿼리 ====================

    // 아직 극복 못한 채팅 약점 (review_count < 3)
    @Query("""
    SELECT uw FROM UserWeakness uw
    WHERE uw.user.id = :userId 
      AND uw.reviewCount < 3
    ORDER BY uw.createdAt DESC
    """)
    List<UserWeakness> findUnresolvedWeaknesses(@Param("userId") Long userId);
}
