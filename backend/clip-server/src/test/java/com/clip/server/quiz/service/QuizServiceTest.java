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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @InjectMocks
    private QuizService quizService;
    @Mock
    private OpenAIService openAIService;
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
                .word("Consistent").content("His actions are consistent.").translation("그의 행동은 일관된다.")
                .question("맞을까?").answer("O").explanation("설명").build();

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

        // AI는 2개만 성공했다고 가정
        List<OpenAIQuizDataResponse> aiPartialResponse = List.of(
                OpenAIQuizDataResponse.builder().word("apple").question("apple").answer("사과").build(),
                OpenAIQuizDataResponse.builder().word("banana").question("banana").answer("바나나").build()
        );
        given(openAIService.generateMatchingQuiz(anyList())).willReturn(aiPartialResponse);
        given(quizResultRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        List<QuizDetailResponse> responses = quizService.createMatchingQuiz(sessionId, userId, requests);

        // then
        assertThat(responses).hasSize(5); // AI 2개 + 로컬 보충 3개 = 총 5개

        // 1번째 (AI 데이터 기반)
        assertThat(responses.get(0).getQuestion()).isEqualTo("apple");
        assertThat(responses.get(0).getAnswer()).isEqualTo("사과");

        // 5번째 (로컬 데이터 보충 기반)
        assertThat(responses.get(4).getQuestion()).isEqualTo("elderberry");
        assertThat(responses.get(4).getAnswer()).isEqualTo("엘더베리");

        verify(quizResultRepository, times(5)).save(any());
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
        assertThat(response.getCurrentExp()).isEqualTo(200); // 100 + 100
        assertThat(response.getFeedback()).isEqualTo("정답이에요! 아주 잘했어요.");
    }

    @Test
    @DisplayName("매칭 퀴즈 AI 생성 실패 시 500 에러를 던져 폴백 로직을 유도한다")
    void createMatchingQuiz_AIFail_ThrowException() {
        // given
        given(quizSessionRepository.findById(any())).willReturn(Optional.of(quizSession));
        given(userRepository.findById(any())).willReturn(Optional.of(user));
        given(openAIService.generateMatchingQuiz(any())).willReturn(null); // AI 완전 실패

        // when & then
        assertThatThrownBy(() -> quizService.createMatchingQuiz(10L, 1L, List.of()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
    }
}