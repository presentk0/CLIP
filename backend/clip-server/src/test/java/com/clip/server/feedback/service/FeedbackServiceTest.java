package com.clip.server.feedback.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.feedback.dto.request.FeedbackRequest;
import com.clip.server.feedback.dto.response.FeedbackCheckResponse;
import com.clip.server.feedback.dto.response.FeedbackResponse;
import com.clip.server.feedback.entity.UserFeedback;
import com.clip.server.feedback.repository.UserFeedbackRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @InjectMocks
    private FeedbackService feedbackService;

    @Mock
    private UserFeedbackRepository userFeedbackRepository;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("postFeedBack - 피드백 제출 테스트")
    class PostFeedbackTest {

        @Test
        @DisplayName("성공: 유저가 유효하고 피드백 데이터가 정상일 때 피드백 저장 성공 후 응답을 반환한다")
        void postFeedback_success() {
            // given
            Long userId = 1L;
            FeedbackRequest request = new FeedbackRequest(5, "영상이 유익해요", "퀴즈 난이도가 좀 더 높았으면 좋겠습니다");

            User mockUser = mock(User.class);
            UserFeedback mockSavedFeedback = UserFeedback.builder()
                    .user(mockUser)
                    .satisfactionScore(request.getSatisfactionScore())
                    .goodPoint(request.getGoodPoint())
                    .improvePoint(request.getImprovePoint())
                    .build();

            UserFeedback spySavedFeedback = spy(mockSavedFeedback);
            given(spySavedFeedback.getId()).willReturn(100L);
            given(spySavedFeedback.getCreatedAt()).willReturn(LocalDateTime.now());

            given(userRepository.findById(userId)).willReturn(Optional.of(mockUser));
            given(userFeedbackRepository.saveAndFlush(any(UserFeedback.class))).willReturn(spySavedFeedback);

            // when
            FeedbackResponse response = feedbackService.postFeedBack(userId, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getFeedbackId()).isEqualTo(100L);
            assertThat(response.getSubmittedAt()).isNotNull();

            verify(userRepository, times(1)).findById(userId);
            verify(userFeedbackRepository, times(1)).saveAndFlush(any(UserFeedback.class));
        }

        @Test
        @DisplayName("실패: 유저가 존재하지 않으면 USER_NOT_FOUND 예외를 던진다")
        void postFeedback_fail_userNotFound() {
            // given
            Long userId = 999L;
            FeedbackRequest request = new FeedbackRequest(5, "Good", "Bad");

            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> feedbackService.postFeedBack(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());

            verify(userRepository, times(1)).findById(userId);
            verify(userFeedbackRepository, never()).saveAndFlush(any(UserFeedback.class));
        }
    }

    @Nested
    @DisplayName("getFeedBackCheck - 피드백 제출 여부 체크 테스트")
    class GetFeedBackCheckTest {

        @Test
        @DisplayName("성공: 피드백을 이미 제출한 사용자는 isSubmitted가 true인 응답을 반환한다")
        void getFeedBackCheck_true() {
            // given
            Long userId = 1L;
            given(userRepository.existsById(userId)).willReturn(true);
            given(userFeedbackRepository.existsByUserId(userId)).willReturn(true);

            // when
            FeedbackCheckResponse response = feedbackService.getFeedBackCheck(userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getIsSubmitted()).isTrue();

            verify(userRepository, times(1)).existsById(userId);
            verify(userFeedbackRepository, times(1)).existsByUserId(userId);
        }

        @Test
        @DisplayName("성공: 피드백을 아직 제출하지 않은 사용자는 isSubmitted가 false인 응답을 반환한다")
        void getFeedBackCheck_false() {
            // given
            Long userId = 2L;
            given(userRepository.existsById(userId)).willReturn(true);
            given(userFeedbackRepository.existsByUserId(userId)).willReturn(false);

            // when
            FeedbackCheckResponse response = feedbackService.getFeedBackCheck(userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getIsSubmitted()).isFalse();

            verify(userRepository, times(1)).existsById(userId);
            verify(userFeedbackRepository, times(1)).existsByUserId(userId);
        }

        @Test
        @DisplayName("실패: 조회 대상 유저가 존재하지 않으면 USER_NOT_FOUND 예외를 던진다")
        void getFeedBackCheck_fail_userNotFound() {
            // given
            Long userId = 999L;
            given(userRepository.existsById(userId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> feedbackService.getFeedBackCheck(userId))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining(ErrorCode.USER_NOT_FOUND.getMessage());

            verify(userRepository, times(1)).existsById(userId);
            verify(userFeedbackRepository, never()).existsByUserId(anyLong());
        }
    }
}