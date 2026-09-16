package com.clip.server.video.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.entity.preference.AbsoluteLevel;
import com.clip.server.user.entity.preference.DifficultyLevel;
import com.clip.server.user.entity.preference.LearningGoal;
import com.clip.server.user.entity.preference.UserPreference;
import com.clip.server.user.repository.LearningHistoryRepository;
import com.clip.server.user.repository.UserPreferenceRepository;
import com.clip.server.video.dto.ai.response.AIRecommendResponse;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * VideoRecommendationService 단위 테스트
 *
 * Phase 7: AI Agent 기반 추천 시스템 테스트
 * - Rule-based → AI 서버 위임
 * - AIRecommendationClient Mock
 * - Video 자동 등록 검증
 */
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

    @Mock
    private AIRecommendationClient aiRecommendationClient;

    @Test
    @DisplayName("성공: AI 서버가 반환한 Top 6 영상을 Video 테이블 조회/등록하여 반환한다.")
    void getRecommendedVideos_Success() {
        // given
        Long userId = 1L;

        // 1. 유저 설정 모킹
        UserPreference userPreference = UserPreference.builder()
                .learningGoal(LearningGoal.TRAVEL)
                .build();
        userPreference.update(LearningGoal.TRAVEL, DifficultyLevel.CURRENT, AbsoluteLevel.INTERMEDIATE);

        given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.of(userPreference));

        // 2. 시청 이력 모킹
        given(learningHistoryRepository.findVideoByUserId(userId))
                .willReturn(List.of("WATCHED_VIDEO_1", "WATCHED_VIDEO_2"));

        // 3. AI 서버 응답 모킹
        List<AIRecommendResponse.RecommendedVideo> aiRecommendations = List.of(
                createAIVideo("AI_VIDEO_1", 1, "당신의 여행 학습 목표에 완벽한 영상입니다.", 100.0),
                createAIVideo("AI_VIDEO_2", 2, "중급 수준에서 도전해볼만한 영상입니다.", 90.0),
                createAIVideo("AI_VIDEO_3", 3, "여행 회화에 유용한 표현들을 배울 수 있습니다.", 80.0),
                createAIVideo("AI_VIDEO_4", 4, "실제 여행 상황에서 쓸 수 있는 표현입니다.", 70.0),
                createAIVideo("AI_VIDEO_5", 5, "여행 초보자에게 추천하는 영상입니다.", 60.0),
                createAIVideo("AI_VIDEO_6", 6, "여행 어휘를 확장하는데 도움이 됩니다.", 50.0)
        );

        AIRecommendResponse aiResponse = new AIRecommendResponse(
                aiRecommendations,
                "여행 초급자를 위한 개인화된 학습 콘텐츠",
                45,      // totalCandidates
                2,       // excludedCount
                15000,   // processingTimeMs
                3500,    // tokensUsed
                1.0,     // qualityScore
                0        // retryCount
        );
        given(aiRecommendationClient.getFullRecommendations(any())).willReturn(aiResponse);

        // 4. Video 테이블 조회 모킹 - 일부는 이미 있음, 일부는 신규
        given(videoRepository.findByVideoId("AI_VIDEO_1")).willReturn(Optional.of(createExistingVideo("AI_VIDEO_1")));
        given(videoRepository.findByVideoId("AI_VIDEO_2")).willReturn(Optional.empty());  // 신규 등록
        given(videoRepository.findByVideoId("AI_VIDEO_3")).willReturn(Optional.of(createExistingVideo("AI_VIDEO_3")));
        given(videoRepository.findByVideoId("AI_VIDEO_4")).willReturn(Optional.empty());  // 신규 등록
        given(videoRepository.findByVideoId("AI_VIDEO_5")).willReturn(Optional.of(createExistingVideo("AI_VIDEO_5")));
        given(videoRepository.findByVideoId("AI_VIDEO_6")).willReturn(Optional.empty());  // 신규 등록

        given(videoRepository.save(any(Video.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        VideoRecommendationResponse response = videoRecommendationService.getRecommendedVideos(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRecommendations()).hasSize(6);

        // AI가 생성한 개인화 이유가 포함되어야 함
        assertThat(response.getRecommendations().get(0).getRecommendationReason())
                .contains("여행 학습 목표");
        assertThat(response.getRecommendations().get(1).getRecommendationReason())
                .contains("중급 수준");

        // 각 메서드 호출 검증
        verify(userPreferenceRepository).findByUserId(userId);
        verify(learningHistoryRepository).findVideoByUserId(userId);
        verify(aiRecommendationClient).getFullRecommendations(any());
    }

    @Test
    @DisplayName("성공: 학습 목표가 NONE인 경우에도 AI가 난이도 기반 추천을 제공한다.")
    void getRecommendedVideos_Success_WhenGoalIsNone() {
        // given
        Long userId = 1L;
        UserPreference userPreference = UserPreference.builder().build();
        userPreference.update(LearningGoal.NONE, DifficultyLevel.CURRENT, AbsoluteLevel.BEGINNER);

        given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.of(userPreference));
        given(learningHistoryRepository.findVideoByUserId(userId)).willReturn(List.of());

        List<AIRecommendResponse.RecommendedVideo> aiRecommendations = List.of(
                createAIVideo("NONE_VIDEO_1", 1, "초급 수준에 적합한 일반 영어 학습 영상입니다.", 100.0),
                createAIVideo("NONE_VIDEO_2", 2, "다양한 주제로 영어를 배울 수 있습니다.", 90.0),
                createAIVideo("NONE_VIDEO_3", 3, "초보자를 위한 실용 영어 콘텐츠입니다.", 80.0),
                createAIVideo("NONE_VIDEO_4", 4, "일상 대화에 유용한 표현들입니다.", 70.0),
                createAIVideo("NONE_VIDEO_5", 5, "기초 영어 실력 향상에 도움이 됩니다.", 60.0),
                createAIVideo("NONE_VIDEO_6", 6, "쉬운 영어로 시작하는 학습 영상입니다.", 50.0)
        );

        AIRecommendResponse aiResponse = new AIRecommendResponse(
                aiRecommendations,
                "초급자를 위한 일반 영어 학습 콘텐츠",
                40,      // totalCandidates
                0,       // excludedCount
                14000,   // processingTimeMs
                3200,    // tokensUsed
                1.0,     // qualityScore
                0        // retryCount
        );

        given(aiRecommendationClient.getFullRecommendations(any())).willReturn(aiResponse);
        given(videoRepository.findByVideoId(any())).willReturn(Optional.empty());
        given(videoRepository.save(any(Video.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        VideoRecommendationResponse response = videoRecommendationService.getRecommendedVideos(userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRecommendations()).hasSize(6);
        assertThat(response.getRecommendations().get(0).getRecommendationReason())
                .contains("초급 수준");

        verify(aiRecommendationClient).getFullRecommendations(any());
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

    @Test
    @DisplayName("예외: AI 서버 호출 실패 시 BusinessException을 던진다.")
    void getRecommendedVideos_ThrowsException_WhenAIServerFails() {
        // given
        Long userId = 1L;
        UserPreference userPreference = UserPreference.builder().build();
        userPreference.update(LearningGoal.TRAVEL, DifficultyLevel.CURRENT, AbsoluteLevel.INTERMEDIATE);

        given(userPreferenceRepository.findByUserId(userId)).willReturn(Optional.of(userPreference));
        given(learningHistoryRepository.findVideoByUserId(userId)).willReturn(List.of());
        given(aiRecommendationClient.getFullRecommendations(any()))
                .willThrow(new BusinessException(ErrorCode.AI_SERVER_ERROR));

        // when & then
        assertThatThrownBy(() -> videoRecommendationService.getRecommendedVideos(userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.AI_SERVER_ERROR.getMessage());
    }

    // ─────────────────────────────────────────────────────────
    // 헬퍼 메서드
    // ─────────────────────────────────────────────────────────

    /**
     * AI 응답용 RecommendedVideo 생성 (Record 생성자 사용)
     */
    private AIRecommendResponse.RecommendedVideo createAIVideo(
            String videoId, int rank, String reason, double relevanceScore) {
        return new AIRecommendResponse.RecommendedVideo(
                videoId,                                                    // videoId
                rank,                                                       // rank
                reason,                                                     // reason
                relevanceScore,                                             // relevanceScore
                "Test Title for " + videoId,                                // title
                "UC_TEST_CHANNEL",                                          // channelId
                "Test Channel",                                             // channelName
                "https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg",    // thumbnailUrl
                "Test description",                                         // description
                "2024-01-01T00:00:00Z"                                     // publishedAt
        );
    }

    /**
     * 이미 DB에 존재하는 Video 엔티티 생성
     */
    private Video createExistingVideo(String videoId) {
        return Video.builder()
                .videoId(videoId)
                .title("Existing Title for " + videoId)
                .thumbnailUrl("https://i.ytimg.com/vi/" + videoId + "/hqdefault.jpg")
                .duration(480)
                .channelName("Existing Channel")
                .channelProfileImageUrl("https://channel-profile.jpg")
                .learningGoal(LearningGoal.TRAVEL)
                .difficultyLevel(VideoDifficulty.INTERMEDIATE)
                .build();
    }
}
