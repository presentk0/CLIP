package com.clip.server.chat.service;

import com.clip.server.ai.client.AzureSttClient;
import com.clip.server.ai.client.AzureTtsClient;
import com.clip.server.chat.client.ChatAiClient;
import com.clip.server.chat.dto.ai.AiChatResult;
import com.clip.server.chat.dto.request.ChatMessageSendRequest;
import com.clip.server.chat.dto.response.websocket.ChatWebSocketDto;
import com.clip.server.chat.dto.response.websocket.MessageType;
import com.clip.server.chat.entity.ChatMessage;
import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.entity.ChatRoomStatus;
import com.clip.server.chat.entity.InputMode;
import com.clip.server.chat.repository.ChatRoomRepository;
import com.clip.server.chat.dto.response.websocket.ChatWebSocketSender;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.file.service.S3Service;
import com.clip.server.word.entity.WordMeaning;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.clip.server.chat.dto.ai.HintCardData;
import com.clip.server.word.entity.CollectedWord;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageOrchestrator {

    private static final int MAX_TURN = 10;
    private static final int CONTEXT_MESSAGE_LIMIT = 10;  // AI에게 보낼 이전 대화 개수
    private static final int HINT_CHECK_TURN_THRESHOLD = 3;  // 3턴까지 단어 미사용 시 힌트 제안
    private static final int HINT_RECENT_MESSAGE_LIMIT = 4;  // 힌트 생성 시 참고할 최근 메시지 수

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageService chatMessageService;
    private final ChatAiClient chatAiClient;
    private final ChatWebSocketSender webSocketSender;
    private final UserWeaknessService userWeaknessService;

    private final AzureSttClient azureSttClient;
    private final AzureTtsClient azureTtsClient;
    private final S3Service s3Service;


    /**
     * 사용자 메시지 처리 전체 흐름
     * - 비동기 처리 (WebSocket 컨트롤러를 빠르게 반환)
     */
    @Async("chatTaskExecutor")
    public void processMessage(Long userId, ChatMessageSendRequest request) {
        try {
            // 1. 채팅방 검증
            ChatRoom chatRoom = validateAndGetChatRoom(userId, request.getChatRoomId());

            // 2. 컨텐츠 추출 (TEXT/VOICE 분기)
            String userContent = extractUserContent(request);

            // 3. 현재 턴
            int currentTurn = calculateCurrentTurn(chatRoom.getId());

            // 4. 최대 턴 체크
            if (currentTurn >= MAX_TURN) {
                sendError(userId, "MAX_TURN_REACHED", "최대 대화 턴에 도달했습니다.");
                return;
            }

            // 5. 사용자 메시지 저장
            ChatMessage userMessage = chatMessageService.saveUserMessage(
                    chatRoom,
                    userContent,
                    request.getAudioUrl(),
                    currentTurn + 1
            );

            //  5-1. VOICE 모드면 USER_TEXT 푸시 (사용자에게 변환 결과 보여주기)
            if (request.getInputMode() == InputMode.VOICE) {
                sendUserText(userId, userMessage.getId(), userContent, request.getAudioUrl());
            }

            // 6. 이전 메시지
            List<ChatMessage> previousMessages = chatMessageService
                    .getRecentMessages(chatRoom.getId(), CONTEXT_MESSAGE_LIMIT);

            // 7. AI 호출
            AiChatResult aiResult = chatAiClient.getChatResponse(
                    chatRoom,
                    previousMessages,
                    userContent,
                    currentTurn + 1
            );

            // 8. 평가 업데이트
            chatMessageService.updateUserMessageFeedback(
                    userMessage.getId(),
                    aiResult.getIsNatural(),
                    aiResult.getRecommendedAlternative()
            );

            // 8-1. 약점 저장
            if (Boolean.FALSE.equals(aiResult.getIsNatural())
                    && aiResult.getRecommendedAlternative() != null) {
                userWeaknessService.saveWeakness(
                        userId,
                        chatRoom.getId(),
                        userContent,
                        aiResult.getRecommendedAlternative()
                );
            }

            //  9. AI 메시지 저장 + TTS (VOICE 모드일 때)
            ChatMessage aiMessage;
            String aiAudioUrl = null;

            if (request.getInputMode() == InputMode.VOICE) {
                // 9-1. TTS 변환
                log.info("TTS 변환 시작. text={}", aiResult.getAiResponse());
                byte[] aiAudioData = azureTtsClient.synthesize(
                        aiResult.getAiResponse(),
                        chatRoom.getAiGender().name()
                );

                // 9-2. S3 업로드 + 재생 URL 받기
                aiAudioUrl = s3Service.uploadAiAudioAndGetUrl(aiAudioData, "audio/mpeg");
                log.info("AI 음성 S3 업로드 완료. url={}", aiAudioUrl);

                // 9-3. AI 메시지 저장 (audioUrl 포함)
                aiMessage = chatMessageService.saveAiMessageWithAudio(
                        chatRoom,
                        aiResult.getAiResponse(),
                        aiAudioUrl,
                        currentTurn + 1
                );
            } else {
                // TEXT 모드: 기존 로직
                aiMessage = chatMessageService.saveAiMessage(
                        chatRoom,
                        aiResult.getAiResponse(),
                        currentTurn + 1
                );
            }

            // 10. AI_TEXT_DONE 푸시
            sendAiTextDone(userId, aiMessage.getId(), aiResult);

            //  10-1. VOICE 모드면 AI_AUDIO 푸시
            if (request.getInputMode() == InputMode.VOICE && aiAudioUrl != null) {
                sendAiAudio(userId, aiMessage.getId(), aiAudioUrl);
            }

            // 11. PROGRESS 푸시
            int newTurn = currentTurn + 1;
            sendProgress(userId, newTurn);

            // 11-1. 힌트 제안 조건 체크
            checkAndOfferHint(userId, chatRoom, aiResult, userContent, newTurn);

            // 12. 자동 종료
            if (newTurn >= MAX_TURN) {
                completeChat(chatRoom);
                sendCompletion(userId, chatRoom.getId());
            }

        } catch (BusinessException e) {
            log.warn("비즈니스 예외. userId={}, message={}", userId, e.getMessage());
            sendError(userId, e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            log.error("메시지 처리 중 예상치 못한 에러. userId={}", userId, e);
            sendError(userId, "INTERNAL_ERROR", "서버 오류가 발생했습니다.");
        }
    }

    /**
     *  채팅 시작 - AI 인트로 메시지 생성 및 푸시
     * - 메시지가 0개일 때만 동작 (중복 방지)
     * - VOICE/TEXT 모드 무관하게 인트로는 항상 음성 포함 (시나리오 몰입감)
     */
    @Async("chatTaskExecutor")
    public void startChat(Long userId, Long chatRoomId) {
        try {
            // 1. 채팅방 검증 (소유권 + 상태)
            ChatRoom chatRoom = validateAndGetChatRoom(userId, chatRoomId);

            // 2. 중복 방지: 이미 메시지가 있으면 인트로 생략
            long messageCount = chatMessageService.countAllMessages(chatRoom.getId());
            if (messageCount > 0) {
                log.info("이미 메시지가 존재하여 인트로를 생략합니다. chatRoomId={}, count={}",
                        chatRoomId, messageCount);
                return;
            }

            // 3. AI 인트로 생성
            log.info("AI 인트로 생성 시작. chatRoomId={}", chatRoomId);
            AiChatResult introResult = chatAiClient.getOpeningMessage(chatRoom);

            // 4. TTS 변환 + S3 업로드 (실패해도 텍스트는 보냄)
            String aiAudioUrl = null;
            try {
                byte[] audioData = azureTtsClient.synthesize(
                        introResult.getAiResponse(),
                        chatRoom.getAiGender().name()
                );
                aiAudioUrl = s3Service.uploadAiAudioAndGetUrl(audioData, "audio/mpeg");
                log.info("인트로 음성 S3 업로드 완료. url={}", aiAudioUrl);
            } catch (Exception e) {
                log.warn("인트로 TTS 실패. 텍스트만 푸시합니다. chatRoomId={}", chatRoomId, e);
            }

            // 5. AI 메시지 저장 (turn 0 = 인트로)
            ChatMessage aiMessage;
            if (aiAudioUrl != null) {
                aiMessage = chatMessageService.saveAiMessageWithAudio(
                        chatRoom,
                        introResult.getAiResponse(),
                        aiAudioUrl,
                        0
                );
            } else {
                aiMessage = chatMessageService.saveAiMessage(
                        chatRoom,
                        introResult.getAiResponse(),
                        0
                );
            }

            // 6. AI_TEXT_DONE 푸시
            sendAiTextDone(userId, aiMessage.getId(), introResult);

            // 7. AI_AUDIO 푸시 (TTS 성공 시)
            if (aiAudioUrl != null) {
                sendAiAudio(userId, aiMessage.getId(), aiAudioUrl);
            }

            // 8. PROGRESS 푸시 안 함 — 사용자 턴은 아직 시작 전 (turn 0)

        } catch (BusinessException e) {
            log.warn("인트로 생성 비즈니스 예외. userId={}, message={}", userId, e.getMessage());
            sendError(userId, e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            log.error("인트로 생성 중 예상치 못한 에러. userId={}", userId, e);
            sendError(userId, "INTERNAL_ERROR", "인트로 생성 중 오류가 발생했습니다.");
        }
    }

    /**
     * 채팅방 검증 (소유권 + 상태)
     */
    private ChatRoom validateAndGetChatRoom(Long userId, Long chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository .findByIdAndUserIdWithWord(chatRoomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        if (chatRoom.getStatus() == ChatRoomStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_COMPLETED);
        }

        return chatRoom;
    }

    /**
     * 입력 모드에 따른 사용자 컨텐츠 추출
     */
    private String extractUserContent(ChatMessageSendRequest request) {
        if (request.getInputMode() == InputMode.TEXT) {
            if (request.getContent() == null || request.getContent().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "TEXT 모드는 content가 필수입니다.");
            }
            return request.getContent();
        } else {
            // VOICE 모드: STT로 변환
            if (request.getAudioUrl() == null || request.getAudioUrl().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "VOICE 모드는 audioUrl이 필수입니다.");
            }

            log.info("STT 변환 시작. audioUrl={}", request.getAudioUrl());
            String transcribed = azureSttClient.transcribe(request.getAudioUrl());
            log.info("STT 변환 완료. text={}", transcribed);

            return transcribed;
        }
    }

    /**
     * 현재 턴 계산 (USER 메시지 개수 기준)
     */
    private int calculateCurrentTurn(Long chatRoomId) {
        return chatMessageService.countUserMessages(chatRoomId);
    }

    /**
     * 채팅방 자동 종료
     */
    @Transactional
    public void completeChat(ChatRoom chatRoom) {
        chatRoom.complete();
        chatRoomRepository.save(chatRoom);
        log.info("채팅방 자동 종료. chatRoomId={}", chatRoom.getId());
    }

    /**
     * USER_TEXT 푸시 (사용자 음성 변환 결과)
     */
    private void sendUserText(Long userId, Long messageId, String content, String audioUrl) {
        ChatWebSocketDto.UserText payload = ChatWebSocketDto.UserText.builder()
                .messageId(messageId)
                .content(content)
                .audioUrl(audioUrl)
                .build();

        webSocketSender.send(userId, MessageType.USER_TEXT, payload);
    }

    /**
     * AI_AUDIO 푸시 (AI 음성 URL)
     */
    private void sendAiAudio(Long userId, Long messageId, String audioUrl) {
        ChatWebSocketDto.AiAudio payload = ChatWebSocketDto.AiAudio.builder()
                .messageId(messageId)
                .audioUrl(audioUrl)
                .build();

        webSocketSender.send(userId, MessageType.AI_AUDIO, payload);
    }

    // ==================== WebSocket  ====================

    private void sendAiTextDone(Long userId, Long messageId, AiChatResult aiResult) {
        ChatWebSocketDto.AiTextDone payload = ChatWebSocketDto.AiTextDone.builder()
                .messageId(messageId)
                .content(aiResult.getAiResponse())
                .highlightWord(aiResult.getHighlightWord())
                .isNatural(aiResult.getIsNatural())
                .recommendedAlternative(aiResult.getRecommendedAlternative())
                .wordUsedNaturally(aiResult.getWordUsedNaturally())
                .build();

        webSocketSender.send(userId, MessageType.AI_TEXT_DONE, payload);
    }

    private void sendProgress(Long userId, int currentTurn) {
        boolean isCompleted = currentTurn >= MAX_TURN;

        ChatWebSocketDto.Progress payload = ChatWebSocketDto.Progress.builder()
                .currentTurn(currentTurn)
                .remainingTurn(MAX_TURN - currentTurn)
                .maxTurn(MAX_TURN)
                .completed(isCompleted)
                .build();

        webSocketSender.send(userId, MessageType.PROGRESS, payload);
    }

    private void sendCompletion(Long userId, Long chatRoomId) {
        ChatWebSocketDto.Completion payload = ChatWebSocketDto.Completion.builder()
                .chatRoomId(chatRoomId)
                .message("대화가 완료되었습니다. 리포트를 확인해보세요!")
                .build();

        webSocketSender.send(userId, MessageType.COMPLETION, payload);
    }

    private void sendError(Long userId, String code, String message) {
        ChatWebSocketDto.Error payload = ChatWebSocketDto.Error.builder()
                .code(code)
                .message(message)
                .build();

        webSocketSender.send(userId, MessageType.ERROR, payload);
    }

    /**
     *  힌트 제안 조건 체크 후 HINT_OFFER 푸시
     *
     * C안 = A OR B 조합:
     * - A안: 사용자가 어색한 표현 사용 (isNatural == false)
     * - B안: 일정 턴 경과 + 타겟 단어 미사용
     */
    private void checkAndOfferHint(
            Long userId,
            ChatRoom chatRoom,
            AiChatResult aiResult,
            String userContent,
            int currentTurn
    ) {
        try {
            // 1. 이미 힌트 제안한 방이면 스킵 (중복 방지)
            if (chatRoom.isHintOffered()) {
                return;
            }

            // 2. 마지막 턴이면 굳이 힌트 안 줌
            if (currentTurn >= MAX_TURN) {
                return;
            }

            // 3. 타겟 단어 없으면 힌트 불가
            if (chatRoom.getWord() == null) {
                return;
            }

            // 4. 조건 체크
            // A안: 어색한 표현 사용
            boolean conditionA = Boolean.FALSE.equals(aiResult.getIsNatural());

            // B안: 3턴 경과 + 타겟 단어 미사용
            String targetWord = chatRoom.getWord().getWord();
            boolean targetWordUsed = userContent.toLowerCase()
                    .contains(targetWord.toLowerCase());
            boolean conditionB = currentTurn >= HINT_CHECK_TURN_THRESHOLD
                    && !targetWordUsed
                    && Boolean.FALSE.equals(aiResult.getWordUsedNaturally());

            // C안: 둘 중 하나만 충족해도 OK
            if (!(conditionA || conditionB)) {
                return;
            }

            log.info("힌트 제안 트리거. chatRoomId={}, conditionA={}, conditionB={}",
                    chatRoom.getId(), conditionA, conditionB);

            // 5. HINT_OFFER 푸시
            ChatWebSocketDto.HintOffer payload = ChatWebSocketDto.HintOffer.builder()
                    .message("지금이 힌트 보기 좋은 타이밍이에요!")
                    .subMessage("한 번 같이 볼까요?")
                    .hintAvailable(true)
                    .build();

            webSocketSender.send(userId, MessageType.HINT_OFFER, payload);

            // 6. 중복 제안 방지 플래그 설정
            markHintOffered(chatRoom);

        } catch (Exception e) {
            log.error("힌트 제안 체크 중 에러. chatRoomId={}", chatRoom.getId(), e);
            // 힌트 제안 실패는 사용자 경험에 치명적이지 않으므로 에러 푸시하지 않음
        }
    }

    /**
     * 힌트 제안 플래그 저장 (트랜잭션 분리)
     */
    @Transactional
    public void markHintOffered(ChatRoom chatRoom) {
        chatRoom.markHintOffered();
        chatRoomRepository.save(chatRoom);
    }

    /**
     * 🆕 힌트 카드 제공 (사용자가 "예" 클릭 시)
     */
    @Async("chatTaskExecutor")
    public void provideHintCard(Long userId, Long chatRoomId) {
        try {
            // 1. 채팅방 검증
            ChatRoom chatRoom = validateAndGetChatRoom(userId, chatRoomId);

            // 2. 타겟 단어 체크
            CollectedWord word = chatRoom.getWord();
            if (word == null) {
                sendError(userId, "HINT_NOT_AVAILABLE", "타겟 단어가 없어 힌트를 제공할 수 없습니다.");
                return;
            }

            String targetWord = word.getWord();
            List<String> meanings = extractMeanings(word.getMeaningsByPos());
            String targetMeaning = String.join(", ", meanings);

            log.info("힌트 카드 생성 시작. chatRoomId={}, word={}", chatRoomId, targetWord);

            // 3. 최근 대화 가져오기
            List<ChatMessage> recentMessages = chatMessageService
                    .getRecentMessages(chatRoom.getId(), HINT_RECENT_MESSAGE_LIMIT);

            // 4. AI 힌트 카드 생성
            HintCardData hintData = chatAiClient.generateHintCard(
                    chatRoom, recentMessages, targetWord, targetMeaning
            );

            // 5. HINT_CARD 푸시
            ChatWebSocketDto.HintCard payload = ChatWebSocketDto.HintCard.builder()
                    .word(targetWord)
                    .meanings(meanings)
                    .contextMessage(hintData.getContextMessage())
                    .guideMessage(hintData.getGuideMessage())
                    .exampleSentence(hintData.getExampleSentence())
                    .exampleTranslation(hintData.getExampleTranslation())
                    .highlightWord(targetWord)
                    .build();

            webSocketSender.send(userId, MessageType.HINT_CARD, payload);

            log.info("힌트 카드 푸시 완료. chatRoomId={}", chatRoomId);

        } catch (BusinessException e) {
            log.warn("힌트 제공 비즈니스 예외. userId={}, message={}", userId, e.getMessage());
            sendError(userId, e.getErrorCode().name(), e.getMessage());
        } catch (Exception e) {
            log.error("힌트 카드 제공 중 예상치 못한 에러. userId={}", userId, e);
            sendError(userId, "HINT_FAILED", "힌트 생성 중 오류가 발생했습니다.");
        }
    }

    /**
     * WordMeaning 리스트에서 모든 의미를 평탄화하여 추출
     *
     * 예) [{"명사", ["관점", "시각"]}, {"동사", ["보다"]}]
     *  → ["관점", "시각", "보다"]
     */
    private List<String> extractMeanings(List<WordMeaning> meaningsByPos) {
        if (meaningsByPos == null || meaningsByPos.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> result = new ArrayList<>();
        for (WordMeaning wm : meaningsByPos) {
            if (wm.getMeanings() != null) {
                result.addAll(wm.getMeanings());
            }
        }
        return result;
    }
}