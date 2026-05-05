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
    private UserBadgeRepository userBadgeRepository;
    @Mock
    private QuizResultRepository quizResultRepository;

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
                .duration(600) // 10분 영상 -> 섹션 1개 (전략표 기준 600초 미만은 섹션 1개)
                .build();

        session = QuizSession.builder()
                .user(user)
                .video(video)
                .sessionType(SessionType.NORMAL)
                .totalQuizCount(8) // 3(섹션) + 5(매칭)
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

        // 해당 섹션에 COLLECT 타입 단어 1개 존재 시뮬레이션
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
        // 수집 단어가 1개뿐이므로 OX 퀴즈가 먼저 생성됨 (로직상 candidates.get(0) 사용)
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

        // 세션에 단어가 3개만 저장되어 있는 상황
        List<QuizSessionWord> words = new ArrayList<>(List.of(
                QuizSessionWord.builder().word("A").wordType(WordType.COLLECT).build(),
                QuizSessionWord.builder().word("B").wordType(WordType.COLLECT).build(),
                QuizSessionWord.builder().word("C").wordType(WordType.COLLECT).build()
        ));
        given(sessionWordRepository.findAllByQuizSession(any())).willReturn(words);

        // AI가 단어 2개를 더 추천해줌
        List<Map<String, String>> aiRecs = List.of(
                Map.of("word", "D", "meaning", "뜻D"),
                Map.of("word", "E", "meaning", "뜻E")
        );
        given(openAIService.recommendImportantWords(anyString(), eq(2))).willReturn(aiRecs);

        given(quizService.createMatchingQuiz(anyLong(), anyLong(), anyList())).willReturn(List.of());

        // when
        quizSessionService.generateMatchingQuiz(1L, request);

        // then
        // 최종적으로 5개의 단어가 QuizService로 전달되었는지 확인
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

        // 세션이 아직 없는 상태
        given(quizSessionRepository.findByUserAndVideo(any(), any())).willReturn(Optional.empty());
        given(quizSessionRepository.save(any())).willReturn(session);

        // 동기화할 소스 데이터들
        given(collectedWordRepository.findAllByUserAndVideo(any(), any())).willReturn(List.of());
        given(videoKeyWordRepository.findAllByVideo(any())).willReturn(List.of(
                VideoKeyWord.builder().word("system").build()
        ));

        // when
        quizSessionService.generateSectionQuiz(1L, request);

        // then
        // 세션이 저장되고 단어 스냅샷이 실행되었는지 확인
        verify(quizSessionRepository).save(any());
        verify(  sessionWordRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("매칭 퀴즈 생성 - 단어 순서가 섞여있어도 우선순위(COLLECT > POPUP > SYSTEM)대로 정렬된다")
    void generateMatchingQuiz_PrioritySorting() {
        // given
        QuizGenerateRequest request = new QuizGenerateRequest("v123", 0, SessionType.NORMAL);

        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(videoRepository.findById("v123")).willReturn(Optional.of(video));
        given(quizSessionRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(session));

        // 뒤섞인 데이터 준비
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

        // 디버깅을 위해 결과 리스트가 기대한 등급 순서인지 더 명확하게 검증
        assertThat(words).hasSize(5);

        // 1. COLLECT 등급 (우선순위 1) - 인덱스 0, 1
        assertThat(words.get(0)).as("전체 결과: " + words).contains("Collect");
        assertThat(words.get(1)).as("전체 결과: " + words).contains("Collect");

        // 2. POPUP 등급 (우선순위 2) - 인덱스 2, 3
        assertThat(words.get(2)).as("전체 결과: " + words).contains("Popup");
        assertThat(words.get(3)).as("전체 결과: " + words).contains("Popup");

        // 3. SYSTEM 등급 (우선순위 3) - 인덱스 4
        assertThat(words.get(4)).as("전체 결과: " + words).isEqualTo("System1");
    }

    @Test
    @DisplayName("퀴즈 세션 완료 시 경험치가 정산되고 브론즈 배지가 부여된다")
    void completeQuizSession_Success_BronzeBadge() {
        // given
        Long userId = 1L;
        Long sessionId = 100L;

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(quizSessionRepository.findById(sessionId)).willReturn(Optional.of(session));

        // 처음 완주하는 상황 가정 (LearningHistory가 새로 생성됨)
        given(learningHistoryRepository.findByUserAndVideo(any(), any())).willReturn(Optional.empty());
        given(learningHistoryRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // 단어 5개 수집 상태
        given(collectedWordRepository.countByUserIdAndVideoVideoIdAndWordType(any(), any(), any()))
                .willReturn(5L);

        // 퀴즈 결과: 2문제 중 2문제 정답 (각 100 EXP 가정)
        QuizResult r1 = QuizResult.builder()
                .word("apple")
                .correctAnswer("apple")
                .quizType(QuizType.MATCHING)
                .build();
        r1.submitAnswer("apple", true, 100);

        QuizResult r2 = QuizResult.builder()
                .word("banana")
                .correctAnswer("banana")
                .quizType(QuizType.MATCHING)
                .build();
        r2.submitAnswer("banana", true, 100);

        given(quizResultRepository.findByQuizSession(session)).willReturn(List.of(r1, r2));

        // 배지 중복 체크: 아직 없음
        given(userBadgeRepository.existsByUserAndVideoAndBadgeType(any(), any(), eq(BadgeType.BRONZE)))
                .willReturn(false);

        // when
        QuizCompleteResponse response = quizSessionService.completeQuizSession(userId, sessionId);

        // then
        // 1. 경험치 검증: 퀴즈(200) + 영상보상(기본값) + 브론즈보너스(300)
        // VIDEO_COMPENSATION이 200이라 가정하면 총 700 EXP
        assertThat(response.getEarnedExp()).isGreaterThan(500);
        assertThat(user.getExp()).isEqualTo(response.getEarnedExp());

        // 2. 상태 변경 검증
        assertThat(session.getStatus()).isEqualTo(SessionStatus.COMPLETED);
        assertThat(response.getNewBadge().getBadgeType()).isEqualTo(BadgeType.BRONZE);

        // 3. 호출 횟수 검증
        verify(userBadgeRepository, times(1)).save(any(UserBadge.class));
        verify(learningHistoryRepository, times(1)).save(any(LearningHistory.class));
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
        // LearningHistory의 completionCount가 2인 상태로 설정 (이번이 3번째)
        LearningHistory history = LearningHistory.builder().user(user).video(video).build();
        ReflectionTestUtils.setField(history, "completionCount", 2);
        given(learningHistoryRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(history));

        // 배지 중복 체크: 골드 배지는 아직 없음
        given(userBadgeRepository.existsByUserAndVideoAndBadgeType(any(), any(), eq(BadgeType.GOLD))).willReturn(false);

        // 퀴즈 결과: 0점 가정 (보너스만 확인하기 위해)
        given(quizResultRepository.findByQuizSession(any())).willReturn(List.of());

        // when
        QuizCompleteResponse response = quizSessionService.completeQuizSession(user.getId(), session.getId());

        // then
        // 보너스(1000) + 영상보상(200) = 1200 EXP 확인
        assertThat(response.getEarnedExp()).isEqualTo(1200);
        assertThat(response.getNewBadge().getBadgeType()).isEqualTo(BadgeType.GOLD);
    }
}