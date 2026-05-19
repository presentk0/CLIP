package com.clip.server.user.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.dto.response.UserProfileResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService 단위 테스트")
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Nested
    @DisplayName("프로필 조회 (showProfile)")
    class ShowProfile {

        @Test
        @DisplayName("성공: 존재하는 유저 ID로 조회하면 경험치 및 퍼센티지가 계산된 프로필을 반환한다")
        void success_showProfile() {
            // given
            Long userId = 1L;

            // 테스트용 유저 객체 생성
            User user = User.builder()
                    .email("senior@clip.com")
                    .name("개발자")
                    .profileImageUrl("https://image.clip.com/profile.png")
                    .build();

            ReflectionTestUtils.setField(user, "id", userId);
            ReflectionTestUtils.setField(user, "level", 1);
            ReflectionTestUtils.setField(user, "exp", 2000); // 1레벨(기존 누적 1500 필요)에서 500 더 쌓은 상태
            ReflectionTestUtils.setField(user, "createdAt", LocalDateTime.of(2026, 5, 19, 0, 0));

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            UserProfileResponse response = userService.showProfile(userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(userId);
            assertThat(response.getEmail()).isEqualTo("senior@clip.com");
            assertThat(response.getLevel()).isEqualTo(1);

            // 수학적 검증 (이전 단계에서 검증한 1레벨 -> 2레벨 구간 데이터)
            // 다음 레벨까지 필요한 총 누적(3,360) - 현재 누적(2,000) = 1,360
            assertThat(response.getNextLevelExp()).isEqualTo(1360);
            // 현재 레벨 구간 총량(1,860) 중 500 채웠으므로 약 26.9%
            assertThat(response.getProgressPercentage()).isEqualTo(26.9);

            assertThat(response.getCreatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 19, 0, 0));

            // 가짜 객체가 실제로 한 번 호출되었는지 검증
            verify(userRepository).findById(userId);
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
}
