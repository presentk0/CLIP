package com.clip.server.chat.service;

import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.repository.ChatRoomRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.chat.entity.UserWeakness;
import com.clip.server.chat.repository.UserWeaknessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserWeaknessService {

    private final UserWeaknessRepository userWeaknessRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 약점 저장 (어색한 표현 + 추천 표현)
     */
    @Transactional
    public void saveWeakness(
            Long userId,
            Long chatRoomId,
            String weakExpression,
            String recommendedExpression
    ) {
        // null/빈 값 체크
        if (weakExpression == null || weakExpression.isBlank() ||
                recommendedExpression == null || recommendedExpression.isBlank()) {
            return;
        }

        // 연관 엔티티 조회
        User userRef = userRepository.getReferenceById(userId);
        ChatRoom chatRoomRef = chatRoomRepository.getReferenceById(chatRoomId);

        UserWeakness weakness = UserWeakness.builder()
                .user(userRef)
                .chatRoom(chatRoomRef)
                .weakExpression(weakExpression)
                .recommendedExpression(recommendedExpression)
                .build();

        userWeaknessRepository.save(weakness);
        log.info("약점 저장. userId={}, chatRoomId={}, weak='{}'",
                userId, chatRoomId, weakExpression);
    }

    /**
     * 채팅방의 약점 목록 조회
     */
    @Transactional(readOnly = true)
    public List<UserWeakness> getWeaknessesByChatRoom(Long chatRoomId) {
        return userWeaknessRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId);
    }
}