package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.OpenAIQuizDataResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.entity.QuizResult;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizType;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final OpenAIService openAIService;
    private final QuizResultRepository quizResultRepository;
    private final UserRepository userRepository;
    private final QuizSessionRepository quizSessionRepository;

    /**
     * OX 퀴즈 생성 및 저장
     */
    @Transactional
    public QuizDetailResponse createOXQuiz(Long sessionId, Long userId, QuizWordRequest request) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. OpenAI를 통한 OX 퀴즈 데이터 생성
        OpenAIQuizDataResponse aiData = openAIService.generateOXQuiz(request.getWord(), request.getMeaning());
        if (aiData == null) throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);

        // 2. 결과 엔티티 빌드 (AI 응답 단어가 누락된 경우 요청 단어 사용)
        QuizResult result = QuizResult.builder()
                .quizSession(session)
                .user(user)
                .word(aiData.getWord() != null ? aiData.getWord() : request.getWord())
                .quizType(QuizType.OX)
                .content(aiData.getContent())       // 영어 예문
                .translation(aiData.getTranslation()) // 한글 해석
                .question(aiData.getQuestion())     // 캐릭터 가이드 질문
                .correctAnswer(aiData.getAnswer())
                .explanation(aiData.getExplanation()) // 친절한 뉘앙스 해설
                .videoTimestamp(request.getVideoTimeStamp())
                .build();

        QuizResult saved = quizResultRepository.save(result);

        // 3. 프론트엔드용 DTO 매핑
        return mapToResponse(saved, null);
    }

    /**
     * 빈칸 채우기 퀴즈 생성 및 저장 (4지선다형)
     */
    @Transactional
    public QuizDetailResponse createBlankQuiz(Long sessionId, Long userId, QuizWordRequest request) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. OpenAI를 통한 빈칸 퀴즈 데이터 생성 (options 포함)
        OpenAIQuizDataResponse aiData = openAIService.generateBlankQuiz(request.getWord(), request.getMeaning());
        if (aiData == null) throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);

        // 2. 결과 엔티티 빌드
        QuizResult result = QuizResult.builder()
                .quizSession(session)
                .user(user)
                .word(aiData.getWord() != null ? aiData.getWord() : request.getWord())
                .quizType(QuizType.BLANK)
                .content(aiData.getContent())
                .translation(aiData.getTranslation())
                .question(aiData.getQuestion())
                .correctAnswer(aiData.getAnswer())
                .explanation(aiData.getExplanation())
                .videoTimestamp(request.getVideoTimeStamp())
                .build();

        QuizResult saved = quizResultRepository.save(result);

        // 3. 프론트엔드용 DTO 매핑 (options 리스트 포함)
        return mapToResponse(saved, aiData.getOptions());
    }

    /**
     * 매칭 퀴즈 세트 생성 및 저장
     */
    @Transactional
    public List<QuizDetailResponse> createMatchingQuiz(Long sessionId, Long userId, List<QuizWordRequest> requests) {
        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 1. OpenAI 요청용 단어 리스트 구성
        List<Map<String, String>> wordList = requests.stream()
                .map(r -> Map.of("word", r.getWord(), "meaning", r.getMeaning()))
                .collect(Collectors.toList());

        // 2. OpenAI를 통한 매칭 퀴즈 세트 생성
        List<OpenAIQuizDataResponse> aiDataList = openAIService.generateMatchingQuiz(wordList);
        if (aiDataList == null || aiDataList.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 3. 인덱스를 활용하여 원본 요청 데이터와 매핑하며 저장
        return IntStream.range(0, aiDataList.size()).mapToObj(i -> {
            OpenAIQuizDataResponse data = aiDataList.get(i);
            String finalWord = (data.getWord() != null && !data.getWord().isBlank())
                    ? data.getWord()
                    : requests.get(i).getWord();

            QuizResult result = QuizResult.builder()
                    .quizSession(session)
                    .user(user)
                    .word(finalWord)
                    .quizType(QuizType.MATCHING)
                    .question(data.getQuestion())      // 매칭 퀴즈는 question이 곧 단어
                    .correctAnswer(data.getAnswer())   // correctAnswer가 한글 뜻
                    .explanation(data.getExplanation()) // 암기 팁 등
                    .videoTimestamp(requests.get(i).getVideoTimeStamp())
                    .build();

            QuizResult saved = quizResultRepository.save(result);
            return mapToResponse(saved, null);
        }).collect(Collectors.toList());
    }

    /**
     * 🔥 엔티티 -> 응답 DTO 변환 유틸리티
     */
    private QuizDetailResponse mapToResponse(QuizResult result, List<String> options) {
        return QuizDetailResponse.builder()
                .quizId(result.getId())
                .quizType(result.getQuizType())
                .content(result.getContent())
                .translation(result.getTranslation())
                .question(result.getQuestion())
                .options(options)
                .videoTimeStamp(result.getVideoTimestamp())
                .build();
    }
}