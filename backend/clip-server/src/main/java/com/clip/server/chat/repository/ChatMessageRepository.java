package com.clip.server.chat.repository;

import com.clip.server.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import com.clip.server.chat.entity.SenderType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * 채팅방에 메시지가 존재하는지 확인 (이어하기 응답용)
     */
    boolean existsByChatRoomId(Long chatRoomId);

    /**
     * 채팅방의 모든 메시지를 시간순으로 조회
     */
    List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

    /**
    * 채팅방 최근 10개이 메시지 조회
     */
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE m.chatRoom.id = :chatRoomId
        ORDER BY m.createdAt DESC
        """)
    List<ChatMessage> findRecentMessages(
            @Param("chatRoomId") Long chatRoomId,
            Pageable pageable  // PageRequest.of(0, 10)으로 호출
    );

    /**
     * 발신자 타입별 메시지 개수
     */
    int countByChatRoomIdAndSenderType(Long chatRoomId, SenderType senderType);

    /**
     * 채팅방 ID로 전체 메시지 개수 조회
     * (USER + AI 메시지 모두 카운트)
     * 인트로 중복 방지용
     */
    long countByChatRoomId(Long chatRoomId);

    // ========================= 관리자용 ====================================

    // 인덱스(chat_room_id, sender_type) 및 시간 조건 활용 카운팅
    @Query("SELECT COUNT(m) FROM ChatMessage m " +
            "WHERE m.senderType = 'AI' " +
            "AND m.createdAt BETWEEN :start AND :end")
    Long countAiMessagesByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // 방당 평균 주고받은 대화 수 계산 (Group By 후 AVG 처리)
    @Query("SELECT AVG(sub.msgCount) FROM (" +
            "SELECT COUNT(m) as msgCount FROM ChatMessage m " +
            "WHERE m.createdAt BETWEEN :start AND :end " +
            "GROUP BY m.chatRoom.id" +
            ") sub")
    Double getAverageTurnsPerRoom(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Azure STT 평점 기반, 유저 발음 평균 점수 계산 (값이 존재하는 것만 집계)
    @Query("SELECT AVG(m.pronunciationScore) FROM ChatMessage m " +
            "WHERE m.senderType = 'USER' " +
            "AND m.pronunciationScore IS NOT NULL " +
            "AND m.createdAt BETWEEN :start AND :end")
    Double getAveragePronunciationScore(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
