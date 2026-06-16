package com.clip.server.quiz.service;

import com.clip.server.quiz.ai.QuizFallbackService;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.entity.QuizSession;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuizFallbackServiceTest {

    @Mock
    private QuizResultRepository quizResultRepository;
    @Mock
    private QuizSessionRepository quizSessionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private QuizFallbackService quizFallbackService;

    private User user;
    private QuizSession quizSession;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@test.com")
                .name("테스터")
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        quizSession = QuizSession.builder().build();
        ReflectionTestUtils.setField(quizSession, "id", 10L);
    }

    @Test
    @DisplayName("매칭 퀴즈 로컬 생성 - AI 없이 서버 데이터로만 5개 퀴즈를 생성한다")
    void createLocalMatchingQuiz_Success() {
        // given
        List<QuizWordRequest> requests = List.of(
                new QuizWordRequest("word1", "뜻1", "00:01"),
                new QuizWordRequest("word2", "뜻2", "00:02"),
                new QuizWordRequest("word3", "뜻3", "00:03"),
                new QuizWordRequest("word4", "뜻4", "00:04"),
                new QuizWordRequest("word5", "뜻5", "00:05")
        );

        given(quizSessionRepository.findById(any())).willReturn(Optional.of(quizSession));
        given(userRepository.findById(any())).willReturn(Optional.of(user));

        given(quizResultRepository.save(any())).willAnswer(invocation -> {
            Object entity = invocation.getArgument(0);
            ReflectionTestUtils.setField(entity, "id", 100L); // 가짜 ID 부여
            return entity;
        });

        // when
        List<QuizDetailResponse> responses = quizFallbackService.createLocalMatchingQuiz(10L, 1L, requests);

        // then
        assertThat(responses).hasSize(5);
        assertThat(responses.get(0).getQuestion()).isEqualTo("word1");
        assertThat(responses.get(0).getAnswer()).isEqualTo("뜻1");
        assertThat(responses.get(0).getQuizId()).isEqualTo(100L);

        verify(quizResultRepository, times(5)).save(any());
    }
}