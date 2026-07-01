package com.clip.server.chat.service;

import com.clip.server.chat.dto.response.ChatMessageListResponse;
import com.clip.server.chat.entity.ChatMessage;
import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.entity.SenderType;
import com.clip.server.chat.repository.ChatMessageRepository;
import com.clip.server.chat.repository.ChatRoomRepository;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.file.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final S3Service s3Service;

    private static final int MAX_TURN = 10; // AI 채팅 최대 턴

    public ChatMessageListResponse getMessage(Long userId, Long chatRoomId) {
        log.info("메시지 조회 요청. userId={}, chatRoomId={}", userId, chatRoomId);

        // 1. 채팅방 존재+사용자 소유권 검증
        ChatRoom chatRoom = chatRoomRepository.findByIdAndUserId(chatRoomId, userId).orElseThrow(()->new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 메시지 조회
        List<ChatMessage> messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(chatRoomId);

        // 3. DTO 변환
        List<ChatMessageListResponse.MessageDto> messageDtos = messages.stream()
                .map(this::mapToMessageDto)
                .toList();

        return ChatMessageListResponse.builder()
                .messages(messageDtos)
                .build();

    }

    // MessageDto 변환 메서드
    private ChatMessageListResponse.MessageDto mapToMessageDto(ChatMessage chatMessage) {
        String signedAudioUrl = s3Service.generatePresignedGetUrl(chatMessage.getAudioUrl());
        int remainingTurn = MAX_TURN - chatMessage.getTurnNumber();

        return ChatMessageListResponse.MessageDto.builder()
                .messageId(chatMessage.getId())
                .senderType(chatMessage.getSenderType())
                .content(chatMessage.getContent())
                .audioUrl(signedAudioUrl)
                .createdAt(chatMessage.getCreatedAt())
                .turnNumber(chatMessage.getTurnNumber())
                .remainingTurn(remainingTurn)
                .build();
    }

    /**
     * 사용자 메시지 저장
     */
    @Transactional
    public ChatMessage saveUserMessage(
            ChatRoom chatRoom,
            String content,
            String audioUrl,
            int turnNumber
    ) {
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .senderType(SenderType.USER)
                .content(content)
                .audioUrl(audioUrl)
                .turnNumber(turnNumber)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        log.info("사용자 메시지 저장. messageId={}, turnNumber={}", saved.getId(), turnNumber);
        return saved;
    }

    /**
     * AI 메시지 저장
     */
    @Transactional
    public ChatMessage saveAiMessage(
            ChatRoom chatRoom,
            String content,
            int turnNumber
    ) {
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .senderType(SenderType.AI)
                .content(content)
                .turnNumber(turnNumber)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        log.info("AI 메시지 저장. messageId={}, turnNumber={}", saved.getId(), turnNumber);
        return saved;
    }

    /**
     * 사용자 평가 정보 업데이트 (자연스러움, 추천 표현)
     */
    @Transactional
    public void updateUserMessageFeedback(
            Long messageId,
            Boolean isNatural,
            String recommendedAlternative
    ) {
        chatMessageRepository.findById(messageId).ifPresent(message -> {
            message.updateFeedback(isNatural, recommendedAlternative);
        });
    }

    /**
     * 채팅방의 이전 메시지 조회 (최근 N개)
     */
    public List<ChatMessage> getRecentMessages(Long chatRoomId, int limit) {
        return chatMessageRepository
                .findRecentMessages(chatRoomId, PageRequest.of(0, limit));
    }

    /**
     * 채팅방의 USER 메시지 개수 (현재 턴 계산용)
     */
    public int countUserMessages(Long chatRoomId) {
        return chatMessageRepository.countByChatRoomIdAndSenderType(
                chatRoomId, SenderType.USER
        );
    }

    /**
     * AI 메시지 저장 (음성 URL 포함)
     */
    @Transactional
    public ChatMessage saveAiMessageWithAudio(
            ChatRoom chatRoom,
            String content,
            String audioUrl,
            int turnNumber
    ) {
        ChatMessage message = ChatMessage.builder()
                .chatRoom(chatRoom)
                .senderType(SenderType.AI)
                .content(content)
                .audioUrl(audioUrl)        //  AI 음성 URL
                .turnNumber(turnNumber)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        log.info("AI 메시지(음성) 저장. messageId={}, audioUrl={}", saved.getId(), audioUrl);
        return saved;
    }

    // 채팅방의 전체 메시지 개수 조회 (USER + AI 모두 포함) - 인트로 중복 방지용
    public long countAllMessages(Long chatRoomId) {
        return chatMessageRepository.countByChatRoomId(chatRoomId);
    }
}
