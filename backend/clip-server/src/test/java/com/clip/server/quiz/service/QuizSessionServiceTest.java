package com.clip.server.quiz.service;

import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizGenerateResponse;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizSessionWord;
import com.clip.server.quiz.entity.SessionType;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.quiz.repository.QuizSessionWordRepository;
import com.clip.server.user.entity.User;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuizSessionServiceTest {

    @InjectMocks
    private QuizSessionService quizSessionService;

    @Mock
    private QuizService quizService;
    @Mock
    private GeminiService geminiService;
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
        QuizGenerateResponse response = quizSessionService.generateSectionQuiz(1L, "v123", 1, SessionType.NORMAL);

        // then
        assertThat(response.getSessionId()).isEqualTo(100L);
        // 수집 단어가 1개뿐이므로 OX 퀴즈가 먼저 생성됨 (로직상 candidates.get(0) 사용)
        verify(quizService).createOXQuiz(eq(100L), eq(1L), any(QuizWordRequest.class));
    }

    @Test
    @DisplayName("최종 매칭 퀴즈 생성 - 단어가 5개 미만이면 AI 추천 단어를 보충한다")
    void generateMatchingQuiz_WithAIRecommendation() {
        // given
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
        given(geminiService.recommendImportantWords(anyString(), eq(2))).willReturn(aiRecs);

        given(quizService.createMatchingQuiz(anyLong(), anyLong(), anyList())).willReturn(List.of());

        // when
        quizSessionService.generateMatchingQuiz(1L, "v123", SessionType.NORMAL);

        // then
        // 최종적으로 5개의 단어가 QuizService로 전달되었는지 확인
        verify(geminiService).recommendImportantWords(anyString(), eq(2));
        verify(quizService).createMatchingQuiz(eq(100L), eq(1L), argThat(list -> list.size() == 5));
    }

    @Test
    @DisplayName("새로운 세션 생성 시 단어 스냅샷(동기화)이 수행된다")
    void getOrCreateSession_SyncWords() {
        // given
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
        quizSessionService.generateSectionQuiz(1L, "v123", 1, SessionType.NORMAL);

        // then
        // 세션이 저장되고 단어 스냅샷이 실행되었는지 확인
        verify(quizSessionRepository).save(any());
        verify(sessionWordRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("매칭 퀴즈 생성 - 단어 순서가 섞여있어도 우선순위(COLLECT > POPUP > SYSTEM)대로 정렬된다")
    void generateMatchingQuiz_PrioritySorting() {
        // given
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(videoRepository.findById("v123")).willReturn(Optional.of(video));
        given(quizSessionRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(session));

        // 일부러 뒤섞인 순서로 단어 리스트 준비
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
        quizSessionService.generateMatchingQuiz(1L, "v123", SessionType.NORMAL);

        // then
        // QuizService로 전달되는 단어 리스트가 우선순위대로 정렬되었는지 검증 (COLLECT 가 0,1번 인덱스에 와야 함)
        verify(quizService).createMatchingQuiz(eq(100L), eq(1L), argThat(list ->
                list.get(0).getWord().startsWith("Collect") &&
                        list.get(1).getWord().startsWith("Collect") &&
                        list.get(2).getWord().startsWith("Popup") &&
                        list.get(4).getWord().equals("System1")
        ));
    }
}