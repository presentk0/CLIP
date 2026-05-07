package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizGenerateRequest;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.QuizCompleteResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizGenerateResponse;
import com.clip.server.quiz.entity.*;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.quiz.repository.QuizSessionWordRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.badge.BadgeType;
import com.clip.server.user.entity.badge.UserBadge;
import com.clip.server.user.entity.learning.LearningHistory;
import com.clip.server.user.repository.LearningHistoryRepository;
import com.clip.server.user.repository.UserBadgeRepository;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.entity.VideoKeyWord;
import com.clip.server.video.repository.VideoKeyWordRepository;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.word.entity.WordType;
import com.clip.server.word.repository.CollectedWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private QuizResultRepository quizResultRepository;
    @Mock
    private UserBadgeRepository userBadgeRepository;
    @Mock
    private QuizFeedbackGenerator quizFeedbackGenerator;

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
                .duration(600) // 10분 영상 -> 섹션 1개
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
    @DisplayName("섹션 퀴즈 생성 - 유저가 수집한 단어가 있는 경우 빈칸 퀴즈를 포함한다")
    void generateSectionQuiz_WithUserWords() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(videoRepository.findById(anyString())).willReturn(Optional.of(video));
        given(quizSessionRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(session));

        QuizSessionWord word = QuizSessionWord.builder()
                .word("implement")
                .wordType(WordType.COLLECT)
                .timestamp(150.0)
                .build();
        given(sessionWordRepository.findWordsBySection(eq(100L), anyDouble(), anyDouble()))
                .willReturn(new ArrayList<>(List.of(word)));

        given(quizService.createOXQuiz(anyLong(), anyLong(), any())).willReturn(mock(QuizDetailResponse.class));

        // when
        QuizGenerateResponse response = quizSessionService.generateSectionQuiz(1L, request);

        // then
        assertThat(response.getSessionId()).isEqualTo(100L);
        verify(quizService).createOXQuiz(eq(100L), eq(1L), any(QuizWordRequest.class));
    }

    @Test
    @DisplayName("최종 매칭 퀴즈 생성 - 단어가 5개 미만이면 AI 추천 단어를 보충한다")
    void generateMatchingQuiz_WithAIRecommendation() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(videoRepository.findById(anyString())).willReturn(Optional.of(video));
        given(quizSessionRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(session));

        List<QuizSessionWord> words = new ArrayList<>(List.of(
                QuizSessionWord.builder().word("A").wordType(WordType.COLLECT).build(),
                QuizSessionWord.builder().word("B").wordType(WordType.COLLECT).build(),
                QuizSessionWord.builder().word("C").wordType(WordType.COLLECT).build()
        ));
        given(sessionWordRepository.findAllByQuizSession(any())).willReturn(words);

        List<Map<String, String>> aiRecs = List.of(
                Map.of("word", "D", "meaning", "뜻D"),
                Map.of("word", "E", "meaning", "뜻E")
        );
        given(openAIService.recommendImportantWords(anyString(), eq(2))).willReturn(aiRecs);

        given(quizService.createMatchingQuiz(anyLong(), anyLong(), anyList())).willReturn(List.of());

        // when
        quizSessionService.generateMatchingQuiz(1L, request);

        // then
        verify(openAIService).recommendImportantWords(anyString(), eq(2));
        verify(quizService).createMatchingQuiz(eq(100L), eq(1L), argThat(list -> list.size() == 5));
    }

    @Test
    @DisplayName("새로운 세션 생성 시 단어 스냅샷(동기화)이 수행된다")
    void getOrCreateSession_SyncWords() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 1, SessionType.NORMAL);
        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(videoRepository.findById(anyString())).willReturn(Optional.of(video));

        given(quizSessionRepository.findByUserAndVideo(any(), any())).willReturn(Optional.empty());
        given(quizSessionRepository.save(any())).willReturn(session);

        given(collectedWordRepository.findAllByUserAndVideo(any(), any())).willReturn(List.of());
        given(videoKeyWordRepository.findAllByVideo(any())).willReturn(List.of(
                VideoKeyWord.builder().word("system").build()
        ));

        // when
        quizSessionService.generateSectionQuiz(1L, request);

        // then
        verify(quizSessionRepository).save(any());
        verify(sessionWordRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("매칭 퀴즈 생성 - 단어 순서가 섞여있어도 우선순위(COLLECT > POPUP > SYSTEM)대로 정렬된다")
    void generateMatchingQuiz_PrioritySorting() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 0, SessionType.NORMAL);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(videoRepository.findById("v123")).willReturn(Optional.of(video));
        given(quizSessionRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(session));

        List<QuizSessionWord> mixedWords = new ArrayList<>(List.of(
                QuizSessionWord.builder().word("System1").wordType(WordType.SYSTEM).build(),
                QuizSessionWord.builder().word("Collect1").wordType(WordType.COLLECT).build(),
                QuizSessionWord.builder().word("Popup1").wordType(WordType.POPUP).build(),
                QuizSessionWord.builder().word("Collect2").wordType(WordType.COLLECT).build(),
                QuizSessionWord.builder().word("Popup2").wordType(WordType.POPUP).build()
        ));
        given(sessionWordRepository.findAllByQuizSession(any())).willReturn(mixedWords);
        given(quizService.createMatchingQuiz(anyLong(), anyLong(), anyList())).willReturn(new ArrayList<>());

        // when
        quizSessionService.generateMatchingQuiz(1L, request);

        // then
        ArgumentCaptor<List<QuizWordRequest>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(quizService).createMatchingQuiz(eq(100L), eq(1L), listCaptor.capture());

        List<QuizWordRequest> capturedList = listCaptor.getValue();
        List<String> words = capturedList.stream().map(QuizWordRequest::getWord).toList();

        assertThat(words).hasSize(5);
        assertThat(words.get(0)).contains("Collect");
        assertThat(words.get(4)).isEqualTo("System1");
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

    @Test
    @DisplayName("영상을 3번째 완주하면 골드 배지와 1000 EXP 보너스를 받는다")
    void completeQuizSession_Success_GoldBadge() {
        // given
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(quizSessionRepository.findById(session.getId())).willReturn(Optional.of(session));

        LearningHistory history = LearningHistory.builder().user(user).video(video).build();
        ReflectionTestUtils.setField(history, "completionCount", 2);
        given(learningHistoryRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(history));
        given(userBadgeRepository.existsByUserAndVideoAndBadgeType(any(), any(), eq(BadgeType.GOLD))).willReturn(false);
        given(quizResultRepository.findByQuizSession(any())).willReturn(List.of());

        given(quizFeedbackGenerator.generateFeedback(anyDouble(), anyString(), any()))
                .willReturn("골드 배지 축하 피드백!");

        // when
        QuizCompleteResponse response = quizSessionService.completeQuizSession(user.getId(), session.getId());

        // then
        assertThat(response.getFeedback()).isEqualTo("골드 배지 축하 피드백!");
        assertThat(response.getEarnedExp()).isEqualTo(1200);
        assertThat(response.getNewBadge().getBadgeType()).isEqualTo(BadgeType.GOLD);
    }
}