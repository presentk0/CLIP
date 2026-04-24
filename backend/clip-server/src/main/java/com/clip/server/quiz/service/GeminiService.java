package com.clip.server.quiz.service;

import com.clip.server.quiz.dto.request.GeminiRequest;
import com.clip.server.quiz.dto.response.GeminiQuizData;
import com.clip.server.quiz.dto.response.GeminiResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 특정 단어와 뜻을 기반으로 OX 퀴즈를 생성합니다.
     */
    public GeminiQuizData generateOXQuiz(String word, String meaning) {
        String prompt = String.format("""
                당신은 영어 원어민 강사입니다. 
                단어 [%s](뜻: %s)를 활용해 학습용 OX 퀴즈를 1개 생성하세요.

                [조건]
                1. 'question'은 "~의 뜻은 '...'이다."와 같은 형태의 문장이어야 합니다.
                2. 'answer'는 정답이 맞으면 "O", 틀리면 "X"로 답하세요.
                3. 'explanation'에는 왜 정답/오답인지 토익 수험생에게 설명하듯 친절하게 적어주세요.
                4. 'quizType'은 "OX"로 고정하세요.
                            
                [응답 형식]
                반드시 아래 필드를 포함한 순수 JSON 데이터만 출력하세요. (마크다운 기호 ```json 금지)
                {
                  "word": "%s",
                  "quizType": "OX",
                  "question": "생성된 질문",
                  "answer": "O/X",
                  "explanation": "설명"
                }
                """, word, meaning, word);

        return processQuizGeneration(prompt);
    }

    /**
     * 특정 단어와 뜻을 기반으로 빈칸 채우기 퀴즈를 생성합니다.
     */
    public GeminiQuizData generateBlankQuiz(String word, String meaning) {
        String prompt = String.format("""
                당신은 영어 원어민 강사입니다.
                단어 [%s](뜻: %s)를 활용해 학습용 빈칸 채우기 퀴즈를 1개 생성하세요.

                [조건]
                1. 'question'은 해당 단어가 들어갈 자리에 '____'가 포함된 영어 예문이어야 합니다.
                2. 예문은 토익 850점 수준에 적합한 비즈니스 또는 일상 대화 문맥이어야 합니다.
                3. 'answer'는 정답 단어인 "%s"여야 합니다.
                4. 'explanation'에는 문장의 해석과 해당 단어가 왜 그 자리에 들어가는지 문법적/문맥적 설명을 친절하게 적어주세요.
                5. 'quizType'은 "BLANK"로 고정하세요.

                [응답 형식]
                반드시 아래 필드를 포함한 순수 JSON 데이터만 출력하세요. (마크다운 기호 ```json 금지)
                {
                  "word": "%s",
                  "quizType": "BLANK",
                  "question": "____가 포함된 영어 예문",
                  "answer": "%s",
                  "explanation": "해석 및 설명"
                }
                """, word, meaning, word, word, word);

        return processQuizGeneration(prompt);
    }

    /**
     * 여러 단어 리스트를 기반으로 단어-뜻 매칭 퀴즈를 생성합니다.
     * @param wordMeaningList 매칭할 단어와 뜻의 리스트 (Map 형태: "word" -> 단어, "meaning" -> 뜻)
     */
    public List<GeminiQuizData> generateMatchingQuiz(List<Map<String, String>> wordMeaningList) {
        String prompt = String.format("""
                당신은 영어 원어민 강사입니다.
                아래 제공된 단어와 뜻 리스트를 기반으로 단어-뜻 매칭 퀴즈 데이터를 생성하세요.
                리스트: %s
                
                [조건]
                1. 리스트에 있는 각 단어에 대해 'word' 필드에는 영어 단어를, 'answer' 필드에는 해당 단어의 한국어 뜻을 넣으세요.
                2. 'quizType'은 "MATCHING"으로 설정하세요.
                3. 'question' 필드는 단어 자체를 넣거나 비워두어도 됩니다.
                4. 'explanation'은 해당 단어의 뜻풀이를 간단히 적어주세요.
                5. 응답 형식은 반드시 아래 형식을 따르는 JSON 배열이어야 합니다. (마크다운 기호 ```json 금지)
                
                [응답 형식]
                [
                  {
                    "word": "단어",
                    "quizType": "MATCHING",
                    "question": "단어",
                    "answer": "한국어 뜻",
                    "explanation": "설명"
                  },
                  ...
                ]
                """, wordMeaningList.toString());

        String jsonResponse = callGemini(prompt);
        try {
            return objectMapper.readValue(jsonResponse, new TypeReference<List<GeminiQuizData>>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패! AI 응답: {}", jsonResponse);
            throw new RuntimeException("매칭 퀴즈 데이터 변환 중 오류가 발생했습니다.", e);
        }
    }

    public List<Map<String, String>> recommendImportantWords(String subtitles, int count) {
        String prompt = String.format("""
                당신은 토익 850점 수준의 전문 강사입니다. 
                아래 제공된 영상 자막에서 학습자가 공부하기 좋은 중요 단어 %d개를 선정하세요.
                
                [조건]
                1. 비즈니스 환경이나 토익 시험에 자주 나오는 단어 위주로 선정하세요.
                2. 각 단어의 정확한 사전적 의미(뜻)를 한국어로 적으세요.
                
                [자막 내용]
                %s
                
                [응답 형식]
                반드시 아래 형식을 지킨 JSON 배열만 반환하세요. (마크다운 기호 금지)
                [
                  {"word": "implement", "meaning": "구현하다, 실행하다"},
                  ...
                ]
                """, count, subtitles);

        // callGemini 메서드를 통해 AI 응답을 가져옵니다.
        String jsonResponse = callGemini(prompt);

        try {
            return objectMapper.readValue(jsonResponse, new TypeReference<List<Map<String, String>>>() {});
        } catch (JsonProcessingException e) {
            log.error("AI 단어 추천 JSON 파싱 실패: {}", jsonResponse);
            return List.of(); // 실패 시 빈 리스트 반환
        }
    }

    /**
     * 프롬프트를 받아 Gemini API를 호출하고 결과를 객체로 변환하는 공통 로직입니다.
     */
    private GeminiQuizData processQuizGeneration(String prompt) {
        String jsonResponse = callGemini(prompt);
        try {
            return objectMapper.readValue(jsonResponse, GeminiQuizData.class);
        } catch (JsonProcessingException e) {
            log.error("JSON 파싱 실패! AI 응답: {}", jsonResponse);
            throw new RuntimeException("퀴즈 데이터 변환 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Gemini API와의 실제 통신을 담당하는 공통 메서드입니다.
     */
    private String callGemini(String prompt) {
        String finalUrl = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("key", apiKey)
                .build()
                .toUriString();

        GeminiRequest request = GeminiRequest.from(prompt);

        try {
            GeminiResponse response = restTemplate.postForObject(finalUrl, request, GeminiResponse.class);

            if (response != null && response.getAnswer() != null) {
                String rawAnswer = response.getAnswer();
                String cleanedJson = rawAnswer.replaceAll("```json|```", "").trim();

                log.info("🎯 Gemini 응답 수신 및 정제 완료");
                return cleanedJson;
            }
            throw new RuntimeException("Gemini로부터 빈 응답을 받았습니다.");
        } catch (Exception e) {
            log.error("❌ Gemini API 호출 중 에러 발생: {}", e.getMessage());
            throw new RuntimeException("AI 서비스 통신 실패", e);
        }
    }
}