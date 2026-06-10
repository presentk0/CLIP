package com.clip.server.chat.service;

import com.clip.server.chat.dto.request.ChatRoomInitRequest;
import com.clip.server.chat.dto.response.ChatRoomCompleteResponse;
import com.clip.server.chat.dto.response.ChatRoomInitResponse;
import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.entity.ChatRoomStatus;
import com.clip.server.chat.repository.ChatMessageRepository;
import com.clip.server.chat.repository.ChatRoomRepository;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final CollectedWordRepository collectedWordRepository;

    /**
     * AI 채팅방 초기화 (새로 시작 or 이어하기)
     *
     * - 사용자당 IN_PROGRESS 방은 최대 1개만 존재
     * - 새로 시작: 기존 IN_PROGRESS 방을 COMPLETED 처리 후 새 방 생성
     * - 이어하기: 기존 IN_PROGRESS 방 그대로 사용
     */
    @Transactional
    public ChatRoomInitResponse initChatRoom(Long userId, ChatRoomInitRequest request) {
        log.info("채팅방 초기화 요청. userId={}, isNewStart={}", userId, request.getIsNewStart());

        // 기존 채팅방 조회
        Optional<ChatRoom> existingChatRoom = chatRoomRepository
                .findActiveRoomForUpdate(userId, ChatRoomStatus.IN_PROGRESS);

        if (Boolean.TRUE.equals(request.getIsNewStart())) {
            return handleNewStart(userId, request, existingChatRoom);
        } else {
            return handleResume(existingChatRoom);
        }
    }

    /**
     * 새로 시작 처리
     * - 기존 진행 중인 방이 있으면 COMPLETED 처리
     * - 새 방 생성 후 반환
     */
    private ChatRoomInitResponse handleNewStart(
            Long userId,
            ChatRoomInitRequest request,
            Optional<ChatRoom> existingRoom
    ) {
        // 1. 새로 시작 시 필수 필드 검증
        validateNewStartFields(request);

        // 2. 기존 방이 있으면 완료 처리
        existingRoom.ifPresent(room -> {
            room.complete();
            log.info("기존 채팅방 완료 처리. chatRoomId={}", room.getId());
        });

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. 단어 조회 + 소유권 검증
        CollectedWord word = collectedWordRepository.findById(request.getWordId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));

        if (!word.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.WORD_NOT_OWNED);
        }

        // 5. 새 채팅방 생성
        ChatRoom newRoom = ChatRoom.builder()
                .user(user)
                .word(word)
                .selectedScenario(request.getSelectedScenario())
                .aiGender(request.getAiGender())
                .build();

        ChatRoom saved = chatRoomRepository.save(newRoom);
        log.info("새 채팅방 생성 완료. chatRoomId={}", saved.getId());

        return ChatRoomInitResponse.builder()
                .chatRoomId(saved.getId())
                .hasPreviousMessages(false)
                .build();
    }

    /**
     * 이어하기 처리
     * - 진행 중인 방이 없으면 에러
     */
    private ChatRoomInitResponse handleResume(Optional<ChatRoom> existingRoom) {
        ChatRoom room = existingRoom
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_RESUMABLE_CHAT_ROOM));

        boolean hasPreviousMessages = chatMessageRepository.existsByChatRoomId(room.getId());

        log.info("채팅방 이어하기. chatRoomId={}, hasPreviousMessages={}",
                room.getId(), hasPreviousMessages);

        return ChatRoomInitResponse.builder()
                .chatRoomId(room.getId())
                .hasPreviousMessages(hasPreviousMessages)
                .build();
    }

    /**
     * 새로 시작 시 필수 필드 검증
     */
    private void validateNewStartFields(ChatRoomInitRequest request) {
        if (request.getWordId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "wordId는 필수입니다.");
        }
        if (request.getSelectedScenario() == null || request.getSelectedScenario().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "선택한 시나리오는 필수입니다.");
        }
        if (request.getAiGender() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "AI 성별은 필수입니다.");
        }
    }

    @Transactional
    public ChatRoomCompleteResponse completeChatRoom(Long userId, Long chatRoomId) {
        log.info("채팅방 종료 요청. userId={}, chatRoomId={}", userId, chatRoomId);

        // 1. 채팅방 조회 + 소유권 검증
        ChatRoom chatRoom = chatRoomRepository.findByIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 이미 종료된 방인지 체크
        if (chatRoom.getStatus() == ChatRoomStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_COMPLETED);
        }

        // 3. 종료 처리
        chatRoom.complete();

        log.info("채팅방 종료 완료. chatRoomId={}", chatRoom.getId());

        return ChatRoomCompleteResponse.builder()
                .chatRoomId(chatRoom.getId())
                .completedAt(chatRoom.getUpdatedAt())  // BaseEntity 또는 엔티티의 updatedAt
                .build();
    }
}
