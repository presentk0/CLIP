package com.clip.server.user.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.user.dto.response.UserGrowthResponse;
import com.clip.server.user.dto.response.UserProfileResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.badge.BadgeType;
import com.clip.server.user.entity.badge.UserBadge;
import com.clip.server.user.entity.learning.LearningHistory;
import com.clip.server.user.repository.LearningHistoryRepository;
import com.clip.server.user.repository.UserBadgeRepository;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video; // 실제 비디오 엔티티 패키지에 맞게 조정
import com.clip.server.word.repository.CollectedWordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;
    @Mock
    private QuizSessionRepository quizSessionRepository;
    @Mock
    private CollectedWordRepository collectedWordRepository;

    @Mock
    private LearningHistoryRepository learningHistoryRepository;
    @Mock
    private UserBadgeRepository userBadgeRepository;

    /**
     * 테스트용 기본 유저 생성
     */
    private User createTestUser(Long userId) {
        User user = User.builder()
                .email("test@clip.com")
                .name("테스트유저")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "level", 5);
        ReflectionTestUtils.setField(user, "exp", 1000);
        return user;
    }

    /**
     * 학습 안 한 상태로 모든 Mock 기본 설정 (성장 지표용)
     */
    private void mockNoLearning(Long userId) {
        // 기존 퀴즈/단어 통계 Mock 유지
    }

    @Nested
    @DisplayName("프로필 조회 (showProfile)")
    class ShowProfile {

        @Test
        @DisplayName("성공: 존재하는 유저 ID로 조회하면 경험치 및 포맷팅된 가입일, 마스터리 배지가 포함된 프로필을 반환한다")
        void success_showProfile() {
            // given
            Long userId = 1L;

            User user = User.builder()
                    .email("senior@clip.com")
                    .name("개발자")
                    .profileImageUrl("https://image.clip.com/profile.png")
                    .build();

            ReflectionTestUtils.setField(user, "id", userId);
            ReflectionTestUtils.setField(user, "level", 1);
            ReflectionTestUtils.setField(user, "exp", 2000);
            ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 5, 19, 0, 0));

            Video mockVideo = Video.builder()
                    .videoId("dQw4w9WgXcQ")
                    .title("Contrary to popular belief...")
                    .thumbnailUrl("https://yt3...")
                    .duration(863) // 14분 23초
                    .channelName("BBC Learning English")
                    .channelProfileImageUrl("https://yt3_profile...")
                    .build();

            LearningHistory mockHistory = LearningHistory.builder()
                    .video(mockVideo)
                    .build();

            UserBadge mockBadge = UserBadge.builder()
                    .badgeType(BadgeType.SILVER)
                    .build();

            // Mocking 정의
            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(learningHistoryRepository.findFirstByUserOrderByLastAccessAtDesc(user))
                    .willReturn(Optional.of(mockHistory));
            given(userBadgeRepository.findTopByUserIdAndVideo_VideoIdOrderByEarnedAtDesc(userId, "dQw4w9WgXcQ"))
                    .willReturn(Optional.of(mockBadge));

            // when
            UserProfileResponse response = userService.showProfile(userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(userId);
            assertThat(response.getEmail()).isEqualTo("senior@clip.com");
            assertThat(response.getLevel()).isEqualTo(1);


            assertThat(response.getCreatedAt()).isEqualTo("2026-05-19");

            assertThat(response.getOngoingMastery()).isNotNull();
            assertThat(response.getOngoingMastery().getVideoId()).isEqualTo("dQw4w9WgXcQ");
            assertThat(response.getOngoingMastery().getVideoDuration()).isEqualTo("14:23"); // 포맷팅 메서드 결과식 검증
            assertThat(response.getOngoingMastery().getCurrentBadge()).isEqualTo("SILVER");

            verify(userRepository).findById(userId);
            verify(learningHistoryRepository).findFirstByUserOrderByLastAccessAtDesc(user);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저 ID로 조회하면 USER_NOT_FOUND 예외가 발생한다")
        void fail_showProfile_userNotFound() {
            // given
            Long nonExistentUserId = 999L;
            given(userRepository.findById(nonExistentUserId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userService.showProfile(nonExistentUserId))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(exception -> {
                        BusinessException businessException = (BusinessException) exception;
                        assertThat(businessException.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_FOUND);
                    });

            verify(userRepository).findById(nonExistentUserId);
        }
    }

        @Nested
        @DisplayName("평균 정확도 계산")
        class AverageAccuracy {

            @Test
            @DisplayName("성공: 정답 16/총 20인 경우 80%를 반환한다")
            void success_calculate80Percent() {
                // given
                Long userId = 1L;
                User user = createTestUser(userId);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(quizSessionRepository.sumTotalQuizCount(userId)).willReturn(20L);
                given(quizSessionRepository.sumCorrectCount(userId)).willReturn(16L);
                mockNoLearning(userId);

                // when
                UserGrowthResponse response = userService.showGrowth(userId);

                // then
                assertThat(response.getAverageAccuracy()).isEqualTo(80);
            }

            @Test
            @DisplayName("성공: 퀴즈 기록이 없으면 0을 반환한다")
            void success_noQuizReturnsZero() {
                // given
                Long userId = 1L;
                User user = createTestUser(userId);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(quizSessionRepository.sumTotalQuizCount(userId)).willReturn(0L);
                given(quizSessionRepository.sumCorrectCount(userId)).willReturn(0L);
                mockNoLearning(userId);

                // when
                UserGrowthResponse response = userService.showGrowth(userId);

                // then
                assertThat(response.getAverageAccuracy()).isEqualTo(0);
            }

            @Test
            @DisplayName("성공: 정답 16/총 19 (84.21%)는 반올림되어 84를 반환한다")
            void success_roundingDown() {
                // given
                Long userId = 1L;
                User user = createTestUser(userId);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(quizSessionRepository.sumTotalQuizCount(userId)).willReturn(19L);
                given(quizSessionRepository.sumCorrectCount(userId)).willReturn(16L);
                mockNoLearning(userId);

                // when
                UserGrowthResponse response = userService.showGrowth(userId);

                // then
                assertThat(response.getAverageAccuracy()).isEqualTo(84);
            }

            @Test
            @DisplayName("성공: 모두 정답인 경우 100을 반환한다")
            void success_allCorrect() {
                // given
                Long userId = 1L;
                User user = createTestUser(userId);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(quizSessionRepository.sumTotalQuizCount(userId)).willReturn(20L);
                given(quizSessionRepository.sumCorrectCount(userId)).willReturn(20L);
                mockNoLearning(userId);

                // when
                UserGrowthResponse response = userService.showGrowth(userId);

                // then
                assertThat(response.getAverageAccuracy()).isEqualTo(100);
            }
        }

    @Nested
    @DisplayName("연속 학습 일수 계산")
    class Streak {

        @Test
        @DisplayName("성공: 오늘과 어제 모두 학습 안 했으면 streak는 0이다")
        void success_noLearningReturnsZero() {
            // given
            Long userId = 1L;
            User user = createTestUser(userId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(quizSessionRepository.sumTotalQuizCount(userId)).willReturn(0L);
            given(quizSessionRepository.sumCorrectCount(userId)).willReturn(0L);

            // 모든 날 학습 안 함
            given(quizSessionRepository.existsCompletedQuizToday(eq(userId), any(), any()))
                    .willReturn(false);
            given(collectedWordRepository.existsCollectedWordToday(eq(userId), any(), any()))
                    .willReturn(false);

            given(quizSessionRepository.sumTotalQuizCountBetween(eq(userId), any(), any()))
                    .willReturn(0L);
            given(quizSessionRepository.sumCorrectCountBetween(eq(userId), any(), any()))
                    .willReturn(0L);

            // when
            UserGrowthResponse response = userService.showGrowth(userId);

            // then
            assertThat(response.getStreak()).isEqualTo(0);
        }

        @Test
        @DisplayName("성공: 매일 학습했으면 streak는 1 이상이다")
        void success_everyDayLearning() {
            // given
            Long userId = 1L;
            User user = createTestUser(userId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(quizSessionRepository.sumTotalQuizCount(userId)).willReturn(0L);
            given(quizSessionRepository.sumCorrectCount(userId)).willReturn(0L);

            // 매일 학습 (퀴즈)
            given(quizSessionRepository.existsCompletedQuizToday(eq(userId), any(), any()))
                    .willReturn(true);
            given(quizSessionRepository.sumTotalQuizCountBetween(eq(userId), any(), any()))
                    .willReturn(0L);
            given(quizSessionRepository.sumCorrectCountBetween(eq(userId), any(), any()))
                    .willReturn(0L);

            // when
            UserGrowthResponse response = userService.showGrowth(userId);

            // then
            // 9999일까지 카운트하다가 break (안전 장치)
            assertThat(response.getStreak()).isGreaterThanOrEqualTo(1);
        }

        @Test
        @DisplayName("성공: 단어 수집만 매일 했어도 streak는 1 이상이다")
        void success_onlyCollectedWord() {
            // given
            Long userId = 1L;
            User user = createTestUser(userId);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(quizSessionRepository.sumTotalQuizCount(userId)).willReturn(0L);
            given(quizSessionRepository.sumCorrectCount(userId)).willReturn(0L);

            // 퀴즈는 X, 단어 수집만 O
            given(quizSessionRepository.existsCompletedQuizToday(eq(userId), any(), any()))
                    .willReturn(false);
            given(collectedWordRepository.existsCollectedWordToday(eq(userId), any(), any()))
                    .willReturn(true);

            given(quizSessionRepository.sumTotalQuizCountBetween(eq(userId), any(), any()))
                    .willReturn(0L);
            given(quizSessionRepository.sumCorrectCountBetween(eq(userId), any(), any()))
                    .willReturn(0L);

            // when
            UserGrowthResponse response = userService.showGrowth(userId);

            // then
            assertThat(response.getStreak()).isGreaterThanOrEqualTo(1);
        }
    }

        @Nested
        @DisplayName("주간 정확도 계산")
        class WeeklyAccuracy {

            @Test
            @DisplayName("성공: 4주치 데이터가 정상적으로 반환된다")
            void success_returns4Weeks() {
                // given
                Long userId = 1L;
                User user = createTestUser(userId);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(quizSessionRepository.sumTotalQuizCount(userId)).willReturn(0L);
                given(quizSessionRepository.sumCorrectCount(userId)).willReturn(0L);
                given(quizSessionRepository.existsCompletedQuizToday(eq(userId), any(), any()))
                        .willReturn(false);
                given(collectedWordRepository.existsCollectedWordToday(eq(userId), any(), any()))
                        .willReturn(false);

                // 모든 주에 동일하게 80% (8/10)
                given(quizSessionRepository.sumTotalQuizCountBetween(eq(userId), any(), any()))
                        .willReturn(10L);
                given(quizSessionRepository.sumCorrectCountBetween(eq(userId), any(), any()))
                        .willReturn(8L);

                // when
                UserGrowthResponse response = userService.showGrowth(userId);

                // then
                UserGrowthResponse.WeeklyAccuracyInfo weekly = response.getWeeklyAccuracy();
                assertThat(weekly.getWeeklyData()).hasSize(4);
                assertThat(weekly.getWeeklyData().get(0).getWeek()).isEqualTo("3주 전");
                assertThat(weekly.getWeeklyData().get(1).getWeek()).isEqualTo("2주 전");
                assertThat(weekly.getWeeklyData().get(2).getWeek()).isEqualTo("저번 주");
                assertThat(weekly.getWeeklyData().get(3).getWeek()).isEqualTo("이번 주");

                // 모두 80%
                assertThat(weekly.getWeeklyData().get(0).getAccuracy()).isEqualTo(80);
                assertThat(weekly.getWeeklyData().get(3).getAccuracy()).isEqualTo(80);
                assertThat(weekly.getThisWeek()).isEqualTo(80);
                assertThat(weekly.getLastWeek()).isEqualTo(80);
                assertThat(weekly.getGrowthRate()).isEqualTo(0);
            }

            @Test
            @DisplayName("성공: 주간 데이터가 전혀 없으면 모두 0%로 반환된다")
            void success_noDataReturnsZero() {
                // given
                Long userId = 1L;
                User user = createTestUser(userId);

                given(userRepository.findById(userId)).willReturn(Optional.of(user));
                given(quizSessionRepository.sumTotalQuizCount(userId)).willReturn(0L);
                given(quizSessionRepository.sumCorrectCount(userId)).willReturn(0L);
                mockNoLearning(userId);

                // when
                UserGrowthResponse response = userService.showGrowth(userId);

                // then
                UserGrowthResponse.WeeklyAccuracyInfo weekly = response.getWeeklyAccuracy();
                assertThat(weekly.getThisWeek()).isEqualTo(0);
                assertThat(weekly.getLastWeek()).isEqualTo(0);
                assertThat(weekly.getGrowthRate()).isEqualTo(0);
                assertThat(weekly.getWeeklyData()).hasSize(4);
                assertThat(weekly.getWeeklyData())
                        .allSatisfy(data -> assertThat(data.getAccuracy()).isEqualTo(0));
            }

    }
}
