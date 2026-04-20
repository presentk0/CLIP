package com.clip.server.subtitle.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.subtitle.dto.request.SubtitleRequest;
import com.clip.server.subtitle.dto.respnse.SubtitleDetailResponse;
import com.clip.server.subtitle.dto.respnse.SubtitleListResponse;
import com.clip.server.subtitle.dto.respnse.SubtitleResponse;
import com.clip.server.subtitle.entity.Subtitle;
import com.clip.server.subtitle.repository.SubtitleRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static com.clip.server.common.exception.ErrorCode.USER_NOT_FOUND;
import static com.clip.server.common.exception.ErrorCode.VIDEO_NOT_FOUND;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubtitleService {

    private final SubtitleRepository subtitleRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;

    @Transactional
    public SubtitleResponse saveSubtitle(Long userId, String videoId, SubtitleRequest subtitleRequest) {
        // 1. 유저 정보 확인
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(USER_NOT_FOUND));

        // 2. Video 정보 확인 없으면 생성
        Video video = videoRepository.findById(videoId).
                orElseGet(()->{
                    Video newVideo = Video.builder()
                            .videoId(videoId)
                            .title(subtitleRequest.getTitle())
                            .build();
                    return videoRepository.save(newVideo);
                }
        );

        // 3. 자막 중복 체크(startTime 중복 여부 확인)
        Optional<Subtitle> existingSubtitle = subtitleRepository.findByVideoAndStartTime(video, subtitleRequest.getStartTime());

        if(existingSubtitle.isPresent()) {
            return mapToSubtitleResponse(existingSubtitle.get());
        }

        Subtitle subtitle = Subtitle.builder()
                .video(video)
                .text(subtitleRequest.getText())
                .translation(subtitleRequest.getTranslation())
                .startTime(subtitleRequest.getStartTime())
                .endTime(subtitleRequest.getEndTime())
                .build();
        subtitleRepository.save(subtitle);

        return mapToSubtitleResponse(subtitle);
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

       return SubtitleListResponse.builder()
               .videoId(videoId)
               .subtitles(subtitles)
               .build();
    }

    public SubtitleResponse mapToSubtitleResponse(Subtitle subtitle) {
        return SubtitleResponse.builder()
                .videoId(subtitle.getVideo().getVideoId())
                .subtitleId(subtitle.getId())
                .text(subtitle.getText())
                .translation(subtitle.getTranslation())
                .startTime(subtitle.getStartTime())
                .endTime(subtitle.getEndTime())
                .build();
    }

    public SubtitleDetailResponse mapToSubtitleDetailResponse(Subtitle subtitle) {
        return SubtitleDetailResponse.builder()
                .subtitleId(subtitle.getId())
                .text(subtitle.getText())
                .translation(subtitle.getTranslation())
                .startTime(subtitle.getStartTime())
                .endTime(subtitle.getEndTime())
                .build();
    }
}
