package com.clip.server.quiz.ai;

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
import java.util.concurrent.ThreadLocalRandom;

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

    @Value("${openai.api.model}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // ==================== OX 퀴즈 생성 ====================

    /**
     * OX 퀴즈 생성
     *
     * @param word     단어
     * @param meaning  의미
     * @param failHint 이전 시도 실패 사유 (없으면 null) → Self-Correction용
     */
    public OpenAIQuizDataResponse generateOXQuiz(String word, String meaning, String failHint, String difficulty) {

        String hintSection = buildHintSection(failHint);
        String difficultySection = buildDifficultySection(difficulty);

        // 정답 50:50 랜덤 결정 (정답 분포 균등화)
        boolean isCorrectAnswer = ThreadLocalRandom.current().nextBoolean();
        String answerType = isCorrectAnswer ? "O" : "X";

        // 정답에 따른 명확한 지시
        String answerSection = String.format("""
        ## ⭐ 이번 문제 정답 지정 (반드시 준수)
        정답: %s
        
        %s
        """,
                answerType,
                isCorrectAnswer
                        ? String.format("""
                        → content에 단어 '%s'를 자연스러운 문장 안에 사용하세요.
                        → 일상 회화에서 쓸 법한 자연스러운 문장으로 작성하세요.
                        → 단어가 문맥에 자연스럽게 어울려야 합니다 ('O' 정답).
                        
                        ✅ 좋은 예시 (단어가 'travel'일 때):
                        - "I want to travel to Japan next year."
                        - "She loves to travel by train."
                        
                        ❌ 나쁜 예시 (피할 것):
                        - "This word means travel." (단어 설명 문장 금지)
                        - "Travel is a verb." (메타 설명 금지)
                        """, word)
                        : String.format("""
                        → content에 단어 '%s'를 절대 사용하지 마세요.
                        → similarWord를 사용한 문장을 작성하되,
                          입력 단어(word)를 사용하는 것이 훨씬 자연스러운 문맥이어야 함
                        
                        → 사용자가 문장을 읽었을 때
                          "문법 오류"가 아니라
                          "이 자리는 word가 더 적절하다"
                          라고 판단할 수 있어야 함
                        
                        → X 문제는 반드시
                          word와 similarWord의 미묘한 쓰임 차이를 이용해 생성
                        
                        예시
                        
                        word = travel
                        similarWord = go
                        
                        좋은 예시
                        I want to go around the world by train.
                        → travel이 더 적절
                        
                        나쁜 예시
                        I go to school every day.
                        → go가 자연스러우므로 문제로 부적절
                        """, word, word)
        );

        log.debug("OX 퀴즈 생성 - word: {}, 지정 정답: {}", word, answerType);

        String prompt = String.format("""
            ## 역할
            당신은 CLIPZY의 개구리 캐릭터 '클립 프로그'입니다.
            사용자와 함께 단어를 수집하며 세계를 넓혀가는 동반자입니다.
            %s
            %s
            %s
            
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
              → 입력 단어와 같은 품사여야 함
              → 의미가 일부 겹쳐야 함
              → RELATED 단어 금지
              → 동사면 동사, 명사면 명사만 사용
              → 입력 단어보다 훨씬 어렵거나 희귀한 단어 사용 금지
            
            예시
            travel → go
            donate → give
            purchase → buy
            
            잘못된 예시
            travel → airplane
            donate → money
            purchase → shopping

            ## 퀴즈 생성 규칙
            1. content:
               - 위 '이번 문제 정답 지정' 섹션의 지시를 반드시 따를 것
               - 영어 문장만 작성
               - 한글 절대 포함 금지
               - translation 내용을 포함하지 말 것
               - content는 반드시 실제 회화에서 사용할 수 있는 자연스러운 문장이어야 함
                   ⚠️ 단어 설명 문장 금지
                   금지 예시:
                   "This word means travel."
                   "Travel is a verb."
                   "Travel and go are different."
            2. translation:
               - 자연스러운 한글 해석
            3. question:
               - 반드시 아래 문장 사용
               - "이 문장은 자연스러운 표현일까요?"
            4. answer:
               - 반드시 "%s"를 그대로 작성 (O 또는 X)     
            5. word 필드:
               - 반드시 입력 단어 "%s"를 그대로 작성
                - ⚠️ "O"나 "X"를 word 필드에 넣지 마세요!                    
            6. explanation:
               - word와 similarWord를 모두 포함
               - 왜 word가 더 적절한지 설명
               - similarWord가 왜 덜 적절한지 설명
               - 사전식 정의 금지
               - 실제 사용 상황 차이를 설명
            
                좋은 예시:
                travel은 여행이나 장거리 이동을 말할 때 사용해요.
                go는 단순 이동을 의미하는 더 일반적인 표현이에요.
                
                나쁜 예시:
                travel은 여행이다.
                go는 가다.
            7. relatedExpressions (함께 알아두면 좋은 표현):
               - word와 관련된 표현이나 활용법 1~2개
               - 형식: "표현 : 의미 (영어 예문. 한글 해석.)"
               - 각 표현은 줄바꿈(\\n)으로 구분
               - 예: "stop -ing : ~하는 것을 멈추다 (He stopped smoking. 그는 담배를 끊었다.)\\nstop to 동사원형 : ~하기 위해 멈추다 (He stopped to smoke. 그는 담배를 피우려고 멈췄다.)"

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
              "answer": "%s",
              "similarWord": "go",
              "explanation": "travel은 여행 의미이고 go는 단순 이동이에요.",
              "correctFeedback": "정답 피드백",
              "wrongFeedback": "오답 피드백",
              "relatedExpressions": "go : 단순 이동을 의미해요 (I go to school. 나는 학교에 가요.)\\ntravel to : ~로 여행하다 (I travel to Japan. 나는 일본으로 여행해요.)",
              "options": null
            }
            """, hintSection, answerSection, difficultySection, word, meaning, answerType, word, word, answerType);

        return processSingle(prompt);
    }

    // ==================== 빈칸 퀴즈 생성 ====================

    /**
     * 빈칸 채우기 퀴즈 생성
     *
     * @param failHint 이전 시도 실패 사유 (없으면 null)
     */
    public OpenAIQuizDataResponse generateBlankQuiz(String word, String meaning, String failHint, String difficulty) {
        String hintSection = buildHintSection(failHint);
        String difficultySection = buildDifficultySection(difficulty);

        String prompt = String.format("""
                ## 역할
                당신은 CLIPZY의 개구리 캐릭터 '클립 프로그'입니다.
                사용자와 함께 단어를 수집하며 세계를 넓혀가는 동반자입니다.
                %s
                %s
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

                   - answer 단어는 content 어디에도 등장하면 안 됨
                   - answer의 변형 형태도 등장하면 안 됨
                     (예: travel, traveled, traveling, traveler 모두 금지)

                   - 사용자는 content만 보고 정답을 추론할 수 있어야 함
                   - 정답을 직접 암시하는 표현 금지

                   잘못된 예시:
                   - Travel is fun. I want to [ ] around the world.
                   - I am traveling now. Let's [ ] tomorrow.

                   올바른 예시:
                   - I want to [ ] around the world.
                   - She decided to [ ] by train this summer.
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
                7. relatedExpressions (함께 알아두면 좋은 표현):
                   - 정답 단어와 관련된 표현이나 활용법 1~2개
                   - 형식: "표현 : 의미 (영어 예문. 한글 해석.)"
                   - 각 표현은 줄바꿈(\\n)으로 구분
                   - 예: "travel by : ~로 여행하다 (I travel by train. 나는 기차로 여행해요.)\\ntravel abroad : 해외 여행하다 (She loves to travel abroad. 그녀는 해외 여행을 좋아해요.)"
                   
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
                  "relatedExpressions": "travel by : ~로 여행하다 (I travel by train. 나는 기차로 여행해요.)\\ntravel abroad : 해외 여행하다 (She loves to travel abroad. 그녀는 해외 여행을 좋아해요.)",
                  "correctFeedback": "정답 피드백",
                  "wrongFeedback": "오답 피드백"
                }
               """, hintSection, difficultySection, word, meaning, word, word);


        return processSingle(prompt);
    }

    // ==================== 매칭 퀴즈 생성 ====================

    /**
     * 매칭 퀴즈 세트 생성
     *
     * @param failHint 이전 시도 실패 사유 (없으면 null)
     */
    public List<OpenAIQuizDataResponse> generateMatchingQuiz(List<Map<String, String>> wordList, String failHint) {
        try {
            String wordsJson = objectMapper.writeValueAsString(wordList);
            String hintSection = buildHintSection(failHint);

            String prompt = String.format("""
                    ## 역할
                    당신은 CLIPZY의 개구리 캐릭터 '클립 프로그'입니다.
                    사용자와 함께 단어를 수집하며 세계를 넓혀가는 동반자입니다.
                    %s
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
                      - 반드시 해당 단어 자체의 기본 뜻만 작성
                      - 복합어나 문맥 속 의미가 아닌, 단어 사전적 의미 사용
                      - 올바른 예시: service → 서비스, 봉사
                      - 잘못된 예시: service → 보조견 (service dog의 뜻이므로 틀림)
                    - content: 자연스러운 예문
                    - translation: 해석
                    - explanation: 단어 의미 간단 설명 (1문장)
                    - relatedExpressions: 함께 알아두면 좋은 표현 1~2개 (선택사항, 없으면 null 가능)
                      형식: "표현 : 의미 (영어 예문. 한글 해석.)"
                      각 표현은 줄바꿈(\\n)으로 구분
                      예: "travel by : ~로 여행하다 (I travel by train. 나는 기차로 여행해요.)"
                      
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

                    ## JSON 형식 - 반드시 아래 JSON 형식대로만 출력할 것
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
                          "relatedExpressions": "travel by : ~로 여행하다 (I travel by train. 나는 기차로 여행해요.)",
                          "correctFeedback": "맞았어요!",
                          "wrongFeedback": "괜찮아요. 이 단어는 여행 의미예요."
                        }
                      ]
                    }
                    """, hintSection, wordsJson);

            return processList(prompt, new TypeReference<List<OpenAIQuizDataResponse>>() {});
        } catch (Exception e) {
            log.error("매칭 퀴즈 프롬프트 생성 실패", e);
            return List.of();
        }
    }

    // ==================== 중요 단어 추천 ====================

    /**
     * 중요 단어 추천 (재시도 패턴 미적용 - 필요 시 추가 가능)
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

    // ==================== Self-Correction 힌트 빌더 ====================

    /**
     * 이전 시도 실패 사유를 프롬프트 섹션으로 변환
     * - null이면 빈 문자열 반환
     * - 값이 있으면 강조 섹션 생성
     */
    private String buildHintSection(String failHint) {
        if (failHint == null || failHint.isBlank()) {
            return "";
        }
        return String.format("""
                
                ## ⚠️ 이전 시도 실패 사유 (반드시 이를 피해서 다시 생성하세요)
                %s
                
                위 실패 사유를 분석하고, 이번에는 반드시 해당 문제를 피해서 응답하세요.
                """, failHint);
    }

    // ==================== 난이도 조절 섹션 빌더 ====================

    /**
     * Agent Strategy에서 결정된 난이도를 프롬프트 섹션으로 변환
     * - MEDIUM 또는 null이면 빈 문자열 반환 (기존 프롬프트 그대로 사용)
     * - EASY: 초보자용 규칙 추가
     * - HARD: 고급자용 규칙 추가
     */
    private String buildDifficultySection(String difficulty) {
        if (difficulty == null || "MEDIUM".equalsIgnoreCase(difficulty)) {
            return "";  // 검증된 표준 프롬프트 그대로 사용
        }

        if ("EASY".equalsIgnoreCase(difficulty)) {
            return """
                    
                    ## 🎯 난이도: EASY (초보자용 - 자신감 회복)
                    다음 규칙을 반드시 추가로 준수하세요:
                    
                    - 문장 길이: 5~8단어 이내로 짧게
                    - 어휘: 초등~중학교 수준의 기본 단어만 사용
                    - 문법: 단순 현재형 또는 과거형만 사용
                    - 복잡한 부사구, 종속절, 관계대명사 사용 금지
                    - 문장 구조: 주어 + 동사 + 목적어 정도의 단순 구조
                    
                    좋은 예시 (EASY):
                    - "I want to travel to Japan."
                    - "She loves to travel."
                    
                    피해야 할 예시 (너무 어려움):
                    - "The delegation intends to travel abroad next quarter."
                    - "Having traveled extensively, she felt confident."
                    """;
        }

        if ("HARD".equalsIgnoreCase(difficulty)) {
            return """
                    
                    ## 🎯 난이도: HARD (고급자용 - 도전 강화)
                    다음 규칙을 반드시 추가로 준수하세요:
                    
                    - 문장 길이: 10단어 이상으로 충분히 길게
                    - 어휘: 격식체 표현 또는 학술적 어휘 활용
                    - 문법: 복잡한 시제(완료형, 진행형), 부사구, 종속절 포함
                    - 문맥: 비즈니스/학술/뉴스 등 실전 상황 반영
                    - 유사어 트릭: 매우 미묘한 뉘앙스 차이를 활용
                    
                    좋은 예시 (HARD):
                    - "The tourist decided to travel extensively before returning home."
                    - "Delegates from multiple countries traveled to attend the conference."
                    
                    피해야 할 예시 (너무 쉬움):
                    - "I travel."
                    - "She goes to school."
                    """;
        }

        return "";  // 알 수 없는 값이면 기본 프롬프트
    }

    // ==================== 공통 처리 로직 ====================

    private OpenAIQuizDataResponse processSingle(String prompt) {
        String raw = callOpenAI(prompt);
        if (raw == null) return null;

        try {
            JsonNode node = extractContentNode(raw);

            // 루트가 객체이고 실제 데이터가 한 단계 아래에 있을 경우
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
                body.put("temperature", 0.7);
                body.put("messages", List.of(
                        Map.of("role", "system", "content", "당신은 모든 응답을 JSON으로만 해야 하는 영어 교육 봇입니다. 절대 JSON 외의 텍스트를 포함하지 마세요."),
                        Map.of("role", "user", "content", prompt)
                ));
                body.put("response_format", Map.of("type", "json_object"));

                return restTemplate.postForObject(apiUrl, new HttpEntity<>(body, headers), String.class);
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("OpenAI Rate Limit - {}회 재시도 대기 {}ms", i + 1, wait);
                try { Thread.sleep(wait); } catch (InterruptedException ignored) {}
                wait *= 2;
            } catch (Exception e) {
                log.error("OpenAI 호출 실패", e);
                break;
            }
        }
        return null;
    }

    private JsonNode extractContentNode(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
        String content = root.path("choices").get(0).path("message").path("content").asText();
        String cleaned = content.replaceAll("```json|```", "").trim();
        return objectMapper.readTree(cleaned);
    }
}