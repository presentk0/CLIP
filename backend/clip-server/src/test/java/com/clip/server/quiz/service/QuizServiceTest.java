package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.GeminiQuizData;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class QuizServiceTest {

    @InjectMocks
    private QuizService quizService;

    @Mock
    private GeminiService geminiService;

    @Mock
    private QuizResultRepository quizResultRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private QuizSessionRepository quizSessionRepository;

    @Test
    @DisplayName("OX 퀴즈 생성 및 저장 성공 테스트")
    void createOXQuiz_Success() {
        // given
        Long sessionId = 1L;
        Long userId = 1L;
        QuizWordRequest request = new QuizWordRequest("Consistent", "일관된", "01:23");

        User user = User.builder().build();
        QuizSession session = QuizSession.builder().build();

        GeminiQuizData aiResponse = new GeminiQuizData("Consistent", QuizType.OX, "Consistent의 뜻은 '일관된'이다.", "O", "맞습니다.");

        QuizResult savedResult = QuizResult.builder()
                .quizType(QuizType.OX)
                .question(aiResponse.getQuestion())
                .videoTimestamp(request.getVideoTimeStamp())
                .build();
        ReflectionTestUtils.setField(savedResult, "id", 100L);

        given(quizSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(geminiService.generateOXQuiz(any(), any())).willReturn(aiResponse);
        given(quizResultRepository.save(any())).willReturn(savedResult);

        // when
        QuizDetailResponse response = quizService.createOXQuiz(sessionId, userId, request);

        // then
        assertThat(response.getQuizId()).isEqualTo(100);
        assertThat(response.getQuizType()).isEqualTo(QuizType.OX);
        assertThat(response.getVideoTimeStamp()).isEqualTo("01:23");

        verify(quizResultRepository).save(any(QuizResult.class));
    }

    @Test
    @DisplayName("빈칸 채우기 퀴즈 생성 및 저장 성공 테스트")
    void createBlankQuiz_Success() {
        // given
        Long sessionId = 1L;
        Long userId = 1L;
        QuizWordRequest request = new QuizWordRequest("Implement", "구현하다", "02:45");

        User user = User.builder().build();
        QuizSession session = QuizSession.builder().build();

        GeminiQuizData aiResponse = new GeminiQuizData("Implement", QuizType.BLANK, "We need to ____ the new policy.", "Implement", "문맥상 '구현하다'가 적절합니다.");

        QuizResult savedResult = QuizResult.builder()
                .quizType(QuizType.BLANK)
                .question(aiResponse.getQuestion())
                .videoTimestamp(request.getVideoTimeStamp())
                .build();
        ReflectionTestUtils.setField(savedResult, "id", 200L);

        given(quizSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(geminiService.generateBlankQuiz(any(), any())).willReturn(aiResponse);
        given(quizResultRepository.save(any())).willReturn(savedResult);

        // when
        QuizDetailResponse response = quizService.createBlankQuiz(sessionId, userId, request);

        // then
        assertThat(response.getQuizId()).isEqualTo(200);
        assertThat(response.getQuizType()).isEqualTo(QuizType.BLANK);
        assertThat(response.getQuestion()).contains("____");

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

        List<GeminiQuizData> aiResponses = List.of(
                new GeminiQuizData("Apple", QuizType.MATCHING, "Apple", "사과", "과일 이름"),
                new GeminiQuizData("Banana", QuizType.MATCHING, "Banana", "바나나", "과일 이름")
        );

        given(quizSessionRepository.findById(sessionId)).willReturn(Optional.of(session));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(geminiService.generateMatchingQuiz(any())).willReturn(aiResponses);

        // save 호출 시마다 다른 ID를 가진 객체 반환 모사
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

        // Reflection을 통해 private final 또는 private 필드인 id에 값을 넣습니다.
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