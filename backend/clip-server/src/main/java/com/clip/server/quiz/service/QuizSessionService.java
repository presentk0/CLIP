package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizGenerateRequest;
import com.clip.server.quiz.dto.response.QuizCompleteResponse;
import com.clip.server.quiz.dto.response.QuizGenerateResponse;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * [QuizSessionService]
 * 퀴즈 생성의 전체 흐름을 제어하는 오케스트레이터 서비스입니다.
 *
 * 💡 핵심: 이 클래스에는 @Transactional이 없습니다!
 * - QuizTxService: REQUIRES_NEW로 세션 생성 후 즉시 커밋
 * - QuizProcessor: 별도 Bean이므로 @Transactional 정상 작동
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuizSessionService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final QuizTxService quizTxService;

    // 실제 비즈니스 로직 처리 (별도 Bean)
    private final QuizProcessor quizProcessor;

    /**
     * 중간 섹션 퀴즈 생성 (OX, 빈칸)
     */
    public QuizGenerateResponse generateSectionQuiz(Long userId, QuizGenerateRequest request) {

        // 1. User, Video 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        log.info("### [Section Quiz] 시작 - userId: {}, videoId: {}", userId, video.getVideoId());

        // 2. 세션 생성 (REQUIRES_NEW → 즉시 커밋)
        QuizSession quizSession = quizTxService.getOrCreateSession(user, video, request.getSessionType());
        log.info("### [Section Quiz] 세션 확보 완료 - sessionId: {}", quizSession.getId());

        // 3. 퀴즈 생성 (별도 Bean 호출 → @Transactional 정상 작동)
        return quizProcessor.generateSectionQuizInternal(userId, quizSession.getId(), request);
    }

    /**
     * 최종 매칭 퀴즈 생성
     */
    public QuizGenerateResponse generateMatchingQuiz(Long userId, QuizGenerateRequest request) {

        // 1. User, Video 검증
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        log.info("### [Matching Quiz] 시작 - userId: {}, videoId: {}", userId, video.getVideoId());

        // 2. 세션 생성 (REQUIRES_NEW → 즉시 커밋)
        QuizSession quizSession = quizTxService.getOrCreateSession(user, video, request.getSessionType());
        log.info("### [Matching Quiz] 세션 확보 완료 - sessionId: {}", quizSession.getId());

        // 3. 퀴즈 생성 (별도 Bean 호출)
        return quizProcessor.generateMatchingQuizInternal(userId, quizSession.getId(), video);
    }

    /**
     * 퀴즈 세션 완료
     */
    public QuizCompleteResponse completeQuizSession(Long userId, Long sessionId) {
        log.info("### [Quiz Complete] 시작 - userId: {}, sessionId: {}", userId, sessionId);
        return quizProcessor.completeQuizSession(userId, sessionId);
    }
}