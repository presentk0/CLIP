package com.clip.server.subtitle.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.progress.entity.UserVideoProgress;
import com.clip.server.progress.repository.UserVideoProgressRepository;
import com.clip.server.quiz.service.KeywordExtractionService;
import com.clip.server.subtitle.dto.request.SubtitleRequest;
import com.clip.server.subtitle.dto.response.SubtitleDetailResponse;
import com.clip.server.subtitle.dto.response.SubtitleListResponse;
import com.clip.server.subtitle.dto.response.SubtitleResponse;
import com.clip.server.subtitle.entity.Subtitle;
import com.clip.server.subtitle.repository.SubtitleRepository;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.analyzer.VideoDifficultyAnalyzer;
import com.clip.server.video.entity.Video;
import com.clip.server.video.entity.VideoKeyWord;
import com.clip.server.video.repository.VideoKeyWordRepository;
import com.clip.server.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.clip.server.video.entity.DifficultySource;

import java.util.List;
import java.util.stream.Collectors;

import static com.clip.server.common.exception.ErrorCode.USER_NOT_FOUND;
import static com.clip.server.common.exception.ErrorCode.VIDEO_NOT_FOUND;

/**
 * 자막 관련 실제 비즈니스 로직을 담당하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleProcessor {

    private static final double DENSITY_FACTOR = 0.8;

    private final SubtitleRepository subtitleRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final UserVideoProgressRepository userVideoProgressRepository;
    private final VideoKeyWordRepository videoKeyWordRepository;
    private final KeywordExtractionService extractionService;
    private final VideoDifficultyAnalyzer difficultyAnalyzer;

    // ==================== 자막 저장 ====================
    @Transactional
    public SubtitleResponse saveSubtitleInternal(Long userId, String videoId, SubtitleRequest subtitleRequest) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        Video managedVideo = videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(VIDEO_NOT_FOUND));

        // 유저 진행도 조회 및 생성
        UserVideoProgress progress = userVideoProgressRepository.findByUserAndVideo(user, managedVideo)
                .orElseGet(() -> userVideoProgressRepository.save(UserVideoProgress.builder()
                        .user(user)
                        .video(managedVideo)
                        .build()));

        // 진도율 계산 및 퀴즈 트리거 체크
        int videoDuration = (managedVideo.getDuration() != null) ? managedVideo.getDuration() : 0;
        boolean isQuizGenerate = false;

        if (!subtitleRequest.getStartTime().equals(progress.getLastAddedStartTime())) {
            double userWatchDuration = subtitleRequest.getEndTime() - subtitleRequest.getStartTime();
            progress.addProgress(userWatchDuration, subtitleRequest.getStartTime());
            isQuizGenerate = checkQuizTrigger(progress, videoDuration);
        }

        // 자막 저장 (중복 체크 후 저장)
        String safeTranslation = (subtitleRequest.getTranslation() != null)
                ? subtitleRequest.getTranslation() : "";

        Subtitle subtitle = subtitleRepository
                .findByVideoAndStartTimeAndText(managedVideo, subtitleRequest.getStartTime(), subtitleRequest.getText())
                .orElseGet(() -> saveSubtitleAndKeywords(
                        managedVideo,
                        subtitleRequest.getText(),
                        safeTranslation,
                        subtitleRequest.getStartTime(),
                        subtitleRequest.getEndTime()
                ));

        return mapToSubtitleResponse(subtitle, isQuizGenerate, progress.getLastQuizzedSection());
    }

    // ==================== 벌크 저장 ====================
    @Transactional
    public void bulkSaveSubtitlesInternal(String videoId,
                                          List<TranslationRequest.SubtitleDetail> requests,
                                          List<String> translatedTexts) {

        Video video = videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(VIDEO_NOT_FOUND));

        // 자막 저장 (기존 로직)
        for (int i = 0; i < requests.size(); i++) {
            TranslationRequest.SubtitleDetail req = requests.get(i);
            String translation = translatedTexts.get(i);
            String safeTranslation = (translation != null) ? translation : "";

            if (subtitleRepository.findByVideoAndStartTimeAndText(video, req.getStartTime(), req.getText()).isEmpty()) {
                saveSubtitleAndKeywords(video, req.getText(), safeTranslation, req.getStartTime(), req.getEndTime());
            }
        }

        // 난이도 분석 (수동 라벨 보호 + 이미 분석된 영상 스킵)
        analyzeDifficultyIfNeeded(video, requests);
    }

    private void analyzeDifficultyIfNeeded(Video video, List<TranslationRequest.SubtitleDetail> requests) {
        // 수동 라벨링된 영상은 보호
        if (video.getDifficultySource() == DifficultySource.MANUAL) {
            log.info("수동 라벨링 영상 - 난이도 분석 생략: {}", video.getVideoId());
            return;
        }

        // 이미 자동 분석된 영상도 스킵 (재분석 방지)
        if (video.getDifficultySource() == DifficultySource.AUTO) {
            log.info("이미 자동 분석된 영상 - 난이도 분석 생략: {}", video.getVideoId());
            return;
        }

        try {
            int duration = (video.getDuration() != null) ? video.getDuration() : 0;

            // SubtitleDetail 리스트를 통째로 전달 (시간 정보 포함)
            VideoDifficultyAnalyzer.DifficultyResult result = difficultyAnalyzer.analyze(requests, duration);

            if (result.isValid()) {
                video.updateAutoDifficulty(result.getLevel(), result.getScore());
                log.info("난이도 자동 분석 완료 - videoId: {}, level: {}, score: {}",
                        video.getVideoId(), result.getLevel(), result.getScore());
            } else {
                // 분석 결과 부족 시 로그
                log.warn("난이도 분석 결과 부족 - videoId: {} (자막 부족 또는 매칭 단어 부족)",
                        video.getVideoId());
            }
        } catch (Exception e) {
            log.error("난이도 분석 실패 - videoId: {}", video.getVideoId(), e);
        }
    }

    // ==================== 자막 조회 ====================
    @Transactional(readOnly = true)
    public SubtitleListResponse getSubtitles(String videoId, Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        if (!videoRepository.existsById(videoId)) {
            throw new BusinessException(VIDEO_NOT_FOUND);
        }

        List<SubtitleDetailResponse> subtitles = subtitleRepository
                .findByVideo_VideoIdOrderByStartTimeAsc(videoId)
                .stream()
                .map(this::mapToSubtitleDetailResponse)
                .toList();

        if (subtitles.isEmpty()) {
            log.error("videoId: {} 에 해당하는 자막 데이터가 존재하지 않습니다.", videoId);
            throw new BusinessException(ErrorCode.SUBTITLE_NOT_FOUND);
        }

        return SubtitleListResponse.builder()
                .videoId(videoId)
                .subtitles(subtitles)
                .build();
    }

    // ==================== Private 메서드 ====================
    private Subtitle saveSubtitleAndKeywords(Video video, String text, String translation,
                                             Double startTime, Double endTime) {
        Subtitle subtitle = Subtitle.builder()
                .video(video)
                .text(text)
                .translation(translation)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        Subtitle saved = subtitleRepository.save(subtitle);

        // 키워드 추출 및 저장
        List<String> keywords = extractionService.extractKeywords(saved.getText());
        List<VideoKeyWord> videoKeyWords = keywords.stream()
                .map(word -> VideoKeyWord.builder()
                        .word(word)
                        .video(video)
                        .timestamp(saved.getStartTime())
                        .sentence(saved.getText())
                        .translation(saved.getTranslation())
                        .build())
                .collect(Collectors.toList());

        videoKeyWordRepository.saveAll(videoKeyWords);

        return saved;
    }

    private boolean checkQuizTrigger(UserVideoProgress progress, int totalDuration) {
        int totalSections = getTargetSectionCount(totalDuration);
        if (totalSections == 0) return false;

        double expectedMaxLearnedTime = (double) totalDuration * DENSITY_FACTOR;
        double progressRate = progress.getLearnedTime() / expectedMaxLearnedTime;
        int currentSection = (int) (progressRate * totalSections);

        if (currentSection > totalSections) {
            currentSection = totalSections;
        }

        if (currentSection > progress.getLastQuizzedSection()) {
            progress.updateSection(currentSection);
            return true;
        }
        return false;
    }

    private int getTargetSectionCount(int totalDuration) {
        int minutes = totalDuration / 60;

        if (minutes < 1) return 0;
        if (minutes < 10) return 1;
        if (minutes < 20) return 2;
        return 3;
    }

    private SubtitleResponse mapToSubtitleResponse(Subtitle subtitle, Boolean isQuizGenerate, int lastQuizSection) {
        int totalSections = getTargetSectionCount(subtitle.getVideo().getDuration());

        return SubtitleResponse.builder()
                .videoId(subtitle.getVideo().getVideoId())
                .subtitleId(subtitle.getId())
                .text(subtitle.getText())
                .translation(subtitle.getTranslation())
                .startTime(subtitle.getStartTime())
                .endTime(subtitle.getEndTime())
                .isQuizGenerate(isQuizGenerate)
                .section(lastQuizSection)
                .totalSections(totalSections)
                .build();
    }

    private SubtitleDetailResponse mapToSubtitleDetailResponse(Subtitle subtitle) {
        return SubtitleDetailResponse.builder()
                .subtitleId(subtitle.getId())
                .text(subtitle.getText())
                .translation(subtitle.getTranslation())
                .startTime(subtitle.getStartTime())
                .endTime(subtitle.getEndTime())
                .build();
    }
}
