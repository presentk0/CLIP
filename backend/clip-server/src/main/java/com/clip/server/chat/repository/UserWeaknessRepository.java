package com.clip.server.chat.repository;

import com.clip.server.chat.entity.UserWeakness;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
