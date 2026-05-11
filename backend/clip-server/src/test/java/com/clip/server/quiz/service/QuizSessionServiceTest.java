package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizGenerateRequest;
import com.clip.server.quiz.dto.response.QuizCompleteResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizGenerateResponse;
import com.clip.server.quiz.entity.*;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.quiz.repository.QuizSessionWordRepository;
import com.clip.server.subtitle.repository.SubtitleRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.badge.BadgeType;
import com.clip.server.user.entity.badge.UserBadge;
import com.clip.server.user.repository.LearningHistoryRepository;
import com.clip.server.user.repository.UserBadgeRepository;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoKeyWordRepository;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.word.entity.WordType;
import com.clip.server.word.repository.CollectedWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizSessionServiceTest {

    @InjectMocks
    private QuizSessionService quizSessionService;

    @Mock
    private QuizService quizService;
    @Mock
    private OpenAIService openAIService;
    @Mock
    private QuizSessionRepository quizSessionRepository;
    @Mock
    private QuizSessionWordRepository sessionWordRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private CollectedWordRepository collectedWordRepository;
    @Mock
    private VideoKeyWordRepository videoKeyWordRepository;
    @Mock
    private LearningHistoryRepository learningHistoryRepository;
    @Mock
    private SubtitleRepository subtitleRepository;

    @Mock
    private QuizResultRepository quizResultRepository;
    @Mock
    private UserBadgeRepository userBadgeRepository;
    @Mock
    private QuizFeedbackGenerator quizFeedbackGenerator;

    @Mock
    private QuizTxService quizTxService;
    @Mock
    private QuizFallbackService quizFallbackService;

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
                .duration(600) // 10분 영상
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
    @DisplayName("섹션 퀴즈 생성 - 유저가 수집한 단어가 있는 경우 퀴즈를 생성한다")
    void generateSectionQuiz_WithUserWords() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(videoRepository.findById(anyString())).willReturn(Optional.of(video));

        given(quizTxService.getOrCreateSession(any(), any(), any())).willReturn(session);

        // 💡 lenient() 적용: 로직에 따라 호출되지 않을 수 있는 스터빙 허용
        lenient().when(subtitleRepository.findByVideoAndStartTimeBetween(any(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        QuizSessionWord word = QuizSessionWord.builder()
                .word("implement")
                .wordType(WordType.COLLECT)
                .timestamp(150.0)
                .build();
        given(sessionWordRepository.findWordsBySection(eq(100L), anyDouble(), anyDouble()))
                .willReturn(new ArrayList<>(List.of(word)));

        lenient().when(quizService.createOXQuiz(anyLong(), anyLong(), any())).thenReturn(mock(QuizDetailResponse.class));
        lenient().when(quizService.createBlankQuiz(anyLong(), anyLong(), any())).thenReturn(mock(QuizDetailResponse.class));

        // when
        QuizGenerateResponse response = quizSessionService.generateSectionQuiz(1L, request);

        // then
        assertThat(response.getSessionId()).isEqualTo(100L);
        verify(quizTxService).getOrCreateSession(any(), any(), any());
    }

    @Test
    @DisplayName("최종 매칭 퀴즈 생성 - AI가 성공하면 5개의 매칭 퀴즈를 반환한다")
    void generateMatchingQuiz_Success() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(videoRepository.findById(anyString())).willReturn(Optional.of(video));
        given(quizTxService.getOrCreateSession(any(), any(), any())).willReturn(session);

        lenient().when(subtitleRepository.findAllByVideo(any())).thenReturn(List.of());

        List<QuizSessionWord> words = new ArrayList<>();
        for(int i=0; i<5; i++) {
            words.add(QuizSessionWord.builder().word("Word"+i).wordType(WordType.COLLECT).build());
        }
        given(sessionWordRepository.findAllByQuizSession(any())).willReturn(words);
        given(quizService.createMatchingQuiz(anyLong(), anyLong(), anyList())).willReturn(new ArrayList<>(List.of(mock(QuizDetailResponse.class))));

        // when
        quizSessionService.generateMatchingQuiz(1L, request);

        // then
        verify(quizService).createMatchingQuiz(eq(100L), eq(1L), anyList());
        verify(quizFallbackService, never()).createLocalMatchingQuizNewTx(any(), any(), any());
    }

    @Test
    @DisplayName("최종 매칭 퀴즈 생성 - AI 실패 시 폴백 서비스가 호출된다")
    void generateMatchingQuiz_AIFail_Fallback() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(videoRepository.findById(anyString())).willReturn(Optional.of(video));
        given(quizTxService.getOrCreateSession(any(), any(), any())).willReturn(session);

        lenient().when(subtitleRepository.findAllByVideo(any())).thenReturn(List.of());
        given(sessionWordRepository.findAllByQuizSession(any())).willReturn(new ArrayList<>());

        // AI 추천 단어 모킹
        List<Map<String, String>> aiRecs = new ArrayList<>();
        for(int i=0; i<5; i++) aiRecs.add(Map.of("word", "W"+i, "meaning", "M"+i));
        lenient().when(openAIService.recommendImportantWords(anyString(), anyInt())).thenReturn(aiRecs);

        given(quizService.createMatchingQuiz(anyLong(), anyLong(), anyList()))
                .willThrow(new RuntimeException("AI Error"));

        // when
        quizSessionService.generateMatchingQuiz(1L, request);

        // then
        verify(quizFallbackService).createLocalMatchingQuizNewTx(eq(100L), eq(1L), anyList());
    }

    @Test
    @DisplayName("퀴즈 세션 완료 시 경험치가 정산되고 브론즈 배지가 부여된다")
    void completeQuizSession_Success_BronzeBadge() {
        // given
        Long userId = 1L;
        Long sessionId = 100L;

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(quizSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(learningHistoryRepository.findByUserAndVideo(any(), any())).willReturn(Optional.empty());
        given(learningHistoryRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));
        given(collectedWordRepository.countByUserIdAndVideoVideoIdAndWordType(any(), any(), any())).willReturn(5L);

        QuizResult r1 = QuizResult.builder().word("apple").correctAnswer("apple").quizType(QuizType.MATCHING).build();
        r1.submitAnswer("apple", true, 100);
        given(quizResultRepository.findByQuizSession(session)).willReturn(List.of(r1));
        given(userBadgeRepository.existsByUserAndVideoAndBadgeType(any(), any(), eq(BadgeType.BRONZE))).willReturn(false);

        given(quizFeedbackGenerator.generateFeedback(anyDouble(), anyString(), any()))
                .willReturn("테스트 피드백입니다.");

        // when
        QuizCompleteResponse response = quizSessionService.completeQuizSession(userId, sessionId);

        // then
        assertThat(response.getFeedback()).isEqualTo("테스트 피드백입니다.");
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("존재하지 않는 세션 ID로 완료 요청 시 예외가 발생한다")
    void completeQuizSession_Fail_SessionNotFound() {
        // given
        given(userRepository.findById(any())).willReturn(Optional.of(user));
        given(quizSessionRepository.findById(any())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizSessionService.completeQuizSession(1L, 999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_NOT_FOUND);
    }
}