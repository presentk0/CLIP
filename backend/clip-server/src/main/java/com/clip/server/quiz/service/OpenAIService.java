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
  1. translation: 사용자가 기준으로 삼을 '정확하고 자연스러운 한글 해석'
  2. content: 위 해석에 대응하는 영어 문장
     - answer='O': 입력 단어 '%s'를 문맥에 정확히 사용
     - answer='X': 헷갈릴 법한 유사어로 대체 (예: travel→move)
  3. question: "~일까?", "~맞을까?" 같은 친근한 구어체
  4. explanation: 뉘앙스 차이를 구체적으로 비교 설명

  [난이도 기준]
  - 목표 정답률: 60-80%%
  - X 케이스는 '완전히 틀린 단어'가 아니라 '뉘앙스상 어색한 유사어'
  - 출제 비율: O 60%%, X 40%%

  [JSON 응답 형식]
  {
    "word": "%s",
    "content": "영어 예문",
    "translation": "한글 해석",
    "question": "캐릭터 질문",
    "answer": "O 또는 X",
    "explanation": "뉘앙스 비교 해설",
    "options": null
  }

  [출력 예시]
  // O 케이스
  {
    "content": "I want to travel around Europe.",
    "translation": "유럽을 여행하고 싶어.",
    "question": "travel이 이 해석이랑 잘 맞을까?",
    "answer": "O",
    "explanation": "맞았어! travel은 긴 여정의 여행을 뜻해서 딱이야!"
  }
  
  // X 케이스
  {
    "content": "I want to move around Europe.",
    "translation": "유럽을 여행하고 싶어.",
    "question": "move가 이 해석이랑 잘 맞을까?",
    "answer": "X",
    "explanation": "틀렸어! move는 단순 이동이라 여행의 뉘앙스가 안 나와. travel이 정답이야!"
  }
  """, word, meaning, word, word);

        return processSingle(prompt);
    }

    /**
     * 🔥 빈칸 채우기 퀴즈 생성 (4지선다형 상세 프롬프트)
     */
    public OpenAIQuizDataResponse generateBlankQuiz(String word, String meaning) {
        String prompt = String.format("""
                당신은 영어 빈칸 채우기 퀴즈 전문가입니다. 
                학습자가 단어의 정확한 문맥적 의미를 파악할 수 있도록 4지선다 퀴즈를 생성하세요.

                [입력 단어 정보]
                - 단어: %s
                - 의미: %s

                [퀴즈 생성 규칙]
                1. content: 정답 단어 위치를 반드시 [ ]로 표시한 영어 문장을 만드세요.
                2. translation: 문장의 한글 해석을 작성하세요.
                3. question: 개구리 캐릭터가 던지는 질문입니다. "~까?", "~일까?" 같은 친근한 구어체를 사용하세요."빈칸에 들어갈 가장 알맞은 단어는 뭘까?"와 같은 가이드 메시지를 작성하세요.
                4. options: 정답인 '%s'를 포함하여, 문맥상 헷갈릴 수 있는 수준 높은 오답 3개를 추가해 총 4개의 단어 리스트를 만드세요.
                5. answer: 정답 단어인 '%s'를 적으세요.
                6. explanation: 정답이 왜 정답인지, 그리고 특히 매력적인 오답이 왜 틀렸는지 뉘앙스 차이를 친절하게 설명하세요.

                [JSON 응답 형식]
                {
                  "word": "%s",
                  "content": "The [ ] was amazing.",
                  "translation": "그 여행은 정말 멋졌어.",
                  "question": "빈칸에 뭐가 들어갈까?",
                  "options": ["travel", "move", "go", "visit"],
                  "answer": "%s",
                  "explanation": "해설 내용"
                }
                """, word, meaning, word, word, word, word);

        return processSingle(prompt);
    }

    /**
     * 🔥 매칭 퀴즈 세트 생성
     */
    public List<OpenAIQuizDataResponse> generateMatchingQuiz(List<Map<String, String>> wordList) {
        try {
            String wordsJson = objectMapper.writeValueAsString(wordList);
            String prompt = String.format("""
            당신은 영어 학습 멘토 '클립 프로그'입니다.
            단어-뜻 매칭 퀴즈용 데이터를 생성하세요.
            
            [입력 단어 리스트]
            %s
            
            [출제 규칙]
            1. 각 단어의 '핵심 뜻' 하나만 간결하게 작성 (2-5글자)
            2. explanation에는 암기 팁이나 짧은 예문 추가
            3. 단어 순서는 그대로 유지 (섞지 마세요)
            4. 프론트엔드에서 랜덤 배치할 예정
            
            [JSON 응답 형식]
            {
              "quizzes": [
                {
                  "word": "travel",
                  "quizType": "MATCHING",
                  "content": null,
                  "translation": null,
                  "question": null,
                  "answer": "여행하다",
                  "explanation": "trip보다 긴 여정을 의미해!",
                  "options": null
                }
              ]
            }
            
            [예시 입력]
            [{"word": "travel", "meaning": "여행하다"}, {"word": "move", "meaning": "이동하다"}]
            
            [예시 출력]
            {
              "quizzes": [
                {
                  "word": "travel",
                  "quizType": "MATCHING",
                  "answer": "여행하다",
                  "explanation": "장거리 여행을 뜻해. 예: travel abroad"
                },
                {
                  "word": "move",
                  "quizType": "MATCHING",
                  "answer": "이동하다",
                  "explanation": "위치를 바꾸는 동작. 예: move to Seoul"
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