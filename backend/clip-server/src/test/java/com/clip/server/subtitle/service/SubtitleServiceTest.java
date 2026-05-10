package com.clip.server.subtitle.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.progress.entity.UserVideoProgress;
import com.clip.server.progress.repository.UserVideoProgressRepository;
import com.clip.server.quiz.service.KeywordExtractionService;
import com.clip.server.subtitle.dto.request.SubtitleRequest;
import com.clip.server.subtitle.dto.response.SubtitleListResponse;
import com.clip.server.subtitle.dto.response.SubtitleResponse;
import com.clip.server.subtitle.entity.Subtitle;
import com.clip.server.subtitle.repository.SubtitleRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.entity.VideoKeyWord;
import com.clip.server.video.repository.VideoKeyWordRepository;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.video.service.VideoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubtitleServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private SubtitleRepository subtitleRepository;
    @Mock
    private UserVideoProgressRepository userVideoProgressRepository;
    @Mock
    private VideoKeyWordRepository videoKeyWordRepository;
    @Mock
    private KeywordExtractionService extractionService;

    @Mock
    private VideoService videoService;

    @InjectMocks
    private SubtitleService subtitleService;

    private final Long userId = 1L;
    private final String videoId = "v12345";
    private User user;
    private Video video;
    private SubtitleRequest request;
    private UserVideoProgress progress;

    @BeforeEach
    void setUp() {
        user = User.builder().name("테스터").build();
        ReflectionTestUtils.setField(user, "id", userId);

        video = Video.builder().videoId(videoId).title("테스트 영상").duration(600).build();
        request = new SubtitleRequest("title1", "The quick brown fox", "빠른 갈색 여우", 0.0, 1.5, 600);

        progress = UserVideoProgress.builder()
                .user(user)
                .video(video)
                .learnedTime(0.0)
                .lastAddedStartTime(-1.0)
                .lastQuizzedSection(0)
                .build();
    }

    @Test
    @DisplayName("영상의 자막 정보를 성공적으로 저장한다.")
    void saveSubtitle_success() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoService.getOrCreateVideo(anyString(), anyString(), anyInt())).willReturn(video);
        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(progress));
        given(subtitleRepository.findByVideoAndStartTimeAndText(any(), any(), any())).willReturn(Optional.empty());

        Subtitle subtitle = Subtitle.builder()
                .video(video)
                .text(request.getText())
                .translation(request.getTranslation())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        ReflectionTestUtils.setField(subtitle, "id", 100L);

        given(subtitleRepository.save(any(Subtitle.class))).willReturn(subtitle);
        given(extractionService.extractKeywords(anyString())).willReturn(List.of());

        // when
        SubtitleResponse subtitleResponse = subtitleService.saveSubtitle(userId, videoId, request);

        // then
        assertThat(subtitleResponse).isNotNull();
        assertThat(subtitleResponse.getText()).isEqualTo("The quick brown fox");
        verify(subtitleRepository, times(1)).save(any());
        verify(videoService, times(1)).getOrCreateVideo(any(), any(), any());
    }

    @Test
    @DisplayName("중복된 자막 저장 시도 시 기존 자막을 반환한다.")
    void saveSubtitle_duplicate() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoService.getOrCreateVideo(anyString(), anyString(), anyInt())).willReturn(video);
        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(progress));

        Subtitle existingSubtitle = Subtitle.builder()
                .video(video)
                .text("이미 있는 자막")
                .translation("Already exists")
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        ReflectionTestUtils.setField(existingSubtitle, "id", 999L);

        given(subtitleRepository.findByVideoAndStartTimeAndText(any(), any(), any())).willReturn(Optional.of(existingSubtitle));

        // when
        SubtitleResponse subtitleResponse = subtitleService.saveSubtitle(userId, videoId, request);

        // then
        assertThat(subtitleResponse.getSubtitleId()).isEqualTo(999L);
        assertThat(subtitleResponse.getText()).isEqualTo("이미 있는 자막");
        verify(subtitleRepository, never()).save(any());
    }

    @Test
    @DisplayName("자막 저장 시 누적 학습량이 기준을 넘으면 퀴즈 트리거가 발생한다")
    void saveSubtitle_TriggerQuiz() {
        // given
        SubtitleRequest triggerRequest = new SubtitleRequest("Title", "Hello", "안녕", 0.0, 310.0, 600);

        given(userRepository.findById(anyLong())).willReturn(Optional.of(user));
        given(videoService.getOrCreateVideo(anyString(), anyString(), anyInt())).willReturn(video);
        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(progress));
        given(subtitleRepository.findByVideoAndStartTimeAndText(any(), any(), any())).willReturn(Optional.empty());

        Subtitle mockSavedSubtitle = Subtitle.builder()
                .video(video)
                .text(triggerRequest.getText())
                .translation(triggerRequest.getTranslation())
                .startTime(triggerRequest.getStartTime())
                .build();
        given(subtitleRepository.save(any(Subtitle.class))).willReturn(mockSavedSubtitle);
        given(extractionService.extractKeywords(anyString())).willReturn(List.of());

        // when
        SubtitleResponse response = subtitleService.saveSubtitle(1L, video.getVideoId(), triggerRequest);

        // then
        assertThat(response.getIsQuizGenerate()).isTrue();
        assertThat(progress.getLearnedTime()).isEqualTo(310.0);
        assertThat(progress.getLastQuizzedSection()).isEqualTo(1);
    }

    @Test
    @DisplayName("자막 저장 시 핵심 키워드를 추출하고 VideoKeyWord 테이블에 저장한다")
    @SuppressWarnings("unchecked")
    void saveSubtitle_ExtractAndSaveKeywords() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoService.getOrCreateVideo(anyString(), anyString(), anyInt())).willReturn(video);
        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(progress));
        given(subtitleRepository.findByVideoAndStartTimeAndText(any(), any(), any())).willReturn(Optional.empty());

        Subtitle savedSubtitle = Subtitle.builder()
                .video(video)
                .text(request.getText())
                .translation(request.getTranslation())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        given(subtitleRepository.save(any(Subtitle.class))).willReturn(savedSubtitle);

        List<String> mockKeywords = List.of("quick", "brown", "fox");
        given(extractionService.extractKeywords(savedSubtitle.getText())).willReturn(mockKeywords);

        ArgumentCaptor<List<VideoKeyWord>> captor = ArgumentCaptor.forClass(List.class);

        // when
        subtitleService.saveSubtitle(userId, videoId, request);

        // then
        verify(extractionService, times(1)).extractKeywords(savedSubtitle.getText());
        verify(videoKeyWordRepository).saveAll(captor.capture());

        List<VideoKeyWord> capturedKeywords = captor.getValue();
        assertThat(capturedKeywords).hasSize(3);
        assertThat(capturedKeywords.get(0).getWord()).isEqualTo("quick");
    }

    @Test
    @DisplayName("영상의 자막 정보를 성공적으로 조회한다.")
    void getSubtitles_success() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.existsById(videoId)).willReturn(true);

        Subtitle subtitle = Subtitle.builder()
                .video(video)
                .text("I'm happy")
                .translation("나는 행복해")
                .startTime(1.01)
                .endTime(1.02)
                .build();

        given(subtitleRepository.findByVideo_VideoIdOrderByStartTimeAsc(videoId)).willReturn(List.of(subtitle));

        SubtitleListResponse subtitleListResponse = subtitleService.getSubtitles(videoId, userId);

        assertThat(subtitleListResponse.getVideoId()).isEqualTo(videoId);
        assertThat(subtitleListResponse.getSubtitles().get(0).getText()).isEqualTo("I'm happy");
    }

    @Test
    @DisplayName("존재하지 않는 비디오 ID로 조회하면 404 에러를 반환한다.")
    void getSubtitles_videoNotFound() {
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.existsById(videoId)).willReturn(false);

        assertThatThrownBy(() -> subtitleService.getSubtitles(videoId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("영상을 찾을 수 없습니다.");
    }
}