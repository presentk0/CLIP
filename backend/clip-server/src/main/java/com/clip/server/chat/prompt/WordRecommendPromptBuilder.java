package com.clip.server.word.prompt;

import com.clip.server.word.entity.CollectedWord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class WordRecommendPromptBuilder {

    private static final String SYSTEM_PROMPT = """
            당신은 영어 학습 추천 엔진입니다.
            
            규칙:
            1. 입력 단어마다 정확히 1개의 추천 단어를 생성하세요.
            2. recommendations 개수는 입력 단어 개수와 반드시 동일해야 합니다.
            3. 추천 단어는 입력 단어와 동일하면 안 됩니다.
            4. 추천 단어는 CEFR A1~B2 수준의 일반 영어 단어만 사용하세요.
            5. 희귀 단어, 전문 용어, SAT 고급 단어는 사용 금지입니다.
            6. relationType은 반드시 다음 중 하나만 사용하세요.
               - SYNONYM (유의어)
               - ANTONYM (반의어)
               - RELATED (관련어)
            7. relationType과 추천 단어 관계가 반드시 일치해야 합니다.
            8. relatedToWord는 입력 단어를 정확히 그대로 사용하세요.
            9. meaningsByPos는 품사별로 분류하여 작성하세요.
            10. 각 품사당 의미는 최대 2개까지만 작성하세요.
            11. 모든 의미는 한국어로 작성하세요.
            12. JSON 외의 텍스트는 절대 출력하지 마세요.
            13. SYNONYM, ANTONYM, RELATED를 다양하게 활용하세요.
            14. relatedToWord는 반드시 입력 단어 목록 중 하나여야 합니다.
            15. 입력 단어와 추천 단어는 절대 동일하면 안 됩니다.
            16. 같은 추천 단어를 여러 입력 단어에 중복 사용하지 마세요.
            
            잘못된 예시:
            - travel → travel (입력과 동일)
            - travel → peregrination (희귀 단어)
            - happy → ecstatic (너무 어려운 단어)
            
            올바른 예시:
            - travel → journey (SYNONYM)
            - happy → sad (ANTONYM)
            - airport → luggage (RELATED)
            """;

    public String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    public String buildUserPrompt(List<CollectedWord> userWords) {
        String wordList = userWords.stream()
                .map(CollectedWord::getWord)
                .collect(Collectors.joining(", "));

        return String.format("""
                다음 영단어들에 대해 각각 추천 단어를 생성해주세요.

                입력 단어: %s

                응답 형식:
                {
                  "recommendations": [
                    {
                      "word": "journey",
                      "meaningsByPos": [
                        {
                          "partOfSpeech": "명사",
                          "meanings": ["여행", "여정"]
                        }
                      ],
                      "relationType": "SYNONYM",
                      "relatedToWord": "travel"
                    }
                  ]
                }
                """, wordList);
    }
}