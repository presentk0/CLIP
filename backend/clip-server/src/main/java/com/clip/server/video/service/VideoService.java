package com.clip.server.video.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static com.clip.server.common.exception.ErrorCode.VIDEO_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;
    private final YouTubeApiService youTubeApiService;

    /**
     * 영상 조회 또는 생성 (기본 버전 - 채널 정보 없음)
     * - 자막 저장 등 채널 정보가 불필요한 경우 사용
     */
    public Video getOrCreateVideo(String videoId, String title, Integer duration) {
        return getOrCreateVideo(videoId, title, duration, null, null);
    }

    /**
     * 영상 조회 또는 생성 (풀 버전 - 채널 정보 포함)
     * - 번역 API 등 영상 첫 등록 시 사용
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Video getOrCreateVideo(
            String videoId,
            String title,
            Integer duration,
            String channelName,
            String thumbnailUrl
    ) {
        return videoRepository.findById(videoId)
                .orElseGet(() -> {
                    // YouTube API 호출 (실패해도 OK, null 반환)
                    String channelProfileImageUrl = youTubeApiService.getChannelProfileUrl(videoId);

                    try {
                        Video newVideo = Video.builder()
                                .videoId(videoId)
                                .title(title)
                                .duration(duration)
                                .channelName(channelName)
                                .thumbnailUrl(thumbnailUrl)
                                .channelProfileImageUrl(channelProfileImageUrl)
                                .build();
                        return videoRepository.saveAndFlush(newVideo);
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Video 동시 저장 감지 - videoId: {}", videoId);
                        return videoRepository.findById(videoId)
                                .orElseThrow(() -> new BusinessException(VIDEO_NOT_FOUND));
                    }
                });
    }
}