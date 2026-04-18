package com.clip.server.subtitle.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.subtitle.dto.request.SubtitleRequest;
import com.clip.server.subtitle.dto.respnse.SubtitleResponse;
import com.clip.server.subtitle.entity.Subtitle;
import com.clip.server.subtitle.repository.SubtitleRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
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
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class SubtitleServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private SubtitleRepository subtitleRepository;

    @InjectMocks
    private SubtitleService subtitleService;
    // 테스트용 상수/객체들
    private final Long userId = 1L;
    private final String videoId = "v12345";
    private User user;
    private Video video;
    private SubtitleRequest request;

    @BeforeEach
    void setUp() {
        user= User.builder()
                .name("테스터")
                .build();
        ReflectionTestUtils.setField(user, "id", userId);

        video = Video.builder().videoId(videoId).title("테스트 영상").build();
        request = new SubtitleRequest("titleEx","Hello", "안녕",
                BigDecimal.valueOf(0.0),
                BigDecimal.valueOf(1.5));
    }

    @Test
    @DisplayName("영상의 자막 정보를 성공적으로 저장한다.")
    void saveSubtitle_success() {

        // 1. given
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(videoRepository.findById(videoId)).willReturn(Optional.empty());
        given(videoRepository.save(any(Video.class))).willReturn(video);
        given(subtitleRepository.findByVideoAndStartTime(any(),any())).willReturn(Optional.empty());

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

        given(subtitleRepository.findByVideoAndStartTime(any(),any())).willReturn(Optional.of(existingSubtitle));

        // when
        SubtitleResponse subtitleResponse = subtitleService.saveSubtitle(userId,videoId,request);

        // then
        assertThat(subtitleResponse.getSubtitleId()).isEqualTo(999L);
        assertThat(subtitleResponse.getText()).isEqualTo("이미 있는 자막");

        verify(subtitleRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 유저 ID로 저장 시도 시 예외가 발생한다.")
    void saveSubtitle_userNotFound() {
        // Given
        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // When & Then 예외가 발생하는지 검증
        assertThatThrownBy(() -> subtitleService.saveSubtitle(userId, videoId, request))
                .isInstanceOf(BusinessException.class);

        verify(videoRepository, never()).findById(any());
        verify(subtitleRepository, never()).save(any());
    }

}
