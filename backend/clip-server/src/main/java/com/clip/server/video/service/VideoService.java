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

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Video getOrCreateVideo(String videoId, String title, Integer duration) {
        return videoRepository.findById(videoId)
                .orElseGet(() -> {
                    try {
                        Video newVideo = Video.builder()
                                .videoId(videoId)
                                .title(title)
                                .duration(duration)
                                .build();
                        return videoRepository.save(newVideo);
                    } catch (DataIntegrityViolationException e) {
                        log.warn("Video 동시 저장 감지 - videoId: {}, 재조회 시도", videoId);
                        return videoRepository.findById(videoId)
                                .orElseThrow(() -> new BusinessException(VIDEO_NOT_FOUND));
                    }
                });
    }
}