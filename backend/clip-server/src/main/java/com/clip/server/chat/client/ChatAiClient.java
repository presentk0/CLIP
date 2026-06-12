package com.clip.server.chat.client;

import com.clip.server.ai.client.OpenAiClient;
import com.clip.server.chat.dto.ai.AiChatResult;
import com.clip.server.chat.entity.ChatMessage;
import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.prompt.ChatMessagePromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatAiClient {

    private final OpenAiClient openAiClient;
    private final ChatMessagePromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    /**
     * 채팅 메시지 → AI 답변 + 평가 (한 번에 처리)
     */
    public AiChatResult getChatResponse(
            ChatRoom chatRoom,
            List<ChatMessage> previousMessages,
            String currentUserMessage,
            int currentTurn
    ) {
        try {
            // 1. 프롬프트 생성
            String systemPrompt = promptBuilder.buildSystemPrompt(chatRoom, currentTurn);
            String userPrompt = promptBuilder.buildUserPrompt(previousMessages, currentUserMessage);

            log.debug("OpenAI 호출. systemPrompt 길이={}, userPrompt 길이={}",
                    systemPrompt.length(), userPrompt.length());

            // 2. OpenAI 호출
            String response = openAiClient.chat(systemPrompt, userPrompt);
            log.debug("OpenAI 응답: {}", response);

            // 3. JSON 파싱
            AiChatResult result = objectMapper.readValue(response, AiChatResult.class);

            // 4. 응답 검증
            validate(result);

            return result;

        } catch (Exception e) {
            log.error("AI 채팅 응답 실패. chatRoomId={}", chatRoom.getId(), e);
            throw new RuntimeException("AI 응답 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * AI 인트로(첫 멘트) 생성
     */
    public AiChatResult getOpeningMessage(ChatRoom chatRoom) {
        try {
            // 1. 인트로 전용 프롬프트 생성
            String systemPrompt = promptBuilder.buildOpeningSystemPrompt(chatRoom);
            String userPrompt = promptBuilder.buildOpeningUserPrompt(chatRoom);

            log.debug("OpenAI 인트로 호출. chatRoomId={}", chatRoom.getId());

            // 2. OpenAI 호출
            String response = openAiClient.chat(systemPrompt, userPrompt);
            log.debug("OpenAI 인트로 응답: {}", response);

            // 3. JSON 파싱
            AiChatResult result = objectMapper.readValue(response, AiChatResult.class);

            // 4. 응답 검증 (인트로는 평가 필드 없으므로 최소 검증만)
            validateOpening(result);

            return result;

        } catch (Exception e) {
            log.error("AI 인트로 생성 실패. chatRoomId={}", chatRoom.getId(), e);
            throw new RuntimeException("AI 인트로 생성 중 오류가 발생했습니다.", e);
        }
    }


    /**
     * AI 채팅 응답 형식 검증
     */
    private void validate(AiChatResult result) {
        if (result == null) {
            throw new RuntimeException("AI 응답이 비어있습니다.");
        }
        if (result.getAiResponse() == null || result.getAiResponse().isBlank()) {
            throw new RuntimeException("AI 답변 내용이 비어있습니다.");
        }
        if (result.getIsNatural() == null) {
            log.warn("isNatural 누락, true로 기본 처리");
        }
        if (result.getWordUsedNaturally() == null) {
            log.warn("wordUsedNaturally 누락, false로 기본 처리");
        }
    }

    /**
     * AI 인트로 응답 형식 검증
     * (평가 관련 필드는 검증하지 않음)
     */
    private void validateOpening(AiChatResult result) {
        if (result == null) {
            throw new RuntimeException("AI 인트로 응답이 비어있습니다.");
        }
        if (result.getAiResponse() == null || result.getAiResponse().isBlank()) {
            throw new RuntimeException("AI 인트로 내용이 비어있습니다.");
        }
    }
}