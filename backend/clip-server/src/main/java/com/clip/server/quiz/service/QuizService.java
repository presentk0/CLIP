package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.GeminiQuizData;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final GeminiService geminiService;
    private final QuizResultRepository quizResultRepository;
    private final UserRepository userRepository;
    private final QuizSessionRepository quizSessionRepository;

    // OX 퀴즈 생성 요청 및 저장 메서드
    @Transactional
    public QuizDetailResponse createOXQuiz(Long sessionId, Long userId, QuizWordRequest quizWordRequest) {

        // id를 통해 퀴즈 세션, 사용자 엔티티 조회(없는 경우 예외 발생)
        QuizSession quizSession = quizSessionRepository.findById(sessionId).orElseThrow(()-> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        // OX 퀴즈 생성 요청
        GeminiQuizData aiData = geminiService.generateOXQuiz(quizWordRequest.getWord(), quizWordRequest.getMeaning());

        QuizResult quizResult = QuizResult.builder()
                .quizSession(quizSession)
                .user(user)
                .word(aiData.getWord())
                .quizType(QuizType.OX)
                .question(aiData.getQuestion())
                .correctAnswer(aiData.getAnswer())
                .explanation(aiData.getExplanation())
                .videoTimestamp(quizWordRequest.getVideoTimeStamp())
                .build();

        QuizResult saved = quizResultRepository.save(quizResult);

        return mapToQuizDetailResponse(saved.getId(), saved.getQuizType(), saved.getQuestion(), saved.getVideoTimestamp());
    }

    // 빈칸 퀴즈 생성 요청 및 저장 메서드
    @Transactional
    public QuizDetailResponse createBlankQuiz(Long sessionId, Long userId, QuizWordRequest quizWordRequest) {
        
        // id를 통해 퀴즈 세션, 사용자 엔티티 조회(없는 경우 예외 발생)
        QuizSession quizSession = quizSessionRepository.findById(sessionId).orElseThrow(()-> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 빈칸 퀴즈 생성 요청
        GeminiQuizData aiData = geminiService.generateBlankQuiz(quizWordRequest.getWord(), quizWordRequest.getMeaning());

        QuizResult quizResult = QuizResult.builder()
                .quizSession(quizSession)
                .user(user)
                .word(aiData.getWord())
                .quizType(QuizType.BLANK)
                .question(aiData.getQuestion())
                .correctAnswer(aiData.getAnswer())
                .explanation(aiData.getExplanation())
                .videoTimestamp(quizWordRequest.getVideoTimeStamp())
                .build();
        
        QuizResult saved = quizResultRepository.save(quizResult);
        
        return mapToQuizDetailResponse(saved.getId(), saved.getQuizType(), saved.getQuestion(), saved.getVideoTimestamp());
    }

    // 매칭 퀴즈 생성 요청 및 저장 메서드
    @Transactional
    public List<QuizDetailResponse> createMatchingQuiz(Long sessionId, Long userId, List<QuizWordRequest> quizWordRequests) {

        // id를 통해 퀴즈 세션, 사용자 엔티티 조회(없는 경우 예외 발생)
        QuizSession quizSession = quizSessionRepository.findById(sessionId).orElseThrow(()-> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 단어별 타임스탬프 Map 관리
        Map<String, String> timestampMap = quizWordRequests.stream()
                .collect(Collectors.toMap(QuizWordRequest::getWord, QuizWordRequest::getVideoTimeStamp));

        // Dto 리스트-> GeminiService가 처리할 수 있는 방식으로 변환
        List<Map<String, String>> wordList = quizWordRequests.stream()
                .map(req->Map.of("word", req.getWord(), "meaning", req.getMeaning()))
                .collect(Collectors.toList());

        // 매칭 퀴즈 생성 요청
        List<GeminiQuizData> aiDataList = geminiService.generateMatchingQuiz(wordList);

        return aiDataList.stream().map(data-> {

            String videoTimestamp = timestampMap.getOrDefault(data.getWord(), "");

            QuizResult quizResult = QuizResult.builder()
                    .quizSession(quizSession)
                    .user(user)
                    .word(data.getWord())
                    .quizType(QuizType.MATCHING)
                    .question(data.getQuestion())
                    .correctAnswer(data.getAnswer())
                    .explanation(data.getExplanation())
                    .videoTimestamp(videoTimestamp)
                    .build();

            QuizResult saved = quizResultRepository.save(quizResult);

            return mapToQuizDetailResponse(saved.getId(),saved.getQuizType(), saved.getQuestion(), saved.getVideoTimestamp());
        }).collect(Collectors.toList());

    }

    // QuizDetailResponse 응답 변환 메서드
    private QuizDetailResponse mapToQuizDetailResponse(Long quizId, QuizType quizType, String question, String videoStamp) {
        return QuizDetailResponse.builder()
                .quizId(quizId)
                .quizType(quizType)
                .question(question)
                .videoTimeStamp(videoStamp)
                .build();
    }
}
