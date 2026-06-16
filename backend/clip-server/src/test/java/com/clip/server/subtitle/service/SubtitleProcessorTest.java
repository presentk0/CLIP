package com.clip.server.subtitle.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.progress.entity.UserVideoProgress;
import com.clip.server.progress.repository.UserVideoProgressRepository;
import com.clip.server.quiz.ai.KeywordExtractionService;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubtitleProcessorTest {

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

    @InjectMocks
    private SubtitleProcessor subtitleProcessor;

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

        video = Video.builder()
                .videoId(videoId)
                .title("테스트 영상")
                .duration(600)
                .build();

        request = new SubtitleRequest("title1", "The quick brown fox", "빠른 갈색 여우", 0.0, 1.5, 600);

        progress = UserVideoProgress.builder()
                .user(user)
                .video(video)
                .learnedTime(0.0)
                .lastAddedStartTime(-1.0)
                .lastQuizzedSection(0)
                .build();

        lenient().when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        lenient().when(videoRepository.findById(anyString())).thenReturn(Optional.of(video));

        lenient().when(subtitleRepository.save(any(Subtitle.class))).thenAnswer(invocation -> {
            Subtitle s = invocation.getArgument(0);
            if (ReflectionTestUtils.getField(s, "id") == null) {
                ReflectionTestUtils.setField(s, "id", 100L);
            }
            return s;
        });
    }

    @Test
    @DisplayName("영상의 자막 정보를 성공적으로 저장한다")
    void saveSubtitleInternal_success() {
        // given
        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(progress));
        given(subtitleRepository.findFirstByVideoAndStartTimeAndText(any(), any(), any())).willReturn(Optional.empty());
        lenient().when(extractionService.extractKeywords(anyString())).thenReturn(new ArrayList<>());

        // when
        SubtitleResponse response = subtitleProcessor.saveSubtitleInternal(userId, videoId, request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getText()).isEqualTo("The quick brown fox");
        verify(subtitleRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("중복된 자막 저장 시도 시 기존 자막을 반환한다")
    void saveSubtitleInternal_duplicate() {
        // given
        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(progress));

        Subtitle existingSubtitle = Subtitle.builder()
                .video(video)
                .text("이미 있는 자막")
                .translation("Already exists")
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        ReflectionTestUtils.setField(existingSubtitle, "id", 999L);

        given(subtitleRepository.findFirstByVideoAndStartTimeAndText(any(), any(), any()))
                .willReturn(Optional.of(existingSubtitle));

        // when
        SubtitleResponse response = subtitleProcessor.saveSubtitleInternal(userId, videoId, request);

        // then
        assertThat(response.getSubtitleId()).isEqualTo(999L);
        assertThat(response.getText()).isEqualTo("이미 있는 자막");
        verify(subtitleRepository, never()).save(any());
    }

    @Test
    @DisplayName("자막 저장 시 누적 학습량이 기준을 넘으면 퀴즈 트리거가 발생한다")
    void saveSubtitleInternal_triggerQuiz() {
        // given
        SubtitleRequest triggerRequest = new SubtitleRequest("Title", "Hello", "안녕", 0.0, 310.0, 600);

        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(progress));
        given(subtitleRepository.findFirstByVideoAndStartTimeAndText(any(), any(), any())).willReturn(Optional.empty());
        lenient().when(extractionService.extractKeywords(anyString())).thenReturn(List.of());

        // when
        SubtitleResponse response = subtitleProcessor.saveSubtitleInternal(userId, videoId, triggerRequest);

        // then
        assertThat(response.getIsQuizGenerate()).isTrue();
        assertThat(progress.getLearnedTime()).isEqualTo(310.0);
        assertThat(progress.getLastQuizzedSection()).isEqualTo(1);
    }

    @Test
    @DisplayName("자막 저장 시 핵심 키워드를 추출하고 VideoKeyWord 테이블에 저장한다")
    @SuppressWarnings("unchecked")
    void saveSubtitleInternal_extractAndSaveKeywords() {
        // given
        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(progress));
        given(subtitleRepository.findFirstByVideoAndStartTimeAndText(any(), any(), any())).willReturn(Optional.empty());

        List<String> mockKeywords = List.of("quick", "brown", "fox");
        given(extractionService.extractKeywords(anyString())).willReturn(mockKeywords);

        ArgumentCaptor<List<VideoKeyWord>> captor = ArgumentCaptor.forClass(List.class);

        // when
        subtitleProcessor.saveSubtitleInternal(userId, videoId, request);

        // then
        verify(extractionService, times(1)).extractKeywords(anyString());
        verify(videoKeyWordRepository).saveAll(captor.capture());

        List<VideoKeyWord> capturedKeywords = captor.getValue();
        assertThat(capturedKeywords).hasSize(3);
        assertThat(capturedKeywords.get(0).getWord()).isEqualTo("quick");
    }

    @Test
    @DisplayName("진행도가 없으면 새로 생성한다")
    void saveSubtitleInternal_createNewProgress() {
        // given
        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.empty());
        given(userVideoProgressRepository.save(any(UserVideoProgress.class))).willReturn(progress);
        given(subtitleRepository.findFirstByVideoAndStartTimeAndText(any(), any(), any())).willReturn(Optional.empty());
        lenient().when(extractionService.extractKeywords(anyString())).thenReturn(new ArrayList<>());

        // when
        SubtitleResponse response = subtitleProcessor.saveSubtitleInternal(userId, videoId, request);

        // then
        assertThat(response).isNotNull();
        verify(userVideoProgressRepository).save(any(UserVideoProgress.class));
    }

    @Test
    @DisplayName("영상의 자막 정보를 성공적으로 조회한다")
    void getSubtitles_success() {
        // given
        given(videoRepository.existsById(videoId)).willReturn(true);

        Subtitle subtitle = Subtitle.builder()
                .video(video)
                .text("I'm happy")
                .translation("나는 행복해")
                .startTime(1.01)
                .endTime(1.02)
                .build();
        ReflectionTestUtils.setField(subtitle, "id", 1L);

        given(subtitleRepository.findByVideo_VideoIdOrderByStartTimeAsc(videoId)).willReturn(List.of(subtitle));

        // when
        SubtitleListResponse response = subtitleProcessor.getSubtitles(videoId, userId);

        // then
        assertThat(response.getVideoId()).isEqualTo(videoId);
        assertThat(response.getSubtitles().get(0).getText()).isEqualTo("I'm happy");
    }

    @Test
    @DisplayName("존재하지 않는 비디오 ID로 조회하면 예외가 발생한다")
    void getSubtitles_videoNotFound() {
        // given
        given(videoRepository.existsById(videoId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> subtitleProcessor.getSubtitles(videoId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("영상을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("자막이 없으면 예외가 발생한다")
    void getSubtitles_subtitleNotFound() {
        // given
        given(videoRepository.existsById(videoId)).willReturn(true);
        given(subtitleRepository.findByVideo_VideoIdOrderByStartTimeAsc(videoId)).willReturn(List.of());

        // when & then
        assertThatThrownBy(() -> subtitleProcessor.getSubtitles(videoId, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("자막");
    }
}