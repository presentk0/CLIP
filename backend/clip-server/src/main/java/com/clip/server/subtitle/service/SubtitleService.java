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
import com.clip.server.video.entity.Video;
import com.clip.server.video.entity.VideoKeyWord;
import com.clip.server.video.repository.VideoKeyWordRepository;
import com.clip.server.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static com.clip.server.common.exception.ErrorCode.USER_NOT_FOUND;
import static com.clip.server.common.exception.ErrorCode.VIDEO_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubtitleService {

    private static final double DENSITY_FACTOR = 0.8;

    private final SubtitleRepository subtitleRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final UserVideoProgressRepository userVideoProgressRepository;
    private final VideoKeyWordRepository videoKeyWordRepository;
    private final KeywordExtractionService extractionService;

    // 번역 후 자막 전체 저장 메서드
    @Transactional
    public void bulkSaveSubtitles(String videoId, String title, Integer duration,
                                  List<TranslationRequest.SubtitleDetail> requests,
                                  List<String> translatedTexts) {

        // 비디오 조회 또는 생성
        Video video;
        try {
            video = videoRepository.findById(videoId)
                    .orElseGet(() -> videoRepository.save(Video.builder()
                            .videoId(videoId)
                            .title(title)
                            .duration(duration)
                            .build()));
        } catch (DataIntegrityViolationException e) {
            video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new BusinessException(VIDEO_NOT_FOUND));
        }

        // final 변수로 복사
        final Video finalVideo = video;

        for (int i = 0; i < requests.size(); i++) {
            TranslationRequest.SubtitleDetail req = requests.get(i);
            String translation = translatedTexts.get(i); // 리스트에서 번역본 추출

            String safeTranslation = (translation != null) ? translation : "";
            // 중복 체크 후 저장
            if (subtitleRepository.findByVideoAndStartTimeAndText(finalVideo, req.getStartTime(), req.getText()).isEmpty()) {
                saveSubtitleAndKeywordsInternal(finalVideo, req.getText(), safeTranslation, req.getStartTime(), req.getEndTime());
            }
        }
    }

    // 유저가 영상 시청시 호출
    @Transactional
    public SubtitleResponse saveSubtitle(Long userId, String videoId, SubtitleRequest subtitleRequest) {

        // 유저 정보 확인
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        // 영상 정보 확인
        Video video;
        try {
            video = videoRepository.findById(videoId)
                    .orElseGet(() -> {
                        Video newVideo = Video.builder()
                                .videoId(videoId)
                                .title(subtitleRequest.getTitle())
                                .duration(subtitleRequest.getDuration())
                                .build();
                        return videoRepository.save(newVideo);
                    });
        } catch (DataIntegrityViolationException e) {
            video = videoRepository.findById(videoId)
                    .orElseThrow(() -> new BusinessException(VIDEO_NOT_FOUND));
        }

        // final 변수로 복사
        final Video finalVideo = video;

        // 유저 진행도 조회 없으면 생성
        UserVideoProgress progress = userVideoProgressRepository.findByUserAndVideo(user, finalVideo)
                .orElseGet(()->{
                    UserVideoProgress newProgress = UserVideoProgress.builder()
                            .user(user)
                            .video(finalVideo)
                            .build();
                    return userVideoProgressRepository.save(newProgress);
                });

        // 자막 존재 여부와 상관없이 진행도는 무조건 체크
        boolean isQuizGenerate = false;
        if (!subtitleRequest.getStartTime().equals(progress.getLastAddedStartTime())) {
            double userWatchDuration = subtitleRequest.getEndTime() - subtitleRequest.getStartTime();
            progress.addProgress(userWatchDuration, subtitleRequest.getStartTime());
            isQuizGenerate = checkoutQuizTrigger(progress, finalVideo.getDuration());
        }

        String safeTranslation = (subtitleRequest.getTranslation() != null) ? subtitleRequest.getTranslation() : "";

        // 자막 저장 체크
        Subtitle subtitle = subtitleRepository.findByVideoAndStartTimeAndText(finalVideo, subtitleRequest.getStartTime(), subtitleRequest.getText())
                .orElseGet(() -> saveSubtitleAndKeywordsInternal(
                        finalVideo,
                        subtitleRequest.getText(),
                        safeTranslation,
                        subtitleRequest.getStartTime(),
                        subtitleRequest.getEndTime()
                ));

        return mapToSubtitleResponse(subtitle, isQuizGenerate, progress.getLastQuizzedSection());
    }

    // 공통 저장 로직
    private Subtitle saveSubtitleAndKeywordsInternal(Video video, String text, String translation, Double startTime, Double endTime) {
        Subtitle subtitle = Subtitle.builder()
                .video(video)
                .text(text)
                .translation(translation)
                .startTime(startTime)
                .endTime(endTime)
                .build();
        Subtitle saved = subtitleRepository.save(subtitle);

        // 키워드 추출 및 저장
        List<String> extraKeyWords = extractionService.extractKeywords(saved.getText());
        List<VideoKeyWord> videoKeyWords = extraKeyWords.stream()
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

    private boolean checkoutQuizTrigger(UserVideoProgress progress, int totalDuration) {
        // 영상 당 섹션 수
        int totalSections = getTargetSectionCount(totalDuration);
        if(totalSections==0) return false; // 1분 미만 영상은 섹션 퀴즈 미출제

        // 보정 로직: 자막 밀도가 전체 영상의 80%
        double expectedMaxLearnedTime = (double) totalDuration * DENSITY_FACTOR;

        // 현재 누적 학습량이 어느 세션인지 확인
        double progressRate = progress.getLearnedTime()/expectedMaxLearnedTime;
        int currentSection = (int) (progressRate*totalSections);

        // 영상이 끝났다면 마지막 세션으로 고정
        if(currentSection>totalSections) currentSection =totalSections;

        // 퀴즈를 낸 섹션보다 현재 섹션이 높으면 트리거
        if(currentSection> progress.getLastQuizzedSection()) {
            progress.updateSection(currentSection);
            return true;
        }
        return false;
    }

    private int getTargetSectionCount(int totalDuration) {
        // 초-> 분 변환
        int minutes = totalDuration/60;

        if(minutes<1) return 0; // 퀴즈 섹션 0개
        if(minutes<10) return 1; // 1분 이상 10분 미만 영상은 섹션 1개
        if(minutes<20) return 2; // 10분 이상 20분미만 영상은 섹션 2개
        return 3; // 20분 이상 무조건 3개 섹션
    }

    public SubtitleListResponse getSubtitles(String videoId, Long userId) {

        // 1. 유저 정보 확인
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(USER_NOT_FOUND));

        // 영상 정보 존재 여부 확인
        if (!videoRepository.existsById(videoId)) {
            throw new BusinessException(VIDEO_NOT_FOUND);
        }

       List<SubtitleDetailResponse> subtitles = subtitleRepository.findByVideo_VideoIdOrderByStartTimeAsc(videoId)
               .stream()
               .map(this::mapToSubtitleDetailResponse)
               .toList();

        // 자막 리스트가 비어있을 경우 에러처리
        if (subtitles.isEmpty()) {
            log.error("videoId: {} 에 해당하는 자막 데이터가 존재하지 않습니다.", videoId);
            throw new BusinessException(ErrorCode.SUBTITLE_NOT_FOUND);
        }

       return SubtitleListResponse.builder()
               .videoId(videoId)
               .subtitles(subtitles)
               .build();
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
