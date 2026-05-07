package com.clip.server.user.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.user.dto.response.BadgeInfoResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.badge.BadgeType;
import com.clip.server.user.entity.badge.UserBadge;
import com.clip.server.user.repository.UserBadgeRepository;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class BadgeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private UserBadgeRepository userBadgeRepository;

    @InjectMocks
    private BadgeService badgeService;

    private User user;
    private Video video;
    private final Long userId = 1L;
    private final String videoId = "v123";

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@clip.com")
                .name("테스터")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        video = Video.builder()
                .videoId(videoId)
                .title("테스트 영상")
                .duration(600)
                .build();
    }

    @Test
    @DisplayName("영상이 DB에 없을 경우 초기 상태(NONE, BRONZE)를 반환한다")
    void getBadge_NoVideo_ReturnDefault() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.findById(videoId)).willReturn(Optional.empty());

        // when
        BadgeInfoResponse response = badgeService.getBadge(userId, videoId);

        // then
        assertThat(response.getVideoId()).isEqualTo(videoId);
        assertThat(response.getCurrentBadge()).isEqualTo(BadgeType.NONE);
        assertThat(response.getNextBadge()).isEqualTo(BadgeType.BRONZE);
        assertThat(response.getEarnedBadges()).isEmpty();
    }

    @Test
    @DisplayName("브론즈 배지만 획득한 경우 현재 등급은 BRONZE, 다음은 SILVER를 반환한다")
    void getBadge_WithBronze_ReturnSilverAsNext() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));

        UserBadge bronzeBadge = UserBadge.builder()
                .badgeType(BadgeType.BRONZE)
                .user(user)
                .video(video)
                .build();
        ReflectionTestUtils.setField(bronzeBadge, "earnedAt", LocalDateTime.now());

        given(userBadgeRepository.findAllByUserAndVideo(user, video)).willReturn(List.of(bronzeBadge));

        // when
        BadgeInfoResponse response = badgeService.getBadge(userId, videoId);

        // then
        assertThat(response.getCurrentBadge()).isEqualTo(BadgeType.BRONZE);
        assertThat(response.getNextBadge()).isEqualTo(BadgeType.SILVER);
        assertThat(response.getEarnedBadges()).hasSize(1);
        assertThat(response.getEarnedBadges().get(0).getBadgeType()).isEqualTo(BadgeType.BRONZE);
    }

    @Test
    @DisplayName("골드까지 획득한 경우 다음 등급은 COMPLETION을 반환한다")
    void getBadge_WithGold_ReturnCompletionAsNext() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));

        UserBadge b = UserBadge.builder().badgeType(BadgeType.BRONZE).user(user).video(video).build();
        UserBadge s = UserBadge.builder().badgeType(BadgeType.SILVER).user(user).video(video).build();
        UserBadge g = UserBadge.builder().badgeType(BadgeType.GOLD).user(user).video(video).build();

        given(userBadgeRepository.findAllByUserAndVideo(user, video)).willReturn(List.of(b, s, g));

        // when
        BadgeInfoResponse response = badgeService.getBadge(userId, videoId);

        // then
        assertThat(response.getCurrentBadge()).isEqualTo(BadgeType.GOLD);
        assertThat(response.getNextBadge()).isEqualTo(BadgeType.COMPLETION);
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID로 조회 시 예외가 발생한다")
    void getBadge_UserNotFound_ThrowException() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> badgeService.getBadge(userId, videoId))
                .isInstanceOf(BusinessException.class);
    }
}