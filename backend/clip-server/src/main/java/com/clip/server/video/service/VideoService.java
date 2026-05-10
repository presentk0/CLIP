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

    @Transactional
    public Video getOrCreateVideo(String videoId, String title, Integer duration) {
        // 1. 먼저 조회
        return videoRepository.findById(videoId)
                .orElseGet(() -> {
                    try {
                        // 2. 없으면 저장 시도
                        Video newVideo = Video.builder()
                                .videoId(videoId)
                                .title(title)
                                .duration(duration)
                                .build();
                        return videoRepository.saveAndFlush(newVideo);
                    } catch (DataIntegrityViolationException e) {
                        // 3. 동시 요청으로 이미 저장됐으면 재조회
                        log.warn("Video 동시 저장 감지 - videoId: {}, 재조회", videoId);
                        return videoRepository.findById(videoId)
                                .orElseThrow(() -> new BusinessException(VIDEO_NOT_FOUND));
                    }
                });
    }
}