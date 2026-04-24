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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.assertThat;
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
    private VideoKeyWordRepository videoKeywordRepository;
    @Mock
    private KeywordExtractionService extractionService;


    @InjectMocks
    private SubtitleService subtitleService;
    // 테스트용 상수/객체들
    private final Long userId = 1L;
    private final String videoId = "v12345";
    private User user;
    private Video video;
    private SubtitleRequest request;
    private UserVideoProgress progress;

    public SubtitleServiceTest(VideoKeyWordRepository videoKeywordRepository) {
        this.videoKeywordRepository = videoKeywordRepository;
    }

    @BeforeEach
    void setUp() {
        user= User.builder()
                .name("테스터")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        video = Video.builder().videoId(videoId).title("테스트 영상").duration(600).build();
        request = new SubtitleRequest("title1","Hello", "안녕", 0.0, 1.5,600);
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

        // 1. given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.findById(videoId)).willReturn(Optional.empty());
        given(videoRepository.save(any(Video.class))).willReturn(video);
        given(subtitleRepository.findByVideoAndStartTimeAndText(any(),any(),any())).willReturn(Optional.empty());
        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.empty());
        given(userVideoProgressRepository.save(any())).willReturn(progress);
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

        // when
        SubtitleResponse subtitleResponse = subtitleService.saveSubtitle(userId, videoId, request);

        // then
        assertThat(subtitleResponse).isNotNull();
        assertThat(subtitleResponse.getText()).isEqualTo("Hello");

        verify(subtitleRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("중복된 자막 저장 시도 시 기존 자막을 반환한다.")
    void saveSubtitle_duplicate() {

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));

        Subtitle existingSubtitle = Subtitle.builder()
                .video(video)
                .text("이미 있는 자막")
                .translation("Already exists")
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        ReflectionTestUtils.setField(existingSubtitle, "id", 999L);

        given(subtitleRepository.findByVideoAndStartTimeAndText(any(),any(), any())).willReturn(Optional.of(existingSubtitle));

        // when
        SubtitleResponse subtitleResponse = subtitleService.saveSubtitle(userId,videoId,request);

        // then
        assertThat(subtitleResponse.getSubtitleId()).isEqualTo(999L);
        assertThat(subtitleResponse.getText()).isEqualTo("이미 있는 자막");

        verify(subtitleRepository, never()).save(any());
    }

    @Test
    @DisplayName("비디오가 DB에 없으면 해당 비디오 정보와 자막을 저장한다.")
    void saveSubtitle_withNewVideo() {
        // 1. given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.findById(videoId)).willReturn(Optional.empty());
        given(videoRepository.save(any(Video.class))).willReturn(video);
        given(userVideoProgressRepository.findByUserAndVideo(any(User.class), any(Video.class)))
                .willReturn(Optional.empty());
        given(userVideoProgressRepository.save(any(UserVideoProgress.class))).willReturn(progress);
        given(subtitleRepository.findByVideoAndStartTimeAndText(any(), any(), any())).willReturn(Optional.empty());

        Subtitle subtitle = Subtitle.builder().video(video).text(request.getText()).startTime(request.getStartTime()).build();
        given(subtitleRepository.save(any(Subtitle.class))).willReturn(subtitle);

        // 2. when
        subtitleService.saveSubtitle(userId, videoId, request);

        // 3. then
        verify(videoRepository, times(1)).save(any(Video.class));
        verify(userVideoProgressRepository, times(1)).save(any(UserVideoProgress.class));
        verify(subtitleRepository, times(1)).save(any(Subtitle.class));
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID로 저장 시도 시 예외가 발생한다.")
    void saveSubtitle_userNotFound() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then 예외가 발생하는지 검증
        assertThatThrownBy(() -> subtitleService.saveSubtitle(userId, videoId, request))
                .isInstanceOf(BusinessException.class);

        verify(videoRepository, never()).findById(any());
        verify(subtitleRepository, never()).save(any());
    }

    @Test
    @DisplayName("영상의 자막 정보를 성공적으로 조회한다.")
    void getSubtitles_success() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.existsById(videoId)).willReturn(true);

        Subtitle subtitle = Subtitle.builder()
                .video(video)
                .text("I'm happy")
                .translation("나는 행복해")
                .startTime(1.01)
                .endTime(1.02)
                .build();

        given(subtitleRepository.findByVideo_VideoIdOrderByStartTimeAsc(videoId))
                .willReturn(List.of(subtitle));

        // when
        SubtitleListResponse subtitleListResponse = subtitleService.getSubtitles(videoId, userId);

        // then
        assertThat(subtitleListResponse.getVideoId()).isEqualTo(videoId);
        assertThat(subtitleListResponse.getSubtitles().get(0).getText()).isEqualTo("I'm happy");
        assertThat(subtitleListResponse.getSubtitles()).hasSize(1);
    }

    @Test
    @DisplayName("존재하지 않는 비디오 ID로 조회하면 404 에러를 반환한다.")
    void getSubtitles_videoNotFound() {
        // given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.existsById(videoId)).willReturn(false);

        // when & then: 예외가 발생하는지 검증
        assertThatThrownBy(()->subtitleService.getSubtitles(videoId,userId)).
                isInstanceOf(BusinessException.class)
                .hasMessageContaining("영상을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("자막 저장 시 누적 학습량이 기준을 넘으면 퀴즈 트리거가 발생한다")
    void saveSubtitle_TriggerQuiz() {
        // given
        SubtitleRequest request = new SubtitleRequest("Title", "Hello", "안녕", 0.0, 170.0, 600);
        // 150초는 600초의 25% 지점 -> 섹션 1 트리거 조건

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(videoRepository.findById(anyString())).thenReturn(Optional.of(video));
        when(userVideoProgressRepository.findByUserAndVideo(any(), any())).thenReturn(Optional.of(progress));
        when(subtitleRepository.findByVideoAndStartTimeAndText(any(), any(), any())).thenReturn(Optional.empty());

        // when
        SubtitleResponse response = subtitleService.saveSubtitle(1L, video.getVideoId(), request);

        // then
        assertThat(response.getIsQuizGenerate()).isTrue();
        assertThat(progress.getLearnedTime()).isEqualTo(170.0);
        assertThat(progress.getLastQuizzedSection()).isEqualTo(1);
    }

    @Test
    @DisplayName("동일한 시작 시간의 자막이 들어오면 학습 시간은 중복으로 더해지지 않는다")
    void saveSubtitle_NoDuplicateTime() {
        // given
        progress.addProgress(100.0, 10.0); // 이미 10초 지점에서 100초 누적
        SubtitleRequest duplicateRequest = new SubtitleRequest("Title", "Second Line", "두번째 줄", 10.0, 20.0, 600);

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(user));
        when(videoRepository.findById(anyString())).thenReturn(Optional.of(video));
        when(userVideoProgressRepository.findByUserAndVideo(any(), any())).thenReturn(Optional.of(progress));

        // when
        subtitleService.saveSubtitle(1L, video.getVideoId(), duplicateRequest);

        // then
        assertThat(progress.getLearnedTime()).isEqualTo(100.0); // 10.0초 지점이라 합산 안됨
    }

//    @Test
//    @DisplayName("자막 저장 시 핵심 키워드를 추출하고 VideoKeyword 테이블에 저장한다")
//    void saveSubtitle_ExtractAndSaveKeywords() {
//        // given
//        given(userRepository.findById(userId)).willReturn(Optional.of(user));
//        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));
//        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(progress));
//        given(subtitleRepository.findByVideoAndStartTimeAndText(any(), any(), any())).willReturn(Optional.empty());
//
//        Subtitle savedSubtitle = Subtitle.builder()
//                .video(video)
//                .text(request.getText())
//                .translation(request.getTranslation())
//                .startTime(request.getStartTime())
//                .endTime(request.getEndTime())
//                .build();
//        given(subtitleRepository.save(any(Subtitle.class))).willReturn(savedSubtitle);
//
//        // OpenNLP 키워드 추출 Mock 결과 설정
//        List<String> mockKeywords = List.of("quick", "brown", "fox");
//        given(extractionService.extractKeywords(savedSubtitle.getText())).willReturn(mockKeywords);
//
//        // when
//        subtitleService.saveSubtitle(userId, videoId, request);
//
//        // then
//        // 1. extractionService가 자막 텍스트로 호출되었는지 확인
//        verify(extractionService, times(1)).extractKeywords(savedSubtitle.getText());
//
//        // 2. videoKeywordRepository.saveAll이 호출되었는지 확인
//        // 추출된 단어 개수(3개)만큼의 리스트가 저장되어야 함
//        verify(videoKeywordRepository, times(1)).saveAll(argThat(list -> {
//            List<VideoKeyWord> keywords = (List<VideoKeyWord>) list;
//            return keywords.size() == 3 &&
//                    keywords.get(0).getWord().equals("quick") &&
//                    keywords.get(0).getSentence().equals("The quick brown fox");
//        }));
//    }
//
//    @Test
//    @DisplayName("중복된 자막인 경우 키워드 추출 및 저장을 수행하지 않는다")
//    void saveSubtitle_Duplicate_NoKeywordExtraction() {
//        // given
//        given(userRepository.findById(userId)).willReturn(Optional.of(user));
//        given(videoRepository.findById(videoId)).willReturn(Optional.of(video));
//
//        Subtitle existingSubtitle = Subtitle.builder()
//                .video(video)
//                .text("이미 있는 자막")
//                .startTime(request.getStartTime())
//                .build();
//        given(subtitleRepository.findByVideoAndStartTimeAndText(any(), any(), any())).willReturn(Optional.of(existingSubtitle));
//        given(userVideoProgressRepository.findByUserAndVideo(any(), any())).willReturn(Optional.of(progress));
//
//        // when
//        subtitleService.saveSubtitle(userId, videoId, request);
//
//        // then
//        // 중복된 경우 로직이 조기 종료되므로 추출/저장 서비스가 호출되면 안 됨
//        verify(extractionService, never()).extractKeywords(anyString());
//        verify(videoKeywordRepository, never()).saveAll(any());
//    }
}
