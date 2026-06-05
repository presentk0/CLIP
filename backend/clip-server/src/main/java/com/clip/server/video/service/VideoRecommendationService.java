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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoRecommendationService {

    private final VideoRepository videoRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final LearningHistoryRepository learningHistoryRepository;

    private static final int CANDIDATE_LIMIT = 20;
    private static final int RECOMMEND_LIMIT = 5;

    public VideoRecommendationResponse getRecommendedVideos(Long userId) {

        // 1. 유저 학습 설정 조회 (유저 검증 + 데이터 조회)
        UserPreference userPreference = userPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        // 2. 절대 레벨 + 상대 페이스 → 타겟 난이도 계산
        VideoDifficulty target = calculateTargetDifficulty(
                userPreference.getUserAbsoluteLevel(),
                userPreference.getDifficultyLevel()
        );

        // 3. 추천 후보 난이도 범위
        List<VideoDifficulty> recommendationRange = target.getRecommendationRange();

        // 4. 시청 이력 제외 목록 처리
        List<String> watchedVideoIds = learningHistoryRepository.findVideoByUserId(userId);
        if (watchedVideoIds.isEmpty()) {
            watchedVideoIds = List.of("DUMMY_GUARD");
        }

        // 학습 목표가 NONE(없음)인 경우의 예외 처리 분기
        List<Video> candidates;

        if (userPreference.getLearningGoal() == LearningGoal.NONE) {
            candidates = videoRepository.findRecommendedVideos(
                    null, //  학습 목표 필터 해제
                    recommendationRange,
                    watchedVideoIds,
                    PageRequest.of(0, CANDIDATE_LIMIT)
            );
        } else {
            candidates = videoRepository.findRecommendedVideos(
                    userPreference.getLearningGoal(),
                    recommendationRange,
                    watchedVideoIds,
                    PageRequest.of(0, CANDIDATE_LIMIT)
            );
        }

        // 6. 점수 계산 → 정렬 → 상위 N개 → DTO 변환
        List<VideoRecommendationResponse.Recommendation> list = candidates.stream()
                .sorted((v1, v2) -> Double.compare(
                        calculateRelevanceScore(v2, userPreference, target),
                        calculateRelevanceScore(v1, userPreference, target)
                ))
                .limit(RECOMMEND_LIMIT)
                .map(video -> {
                    String reason = generateReason(video, userPreference, target);
                    return mapToVideoRecommendationResponse(video, reason);
                })
                .toList();

        return VideoRecommendationResponse.builder()
                .recommendations(list)
                .build();
    }

    /**
     * 영상 매칭 점수 계산
     */
    private double calculateRelevanceScore(
            Video video,
            UserPreference pref,
            VideoDifficulty target) {

        double score = 0.0;

        if (video.getLearningGoal() == pref.getLearningGoal()) {
            score += 60.0;
        }

        VideoDifficulty videoDiff = video.getDifficultyLevel();
        if (videoDiff == target) {
            score += 40.0;
        } else {
            int diff = Math.abs(videoDiff.getLevelCode() - target.getLevelCode());
            if (diff == 1) {
                score += 20.0;
            }
        }

        score += Math.random();

        return score;
    }

    /**
     * 추천 사유 동적 생성
     */
    private String generateReason(
            Video video,
            UserPreference pref,
            VideoDifficulty target) {

        boolean goalMatch = video.getLearningGoal() == pref.getLearningGoal();
        boolean diffMatch = video.getDifficultyLevel() == target;

        if (goalMatch && diffMatch) {
            return String.format("'%s' 학습 목표와 %s 수준에 딱 맞는 영상이에요",
                    pref.getLearningGoal().getDescription(),
                    video.getDifficultyLevel().getDescription());
        }

        if (goalMatch) {
            VideoDifficulty videoDiff = video.getDifficultyLevel();
            if (videoDiff.getLevelCode() < target.getLevelCode()) {
                return String.format("'%s' 학습에 부담 없이 시작할 수 있는 영상이에요",
                        pref.getLearningGoal().getDescription());
            } else {
                return String.format("'%s' 학습에 한 단계 도전해볼만한 영상이에요",
                        pref.getLearningGoal().getDescription());
            }
        }

        return String.format("%s 수준의 추천 영상이에요",
                video.getDifficultyLevel().getDescription());
    }

    /**
     * DTO 변환 메서드
     */
    private VideoRecommendationResponse.Recommendation mapToVideoRecommendationResponse(Video video, String reason) {
        return VideoRecommendationResponse.Recommendation.builder()
                .videoId(video.getVideoId())
                .title(video.getTitle())
                .thumbnailUrl(video.getThumbnailUrl())
                .duration(video.getDuration() != null ? video.getDuration() : 0)
                .channelName(video.getChannelName())
                .channelProfileUrl(video.getChannelProfileImageUrl())
                .recommendationReason(reason)
                .estimatedDifficulty(video.getDifficultyLevel().name())
                .build();
    }

    /**
     * 유저의 AbsoluteLevel + DifficultyLevel → 타겟 영상 난이도
     */
    private VideoDifficulty calculateTargetDifficulty(
            AbsoluteLevel userAbsoluteLevel,
            DifficultyLevel relativeDiff) {

        int targetScore = userAbsoluteLevel.getScore();

        switch (relativeDiff) {
            case RELAXED:
            case EASIER:
                targetScore -= 1;
                break;
            case CURRENT:
                break;
            case HARDER:
                targetScore += 1;
                break;
            case MAX:
                targetScore += 2;
                break;
        }

        if (targetScore <= 1) return VideoDifficulty.BEGINNER;
        if (targetScore == 2) return VideoDifficulty.INTERMEDIATE;
        return VideoDifficulty.ADVANCED;
    }
}