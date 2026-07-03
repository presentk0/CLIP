package com.clip.server.video.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.entity.preference.AbsoluteLevel;
import com.clip.server.user.entity.preference.DifficultyLevel;
import com.clip.server.user.entity.preference.LearningGoal;
import com.clip.server.user.entity.preference.UserPreference;
import com.clip.server.user.repository.LearningHistoryRepository;
import com.clip.server.user.repository.UserPreferenceRepository;
import com.clip.server.video.dto.response.VideoRecommendationResponse;
import com.clip.server.video.entity.Video;
import com.clip.server.video.entity.VideoDifficulty;
import com.clip.server.video.repository.VideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VideoRecommendationServiceTest {

    @InjectMocks
    private VideoRecommendationService videoRecommendationService;

    @Mock
    private VideoRepository videoRepository;

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @Mock
    private LearningHistoryRepository learningHistoryRepository;

    @Test
    @DisplayName("성공: 유저 실력과 목표에 맞는 영상 후보들을 스코어링하여 상위 5개를 정렬해 반환한다.")
    void getRecommendedVideos_Success() {
        // given
        Long userId = 1L;

        // 유저 설정 모킹 (절대: INTERMEDIATE(2점), 상대: HARDER(+1점) -> 타겟 난이도: ADVANCED)
        UserPreference userPreference = UserPreference.builder()
                .learningGoal(LearningGoal.TRAVEL)
                .build();
        // 필요 시 리플렉션이나 엔티티 내부 메서드로 세팅 (여기선 이해를 돕기 위해 가상 게터 반환이라 가정)
        // 만약 builder에 안 들어가면 롬복 상황에 따라 아래처럼 직접 주입 또는 팩토리 메서드 활용
        userPreference.update(LearningGoal.TRAVEL, DifficultyLevel.HARDER, AbsoluteLevel.INTERMEDIATE);

        given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.of(userPreference));
        given(learningHistoryRepository.findVideoByUserId(userId)).willReturn(List.of("VIDEO_1"));

        // 후보 영상 6개 생성 (5개 제한 컷팅 테스트를 위해 6개 준비)
        List<Video> mockCandidates = new ArrayList<>();

        for (int i = 1; i <= 6; i++) {
            mockCandidates.add(Video.builder()
                    .videoId("MOCK_VIDEO_" + i)
                    .title("Test Title " + i)
                    .duration(480)
                    .learningGoal(LearningGoal.TRAVEL)
                    .difficultyLevel(VideoDifficulty.ADVANCED)
                    .build());
        }

        given(videoRepository.findRecommendedVideos(
                eq(LearningGoal.TRAVEL),
                any(),
                eq(List.of("VIDEO_1")),
                any(PageRequest.class)
        )).willReturn(mockCandidates);

        // when
        VideoRecommendationResponse response = videoRecommendationService.getRecommendedVideos(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRecommendations()).hasSize(6); // RECOMMEND_LIMIT(5) 컷팅 검증
        assertThat(response.getRecommendations().get(0).getRecommendationReason())
                .contains("딱 맞는 영상이에요"); // generateReason 동적 문구 검증

        verify(userPreferenceRepository).findByUserId(userId);
        verify(learningHistoryRepository).findVideoByUserId(userId);
    }

    @Test
    @DisplayName("성공: 유저의 학습 목표가 NONE이면 레포지토리 조회 시 목표 필터를 해제(null 전달)하고 난이도 기반으로만 추천한다.")
    void getRecommendedVideos_Success_WhenGoalIsNone() {
        // given
        Long userId = 1L;
        UserPreference userPreference = UserPreference.builder().build();
        userPreference.update(LearningGoal.NONE, DifficultyLevel.CURRENT, AbsoluteLevel.BEGINNER); // 타겟: BEGINNER

        given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.of(userPreference));
        given(learningHistoryRepository.findVideoByUserId(userId)).willReturn(List.of()); // 시청 이력 없음

        List<Video> mockCandidates = List.of(
                Video.builder()
                        .videoId("NONE_GOAL_VIDEO")
                        .title("None Goal Test Title")
                        .duration(360)
                        .learningGoal(LearningGoal.BUSINESS)
                        .difficultyLevel(VideoDifficulty.BEGINNER)
                        .build()
        );

        // 첫 번째 파라미터로 명확하게 null이 넘어가는지 검증하기 위한 eq(null) 선언
        given(videoRepository.findRecommendedVideos(
                eq(null),
                any(),
                eq(List.of("DUMMY_GUARD")), // 시청 이력 비었을 때 가드 작동 검증
                any(PageRequest.class)
        )).willReturn(mockCandidates);

        // when
        VideoRecommendationResponse response = videoRecommendationService.getRecommendedVideos(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRecommendations()).hasSize(1);
        assertThat(response.getRecommendations().get(0).getRecommendationReason())
                .contains("초급 수준의 추천 영상이에요"); // NONE 유저용 사유 분기 검증
    }

    @Test
    @DisplayName("예외: 유저의 학습 설정 데이터가 존재하지 않으면 PREFERENCE_NOT_FOUND 예외를 던진다.")
    void getRecommendedVideos_ThrowsException_WhenPreferenceNotFound() {
        // given
        Long userId = 999L;
        given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> videoRecommendationService.getRecommendedVideos(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.PREFERENCE_NOT_FOUND.getMessage());
    }
}