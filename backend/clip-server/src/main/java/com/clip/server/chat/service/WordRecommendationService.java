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
import com.clip.server.word.prompt.WordRecommendPromptBuilder;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WordRecommendationService {

    private final OpenAiClient openAiClient;
    private final ObjectMapper objectMapper;
    private final WordRecommendPromptBuilder promptBuilder;

    /**
     * 사용자 단어 목록을 받아 AI 추천 단어 생성 (1:1 비율)
     */
    public List<ChatWordsResponse.Word> recommendWords(List<CollectedWord> userWords) {
        if (userWords == null || userWords.isEmpty()) {
            return List.of();
        }

        try {
            // 1. 프롬프트 생성
            String systemPrompt = promptBuilder.getSystemPrompt();
            String userPrompt = promptBuilder.buildUserPrompt(userWords);

            // 2. AI 호출
            String aiResponse = openAiClient.chat(systemPrompt, userPrompt);
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