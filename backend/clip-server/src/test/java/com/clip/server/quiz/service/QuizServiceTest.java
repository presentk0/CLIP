package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.OpenAIQuizDataResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.entity.QuizResult;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizType;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @Test
    @DisplayName("OX 퀴즈 생성 및 저장 성공 테스트 - 예문과 해석이 포함되어야 한다")
    void createOXQuiz_Success() {
        // given
        Long sessionId = 1L;
        Long userId = 1L;
        QuizWordRequest request = new QuizWordRequest("Consistent", "일관된", "01:23");

        User user = User.builder().build();
        QuizSession session = QuizSession.builder().build();

        OpenAIQuizDataResponse aiResponse = OpenAIQuizDataResponse.builder()
                .word("Consistent")
                .quizType("OX")
                .content("His actions are consistent with his words.")
                .translation("그의 행동은 그의 말과 일관된다.")
                .question("문장에 들어간 단어로 저게 맞을까?")
                .answer("O")
                .explanation("맞습니다. 일관된이라는 의미로 잘 쓰였어요.")
                .build();

        QuizResult savedResult = QuizResult.builder()
                .quizType(QuizType.OX)
                .content(aiResponse.getContent())
                .translation(aiResponse.getTranslation())
                .question(aiResponse.getQuestion())
                .videoTimestamp(request.getVideoTimeStamp())
                .build();
        ReflectionTestUtils.setField(savedResult, "id", 100L);

        given(quizSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(openAIService.generateOXQuiz(anyString(), anyString())).willReturn(aiResponse);
        given(quizResultRepository.save(any(QuizResult.class))).willReturn(savedResult);

        // when
        QuizDetailResponse response = quizService.createOXQuiz(sessionId, userId, request);

        // then
        assertThat(response.getQuizId()).isEqualTo(100);
        assertThat(response.getQuizType()).isEqualTo(QuizType.OX);
        assertThat(response.getContent()).isEqualTo("His actions are consistent with his words.");
        assertThat(response.getTranslation()).isEqualTo("그의 행동은 그의 말과 일관된다.");
        assertThat(response.getVideoTimeStamp()).isEqualTo("01:23");

        verify(quizResultRepository).save(any(QuizResult.class));
    }

    @Test
    @DisplayName("빈칸 채우기 퀴즈 생성 및 저장 성공 테스트 - 오답 리스트(options)가 포함되어야 한다")
    void createBlankQuiz_Success() {
        // given
        Long sessionId = 1L;
        Long userId = 1L;
        QuizWordRequest request = new QuizWordRequest("Implement", "구현하다", "02:45");

        User user = User.builder().build();
        QuizSession session = QuizSession.builder().build();

        List<String> options = List.of("Implement", "Ignore", "Increase", "Imagine");
        OpenAIQuizDataResponse aiResponse = OpenAIQuizDataResponse.builder()
                .word("Implement")
                .quizType("BLANK")
                .content("We need to [ ] the new policy.")
                .translation("우리는 새로운 정책을 구현해야 한다.")
                .question("빈칸에 들어갈 알맞은 단어는?")
                .options(options)
                .answer("Implement")
                .explanation("구현하다라는 의미가 적절합니다.")
                .build();

        QuizResult savedResult = QuizResult.builder()
                .quizType(QuizType.BLANK)
                .content(aiResponse.getContent())
                .question(aiResponse.getQuestion())
                .videoTimestamp(request.getVideoTimeStamp())
                .build();
        ReflectionTestUtils.setField(savedResult, "id", 200L);

        given(quizSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(openAIService.generateBlankQuiz(anyString(), anyString())).willReturn(aiResponse);
        given(quizResultRepository.save(any(QuizResult.class))).willReturn(savedResult);

        // when
        QuizDetailResponse response = quizService.createBlankQuiz(sessionId, userId, request);

        // then
        assertThat(response.getQuizId()).isEqualTo(200);
        assertThat(response.getOptions()).hasSize(4);
        assertThat(response.getOptions()).contains("Implement");
        assertThat(response.getContent()).contains("[ ]");

        verify(quizResultRepository).save(any(QuizResult.class));
    }

    @Test
    @DisplayName("매칭 퀴즈 세트 생성 및 저장 성공 테스트")
    void createMatchingQuiz_Success() {
        // given
        Long sessionId = 1L;
        Long userId = 1L;
        List<QuizWordRequest> requests = List.of(
                new QuizWordRequest("Apple", "사과", "00:10"),
                new QuizWordRequest("Banana", "바나나", "00:20")
        );

        User user = User.builder().build();
        QuizSession session = QuizSession.builder().build();

        List<OpenAIQuizDataResponse> aiResponses = List.of(
                OpenAIQuizDataResponse.builder().word("Apple").quizType("MATCHING").question("Apple").answer("사과").build(),
                OpenAIQuizDataResponse.builder().word("Banana").quizType("MATCHING").question("Banana").answer("바나나").build()
        );

        given(quizSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(openAIService.generateMatchingQuiz(any())).willReturn(aiResponses);

        given(quizResultRepository.save(any()))
                .willReturn(createQuizResult(301L, QuizType.MATCHING, "Apple", "00:10"))
                .willReturn(createQuizResult(302L, QuizType.MATCHING, "Banana", "00:20"));

        // when
        List<QuizDetailResponse> responses = quizService.createMatchingQuiz(sessionId, userId, requests);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getQuizId()).isEqualTo(301);
        assertThat(responses.get(1).getQuizId()).isEqualTo(302);
        assertThat(responses.get(0).getVideoTimeStamp()).isEqualTo("00:10");

        verify(quizResultRepository, times(2)).save(any(QuizResult.class));
    }

    private QuizResult createQuizResult(Long id, QuizType type, String question, String timestamp) {
        QuizResult result = QuizResult.builder()
                .quizType(type)
                .question(question)
                .videoTimestamp(timestamp)
                .build();

        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    @Test
    @DisplayName("존재하지 않는 세션 ID로 요청 시 예외가 발생한다")
    void createOXQuiz_SessionNotFound() {
        // given
        Long sessionId = 999L;
        QuizWordRequest request = new QuizWordRequest("test", "test", "00:00");
        given(quizSessionRepository.findById(sessionId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> quizService.createOXQuiz(sessionId, 1L, request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_NOT_FOUND);
    }
}