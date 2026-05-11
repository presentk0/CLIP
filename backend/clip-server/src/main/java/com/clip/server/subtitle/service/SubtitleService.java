package com.clip.server.subtitle.service;

import com.clip.server.subtitle.dto.request.SubtitleRequest;
import com.clip.server.subtitle.dto.response.SubtitleListResponse;
import com.clip.server.subtitle.dto.response.SubtitleResponse;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.video.entity.Video;
import com.clip.server.video.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleService {

    private final VideoService videoService;
    private final SubtitleProcessor subtitleProcessor;

    /**
     * 유저가 영상 시청 시 호출 (자막 저장)
     */
    public SubtitleResponse saveSubtitle(Long userId, String videoId, SubtitleRequest subtitleRequest) {

        log.info("### [Subtitle] 저장 시작 - userId: {}, videoId: {}", userId, videoId);

        // 1. Video 생성/조회 (REQUIRES_NEW → 즉시 커밋)
        Video video = videoService.getOrCreateVideo(
                videoId,
                subtitleRequest.getTitle(),
                subtitleRequest.getDuration()
        );

        // 2. 자막 저장 (별도 Bean 호출 → @Transactional 정상 작동)
        return subtitleProcessor.saveSubtitleInternal(userId, video.getVideoId(), subtitleRequest);
    }

    /**
     * 번역 후 자막 전체 저장
     */
    public void bulkSaveSubtitles(String videoId, String title, Integer duration,
                                  List<TranslationRequest.SubtitleDetail> requests,
                                  List<String> translatedTexts) {

        log.info("### [Subtitle] 벌크 저장 시작 - videoId: {}, count: {}", videoId, requests.size());

        // 1. Video 생성/조회 (REQUIRES_NEW → 즉시 커밋)
        Video video = videoService.getOrCreateVideo(videoId, title, duration);

        // 2. 자막 벌크 저장 (별도 Bean 호출)
        subtitleProcessor.bulkSaveSubtitlesInternal(video.getVideoId(), requests, translatedTexts);
    }

    /**
     * 자막 목록 조회 (읽기 전용)
     */
    public SubtitleListResponse getSubtitles(String videoId, Long userId) {
        return subtitleProcessor.getSubtitles(videoId, userId);
    }
}