package com.clip.server.chat.client;

import com.clip.server.ai.client.OpenAiClient;
import com.clip.server.chat.dto.ai.AiReportEvaluation;
import com.clip.server.chat.entity.ChatMessage;
import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.prompt.ChatReportPromptBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatReportAiClient {

    private final OpenAiClient openAiClient;
    private final ChatReportPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    /**
     * 대화 전체를 LLM이 종합 평가
     */
    public AiReportEvaluation evaluate(
            ChatRoom chatRoom,
            List<ChatMessage> allMessages,
            int totalAwkwardCount
    ) {
        try {
            String systemPrompt = promptBuilder.buildSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(
                    chatRoom, allMessages, totalAwkwardCount
            );

            log.debug("리포트 AI 호출. chatRoomId={}", chatRoom.getId());

            String response = openAiClient.chat(systemPrompt, userPrompt);
            log.debug("리포트 AI 응답: {}", response);

            return objectMapper.readValue(response, AiReportEvaluation.class);

        } catch (Exception e) {
            log.error("리포트 AI 평가 실패. chatRoomId={}", chatRoom.getId(), e);
            // 실패 시 기본값 반환 (리포트가 아예 안 나오는 것보다 나음)
            return getDefaultEvaluation();
        }
    }

    /**
     * AI 호출 실패 시 기본값
     */
    private AiReportEvaluation getDefaultEvaluation() {
        AiReportEvaluation defaultEval = new AiReportEvaluation();
        // ObjectMapper로 기본값 세팅
        try {
            return objectMapper.readValue("""
                    {
                      "overallScore": 70,
                      "goodPoints": "꾸준히 영어로 대화하셨어요!",
                      "improvePoints": "더 다양한 표현을 시도해보세요.",
                      "wordUsageFeedback": "타겟 단어를 활용해보세요.",
                      "expressionFeedback": "자연스러운 표현을 연습해보세요."
                    }
                    """, AiReportEvaluation.class);
        } catch (Exception e) {
            return defaultEval;
        }
    }
}