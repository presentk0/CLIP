package com.clip.server.quiz.Service;

import com.clip.server.quiz.dto.request.QuizStartRequest;
import com.clip.server.quiz.dto.response.QuizStartResponse;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.repository.QuizSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSessionService {

    private final QuizSessionRepository quizSessionRepository;

//    @Transactional
//    public QuizStartResponse postQuizStart(QuizStartRequest quizStartRequest) {
//
//    }

    public QuizStartResponse mapToQuizStartResponse(QuizSession quizSession) {
        return QuizStartResponse.builder()
                .sessionId(quizSession.getId())
                .sessionType(quizSession.getSessionType())
                .videoId(quizSession.getVideo().getVideoId())
                .totalQuizCount(quizSession.getTotalQuizCount())
                .startAt(quizSession.getStartAt())
                .build();
    }
}
