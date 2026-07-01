package com.clip.server.chat.repository;

import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.entity.ChatRoomStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    // 사용자가 진행중인 가장 최신 채팅방 조회
    Optional<ChatRoom> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ChatRoomStatus chatRoomStatus);

    /**
     * 동시성 처리용: 비관적 락으로 활성 방 조회
     * - 새 방 생성 시 동시 요청 방지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT cr FROM ChatRoom cr
            WHERE cr.user.id = :userId
              AND cr.status = :status
            ORDER BY cr.createdAt DESC
            """)
    Optional<ChatRoom> findActiveRoomForUpdate(
            @Param("userId") Long userId,
            @Param("status") ChatRoomStatus status
    );

    Optional<ChatRoom> findByIdAndUserId(Long id, Long userId);

    /**
     * - LazyInitializationException 방지용
     */
    @Query("""
            SELECT cr FROM ChatRoom cr
            LEFT JOIN FETCH cr.word
            WHERE cr.id = :chatRoomId AND cr.user.id = :userId
            """)
    Optional<ChatRoom> findByIdAndUserIdWithWord(
            @Param("chatRoomId") Long chatRoomId,
            @Param("userId") Long userId
    );

    // 사용자 채팅방 조회
    @Query("SELECT cr FROM ChatRoom cr " +
            "LEFT JOIN FETCH cr.word "+
            "WHERE cr.id = :chatRoomId AND cr.user.id =:userId")
    Optional<ChatRoom> findByIdAndUserWithWord(
            @Param("chatRoomId") Long chatRoomId,
            @Param("userId")Long userId);


    // ================= 관리자용 ============================

    // 특정 기간 내 상태별 방 개수 카운트 (인덱스: idx_user_status 활용)
    @Query("SELECT COUNT(r) FROM ChatRoom r " +
            "WHERE r.status = :status " +
            "AND r.createdAt BETWEEN :start AND :end")
    Long countByStatusAndPeriod(@Param("status") ChatRoomStatus status,
                                @Param("start") LocalDateTime start,
                                @Param("end") LocalDateTime end);
}
