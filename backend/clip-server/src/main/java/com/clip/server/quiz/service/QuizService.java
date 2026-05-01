package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizSubmitRequest;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.OpenAIQuizDataResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizSubmitResponse;
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
@Transactional(readOnly = true)
public class QuizService {

    private final OpenAIService openAIService;
    private final QuizResultRepository quizResultRepository;
    private final UserRepository userRepository;
    private final QuizSessionRepository quizSessionRepository;

    private static final int CORRECT_EXP = 100;

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
                .correctFeedback(aiData.getCorrectFeedback())
                .wrongFeedback(aiData.getWrongFeedback())
                .build();

        QuizResult saved = quizResultRepository.save(result);

        // 3. 프론트엔드용 DTO 매핑
        return mapToQuizDetailResponse(saved, null);
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
                .correctFeedback(aiData.getCorrectFeedback())
                .wrongFeedback(aiData.getWrongFeedback())
                .build();

        QuizResult saved = quizResultRepository.save(result);

        // 3. 프론트엔드용 DTO 매핑 (options 리스트 포함)
        return mapToQuizDetailResponse(saved, aiData.getOptions());
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

        // 1. OpenAI 요청용 리스트 구성
        List<Map<String, String>> wordList = requests.stream()
                .map(r -> Map.of("word", r.getWord(), "meaning", r.getMeaning()))
                .collect(Collectors.toList());

        // 2. OpenAI를 통한 매칭 데이터 세트 생성
        List<OpenAIQuizDataResponse> aiDataList = openAIService.generateMatchingQuiz(wordList);
        if (aiDataList == null || aiDataList.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }

        // 3. 인덱스를 활용하여 원본 데이터와 AI 데이터를 동기화하며 저장
        return IntStream.range(0, aiDataList.size()).mapToObj(i -> {
            OpenAIQuizDataResponse data = aiDataList.get(i);

            // AI 응답에 word가 누락된 경우, 순서상 동일한 위치의 요청 단어로 보정
            String finalWord = (data.getWord() != null && !data.getWord().isBlank())
                    ? data.getWord()
                    : requests.get(i).getWord();

            QuizResult result = QuizResult.builder()
                    .quizSession(session)
                    .user(user)
                    .word(finalWord)
                    .quizType(QuizType.MATCHING)
                    .question(data.getQuestion())      // 예: "effort"
                    .content(data.getContent())       // 예: "It takes effort."
                    .translation(data.getTranslation()) // 예: "노력이 필요해."
                    .correctAnswer(data.getAnswer())   // 예: "노력"
                    .explanation(data.getExplanation()) // 예: "힘을 쓰는 이미지!"
                    .videoTimestamp(requests.get(i).getVideoTimeStamp())
                    .correctFeedback(data.getCorrectFeedback())
                    .wrongFeedback(data.getWrongFeedback())
                    .build();

            QuizResult saved = quizResultRepository.save(result);

            // 매칭 퀴즈 응답 시 뜻(answer)을 포함하여 반환해야 프론트에서 표시 가능
            return QuizDetailResponse.builder()
                    .quizId(saved.getId())
                    .quizType(saved.getQuizType())
                    .content(saved.getContent())
                    .translation(saved.getTranslation())
                    .question(saved.getQuestion())
                    .answer(saved.getCorrectAnswer()) // 뜻 데이터를 answer에 담음
                    .videoTimeStamp(saved.getVideoTimestamp())
                    .build();
        }).collect(Collectors.toList());
    }

    // 퀴즈 제출 로직
    @Transactional
    public QuizSubmitResponse submitQuiz(Long userId, QuizSubmitRequest request) {

        // 퀴즈 세션, 사용자 정보 확인
        QuizResult quizResult = quizResultRepository.findById(request.getQuizId())
                .orElseThrow(()-> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean isCorrect = checkAnswer(quizResult.getCorrectAnswer(), request.getUserAnswer());
        int earnedExp = isCorrect? CORRECT_EXP : 0; // 정답이면 exp 오답이면 0

        // 사용자 exp 업데이트
        if(isCorrect) {
            user.addExp(earnedExp);
        }

        // 사용자 답, 정답 여부, 퀴즈를 통해 얻은 exp 저장
        quizResult.submitAnswer(request.getUserAnswer(), isCorrect, earnedExp);

        return mapToQuizSubmitResponse(quizResult, user.getExp());
    }

    // 퀴즈 정답 체크
    private boolean checkAnswer(String correctAnswer, String userAnswer) {
        if (correctAnswer == null || userAnswer == null) return false;
        return correctAnswer.equals(userAnswer);
    }

    /**
     * 🔥 엔티티 -> 응답 DTO 변환 유틸리티
     */
    private QuizDetailResponse mapToQuizDetailResponse(QuizResult result, List<String> options) {
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

    private QuizSubmitResponse mapToQuizSubmitResponse(QuizResult quizResult, Integer currentExp) {
        return QuizSubmitResponse.builder()
                .isCorrect(quizResult.getIsCorrect())
                .correctAnswer(quizResult.getCorrectAnswer())
                .earnedExp(quizResult.getEarnedExp())
                .feedback(quizResult.getIsCorrect()? quizResult.getCorrectFeedback() : quizResult.getWrongFeedback()) // 정오답 여부에 맞는 피드백 제공
                .explanation(quizResult.getExplanation())
                .currentExp(currentExp)
                .build();
    }
}