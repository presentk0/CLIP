package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizWordRequest;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizFallbackService {

    private final QuizResultRepository quizResultRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<QuizDetailResponse> createLocalMatchingQuiz(Long sessionId, Long userId, List<QuizWordRequest> requests) {

        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("### 매칭 퀴즈 자체 생성을 시작합니다. (단어 수: {})", requests.size());

        return requests.stream().map(req -> {
            QuizResult result = QuizResult.builder()
                    .quizSession(session)
                    .user(user)
                    .word(req.getWord())
                    .quizType(QuizType.MATCHING)
                    .question(req.getWord())       // 질문은 영어 단어
                    .correctAnswer(req.getMeaning()) // 정답은 한글 뜻
                    .content("")                   // 자체 생성 시 예문은 비워둠 (혹은 DB에서 가져옴)
                    .translation("")
                    .explanation(req.getWord() + "는 '" + req.getMeaning() + "'라는 뜻입니다.")
                    .videoTimestamp(req.getVideoTimeStamp())
                    .build();

            QuizResult saved = quizResultRepository.save(result);

            return QuizDetailResponse.builder()
                    .quizId(saved.getId())
                    .quizType(saved.getQuizType())
                    .question(saved.getQuestion())
                    .answer(saved.getCorrectAnswer())
                    .videoTimeStamp(saved.getVideoTimestamp())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<QuizDetailResponse> createLocalMatchingQuizNewTx(Long sessionId, Long userId, List<QuizWordRequest> requests) {
        return createLocalMatchingQuiz(sessionId, userId, requests);
    }

}
