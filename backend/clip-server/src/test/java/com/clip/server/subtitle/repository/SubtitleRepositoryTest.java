package com.clip.server.subtitle.repository;

import com.clip.server.common.config.AuditingConfig;
import com.clip.server.subtitle.entity.Subtitle;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@Import(AuditingConfig.class)
public class SubtitleRepositoryTest {

    @Autowired
    private VideoRepository videoRepository;
    @Autowired
    private SubtitleRepository subtitleRepository;

    @Test
    @DisplayName("영상의 자막 정보가 성공적으로 저정되어야 한다.")
    void saveSubtitleTest() {
        // given
        Video video = Video.builder()
                .videoId("a32e3d")
                .title("title1")
                .build();
        videoRepository.save(video);

        Subtitle subtitle = Subtitle.builder()
                .text("I'm happy")
                .translation("나는 행복합니다.")
                .startTime(1.20)
                .endTime(1.21)
                .video(video)
                .build();

        // when
        Subtitle saveSubtitle = subtitleRepository.save(subtitle);

        // then
        assertThat(saveSubtitle.getId()).isNotNull();
        assertThat(saveSubtitle.getText()).isEqualTo("I'm happy");
        assertThat(saveSubtitle.getTranslation()).isEqualTo("나는 행복합니다.");
        assertThat(saveSubtitle.getVideo().getVideoId()).isEqualTo("a32e3d");

    }
}
