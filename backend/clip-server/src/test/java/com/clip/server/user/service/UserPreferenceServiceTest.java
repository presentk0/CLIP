package com.clip.server.user.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.dto.request.InitialPreferenceRequest;
import com.clip.server.user.dto.request.UpdatePreferenceRequest;
import com.clip.server.user.dto.response.PreferenceResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.preference.AbsoluteLevel;
import com.clip.server.user.entity.preference.DifficultyLevel;
import com.clip.server.user.entity.preference.LearningGoal;
import com.clip.server.user.entity.preference.UserPreference;
import com.clip.server.user.repository.UserPreferenceRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("학습 설정 서비스(UserPreferenceService) 단위 테스트")
class UserPreferenceServiceTest {

    @InjectMocks
    private UserPreferenceService userPreferenceService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    private User createMockUser(Long id) {
        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private UserPreference createMockPreference(User user, LearningGoal goal, DifficultyLevel level, AbsoluteLevel absoluteLevel) {
        UserPreference preference = UserPreference.builder()
                .user(user)
                .learningGoal(goal)
                .difficultyLevel(level)
                .userAbsoluteLevel(absoluteLevel)
                .build();
        ReflectionTestUtils.setField(preference, "updatedAt", LocalDateTime.now());
        return preference;
    }

    @Nested
    @DisplayName("온보딩 초기 설정 (postUserPreference)")
    class PostUserPreference {

        @Test
        @DisplayName("성공: 신규 유저가 초기 설정을 등록하면 변경된 Enum 기반의 새로운 preference 엔티티가 저장된다")
        void success_new_preference() {
            // given
            Long userId = 1L;
            User user = createMockUser(userId);

            InitialPreferenceRequest request = mock(InitialPreferenceRequest.class);

            when(request.getLearningGoal()).thenReturn(LearningGoal.TRAVEL);
            when(request.getDifficultyLevel()).thenReturn(DifficultyLevel.RELAXED);
            when(request.getAbsoluteLevel()).thenReturn(AbsoluteLevel.BEGINNER);

            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userPreferenceRepository.findByUser(user)).thenReturn(Optional.empty());

            // when
            PreferenceResponse response = userPreferenceService.postUserPreference(userId, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getLearningGoal()).isEqualTo("TRAVEL");
            assertThat(response.getDifficultyLevel()).isEqualTo("RELAXED");
            assertThat(response.getAbsoluteLevel()).isEqualTo("BEGINNER");
            verify(userPreferenceRepository, times(1)).save(any(UserPreference.class));
        }

        @Test
        @DisplayName("실패: 존재하지 않는 유저 ID로 요청 시 USER_NOT_FOUND 예외가 발생한다")
        void fail_user_not_found() {
            // given
            Long userId = 999L;
            InitialPreferenceRequest request = mock(InitialPreferenceRequest.class);
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userPreferenceService.postUserPreference(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("학습 설정 부분 수정 (updateUserPreference)")
    class UpdateUserPreference {

        @Test
        @DisplayName("성공: 온보딩이 완료된 유저가 난이도만 수정(MAX)하면 변경 사항이 정상 반영된다")
        void success_partial_update() {
            // given
            Long userId = 1L;
            User user = createMockUser(userId);
            // 기존 상태: 여행 / 여유롭게
            UserPreference existingPreference = createMockPreference(user, LearningGoal.TRAVEL, DifficultyLevel.RELAXED, AbsoluteLevel.BEGINNER);

            UpdatePreferenceRequest request = mock(UpdatePreferenceRequest.class);
            when(request.getLearningGoal()).thenReturn(null);
            when(request.getDifficultyLevel()).thenReturn(DifficultyLevel.MAX); // 난이도 최고치로 상향 변경 테스트

            when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(existingPreference));

            // when
            PreferenceResponse response = userPreferenceService.updateUserPreference(userId, request);

            // then
            assertThat(response.getLearningGoal()).isEqualTo("TRAVEL"); // 기존 값 보존 확인
            assertThat(response.getDifficultyLevel()).isEqualTo("MAX"); // 새 값 정상 반영 확인
            assertThat(response.getAbsoluteLevel()).isEqualTo("BEGINNER"); // 기존값 보존 확인
            verify(userPreferenceRepository, never()).save(any());
        }

        @Test
        @DisplayName("실패: 학습 목표와 난이도가 셋 다 null로 들어오면 INVALID_INPUT_VALUE 예외가 발생한다")
        void fail_both_null_inputs() {
            // given
            Long userId = 1L;
            UpdatePreferenceRequest request = mock(UpdatePreferenceRequest.class);
            when(request.getLearningGoal()).thenReturn(null);
            when(request.getDifficultyLevel()).thenReturn(null);
            when(request.getAbsoluteLevel()).thenReturn(null);

            // when & then
            assertThatThrownBy(() -> userPreferenceService.updateUserPreference(userId, request))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    @Nested
    @DisplayName("학습 설정 조회 (getUserPreference)")
    class GetUserPreference {

        @Test
        @DisplayName("성공: 올바른 유저 ID로 조회 시 새롭게 매핑된 정보 DTO를 반환한다")
        void success_get_preference() {
            // given
            Long userId = 1L;
            User user = createMockUser(userId);
            // 가상 세팅: 비즈니스 / 지금이 좋아요
            UserPreference preference = createMockPreference(user, LearningGoal.BUSINESS, DifficultyLevel.CURRENT, AbsoluteLevel.BEGINNER);

            when(userPreferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

            // when
            PreferenceResponse response = userPreferenceService.getUserPreference(userId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getLearningGoal()).isEqualTo("BUSINESS");
            assertThat(response.getDifficultyLevel()).isEqualTo("CURRENT");
            assertThat(response.getAbsoluteLevel()).isEqualTo("BEGINNER");

        }
    }
}