package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizSubmitRequest;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.OpenAIQuizDataResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizSubmitResponse;
import com.clip.server.quiz.entity.QuizResult;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizType;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.quiz.repository.QuizSessionRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @InjectMocks
    private QuizService quizService;

    @Mock
    private OpenAIService openAIService;

    @Mock
    private QuizFallbackService quizFallbackService;

    @Mock
    private QuizResultRepository quizResultRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuizSessionRepository quizSessionRepository;

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

    @Test
    @DisplayName("OX 퀴즈 생성 성공 - AI 응답 기반으로 안전하게 저장된다")
    void createOXQuiz_Success() {
        // given
        QuizWordRequest request = new QuizWordRequest("Consistent", "일관된", "01:23");
        OpenAIQuizDataResponse aiResponse = OpenAIQuizDataResponse.builder()
                .word("Consistent")
                .content("His actions are consistent.")
                .translation("그의 행동은 일관된다.")
                .question("맞을까?")
                .answer("O")
                .explanation("설명")
                .build();

        given(quizSessionRepository.findById(any())).willReturn(Optional.of(quizSession));
        given(userRepository.findById(any())).willReturn(Optional.of(user));
        given(openAIService.generateOXQuiz(anyString(), anyString())).willReturn(aiResponse);
        given(quizResultRepository.save(any(QuizResult.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        QuizDetailResponse response = quizService.createOXQuiz(10L, 1L, request);

        // then
        assertThat(response.getContent()).isEqualTo("His actions are consistent.");
        assertThat(response.getQuizType()).isEqualTo(QuizType.OX);
        verify(quizResultRepository).save(any());
        verify(quizFallbackService, never()).createLocalOXQuiz(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("매칭 퀴즈 하이브리드 생성 - AI 응답이 부족하면 로컬 데이터로 보충하여 5개를 맞춘다")
    void createMatchingQuiz_HybridSuccess() {
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

        given(quizSessionRepository.findById(sessionId)).willReturn(Optional.of(quizSession));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));

        List<OpenAIQuizDataResponse> aiPartialResponse = List.of(
                OpenAIQuizDataResponse.builder().word("apple").question("apple").answer("사과").build(),
                OpenAIQuizDataResponse.builder().word("banana").question("banana").answer("바나나").build()
        );
        given(openAIService.generateMatchingQuiz(anyList())).willReturn(aiPartialResponse);
        given(quizResultRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        List<QuizDetailResponse> responses = quizService.createMatchingQuiz(sessionId, userId, requests);

        // then
        assertThat(responses).hasSize(5);
        assertThat(responses.get(0).getQuestion()).isEqualTo("apple");
        assertThat(responses.get(0).getAnswer()).isEqualTo("사과");
        assertThat(responses.get(4).getQuestion()).isEqualTo("elderberry");

        verify(quizResultRepository, times(5)).save(any());
        verify(quizFallbackService, never()).createLocalMatchingQuiz(anyLong(), anyLong(), anyList());
    }

    @Test
    @DisplayName("퀴즈 정답 제출 시 경험치가 상승하고 피드백을 반환한다")
    void submitQuiz_Success() {
        // given
        QuizSubmitRequest request = new QuizSubmitRequest(10L, 50L, "O");
        given(userRepository.findById(1L)).willReturn(Optional.of(user));
        given(quizResultRepository.findById(50L)).willReturn(Optional.of(quizResult));

        // when
        QuizSubmitResponse response = quizService.submitQuiz(1L, request);

        // then
        assertThat(response.isCorrect()).isTrue();
        assertThat(response.getEarnedExp()).isEqualTo(100);
        assertThat(response.getCurrentExp()).isEqualTo(200);
        assertThat(response.getFeedback()).isEqualTo("정답이에요! 아주 잘했어요.");
    }

    @Nested
    @DisplayName("OX 퀴즈 Fallback 테스트")
    class OXQuizFallbackTest {

        @Test
        @DisplayName("AI 응답이 null이면 Fallback이 호출된다")
        void createOXQuiz_aiNull_fallback() {
            // given
            QuizWordRequest request = new QuizWordRequest("travel", "여행하다", "01:30");

            given(openAIService.generateOXQuiz(anyString(), anyString())).willReturn(null);

            QuizDetailResponse fallbackResponse = QuizDetailResponse.builder()
                    .quizId(1L)
                    .quizType(QuizType.OX)
                    .content("I want to travel every day.")
                    .translation("나는 매일 여행하고 싶어요.")
                    .build();
            given(quizFallbackService.createLocalOXQuiz(anyLong(), anyLong(), any()))
                    .willReturn(fallbackResponse);

            // when
            QuizDetailResponse response = quizService.createOXQuiz(10L, 1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getQuizType()).isEqualTo(QuizType.OX);
            verify(quizFallbackService).createLocalOXQuiz(10L, 1L, request);
        }

        @Test
        @DisplayName("AI 응답 content에 한글이 포함되면 Fallback이 호출된다")
        void createOXQuiz_koreanContent_fallback() {
            // given
            QuizWordRequest request = new QuizWordRequest("travel", "여행하다", "01:30");

            OpenAIQuizDataResponse aiResponse = OpenAIQuizDataResponse.builder()
                    .word("travel")
                    .content("나는 여행하고 싶어요.")  // ❌ 한글 포함
                    .translation("나는 여행하고 싶어요.")
                    .answer("O")
                    .build();

            given(openAIService.generateOXQuiz(anyString(), anyString())).willReturn(aiResponse);

            QuizDetailResponse fallbackResponse = QuizDetailResponse.builder()
                    .quizId(1L)
                    .quizType(QuizType.OX)
                    .build();
            given(quizFallbackService.createLocalOXQuiz(anyLong(), anyLong(), any()))
                    .willReturn(fallbackResponse);

            // when
            quizService.createOXQuiz(10L, 1L, request);

            // then
            verify(quizFallbackService).createLocalOXQuiz(10L, 1L, request);
        }

        @Test
        @DisplayName("AI 호출 중 예외가 발생하면 Fallback이 호출된다")
        void createOXQuiz_aiException_fallback() {
            // given
            QuizWordRequest request = new QuizWordRequest("travel", "여행하다", "01:30");

            given(openAIService.generateOXQuiz(anyString(), anyString()))
                    .willThrow(new RuntimeException("OpenAI 서버 오류"));

            QuizDetailResponse fallbackResponse = QuizDetailResponse.builder()
                    .quizId(1L)
                    .quizType(QuizType.OX)
                    .build();
            given(quizFallbackService.createLocalOXQuiz(anyLong(), anyLong(), any()))
                    .willReturn(fallbackResponse);

            // when
            QuizDetailResponse response = quizService.createOXQuiz(10L, 1L, request);

            // then
            assertThat(response).isNotNull();
            verify(quizFallbackService).createLocalOXQuiz(10L, 1L, request);
        }
    }

    @Nested
    @DisplayName("빈칸 퀴즈 Fallback 테스트")
    class BlankQuizFallbackTest {

        @Test
        @DisplayName("AI 응답이 null이면 Fallback이 호출된다")
        void createBlankQuiz_aiNull_fallback() {
            // given
            QuizWordRequest request = new QuizWordRequest("study", "공부하다", "02:00");

            given(openAIService.generateBlankQuiz(anyString(), anyString())).willReturn(null);

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
        }

        @Test
        @DisplayName("AI 응답 content에 빈칸이 없으면 Fallback이 호출된다")
        void createBlankQuiz_noBlank_fallback() {
            // given
            QuizWordRequest request = new QuizWordRequest("study", "공부하다", "02:00");

            OpenAIQuizDataResponse aiResponse = OpenAIQuizDataResponse.builder()
                    .word("study")
                    .content("I want to study every day.")  // ❌ 빈칸 없음
                    .translation("나는 매일 공부하고 싶어요.")
                    .answer("study")
                    .options(List.of("study", "go", "run", "make"))
                    .build();

            given(openAIService.generateBlankQuiz(anyString(), anyString())).willReturn(aiResponse);

            QuizDetailResponse fallbackResponse = QuizDetailResponse.builder()
                    .quizId(1L)
                    .quizType(QuizType.BLANK)
                    .build();
            given(quizFallbackService.createLocalBlankQuiz(anyLong(), anyLong(), any()))
                    .willReturn(fallbackResponse);

            // when
            quizService.createBlankQuiz(10L, 1L, request);

            // then
            verify(quizFallbackService).createLocalBlankQuiz(10L, 1L, request);
        }

        @Test
        @DisplayName("AI 응답 options가 4개가 아니면 Fallback이 호출된다")
        void createBlankQuiz_invalidOptions_fallback() {
            // given
            QuizWordRequest request = new QuizWordRequest("study", "공부하다", "02:00");

            OpenAIQuizDataResponse aiResponse = OpenAIQuizDataResponse.builder()
                    .word("study")
                    .content("I want to [ ] every day.")
                    .translation("나는 매일 공부하고 싶어요.")
                    .answer("study")
                    .options(List.of("study", "go"))  // ❌ 2개만 있음
                    .build();

            given(openAIService.generateBlankQuiz(anyString(), anyString())).willReturn(aiResponse);

            QuizDetailResponse fallbackResponse = QuizDetailResponse.builder()
                    .quizId(1L)
                    .quizType(QuizType.BLANK)
                    .build();
            given(quizFallbackService.createLocalBlankQuiz(anyLong(), anyLong(), any()))
                    .willReturn(fallbackResponse);

            // when
            quizService.createBlankQuiz(10L, 1L, request);

            // then
            verify(quizFallbackService).createLocalBlankQuiz(10L, 1L, request);
        }
    }

    @Nested
    @DisplayName("매칭 퀴즈 Fallback 테스트")
    class MatchingQuizFallbackTest {

        @Test
        @DisplayName("AI 전체 실패 시 전체 Fallback이 호출된다")
        void createMatchingQuiz_aiTotalFail_fallback() {
            // given
            List<QuizWordRequest> requests = List.of(
                    new QuizWordRequest("apple", "사과", "01:00"),
                    new QuizWordRequest("banana", "바나나", "01:30")
            );

            given(openAIService.generateMatchingQuiz(anyList())).willReturn(null);

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
        }

        @Test
        @DisplayName("AI 빈 리스트 반환 시 전체 Fallback이 호출된다")
        void createMatchingQuiz_aiEmptyList_fallback() {
            // given
            List<QuizWordRequest> requests = List.of(
                    new QuizWordRequest("apple", "사과", "01:00")
            );

            given(openAIService.generateMatchingQuiz(anyList())).willReturn(List.of());

            List<QuizDetailResponse> fallbackResponses = List.of(
                    QuizDetailResponse.builder().quizId(1L).quizType(QuizType.MATCHING).build()
            );
            given(quizFallbackService.createLocalMatchingQuiz(anyLong(), anyLong(), anyList()))
                    .willReturn(fallbackResponses);

            // when
            quizService.createMatchingQuiz(10L, 1L, requests);

            // then
            verify(quizFallbackService).createLocalMatchingQuiz(10L, 1L, requests);
        }
    }

    @Test
    @DisplayName("매칭 퀴즈 AI 전체 실패 시 Fallback 서비스가 호출된다")
    void createMatchingQuiz_AIFail_CallsFallback() {
        // given
        List<QuizWordRequest> requests = List.of(
                new QuizWordRequest("test", "테스트", "00:00")
        );

        given(openAIService.generateMatchingQuiz(any())).willReturn(null);

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