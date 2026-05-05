package com.clip.server.quiz.service;

import com.clip.server.quiz.dto.response.OpenAIQuizDataResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenAIService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url}")
    private String apiUrl;

    @Value("${openai.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 🔥 OX 퀴즈 생성 (와이어프레임 최적화 프롬프트)
     */
    public OpenAIQuizDataResponse generateOXQuiz(String word, String meaning) {
        String prompt = String.format("""
            당신은 친절한 영어 멘토 '클립 프로그(Clip Frog)'입니다. 
            학습자가 단어의 '뉘앙스'를 정확히 이해했는지 확인하는 OX 퀴즈를 생성하세요.
            
            [입력 단어 정보]
            - 단어: %s
            - 의미: %s
            
            [퀴즈 출제 전략]
            1. translation: 자연스러운 한글 해석
            2. content:
               - O: '%s'를 정확히 사용
               - X: 헷갈리는 유사어 사용 (move, go 등)
            3. question: "~맞을까?", "~일까?" 같은 친근한 말투
            4. explanation: 두 단어의 차이를 명확히 비교 설명
            
            [피드백 생성 규칙 ⭐]
            - correctFeedback:
              → 짧고 텐션 높은 칭찬
              → 매번 다르게 생성
            - wrongFeedback:
              → 귀엽고 긍정적인 실패 표현
              → 학습 의욕 유지
            
            [예시 톤]
            - 정답: "오케이! 이건 완전 감 잡았네 😎"
            - 정답: "와 이건 거의 원어민인데?"
            - 오답: "앗! 이건 럭키 미스 😆 다음엔 잡는다!"
            - 오답: "아깝다! 거의 맞았어!"
            
            반드시 아래 JSON 응답 형식에 맞게 응답하시오.
            [JSON 응답 형식]
            {
              "word": "%s",
              "content": "영어 문장",
              "translation": "한글 해석",
              "question": "질문",
              "answer": "O 또는 X",
              "explanation": "해설",
              "correctFeedback": "정답 피드백",
              "wrongFeedback": "오답 피드백",
              "options": null
            }
""", word, meaning, word, word);
        return processSingle(prompt);
    }

    /**
     * 🔥 빈칸 채우기 퀴즈 생성 (4지선다형 상세 프롬프트)
     */
    public OpenAIQuizDataResponse generateBlankQuiz(String word, String meaning) {
        String prompt = String.format("""
                당신은 영어 빈칸 퀴즈 전문가이자 캐릭터 '클립 프로그'입니다.
                학습자가 단어의 정확한 문맥적 의미를 파악할 수 있도록 4지선다 퀴즈를 생성하세요.
                
                [입력 단어]
                - 단어: %s
                - 뜻: %s
                
                [퀴즈 생성 규칙]
                1. content: 정답 위치를 [ ]로 표시
                2. translation: 자연스러운 해석
                3. question: "빈칸에 뭐가 들어갈까?" 같은 친근한 말투
                4. options: 정답 포함 4지선다 (헷갈리는 오답 포함)
                5. answer: 정답 단어
                6. explanation: 정답 + 오답 차이 설명
                
                [피드백 생성 규칙 ⭐]
                - correctFeedback:
                  → 짧고 기분 좋은 칭찬
                - wrongFeedback:
                  → 귀엽고 긍정적인 실패 표현
                
                [예시 톤]
                - 정답: "정확해! 이건 완전 네 거다 🔥"
                - 정답: "와 센스 있다!"
                - 오답: "앗! 이건 살짝 헷갈렸지 😆"
                - 오답: "거의 맞았는데! 다음엔 맞춘다!"
                
                반드시 아래 JSON 응답 형식에 맞게 응답하시오.
                [JSON 응답 형식]
                {
                  "word": "%s",
                  "content": "I want to [ ] around the world.",
                  "translation": "나는 세계를 여행하고 싶어.",
                  "question": "빈칸에 뭐가 들어갈까?",
                  "options": ["travel", "move", "go", "visit"],
                  "answer": "%s",
                  "explanation": "해설",
                  "correctFeedback": "정답 피드백",
                  "wrongFeedback": "오답 피드백"
                }
                """, word, meaning, word, word);

        return processSingle(prompt);
    }

    /**
     * 🔥 매칭 퀴즈 세트 생성
     */
    public List<OpenAIQuizDataResponse> generateMatchingQuiz(List<Map<String, String>> wordList) {
        try {
            String wordsJson = objectMapper.writeValueAsString(wordList);
            String prompt = String.format("""
                    당신은 영어 학습을 도와주는 캐릭터 '클립 프로그'입니다.
                    단어-뜻 매칭 퀴즈 데이터를 생성하세요. 다음 단어들로 매칭 퀴즈를 정확히 5개만 생성해줘.
                    사용자가 지루하지 않도록 친근하고 감정이 담긴 피드백을 제공합니다.
                    
                    [입력 리스트]
                    %s
                    
                    [퀴즈 생성 규칙]
                    1. question: 단어 원문 (예: travel)
                    2. answer: 핵심 한글 뜻 (2~5글자)
                    3. explanation: 짧고 이해 쉬운 설명
                    4. content: 해당 단어를 사용한 자연스러운 영어 예문
                    5. translation: 예문의 한글 해석
                    
                    [피드백 생성 규칙 ⭐ 매우 중요]
                    - correctFeedback: 정답 시 반응
                      → 칭찬 + 텐션 있음 + 짧고 임팩트
                      → 매번 다르게 생성 (절대 반복 금지)
                    
                    - wrongFeedback: 오답 시 반응
                      → 귀엽고 긍정적인 실패 표현 (럭키 미스 느낌)
                      → 학습 의욕 떨어지지 않게
                    
                    [예시 톤]
                    - 정답: "완벽해! 지금 감 잡았는데?"
                    - 정답: "와 이건 거의 원어민인데?"
                    - 오답: "앗! 이건 럭키 미스다 😆 다음에 잡자!"
                    - 오답: "아깝다! 거의 맞았어, 한 번만 더!"
                    
                    반드시 아래 JSON 응답 형식에 맞게 응답하시오.
                    [JSON 응답 형식 - 반드시 이 구조 유지]
                    [중요 규칙]
                    - correctFeedback과 wrongFeedback은 반드시 모두 생성해야 한다.
                    - 절대 생략하지 말 것
                    - 둘 중 하나라도 누락되면 잘못된 응답으로 간주됨
                    {
                      "quizzes": [
                        {
                          "word": "travel",
                          "quizType": "MATCHING",
                          "content": "I want to travel around the world.",
                          "translation": "나는 세계를 여행하고 싶다.",
                          "question": "travel",
                          "answer": "여행",
                          "explanation": "travel은 장거리 이동이나 여행을 의미함",
                          "correctFeedback": "완벽해! 감 제대로 잡았네!",
                          "wrongFeedback": "앗! 이건 럭키 미스  다음엔 맞춘다!"
                        }
                      ]
                    }
                    """, wordsJson);
            return processList(prompt, new TypeReference<List<OpenAIQuizDataResponse>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * 🔥 중요 단어 추천
     */
    public List<Map<String, String>> recommendImportantWords(String subtitles, int count) {
        String prompt = String.format("""
                다음 자막에서 영어 학습자가 꼭 알아야 할 핵심 단어 %d개를 추출하세요.
                추출 기준: 실생활 빈도수가 높거나 토익/수능 필수 단어 위주.

                [자막 내용]
                %s

                [응답 형식 - 반드시 "data" 키로 감싼 배열이어야 함]
                {
                  "data": [
                    {"word": "apple", "meaning": "사과"}
                  ]
                }
                """, count, subtitles);

        return processList(prompt, new TypeReference<List<Map<String, String>>>() {});
    }

    // --- 공통 처리 로직 (안정적인 파싱) ---

    private OpenAIQuizDataResponse processSingle(String prompt) {
        String raw = callOpenAI(prompt);
        if (raw == null) return null;
        try {
            JsonNode node = extractContentNode(raw);
            // 루트가 객체이고 실제 데이터가 한 단계 아래에 있을 경우를 대비한 언래핑
            if (node.isObject() && node.size() == 1 && !node.has("word")) {
                node = node.elements().next();
            }
            return objectMapper.treeToValue(node, OpenAIQuizDataResponse.class);
        } catch (Exception e) {
            log.error("단일 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private <T> List<T> processList(String prompt, TypeReference<List<T>> typeReference) {
        String raw = callOpenAI(prompt);
        if (raw == null) return List.of();
        try {
            JsonNode node = extractContentNode(raw);
            if (node.isObject()) {
                for (JsonNode subNode : node) {
                    if (subNode.isArray()) return objectMapper.convertValue(subNode, typeReference);
                }
            }
            return objectMapper.convertValue(node, typeReference);
        } catch (Exception e) {
            log.error("리스트 파싱 실패: {}", e.getMessage());
            return List.of();
        }
    }

    private String callOpenAI(String prompt) {
        int retry = 3;
        long wait = 1000;
        for (int i = 0; i < retry; i++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setBearerAuth(apiKey);

                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("temperature", 0.5); // 💡 적절한 창의성을 위해 0.5 설정
                body.put("messages", List.of(
                        Map.of("role", "system", "content", "당신은 모든 응답을 JSON으로만 해야 하는 영어 교육 봇입니다. 절대 JSON 외의 텍스트를 포함하지 마세요."),
                        Map.of("role", "user", "content", prompt)
                ));
                body.put("response_format", Map.of("type", "json_object"));

                return restTemplate.postForObject(apiUrl, new HttpEntity<>(body, headers), String.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                try { Thread.sleep(wait); } catch (InterruptedException ignored) {}
                wait *= 2;
            } catch (Exception e) {
                break;
            }
        }
        return null;
    }

    private JsonNode extractContentNode(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        // 마크다운 기호 제거 로직 (ObjectMapper 안정성 확보)
        String cleaned = content.replaceAll("```json|```", "").trim();
        return objectMapper.readTree(cleaned);
    }
}