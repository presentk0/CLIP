package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.ai.OpenAIService;
import com.clip.server.quiz.ai.QuizFallbackService;
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
import com.clip.server.user.service.ExpLogService;
import com.clip.server.video.entity.Video;
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
class QuizProcessorTest {

    @Mock
    private QuizSessionRepository quizSessionRepository;
    @Mock
    private QuizSessionWordRepository quizSessionWordRepository;
    @Mock
    private QuizResultRepository quizResultRepository;
    @Mock
    private SubtitleRepository subtitleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CollectedWordRepository collectedWordRepository;
    @Mock
    private LearningHistoryRepository learningHistoryRepository;
    @Mock
    private UserBadgeRepository userBadgeRepository;
    @Mock
    private QuizService quizService;
    @Mock
    private OpenAIService openAIService;
    @Mock
    private QuizFeedbackGenerator quizFeedbackGenerator;
    @Mock
    private QuizFallbackService quizFallbackService;
    @Mock
    private ExpLogService expLogService;

    @InjectMocks
    private QuizProcessor quizProcessor;

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

    // ==================== 섹션 퀴즈 생성 테스트 ====================

    @Test
    @DisplayName("섹션 퀴즈 생성 - 유저가 수집한 단어가 있으면 OX 퀴즈가 먼저 생성된다")
    void generateSectionQuizInternal_withUserWords() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);

        given(quizSessionRepository.findById(100L)).willReturn(Optional.of(session));

        QuizSessionWord word = QuizSessionWord.builder()
                .word("implement")
                .wordType(WordType.COLLECT)
                .timestamp(150.0)
                .build();
        given(quizSessionWordRepository.findWordsBySection(eq(100L), anyDouble(), anyDouble()))
                .willReturn(new ArrayList<>(List.of(word)));

        lenient().when(subtitleRepository.findByVideoAndStartTimeBetween(any(), anyDouble(), anyDouble()))
                .thenReturn(List.of());

        QuizDetailResponse mockQuiz = mock(QuizDetailResponse.class);
        lenient().when(quizService.createOXQuiz(anyLong(), anyLong(), any())).thenReturn(mockQuiz);
        lenient().when(quizService.createBlankQuiz(anyLong(), anyLong(), any())).thenReturn(mockQuiz);

        // when
        QuizGenerateResponse response = quizProcessor.generateSectionQuizInternal(1L, 100L, request);

        // then
        assertThat(response.getSessionId()).isEqualTo(100L);
        verify(quizService, atLeastOnce()).createOXQuiz(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("섹션 퀴즈 생성 - 단어가 부족하면 AI가 보충한다")
    void generateSectionQuizInternal_aiSupplement() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);

        given(quizSessionRepository.findById(100L)).willReturn(Optional.of(session));
        given(quizSessionWordRepository.findWordsBySection(eq(100L), anyDouble(), anyDouble()))
                .willReturn(new ArrayList<>());
        given(subtitleRepository.findByVideoAndStartTimeBetween(any(), anyDouble(), anyDouble()))
                .willReturn(List.of());

        List<Map<String, String>> aiWords = List.of(
                Map.of("word", "apple", "meaning", "사과"),
                Map.of("word", "banana", "meaning", "바나나"),
                Map.of("word", "cherry", "meaning", "체리")
        );
        given(openAIService.recommendImportantWords(anyString(), anyInt())).willReturn(aiWords);

        QuizDetailResponse mockQuiz = mock(QuizDetailResponse.class);
        lenient().when(quizService.createOXQuiz(anyLong(), anyLong(), any())).thenReturn(mockQuiz);
        lenient().when(quizService.createBlankQuiz(anyLong(), anyLong(), any())).thenReturn(mockQuiz);

        // when
        quizProcessor.generateSectionQuizInternal(1L, 100L, request);

        // then
        verify(openAIService).recommendImportantWords(anyString(), anyInt());
    }

    @Test
    @DisplayName("섹션 퀴즈 생성 - 세션이 없으면 예외가 발생한다")
    void generateSectionQuizInternal_sessionNotFound() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);
        given(quizSessionRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizProcessor.generateSectionQuizInternal(1L, 100L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_NOT_FOUND);
    }

    // ==================== 매칭 퀴즈 생성 테스트 ====================

    @Test
    @DisplayName("매칭 퀴즈 생성 - AI가 성공하면 매칭 퀴즈를 반환한다")
    void generateMatchingQuizInternal_success() {
        // given
        given(quizSessionRepository.findById(100L)).willReturn(Optional.of(session));

        List<QuizSessionWord> words = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            words.add(QuizSessionWord.builder()
                    .word("Word" + i)
                    .wordType(WordType.COLLECT)
                    .translation("뜻" + i)
                    .build());
        }
        given(quizSessionWordRepository.findAllByQuizSession(any())).willReturn(words);

        lenient().when(subtitleRepository.findAllByVideo(any())).thenReturn(List.of());

        List<QuizDetailResponse> mockQuizzes = List.of(mock(QuizDetailResponse.class));
        given(quizService.createMatchingQuiz(anyLong(), anyLong(), anyList())).willReturn(mockQuizzes);

        // when
        QuizGenerateResponse response = quizProcessor.generateMatchingQuizInternal(1L, 100L, video);

        // then
        assertThat(response.getSessionId()).isEqualTo(100L);
        verify(quizService).createMatchingQuiz(eq(100L), eq(1L), anyList());
        verify(quizFallbackService, never()).createLocalMatchingQuizNewTx(any(), any(), any());
    }

    @Test
    @DisplayName("매칭 퀴즈 생성 - AI 실패 시 폴백 서비스가 호출된다")
    void generateMatchingQuizInternal_fallback() {
        // given
        given(quizSessionRepository.findById(100L)).willReturn(Optional.of(session));
        given(quizSessionWordRepository.findAllByQuizSession(any())).willReturn(new ArrayList<>());

        lenient().when(subtitleRepository.findAllByVideo(any())).thenReturn(List.of());

        List<Map<String, String>> aiWords = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            aiWords.add(Map.of("word", "W" + i, "meaning", "M" + i));
        }
        lenient().when(openAIService.recommendImportantWords(anyString(), anyInt())).thenReturn(aiWords);

        given(quizService.createMatchingQuiz(anyLong(), anyLong(), anyList()))
                .willThrow(new RuntimeException("AI Error"));

        List<QuizDetailResponse> fallbackQuizzes = List.of(mock(QuizDetailResponse.class));
        given(quizFallbackService.createLocalMatchingQuizNewTx(anyLong(), anyLong(), anyList()))
                .willReturn(fallbackQuizzes);

        // when
        quizProcessor.generateMatchingQuizInternal(1L, 100L, video);

        // then
        verify(quizFallbackService).createLocalMatchingQuizNewTx(eq(100L), eq(1L), anyList());
    }

    // ==================== 퀴즈 세션 완료 테스트 ====================

    @Test
    @DisplayName("퀴즈 세션 완료 - 경험치가 정산되고 브론즈 배지가 부여된다")
    void completeQuizSession_bronzeBadge() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(quizSessionRepository.findById(100L)).willReturn(Optional.of(session));
        given(learningHistoryRepository.findByUserAndVideo(any(), any())).willReturn(Optional.empty());
        given(learningHistoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(collectedWordRepository.countByUserIdAndVideoVideoIdAndWordType(any(), any(), any())).willReturn(5L);

        QuizResult result = QuizResult.builder()
                .word("apple")
                .correctAnswer("apple")
                .quizType(QuizType.MATCHING)
                .build();
        result.submitAnswer("apple", true, 100);
        given(quizResultRepository.findByQuizSession(session)).willReturn(List.of(result));

        given(userBadgeRepository.existsByUserAndVideoAndBadgeType(any(), any(), eq(BadgeType.BRONZE)))
                .willReturn(false);
        given(quizFeedbackGenerator.generateFeedback(anyDouble(), anyString(), any()))
                .willReturn("잘했어요!");

        // when
        QuizCompleteResponse response = quizProcessor.completeQuizSession(1L, 100L);

        // then
        assertThat(response.getFeedback()).isEqualTo("잘했어요!");
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        verify(userBadgeRepository).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("퀴즈 세션 완료 - 이미 배지가 있으면 중복 부여하지 않는다")
    void completeQuizSession_noDuplicateBadge() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(quizSessionRepository.findById(100L)).willReturn(Optional.of(session));
        given(learningHistoryRepository.findByUserAndVideo(any(), any())).willReturn(Optional.empty());
        given(learningHistoryRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        given(collectedWordRepository.countByUserIdAndVideoVideoIdAndWordType(any(), any(), any())).willReturn(5L);
        given(quizResultRepository.findByQuizSession(session)).willReturn(List.of());

        // 이미 브론즈 배지 존재
        given(userBadgeRepository.existsByUserAndVideoAndBadgeType(any(), any(), eq(BadgeType.BRONZE)))
                .willReturn(true);
        given(quizFeedbackGenerator.generateFeedback(anyDouble(), anyString(), any()))
                .willReturn("피드백");

        // when
        quizProcessor.completeQuizSession(1L, 100L);

        // then
        verify(userBadgeRepository, never()).save(any(UserBadge.class));
    }

    @Test
    @DisplayName("퀴즈 세션 완료 - 유저가 없으면 예외가 발생한다")
    void completeQuizSession_userNotFound() {
        // given
        given(userRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizProcessor.completeQuizSession(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("퀴즈 세션 완료 - 세션이 없으면 예외가 발생한다")
    void completeQuizSession_sessionNotFound() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(quizSessionRepository.findById(anyLong())).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizProcessor.completeQuizSession(1L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_NOT_FOUND);
    }
}