package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizGenerateRequest;
import com.clip.server.quiz.dto.response.QuizCompleteResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizGenerateResponse;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.SessionType;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizSessionServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private QuizTxService quizTxService;

    @Mock
    private QuizProcessor quizProcessor;

    @InjectMocks
    private QuizSessionService quizSessionService;

    private User user;
    private Video video;
    private QuizSession session;

    @BeforeEach
    void setUp() {
        user = User.builder().name("테스터").build();
        ReflectionTestUtils.setField(user, "id", 1L);

        video = Video.builder()
                .videoId("v123")
                .title("테스트 영상")
                .duration(600)
                .build();

        session = QuizSession.builder()
                .user(user)
                .video(video)
                .sessionType(SessionType.NORMAL)
                .totalQuizCount(8)
                .build();
        ReflectionTestUtils.setField(session, "id", 100L);
    }

    @Test
    @DisplayName("섹션 퀴즈 생성 시 QuizTxService와 QuizProcessor가 순서대로 호출된다")
    void generateSectionQuiz_callsServicesInOrder() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);

        QuizGenerateResponse expectedResponse = QuizGenerateResponse.builder()
                .sessionId(100L)
                .sessionType(SessionType.NORMAL)
                .videoId("v123")
                .totalQuizCount(3)
                .quizzes(List.of())
                .build();

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(videoRepository.findById(anyString())).willReturn(Optional.of(video));
        given(quizTxService.getOrCreateSession(any(), any(), any())).willReturn(session);
        given(quizProcessor.generateSectionQuizInternal(anyLong(), anyLong(), any()))
                .willReturn(expectedResponse);

        // when
        QuizGenerateResponse response = quizSessionService.generateSectionQuiz(1L, request);

        // then
        assertThat(response.getSessionId()).isEqualTo(100L);

        // 호출 순서 검증
        var inOrder = inOrder(userRepository, videoRepository, quizTxService, quizProcessor);
        inOrder.verify(userRepository).findById(1L);
        inOrder.verify(videoRepository).findById("v123");
        inOrder.verify(quizTxService).getOrCreateSession(user, video, SessionType.NORMAL);
        inOrder.verify(quizProcessor).generateSectionQuizInternal(1L, 100L, request);
    }

    @Test
    @DisplayName("매칭 퀴즈 생성 시 QuizTxService와 QuizProcessor가 순서대로 호출된다")
    void generateMatchingQuiz_callsServicesInOrder() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);

        QuizGenerateResponse expectedResponse = QuizGenerateResponse.builder()
                .sessionId(100L)
                .sessionType(SessionType.NORMAL)
                .videoId("v123")
                .totalQuizCount(5)
                .quizzes(List.of())
                .build();

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(videoRepository.findById(anyString())).willReturn(Optional.of(video));
        given(quizTxService.getOrCreateSession(any(), any(), any())).willReturn(session);
        given(quizProcessor.generateMatchingQuizInternal(anyLong(), anyLong(), any()))
                .willReturn(expectedResponse);

        // when
        QuizGenerateResponse response = quizSessionService.generateMatchingQuiz(1L, request);

        // then
        assertThat(response.getSessionId()).isEqualTo(100L);

        var inOrder = inOrder(quizTxService, quizProcessor);
        inOrder.verify(quizTxService).getOrCreateSession(user, video, SessionType.NORMAL);
        inOrder.verify(quizProcessor).generateMatchingQuizInternal(1L, 100L, video);
    }

    @Test
    @DisplayName("퀴즈 세션 완료 시 QuizProcessor가 호출된다")
    void completeQuizSession_callsProcessor() {
        // given
        QuizCompleteResponse expectedResponse = QuizCompleteResponse.builder()
                .sessionId(100L)
                .totalQuizCount(3)
                .correctCount(2)
                .wrongCount(1)
                .accuracy(0.67)
                .earnedExp(500)
                .levelUp(false)
                .currentLevel(5)
                .completedAt(LocalDateTime.now())
                .feedback("잘했어요!")
                .build();

        given(quizProcessor.completeQuizSession(anyLong(), anyLong())).willReturn(expectedResponse);

        // when
        QuizCompleteResponse response = quizSessionService.completeQuizSession(1L, 100L);

        // then
        assertThat(response.getSessionId()).isEqualTo(100L);
        assertThat(response.getEarnedExp()).isEqualTo(500);
        verify(quizProcessor).completeQuizSession(1L, 100L);
    }

    @Test
    @DisplayName("존재하지 않는 유저로 섹션 퀴즈 생성 시 예외가 발생한다")
    void generateSectionQuiz_userNotFound() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizSessionService.generateSectionQuiz(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("존재하지 않는 영상으로 섹션 퀴즈 생성 시 예외가 발생한다")
    void generateSectionQuiz_videoNotFound() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(videoRepository.findById(anyString())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizSessionService.generateSectionQuiz(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VIDEO_NOT_FOUND);
    }
}