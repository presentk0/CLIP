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
             - 시나리오 제목: %s
             - 학습자 미션: %s
             - 학습자 상황: %s
             - 타겟 단어: %s
             - AI 성별: %s
             - 현재 턴: %d / 10
             
            [역할]            
            당신은 반드시 "학습자 상황" 속 등장인물만 연기해야 합니다.
            절대로 영어 선생님, 튜터, 평가자처럼 행동하지 마세요.
            
            예:
            - 길 묻기 → 현지인
            - 약국 → 약사
            - 레스토랑 → 직원
            - 호텔 → 여행객
                        
            반드시 해당 역할을 유지하세요.
            
             [규칙]
             1. 위 "학습자 상황"의 설정을 그대로 유지하며, AI는 그 상황 속 등장인물(현지인, 직원 등)을 연기하세요.
             2. "학습자 미션"은 학습자가 달성해야 할 목표이므로, 학습자가 미션을 수행하도록 자연스럽게 유도하세요.
             3. 학습자가 이해하기 쉬운 일상 영어를 사용하세요 (CEFR A2~B1).
             4. 응답은 1~3문장으로 작성하세요.
             5. 대화가 진행되어도 시나리오를 유지하세요. 
             6. 새로운 장소나 새로운 상황을 임의로 만들지 마세요.
             7. AI는 내레이터가 아닙니다.
             
             금지 예시:
             - "당신은 현재 길을 잃은 상태입니다."
             - "현재 박물관을 찾고 있습니다."
             
             허용 예시:
             - "Are you looking for the museum?"
             - "The road is closed today."  
             
             8. 학습자가 타겟 단어 "%s"를 자연스럽게 사용하도록 유도하세요.
             9. 학습자가 아직 타겟 단어를 사용하지 않았다면
                3턴 이내에 사용할 수 있는 질문을 하세요.
             10. 학습자가 타겟 단어를 사용했다면
                 해당 단어를 활용한 후속 질문으로 대화를 이어가세요.
             11. 타겟 단어를 직접 요구하지 말고
                 자연스럽게 사용할 상황을 만드세요.
             12. 학습자의 영어가 어색하면
                 더 자연스러운 표현을 추천해주세요.  
             13. 응답은 반드시 JSON만 반환하세요.                     
           
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
        String scenarioTitle = chatRoom.getScenarioTitle() != null
                ? chatRoom.getScenarioTitle()
                : "자유 대화";
        String scenarioGoal = chatRoom.getScenarioGoal() != null
                ? chatRoom.getScenarioGoal()
                : "특별한 미션 없음";
        String scenarioSituation = chatRoom.getScenarioSituation() != null
                ? chatRoom.getScenarioSituation()
                : "특별한 상황 없음";
        String aiGender = chatRoom.getAiGender() != null
                ? chatRoom.getAiGender().name()
                : "FEMALE";

        return String.format(
                SYSTEM_PROMPT_TEMPLATE,
                scenarioTitle, scenarioGoal, scenarioSituation,
                targetWord, aiGender, currentTurn, targetWord, targetWord
        );
    }

    /**
     * User 프롬프트 생성 (이전 대화 + 현재 입력)
     */
    public String buildUserPrompt(List<ChatMessage> previousMessages, String currentUserMessage) {
        StringBuilder sb = new StringBuilder();

        if (!previousMessages.isEmpty()) {
            sb.append("[이전 대화]\n");
            // 최근 10개만 컨텍스트로 사용 (토큰 비용 절감 + AI 응답 품질)
            previousMessages.stream()
                    .skip(Math.max(0, previousMessages.size() - 10))
                    .forEach(msg -> {
                        String role = msg.getSenderType() == SenderType.USER ? "학습자" : "AI";
                        sb.append(String.format("%s: %s%n", role, msg.getContent()));
                    });
            sb.append("\n");
        }

        sb.append("[현재 학습자 입력]\n");
        sb.append(currentUserMessage);
        return sb.toString();
    }

    // =========== AI 인트로 대화 ===========
    private static final String OPENING_SYSTEM_PROMPT_TEMPLATE = """
         당신은 영어 학습자와 대화하는 자연스러운 원어민입니다.
         지금부터 학습자와 새로운 시나리오 대화를 시작합니다.
         학습자의 입력은 아직 없으며, 당신이 먼저 첫 멘트를 건네야 합니다.
        
         [대화 정보]
         - 시나리오 제목: %s
         - 학습자 미션: %s
         - 학습자 상황: %s
         - 타겟 단어: %s
         - AI 성별: %s
        
         [인트로 작성 규칙]
         1. 위 "학습자 상황"의 설정을 그대로 첫 멘트 배경으로 활용하세요.
            (예: 학습자가 길을 잃은 상황이면, AI는 그 상황 속 현지인으로 등장)
         2. AI는 해당 상황 속 등장인물(직원, 현지인, 손님, 동료 등)을 연기해야 합니다.
         3. 영어 선생님처럼 자기소개하거나 학습을 시작한다고 말하지 마세요.
         4. "학습자 미션"을 자연스럽게 유도할 수 있는 첫 멘트를 만드세요.
         5. 학습자가 이해하기 쉬운 일상 영어를 사용하세요 (CEFR A2~B1).
         6. 1~2문장으로 짧고 자연스럽게 작성하세요.
         7. 학습자가 타겟 단어 "%s"를 사용하도록 유도하는 질문이나 상황을 만드세요.
         8. 학습자에게 답변을 요구하는 형태(질문 또는 말 걸기)로 끝내세요.
         9. 첫 멘트에서 학습자 미션을 직접 설명하지 마세요.
         10. 실제 상황처럼 자연스럽게 대화를 시작하세요.
         11. 응답은 반드시 아래 JSON 형식만 사용하세요. 다른 텍스트 절대 금지.
        
         [응답 형식]
         {
           "aiResponse": "AI의 영어 첫 멘트 (1~2문장)",
           "highlightWord": "멘트에서 강조할 단어 (없으면 null)",
           "isNatural": null,
           "recommendedAlternative": null,
           "wordUsedNaturally": null
         }
        
         [중요]
         - 인트로는 학습자 입력에 대한 평가가 아니므로
           isNatural / recommendedAlternative / wordUsedNaturally는 모두 null로 반환하세요.
        """;

    /**
     * 인트로용 System 프롬프트 생성
     */
    public String buildOpeningSystemPrompt(ChatRoom chatRoom) {
        String targetWord = chatRoom.getWord() != null
                ? chatRoom.getWord().getWord()
                : "(없음)";
        String scenarioTitle = chatRoom.getScenarioTitle() != null
                ? chatRoom.getScenarioTitle()
                : "자유 대화";
        String scenarioGoal = chatRoom.getScenarioGoal() != null
                ? chatRoom.getScenarioGoal()
                : "특별한 미션 없음";
        String scenarioSituation = chatRoom.getScenarioSituation() != null
                ? chatRoom.getScenarioSituation()
                : "특별한 상황 없음";
        String aiGender = chatRoom.getAiGender() != null
                ? chatRoom.getAiGender().name()
                : "FEMALE";

        return String.format(
                OPENING_SYSTEM_PROMPT_TEMPLATE,
                scenarioTitle, scenarioGoal, scenarioSituation,
                targetWord, aiGender, targetWord
        );
    }

    /**
     * 인트로용 User 프롬프트 생성
     */
    public String buildOpeningUserPrompt(ChatRoom chatRoom) {
        return "지금 시나리오가 시작되었습니다. 학습자에게 건넬 첫 멘트를 JSON 형식으로 작성해주세요.";
    }
}