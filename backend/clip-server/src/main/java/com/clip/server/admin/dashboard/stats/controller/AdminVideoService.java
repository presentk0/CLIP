package com.clip.server.admin.dashboard.stats.controller;

import com.clip.server.video.dto.response.VideoMetadata;
import com.clip.server.video.dto.request.AdminVideoRequest;
import com.clip.server.video.entity.DifficultySource;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.video.service.YouTubeApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVideoService {

    private final VideoRepository videoRepository;
    private final YouTubeApiService youTubeApiService;

    /**
     * 큐레이션 영상 등록 (메타데이터는 YouTube API로 자동 조회)
     */
    @Transactional
    public void registerCuratedVideo(AdminVideoRequest request) {
        String videoId = request.getVideoId();

        // 이미 존재하는 영상이면 → 라벨링 정보만 업데이트
        if (videoRepository.existsById(videoId)) {
            updateExistingVideo(videoId, request);
            return;
        }

        // YouTube API로 메타데이터 조회
        VideoMetadata metadata = youTubeApiService.getVideoMetadata(videoId);
        if (metadata == null) {
            throw new IllegalArgumentException("YouTube에서 영상 정보를 가져올 수 없습니다: " + videoId);
        }

        // 새 Video 저장 (수동 라벨링)
        Video video = Video.builder()
                .videoId(metadata.getVideoId())
                .title(metadata.getTitle())
                .thumbnailUrl(metadata.getThumbnailUrl())
                .duration(metadata.getDuration())
                .channelName(metadata.getChannelName())
                .channelProfileImageUrl(metadata.getChannelProfileImageUrl())
                .learningGoal(request.getLearningGoal())
                .difficultyLevel(request.getDifficulty())
                .difficultySource(DifficultySource.MANUAL)  // 수동 라벨링
                .build();

        videoRepository.save(video);
        log.info("큐레이션 영상 등록 완료: videoId={}, title={}, goal={}, difficulty={}",
                videoId, metadata.getTitle(), request.getLearningGoal(), request.getDifficulty());
    }

    /**
     * 기존 영상 라벨링 정보만 업데이트
     */
    private void updateExistingVideo(String videoId, AdminVideoRequest request) {
        Video video = videoRepository.findById(videoId).orElseThrow();
        // 별도 update 메서드 추가 필요 (Video 엔티티에)
        video.updateManualLabel(request.getLearningGoal(), request.getDifficulty());
        log.info("기존 영상 라벨링 업데이트: videoId={}", videoId);
    }
}
