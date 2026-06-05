package com.clip.server.chat.service;

import com.clip.server.ai.client.OpenAiClient;
import com.clip.server.chat.dto.response.AiRecommendation;
import com.clip.server.chat.dto.response.ChatWordsResponse;
import com.clip.server.word.entity.CollectedWord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordRecommendationService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;

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
            8. relatedToWord는 입력 단어를 정확히 그대로 사용하세요 (대소문자 포함).
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
            - run → sprint (B2 이상)
            
            올바른 예시:
            - travel → journey (SYNONYM)
            - happy → sad (ANTONYM)
            - airport → luggage (RELATED)
            - big → small (ANTONYM)
            - book → novel (RELATED)
            """;

    /**
     * 사용자 단어 목록을 받아 AI 추천 단어 생성 (1:1 비율)
     */
    public List<ChatWordsResponse.Word> recommendWords(List<CollectedWord> userWords) {
        if (userWords == null || userWords.isEmpty()) {
            return List.of();
        }

        try {
            // 1. 프롬프트 생성
            String userPrompt = buildPrompt(userWords);

            // 2. AI 호출
            String aiResponse = openAiClient.chat(SYSTEM_PROMPT, userPrompt);
            log.debug("AI 응답: {}", aiResponse);

            // 3. JSON 파싱
            List<AiRecommendation> recommendations = parseResponse(aiResponse);

            // 4. 검증 및 필터링
            List<AiRecommendation> validated = validateAndFilter(recommendations, userWords);

            // 5. 개수 체크
            if (validated.size() != userWords.size()) {
                log.warn("추천 개수 불일치 input={}, output={}",
                        userWords.size(), validated.size());
                return List.of();
            }

            // 6. DTO 변환
            return convertToWordDtos(validated, userWords);

        } catch (Exception e) {
            log.error("AI 추천 실패, 빈 결과 반환", e);
            return List.of();
        }
    }

    /** 검증 및 필터링 (분리해서 가독성 ⬆) */
    private List<AiRecommendation> validateAndFilter(
            List<AiRecommendation> recommendations,
            List<CollectedWord> userWords
    ) {
        // 입력 단어 set (대소문자 무시)
        Set<String> inputWords = userWords.stream()
                .map(cw -> cw.getWord().toLowerCase())
                .collect(Collectors.toSet());

        // 중복 추천 방지용
        Set<String> seenWords = new HashSet<>();

        return recommendations.stream()
                .filter(rec -> isValid(rec, inputWords, seenWords))
                .toList();
    }

    /** 개별 추천 검증 */
    private boolean isValid(
            AiRecommendation rec,
            Set<String> inputWords,
            Set<String> seenWords
    ) {
        // null 체크
        if (rec.getWord() == null || rec.getRelatedToWord() == null) {
            log.debug("필수 필드 누락: {}", rec);
            return false;
        }

        // 입력 단어와 동일하면 안 됨
        if (rec.getWord().equalsIgnoreCase(rec.getRelatedToWord())) {
            log.debug("입력 단어와 동일: {}", rec.getWord());
            return false;
        }

        // relatedToWord가 입력 단어 중 하나여야 함
        if (!inputWords.contains(rec.getRelatedToWord().toLowerCase())) {
            log.debug("relatedToWord가 입력 단어 아님: {}", rec.getRelatedToWord());
            return false;
        }

        // 중복 추천 단어 방지
        if (!seenWords.add(rec.getWord().toLowerCase())) {
            log.debug("중복 추천 단어: {}", rec.getWord());
            return false;
        }

        return true;
    }/** 프롬프트 생성 (단순화) */
    private String buildPrompt(List<CollectedWord> userWords) {
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

    /** JSON 파싱 */
    private List<AiRecommendation> parseResponse(String aiResponse) throws Exception {
        Map<String, List<AiRecommendation>> parsed = objectMapper.readValue(
                aiResponse,
                new TypeReference<Map<String, List<AiRecommendation>>>() {}
        );
        return parsed.getOrDefault("recommendations", List.of());
    }

    /** AiRecommendation → ChatWordsResponse.Word 변환 */
    private List<ChatWordsResponse.Word> convertToWordDtos(
            List<AiRecommendation> recommendations,
            List<CollectedWord> userWords
    ) {
        Map<String, CollectedWord> wordMap = userWords.stream()
                .collect(Collectors.toMap(
                        cw -> cw.getWord().toLowerCase(),
                        cw -> cw,
                        (a, b) -> a
                ));

        return recommendations.stream()
                .map(rec -> toWordDto(rec, wordMap))
                .toList();
    }

    private ChatWordsResponse.Word toWordDto(
            AiRecommendation rec,
            Map<String, CollectedWord> wordMap
    ) {
        ChatWordsResponse.RelationType relationType = parseRelationType(rec.getRelationType());

        ChatWordsResponse.Related relatedTo = null;
        if (rec.getRelatedToWord() != null) {
            CollectedWord related = wordMap.get(rec.getRelatedToWord().toLowerCase());
            if (related != null) {
                relatedTo = ChatWordsResponse.Related.builder()
                        .wordId(related.getId())
                        .word(related.getWord())
                        .build();
            }
        }

        List<ChatWordsResponse.MeaningByPosDto> meanings = rec.getMeaningsByPos() == null
                ? List.of()
                : rec.getMeaningsByPos().stream()
                .map(m -> ChatWordsResponse.MeaningByPosDto.builder()
                        .partOfSpeech(m.getPartOfSpeech())
                        .meanings(m.getMeanings())
                        .build())
                .toList();

        return ChatWordsResponse.Word.builder()
                .wordId(null)
                .word(rec.getWord())
                .meaningsByPos(meanings)
                .source(ChatWordsResponse.WordSource.AI_RECOMMENDED)
                .isRecommended(true)
                .relationType(relationType)
                .relatedTo(relatedTo)
                .build();
    }

    /** 관계 타입 문자열 → Enum */
    private ChatWordsResponse.RelationType parseRelationType(String type) {
        if (type == null) {
            return ChatWordsResponse.RelationType.RELATED;
        }
        try {
            return ChatWordsResponse.RelationType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 관계 타입: {}, RELATED로 처리", type);
            return ChatWordsResponse.RelationType.RELATED;
        }
    }
}