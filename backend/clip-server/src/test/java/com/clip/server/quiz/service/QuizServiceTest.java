package com.clip.server.quiz.service;

import com.clip.server.quiz.ai.OpenAIService;
import com.clip.server.quiz.ai.QuizFallbackService;
import com.clip.server.quiz.dto.request.QuizSubmitRequest;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.OpenAIQuizDataResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizSubmitResponse;
import com.clip.server.quiz.entity.QuizResult;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizType;
import com.clip.server.quiz.persistence.QuizPersistenceService;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @InjectMocks
    private QuizService quizService;

    @Mock
    private QuizGenerator quizGenerator;

    @Mock
    private OpenAIService openAIService;

    @Mock
    private QuizFallbackService quizFallbackService;

    @Mock
    private QuizPersistenceService quizPersistenceService;

    @Mock
    private QuizResultRepository quizResultRepository;

    @Mock
    private UserRepository userRepository;

    private User user;
    private QuizResult quizResult;
    private QuizSession quizSession;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@test.com")
                .name("테스터")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);
        ReflectionTestUtils.setField(user, "exp", 100);

        quizSession = QuizSession.builder().build();
        ReflectionTestUtils.setField(quizSession, "id", 10L);

        quizResult = QuizResult.builder()
                .quizSession(quizSession)
                .user(user)
                .word("effort")
                .quizType(QuizType.OX)
                .correctAnswer("O")
                .correctFeedback("정답이에요! 아주 잘했어요.")
                .wrongFeedback("아쉬워요, effort는 노력이란 뜻이에요.")
                .explanation("기본 해설입니다.")
                .build();
        ReflectionTestUtils.setField(quizResult, "id", 50L);
    }

    // ==================== OX 퀴즈 ====================

    @Test
    @DisplayName("OX 퀴즈 생성 성공 - Generator가 유효한 AI 응답 반환 시 저장 후 응답한다")
    void createOXQuiz_Success() {
        // given
        QuizWordRequest request = new QuizWordRequest("Consistent", "일관된", "01:23");
        OpenAIQuizDataResponse aiResponse = OpenAIQuizDataResponse.builder()
                .word("Consistent")
                .content("His actions are consistent.")
                .translation("그의 행동은 일관됩니다.")
                .question("맞을까?")
                .answer("O")
                .explanation("설명")
                .build();

        QuizResult savedResult = QuizResult.builder()
                .word("Consistent")
                .quizType(QuizType.OX)
                .content("His actions are consistent.")
                .translation("그의 행동은 일관됩니다.")
                .question("맞을까?")
                .correctAnswer("O")
                .build();
        ReflectionTestUtils.setField(savedResult, "id", 100L);

        // ✅ Generator를 통해 AI 호출 + 검증 + 재시도 결과 반환
        given(quizGenerator.generateOXQuiz(anyString(), anyString())).willReturn(aiResponse);
        given(quizPersistenceService.saveOXQuiz(anyLong(), anyLong(), any(), any()))
                .willReturn(savedResult);

        // when
        QuizDetailResponse response = quizService.createOXQuiz(10L, 1L, request);

        // then
        assertThat(response.getContent()).isEqualTo("His actions are consistent.");
        assertThat(response.getQuizType()).isEqualTo(QuizType.OX);
        verify(quizPersistenceService).saveOXQuiz(eq(10L), eq(1L), eq(request), eq(aiResponse));
        verify(quizFallbackService, never()).createLocalOXQuiz(anyLong(), anyLong(), any());
    }

    @Nested
    @DisplayName("OX 퀴즈 Fallback 테스트")
    class OXQuizFallbackTest {

        @Test
        @DisplayName("Generator가 null 반환 시 (재시도 3회 모두 실패) Fallback이 호출된다")
        void createOXQuiz_generatorReturnsNull_fallback() {
            // given
            QuizWordRequest request = new QuizWordRequest("travel", "여행하다", "01:30");

            // ✅ Generator가 null 반환 = 3회 시도 모두 실패
            given(quizGenerator.generateOXQuiz(anyString(), anyString())).willReturn(null);

            QuizDetailResponse fallbackResponse = QuizDetailResponse.builder()
                    .quizId(1L)
                    .quizType(QuizType.OX)
                    .content("I want to travel every day.")
                    .build();
            given(quizFallbackService.createLocalOXQuiz(anyLong(), anyLong(), any()))
                    .willReturn(fallbackResponse);

            // when
            QuizDetailResponse response = quizService.createOXQuiz(10L, 1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getQuizType()).isEqualTo(QuizType.OX);
            verify(quizFallbackService).createLocalOXQuiz(10L, 1L, request);
            verify(quizPersistenceService, never()).saveOXQuiz(anyLong(), anyLong(), any(), any());
        }
    }

    // ==================== 빈칸 퀴즈 ====================

    @Test
    @DisplayName("빈칸 퀴즈 생성 성공 - Generator가 유효한 AI 응답 반환 시 저장 후 응답한다")
    void createBlankQuiz_Success() {
        // given
        QuizWordRequest request = new QuizWordRequest("travel", "여행하다", "01:30");
        OpenAIQuizDataResponse aiResponse = OpenAIQuizDataResponse.builder()
                .word("travel")
                .content("I want to [ ] around the world.")
                .translation("나는 세계를 여행하고 싶어요.")
                .question("빈칸에 뭐가 들어갈까요?")
                .options(List.of("travel", "go", "move", "visit"))
                .answer("travel")
                .build();

        QuizResult savedResult = QuizResult.builder()
                .word("travel")
                .quizType(QuizType.BLANK)
                .content("I want to [ ] around the world.")
                .correctAnswer("travel")
                .build();
        ReflectionTestUtils.setField(savedResult, "id", 101L);

        given(quizGenerator.generateBlankQuiz(anyString(), anyString())).willReturn(aiResponse);
        given(quizPersistenceService.saveBlankQuiz(anyLong(), anyLong(), any(), any()))
                .willReturn(savedResult);

        // when
        QuizDetailResponse response = quizService.createBlankQuiz(10L, 1L, request);

        // then
        assertThat(response.getQuizType()).isEqualTo(QuizType.BLANK);
        assertThat(response.getOptions()).hasSize(4);
        verify(quizPersistenceService).saveBlankQuiz(eq(10L), eq(1L), eq(request), eq(aiResponse));
        verify(quizFallbackService, never()).createLocalBlankQuiz(anyLong(), anyLong(), any());
    }

    @Nested
    @DisplayName("빈칸 퀴즈 Fallback 테스트")
    class BlankQuizFallbackTest {

        @Test
        @DisplayName("Generator가 null 반환 시 Fallback이 호출된다")
        void createBlankQuiz_generatorReturnsNull_fallback() {
            // given
            QuizWordRequest request = new QuizWordRequest("study", "공부하다", "02:00");

            given(quizGenerator.generateBlankQuiz(anyString(), anyString())).willReturn(null);

            QuizDetailResponse fallbackResponse = QuizDetailResponse.builder()
                    .quizId(1L)
                    .quizType(QuizType.BLANK)
                    .content("I want to [ ] every day.")
                    .options(List.of("study", "go", "run", "make"))
                    .build();
            given(quizFallbackService.createLocalBlankQuiz(anyLong(), anyLong(), any()))
                    .willReturn(fallbackResponse);

            // when
            QuizDetailResponse response = quizService.createBlankQuiz(10L, 1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getQuizType()).isEqualTo(QuizType.BLANK);
            verify(quizFallbackService).createLocalBlankQuiz(10L, 1L, request);
            verify(quizPersistenceService, never()).saveBlankQuiz(anyLong(), anyLong(), any(), any());
        }
    }

    // ==================== 매칭 퀴즈 ====================

    @Test
    @DisplayName("매칭 퀴즈 생성 성공 - AI 응답을 받아 Persistence에 저장 위임")
    void createMatchingQuiz_Success() {
        // given
        Long sessionId = 10L;
        Long userId = 1L;
        List<QuizWordRequest> requests = List.of(
                new QuizWordRequest("apple", "사과", "00:10"),
                new QuizWordRequest("banana", "바나나", "00:20"),
                new QuizWordRequest("cherry", "체리", "00:30"),
                new QuizWordRequest("date", "대추", "00:40"),
                new QuizWordRequest("elderberry", "엘더베리", "00:50")
        );

        // ✅ OpenAIService 직접 호출 (매칭은 재시도 X)
        List<OpenAIQuizDataResponse> aiResponse = List.of(
                OpenAIQuizDataResponse.builder().word("apple").question("apple").answer("사과").build(),
                OpenAIQuizDataResponse.builder().word("banana").question("banana").answer("바나나").build(),
                OpenAIQuizDataResponse.builder().word("cherry").question("cherry").answer("체리").build(),
                OpenAIQuizDataResponse.builder().word("date").question("date").answer("대추").build(),
                OpenAIQuizDataResponse.builder().word("elderberry").question("elderberry").answer("엘더베리").build()
        );
        given(openAIService.generateMatchingQuiz(anyList(), any())).willReturn(aiResponse);

        // ✅ Persistence가 저장된 QuizResult 5개 반환
        List<QuizResult> savedList = List.of(
                buildMatchingResult(1L, "apple", "사과"),
                buildMatchingResult(2L, "banana", "바나나"),
                buildMatchingResult(3L, "cherry", "체리"),
                buildMatchingResult(4L, "date", "대추"),
                buildMatchingResult(5L, "elderberry", "엘더베리")
        );
        given(quizPersistenceService.saveMatchingQuizzes(anyLong(), anyLong(), anyList(), anyList(), anyInt()))
                .willReturn(savedList);

        // when
        List<QuizDetailResponse> responses = quizService.createMatchingQuiz(sessionId, userId, requests);

        // then
        assertThat(responses).hasSize(5);
        assertThat(responses.get(0).getQuestion()).isEqualTo("apple");
        assertThat(responses.get(0).getAnswer()).isEqualTo("사과");
        assertThat(responses.get(4).getQuestion()).isEqualTo("elderberry");

        verify(quizPersistenceService).saveMatchingQuizzes(eq(sessionId), eq(userId), eq(requests), eq(aiResponse), eq(5));
        verify(quizFallbackService, never()).createLocalMatchingQuiz(anyLong(), anyLong(), anyList());
    }

    @Nested
    @DisplayName("매칭 퀴즈 Fallback 테스트")
    class MatchingQuizFallbackTest {

        @Test
        @DisplayName("AI가 null 반환 시 전체 Fallback이 호출된다")
        void createMatchingQuiz_aiNull_fallback() {
            // given
            List<QuizWordRequest> requests = List.of(
                    new QuizWordRequest("apple", "사과", "01:00"),
                    new QuizWordRequest("banana", "바나나", "01:30")
            );

            given(openAIService.generateMatchingQuiz(anyList(), any())).willReturn(null);

            List<QuizDetailResponse> fallbackResponses = List.of(
                    QuizDetailResponse.builder().quizId(1L).quizType(QuizType.MATCHING).question("apple").answer("사과").build(),
                    QuizDetailResponse.builder().quizId(2L).quizType(QuizType.MATCHING).question("banana").answer("바나나").build()
            );
            given(quizFallbackService.createLocalMatchingQuiz(anyLong(), anyLong(), anyList()))
                    .willReturn(fallbackResponses);

            // when
            List<QuizDetailResponse> responses = quizService.createMatchingQuiz(10L, 1L, requests);

            // then
            assertThat(responses).hasSize(2);
            verify(quizFallbackService).createLocalMatchingQuiz(10L, 1L, requests);
            verify(quizPersistenceService, never()).saveMatchingQuizzes(anyLong(), anyLong(), anyList(), anyList(), anyInt());
        }

        @Test
        @DisplayName("AI가 빈 리스트 반환 시 전체 Fallback이 호출된다")
        void createMatchingQuiz_aiEmptyList_fallback() {
            // given
            List<QuizWordRequest> requests = List.of(
                    new QuizWordRequest("apple", "사과", "01:00")
            );

            given(openAIService.generateMatchingQuiz(anyList(), any())).willReturn(List.of());

            List<QuizDetailResponse> fallbackResponses = List.of(
                    QuizDetailResponse.builder().quizId(1L).quizType(QuizType.MATCHING).build()
            );
            given(quizFallbackService.createLocalMatchingQuiz(anyLong(), anyLong(), anyList()))
                    .willReturn(fallbackResponses);

            // when
            quizService.createMatchingQuiz(10L, 1L, requests);

            // then
            verify(quizFallbackService).createLocalMatchingQuiz(10L, 1L, requests);
            verify(quizPersistenceService, never()).saveMatchingQuizzes(anyLong(), anyLong(), anyList(), anyList(), anyInt());
        }

        @Test
        @DisplayName("AI 호출 중 예외 발생 시 전체 Fallback이 호출된다")
        void createMatchingQuiz_aiException_fallback() {
            // given
            List<QuizWordRequest> requests = List.of(
                    new QuizWordRequest("test", "테스트", "00:00")
            );

            given(openAIService.generateMatchingQuiz(anyList(), any()))
                    .willThrow(new RuntimeException("OpenAI 서버 오류"));

            List<QuizDetailResponse> fallbackResponses = List.of(
                    QuizDetailResponse.builder().quizId(1L).quizType(QuizType.MATCHING).build()
            );
            given(quizFallbackService.createLocalMatchingQuiz(anyLong(), anyLong(), anyList()))
                    .willReturn(fallbackResponses);

            // when
            List<QuizDetailResponse> responses = quizService.createMatchingQuiz(10L, 1L, requests);

            // then
            assertThat(responses).isNotEmpty();
            verify(quizFallbackService).createLocalMatchingQuiz(10L, 1L, requests);
        }
    }

    // ==================== 퀴즈 제출 ====================

    @Test
    @DisplayName("퀴즈 정답 제출 시 경험치가 상승하고 피드백을 반환한다")
    void submitQuiz_Correct() {
        // given
        QuizSubmitRequest request = new QuizSubmitRequest(10L, 50L, "O");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(quizResultRepository.findById(50L)).willReturn(Optional.of(quizResult));

        // when
        QuizSubmitResponse response = quizService.submitQuiz(1L, request);

        // then
        assertThat(response.isCorrect()).isTrue();
        assertThat(response.getEarnedExp()).isEqualTo(100);
        assertThat(response.getCurrentExp()).isEqualTo(200);  // 기존 100 + 100
        assertThat(response.getFeedback()).isEqualTo("정답이에요! 아주 잘했어요.");
    }

    @Test
    @DisplayName("퀴즈 오답 제출 시 경험치가 오르지 않고 오답 피드백을 반환한다")
    void submitQuiz_Wrong() {
        // given
        QuizSubmitRequest request = new QuizSubmitRequest(10L, 50L, "X");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(quizResultRepository.findById(50L)).willReturn(Optional.of(quizResult));

        // when
        QuizSubmitResponse response = quizService.submitQuiz(1L, request);

        // then
        assertThat(response.isCorrect()).isFalse();
        assertThat(response.getEarnedExp()).isEqualTo(0);
        assertThat(response.getCurrentExp()).isEqualTo(100);  // 변동 없음
        assertThat(response.getFeedback()).isEqualTo("아쉬워요, effort는 노력이란 뜻이에요.");
    }

    // ==================== 헬퍼 메서드 ====================

    private QuizResult buildMatchingResult(Long id, String word, String answer) {
        QuizResult result = QuizResult.builder()
                .quizSession(quizSession)
                .user(user)
                .word(word)
                .quizType(QuizType.MATCHING)
                .content(word + " example")
                .translation("예시")
                .question(word)
                .correctAnswer(answer)
                .build();
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }
}