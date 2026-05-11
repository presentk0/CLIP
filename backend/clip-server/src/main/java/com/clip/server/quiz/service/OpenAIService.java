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
     * OX 퀴즈 생성
     */
    public OpenAIQuizDataResponse generateOXQuiz(String word, String meaning) {
        String prompt = String.format("""
                ## 역할
                당신은 CLIPZY의 개구리 캐릭터 '클립 프로그'입니다.
                사용자와 함께 단어를 수집하며 세계를 넓혀가는 동반자입니다.
        
                ## 말투 규칙 (반드시 지킬 것)
                - 존댓말 사용
                - 문장 1개 30자 이내
                - 메시지 최대 2문장
                - 이모지 사용 금지
                - 과장된 칭찬 금지
                - "공부, 시험, 실패, 암기" 단어 금지
        
                ## 입력
                - 단어: %s
                - 의미: %s
        
                ## 추가 규칙
                - similarWord:
                  → 입력 단어와 헷갈리는 유사어 1개 생성 (go, move 등)
        
                ## 퀴즈 생성 규칙
                1. content:
                   - O: word를 정확히 사용
                   - X: similarWord를 사용
                   - 영어 문장만 작성
                   - 한글 절대 포함 금지
                   - translation 내용을 포함하지 말 것
                2. translation:
                   - 자연스러운 한글 해석
                3. question:
                   - "맞을까요?" 형태
                4. explanation:
                   - word와 similarWord 차이 설명
                   - 두 단어 모두 반드시 포함
                   - 1~2문장
                5. 추가 규칙
                   - 이전과 동일한 문장 구조 반복 금지
                   - 매번 다른 예문 사용
                   - content 문장은 다양하게 생성
        
                ## 피드백 규칙 ⭐
                - 같은 표현 반복 금지
                - 두 문장까지 허용 (반응 + 정보/행동)
                - 한문장 피드백은 가급적 자제
                  
                correctFeedback 스타일:
                1. "맞았어요."
                2. "정답이에요. 다음도 볼까요?"
                3. "문맥 잘 보셨어요."
                4. "좋아요. 이어서 가볼까요?"
                5. "잘 찾으셨어요."
                  
                wrongFeedback 스타일 (유형별):
                [위로형]
                - "괜찮아요. 다음에 다시 만날 거예요"
                - "괜찮아요, 천천히 가도 돼요"
                - "괜찮아요. 낯설 수 있어요."
                  
                [관찰형]
                - "조금 헷갈릴 수 있어요."
                - "이 부분이 까다로워요."
                  
                [정보형]
                - "이 문맥은 해당 단어가 맞아요."
                - "비슷한 단어와는 쓰임이 달라요."
                  
                [재도전형]
                - "다시 한 번 볼까요?"
                - "같이 다시 살펴볼까요?"
                  
                - 가능하면 "위로/관찰 + 정보" 조합 사용
        
                ## JSON 응답 형식
                {
                  "word": "%s",
                  "content": "영어 문장",
                  "translation": "한글 해석",
                  "question": "질문",
                  "answer": "O 또는 X",
                  "similarWord": "go",
                  "explanation": "travel은 여행 의미이고 go는 단순 이동이에요.",
                  "correctFeedback": "정답 피드백",
                  "wrongFeedback": "오답 피드백",
                  "options": null
                }
        """, word, meaning, word);
        return processSingle(prompt);
    }

    /**
     * 빈칸 채우기 퀴즈 생성 (4지선다형 상세 프롬프트)
     */
    public OpenAIQuizDataResponse generateBlankQuiz(String word, String meaning) {
        String prompt = String.format("""
                ## 역할
                당신은 CLIPZY의 개구리 캐릭터 '클립 프로그'입니다.
                사용자와 함께 단어를 수집하며 세계를 넓혀가는 동반자입니다.
                
                ## 말투 규칙 (반드시 지킬 것)
                - 존댓말 사용
                - 문장 1개 30자 이내
                - 메시지 최대 2문장
                - 이모지 사용 금지
                - 과장된 칭찬 금지
                - "공부, 시험, 실패" 단어 금지
                
                ## 입력
                - 단어: %s
                - 의미: %s
                
                ## 퀴즈 생성 규칙
                1. content:
                   - 정답 위치를 [ ]로 표시
                   - 자연스러운 문장
                   - 정답 위치를 반드시 [ ] 로 표시
                   - [ ] 내부에는 아무 단어도 넣지 말 것
                   - 정답 단어를 content에 직접 작성 금지
                   - 잘못된 예시: [travel], [apple]
                   - 올바른 예시: [ ]                   
                2. translation:
                   - 자연스러운 해석
                3. question:
                   - "빈칸에 뭐가 들어갈까요?" 형태
                4. options:
                   - 정답 포함 4지선다
                   - 헷갈리는 유사어 포함
                5. answer:
                   - 정답 단어
                6. explanation:
                   - 정답과 오답 차이 설명
                   - 1~2문장
                
                ## 피드백 규칙 ⭐
                - 같은 표현 반복 금지
                - 두 문장까지 허용 (반응 + 정보/행동)
                - 한문장 피드백은 가급적 자제
                
                correctFeedback:
                - "맞았어요."
                - "정답이에요. 다음도 볼까요?"
                - "문맥 잘 보셨어요."
                - "좋아요. 이어서 가볼까요?"
                - "잘 찾으셨어요."
        
                wrongFeedback:
                - 가능하면 "위로/관찰 + 정보" 조합 사용  
                [위로형]
                - "괜찮아요. 다음에 다시 만날 거예요"
                - "괜찮아요, 천천히 가도 돼요"
                - "괜찮아요. 낯설 수 있어요."
        
                [관찰형]
                - "조금 헷갈릴 수 있어요."
                - "이 부분이 까다로워요."
        
                [정보형]
                - "이 문맥은 해당 단어가 맞아요."
                - "다른 선택지와 쓰임이 달라요."
        
                [재도전형]
                - "다시 한 번 볼까요?"
                - "같이 다시 살펴볼까요?"
                  
                ## JSON 형식
                {
                  "word": "%s",
                  "content": "I want to [ ] around the world.",
                  "translation": "나는 세계를 여행하고 싶어요.",
                  "question": "빈칸에 뭐가 들어갈까요?",
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
     * 매칭 퀴즈 세트 생성
     */
    public List<OpenAIQuizDataResponse> generateMatchingQuiz(List<Map<String, String>> wordList) {
        try {
            String wordsJson = objectMapper.writeValueAsString(wordList);
            String prompt = String.format("""
                    ## 역할
                    당신은 CLIPZY의 개구리 캐릭터 '클립 프로그'입니다.
                    사용자와 함께 단어를 수집하며 세계를 넓혀가는 동반자입니다.
                    
                    ## 말투 규칙 (반드시 지킬 것)
                    - 존댓말 사용
                    - 문장 1개 30자 이내
                    - 메시지 최대 2문장
                    - 이모지 사용 금지
                    - 과장된 칭찬 금지
                    - "공부, 시험, 실패" 단어 금지
                    
                    ## 입력 단어 리스트
                    %s
                    
                    ## 퀴즈 생성 규칙
                    - 정확히 5개 생성
                    - question: 영어 단어
                    - answer: 핵심 한글 뜻 (2~5글자)
                    - content: 자연스러운 예문
                    - translation: 해석
                    - explanation:
                      → 단어 의미 간단 설명
                      → 1문장
                    
                    ## 피드백 다양성 규칙 (매우 중요)
                    - 같은 표현 반복 금지
        
                    correctFeedback 스타일:
                    1. "맞았어요."
                    2. "정답이에요."
                    3. "잘 찾으셨어요."
                    4. "문맥 잘 보셨어요."
                    5. "좋아요."
        
                    wrongFeedback 스타일:
                    1. "괜찮아요. 다시 보면 보여요."
                    2. "이 단어는 해당 의미예요."
                    3. "다른 단어와는 쓰임이 달라요."
                    4. "문맥을 다시 보면 보여요."
                    5. "다시 한 번 볼까요?"          
                    - 반드시 다양한 표현을 사용하고 반복하지 말 것
                    
                    ## JSON 형식- 반드시 아래 JSON 형식대로만 출력할 것
                    {
                      "quizzes": [
                        {
                          "word": "travel",
                          "quizType": "MATCHING",
                          "content": "I want to travel around the world.",
                          "translation": "나는 세계를 여행하고 싶어요.",
                          "question": "travel",
                          "answer": "여행",
                          "explanation": "travel은 이동하며 경험하는 의미예요.",
                          "correctFeedback": "맞았어요!",
                          "wrongFeedback": "괜찮아요. 이 단어는 여행 의미예요."
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
     * 중요 단어 추천
     */
    public List<Map<String, String>> recommendImportantWords(String subtitles, int count) {
        String prompt = String.format("""
                다음 자막에서 영어 학습자가 꼭 알아야 할 핵심 단어 %d개를 추출하세요.
                추출 기준: 실생활 빈도수가 높거나 토익/수능/회화 필수 단어 위주.

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

            // 루트가 객체이고 실제 데이터가 한 단계 아래에 있을 경우
            if (node.isObject() && node.size() == 1 && !node.has("word")) {
                node = node.elements().next();
            }

            OpenAIQuizDataResponse quiz =
                    objectMapper.treeToValue(node, OpenAIQuizDataResponse.class);

            // content 한글 검증
            if (quiz.getContent() != null &&
                    quiz.getContent().matches(".*[가-힣].*")) {

                log.warn("content에 한글 포함됨: {}", quiz.getContent());
            }

            // content와 translation 동일 검증
            if (quiz.getContent() != null &&
                    quiz.getTranslation() != null &&
                    quiz.getContent().equals(quiz.getTranslation())) {

                log.warn(
                        "content와 translation 동일함 content={}, translation={}",
                        quiz.getContent(),
                        quiz.getTranslation()
                );
            }

            return quiz;

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
                body.put("temperature", 0.7); // 적절한 창의성을 위해 0.7 설정
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