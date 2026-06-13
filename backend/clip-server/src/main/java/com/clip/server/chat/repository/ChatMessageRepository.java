package com.clip.server.chat.repository;

import com.clip.server.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import com.clip.server.chat.entity.SenderType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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
}
