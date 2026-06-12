package com.clip.server.chat.prompt;

import com.clip.server.chat.entity.ChatMessage;
import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.entity.SenderType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ChatReportPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            당신은 영어 학습자의 대화를 종합 평가하는 전문 영어 교사입니다.
            
            아래 대화 전체를 분석해 학습자의 영어 실력을 종합적으로 평가해주세요.
            
            [응답 형식 - 반드시 JSON만]
            {
              "overallScore": 0~100 사이 정수,
              "goodPoints": "잘한 점 (학습자에게 친근하게 한국어로, 1~2문장)",
              "improvePoints": "개선할 점 (구체적이고 친근하게 한국어로, 1~2문장)",
              "wordUsageFeedback": "타겟 단어 사용에 대한 피드백 (한국어, 1문장)",
              "expressionFeedback": "표현 자연스러움에 대한 피드백 (한국어, 1문장)"
            }
            
            [평가 기준]
            - overallScore: 학습자의 영어 자연스러움 + 타겟 단어 활용도 종합 점수
            - goodPoints: 칭찬할 점 (예: "타겟 단어를 자연스럽게 활용하셨어요!")
            - improvePoints: 개선점 (예: "더 정중한 표현을 사용하면 좋겠어요")
            - 어조: 친근하고 격려하는 톤, 너무 딱딱하지 않게
            """;

    /**
     * System 프롬프트
     */
    public String buildSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    /**
     * User 프롬프트 - 대화 전체 + 시나리오 정보
     */
    public String buildUserPrompt(
            ChatRoom chatRoom,
            List<ChatMessage> allMessages,
            int totalAwkwardCount
    ) {
        StringBuilder sb = new StringBuilder();

        sb.append("[대화 정보]\n");
        sb.append("- 시나리오 제목: ").append(
                chatRoom.getScenarioTitle() != null ? chatRoom.getScenarioTitle() : "(없음)"
        ).append("\n");
        sb.append("- 학습자 미션: ").append(
                chatRoom.getScenarioGoal() != null ? chatRoom.getScenarioGoal() : "(없음)"
        ).append("\n");
        sb.append("- 학습자 상황: ").append(
                chatRoom.getScenarioSituation() != null ? chatRoom.getScenarioSituation() : "(없음)"
        ).append("\n");
        sb.append("- 타겟 단어: ").append(
                chatRoom.getWord() != null ? chatRoom.getWord().getWord() : "(없음)"
        ).append("\n");
        sb.append("- 총 마디: ").append(allMessages.size() / 2).append("\n");
        sb.append("- 어색했던 표현 수: ").append(totalAwkwardCount).append("\n\n");

        sb.append("[전체 대화 내역]\n");
        for (ChatMessage msg : allMessages) {
            String role = msg.getSenderType() == SenderType.USER ? "학습자" : "AI";
            sb.append(role).append(": ").append(msg.getContent()).append("\n");

            // 학습자 메시지의 평가 정보도 함께 제공
            if (msg.getSenderType() == SenderType.USER && msg.getIsNatural() != null) {
                sb.append("  → 자연스러움: ").append(msg.getIsNatural() ? "OK" : "어색");
                if (msg.getRecommendedAlternative() != null) {
                    sb.append(", 추천: ").append(msg.getRecommendedAlternative());
                }
                sb.append("\n");
            }
        }

        return sb.toString();
    }
}