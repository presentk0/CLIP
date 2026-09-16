package com.clip.server.video.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.entity.preference.LearningGoal;
import com.clip.server.user.entity.preference.UserPreference;
import com.clip.server.user.repository.LearningHistoryRepository;
import com.clip.server.user.repository.UserPreferenceRepository;
import com.clip.server.video.dto.ai.request.AIRecommendRequest;
import com.clip.server.video.dto.ai.response.AIRecommendResponse;
import com.clip.server.video.dto.response.VideoRecommendationResponse;
import com.clip.server.video.entity.Video;
import com.clip.server.video.entity.VideoDifficulty;
import com.clip.server.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 영상 추천 서비스 (AI Agent 기반)
 *
 * Rule-based → AI Agent 전환:
 * - Spring: UserPreference 조회, 시청 이력 조회, DB 관리
 * - FastAPI: 프로필 분석, YouTube 검색, Top 6 선정, reason 생성
 *
 * 흐름:
 * 1. UserPreference & 시청 이력 조회
 * 2. AI 서버에 추천 요청
 * 3. AI 응답의 video_id로 Video 테이블 조회
 * 4. 없으면 자동 등록 (AI가 준 정보 활용)
 * 5. 응답 조립 및 반환
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class VideoRecommendationService {

    private final VideoRepository videoRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    private final AIRecommendationClient aiRecommendationClient;

    /**
     * 사용자 맞춤 추천 영상 조회
     *
     * @param userId 사용자 ID
     * @return Top 6 개인화 추천 영상
     */
    public VideoRecommendationResponse getRecommendedVideos(Long userId) {

        // 1. 유저 학습 설정 조회
        UserPreference userPreference = userPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        log.info("사용자 프로필: userId={}, goal={}, level={}",
                userId,
                userPreference.getLearningGoal(),
                userPreference.getUserAbsoluteLevel());

        // 2. 시청 이력 조회 (AI에게 전달할 제외 목록)
        List<String> watchedVideoIds = learningHistoryRepository.findVideoByUserId(userId);
        log.info("📺 시청 이력: {}개", watchedVideoIds.size());

        // 3. AI 서버 호출
        AIRecommendRequest aiRequest = AIRecommendRequest.builder()
                .userId(userId)
                .learningGoal(userPreference.getLearningGoal().name())
                .absoluteLevel(userPreference.getUserAbsoluteLevel().name())
                .excludedVideoIds(watchedVideoIds)
                // TODO: 나중에 개인화 데이터 추가
                .weaknesses(new ArrayList<>())
                .weakWords(new ArrayList<>())
                .collectedWords(new ArrayList<>())
                .build();

        AIRecommendResponse aiResponse = aiRecommendationClient.getFullRecommendations(aiRequest);

        log.info("AI 응답: recommendations={}, strategy=[{}]",
                aiResponse.recommendations().size(),
                aiResponse.strategy());

        // 4. AI 응답 → Video 조회/등록 → 응답 조립
        List<VideoRecommendationResponse.Recommendation> recommendations =
                aiResponse.recommendations().stream()
                        .map(this::processRecommendation)
                        .filter(Objects::nonNull)
                        .toList();

        // 최종 6개 미만이면 로그만 남기고 그대로 반환
        if (recommendations.size() < 6) {
            log.warn("최종 추천 영상이 6개 미만: {}개", recommendations.size());
        }

        return VideoRecommendationResponse.builder()
                .recommendations(recommendations)
                .build();
    }

    /**
     * AI 추천 영상 처리 (조회 or 자동 등록)
     *
     * @param aiVideo AI가 추천한 영상 정보
     * @return VideoRecommendationResponse.Recommendation
     */
    private VideoRecommendationResponse.Recommendation processRecommendation(
            AIRecommendResponse.RecommendedVideo aiVideo) {

        try {
            // Video 테이블에서 조회 or 자동 등록
            Video video = getOrCreateVideo(aiVideo);

            return VideoRecommendationResponse.Recommendation.builder()
                    .videoId(video.getVideoId())
                    .title(video.getTitle())
                    .thumbnailUrl(video.getThumbnailUrl())
                    .duration(video.getDuration() != null ? video.getDuration() : 0)
                    .channelName(video.getChannelName())
                    .channelProfileUrl(video.getChannelProfileImageUrl())
                    .recommendationReason(aiVideo.reason())      // AI가 생성한 추천 이유
                    .estimatedDifficulty(video.getDifficultyLevel().name())
                    .build();

        } catch (Exception e) {
            log.error("추천 영상 처리 실패: videoId={}, error={}",
                    aiVideo.videoId(), e.getMessage());
            return null;  // 이 영상은 스킵
        }
    }

    /**
     * Video 테이블에서 조회 or 자동 등록
     *
     * - 있으면: 기존 데이터 사용
     * - 없으면: AI가 준 정보로 신규 등록
     *
     * @param aiVideo AI가 추천한 영상
     * @return Video 엔티티
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Video getOrCreateVideo(AIRecommendResponse.RecommendedVideo aiVideo) {
        // 1. Video 테이블에서 조회
        return videoRepository.findByVideoId(aiVideo.videoId())
                .orElseGet(() -> createNewVideo(aiVideo));
    }

    /**
     * AI 정보로 신규 Video 등록
     *
     * ⚠️ 임시 처리:
     * - duration: 0 (YouTube API 재조회 필요)
     * - difficulty: INTERMEDIATE 기본값
     * - learningGoal: NONE 기본값
     *
     * 나중에 배치 작업으로 상세 정보 보강 필요
     */
    private Video createNewVideo(AIRecommendResponse.RecommendedVideo aiVideo) {
        log.info("신규 영상 자동 등록: videoId={}, title={}",
                aiVideo.videoId(), aiVideo.title());

        // 임시 처리 (나중에 배치로 보강 필요):
        // - duration: 0 → YouTube API로 재조회 필요
        // - channelProfileImageUrl: "" → YouTube channels API 필요
        // - difficultyLevel: INTERMEDIATE → Video Analyzer 필요
        log.warn("신규 영상 정보 불완전 (배치 보강 필요): videoId={}", aiVideo.videoId());

        Video newVideo = Video.builder()
                .videoId(aiVideo.videoId())
                .title(aiVideo.title())
                .thumbnailUrl(aiVideo.thumbnailUrl())
                .channelName(aiVideo.channelName())
                .channelProfileImageUrl("")  // AI가 안 줌 (임시)
                .duration(0)                  // 임시 (나중에 보강)
                .difficultyLevel(VideoDifficulty.INTERMEDIATE)  // 기본값
                .learningGoal(LearningGoal.NONE)                 // 기본값
                .build();

        return videoRepository.save(newVideo);
    }
}