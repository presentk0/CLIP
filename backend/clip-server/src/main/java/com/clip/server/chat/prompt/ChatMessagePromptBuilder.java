package com.clip.server.chat.prompt;

import com.clip.server.chat.entity.ChatMessage;
import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.entity.SenderType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatMessagePromptBuilder {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
             당신은 영어 학습자와 대화하는 자연스러운 원어민입니다.
            
             [대화 정보]
             - 시나리오: %s
             - 타겟 단어: %s
             - AI 성별: %s
             - 현재 턴: %d / 10
            
             [규칙]
             1. 시나리오 상황에 맞게 자연스럽게 대화하세요.
             2. 학습자가 이해하기 쉬운 일상 영어를 사용하세요 (CEFR A2~B1).
             3. 한 번에 1~3문장으로 짧게 답변하세요.
             4. 학습자가 타겟 단어 "%s"를 자연스럽게 사용하도록 유도하세요.
             5. 학습자의 영어가 어색하면 더 자연스러운 표현을 추천해주세요.
             6. 응답은 반드시 아래 JSON 형식만 사용하세요. 다른 텍스트 절대 금지.
            
             [응답 형식]
             {
               "aiResponse": "AI의 영어 답변 (1~3문장)",
               "highlightWord": "답변에서 강조할 단어 (없으면 null)",
               "isNatural": true 또는 false,
               "recommendedAlternative": "자연스럽지 않으면 추천 표현, 자연스러우면 null",
               "wordUsedNaturally": true 또는 false
             }
            
             [평가 기준 - 매우 중요!]
             - isNatural: 학습자의 영어 문법, 표현, 자연스러움
             - recommendedAlternative: 더 원어민다운 표현 제안
             - wordUsedNaturally: ★★★ 학습자의 이번 메시지(현재 입력)에 타겟 단어 "%s"가
               실제로 포함되어 있고 문맥에 맞게 사용되었는지를 판단.
               ⚠️ 학습자 메시지에 타겟 단어가 없으면 반드시 false.
               ⚠️ AI 응답이나 이전 대화는 평가 대상이 아님.
               ⚠️ 학습자가 타겟 단어를 직접 사용해야만 true.
            """;

    /**
     * System 프롬프트 생성 (시나리오 + 타겟 단어 컨텍스트)
     */
    public String buildSystemPrompt(ChatRoom chatRoom, int currentTurn) {
        String targetWord = chatRoom.getWord() != null
                ? chatRoom.getWord().getWord()
                : "(없음)";
        String scenario = chatRoom.getSelectedScenario() != null
                ? chatRoom.getSelectedScenario()
                : "자유 대화";
        String aiGender = chatRoom.getAiGender() != null
                ? chatRoom.getAiGender().name()
                : "FEMALE";

        return String.format(
                SYSTEM_PROMPT_TEMPLATE,
                scenario, targetWord, aiGender, currentTurn, targetWord, targetWord
        );
    }

    /**
     * User 프롬프트 생성 (이전 대화 + 현재 입력)
     */
    public String buildUserPrompt(List<ChatMessage> previousMessages, String currentUserMessage) {
        StringBuilder sb = new StringBuilder();

        // 이전 대화 컨텍스트 (최대 10개)
        if (!previousMessages.isEmpty()) {
            sb.append("[이전 대화]\n");
            previousMessages.stream()
                    .limit(10)
                    .forEach(msg -> {
                        String role = msg.getSenderType() == SenderType.USER ? "학습자" : "AI";
                        sb.append(String.format("%s: %s%n", role, msg.getContent()));
                    });
            sb.append("\n");
        }

        // 현재 사용자 입력
        sb.append("[현재 학습자 입력]\n");
        sb.append(currentUserMessage);

        return sb.toString();
    }
}