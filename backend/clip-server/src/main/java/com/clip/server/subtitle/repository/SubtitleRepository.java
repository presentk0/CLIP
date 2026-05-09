package com.clip.server.subtitle.repository;

import com.clip.server.subtitle.entity.Subtitle;
import com.clip.server.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SubtitleRepository extends JpaRepository<Subtitle, Long> {
    Optional<Subtitle> findByVideoAndStartTimeAndText(Video video, Double startTime, String text);
    List<Subtitle> findByVideo_VideoIdOrderByStartTimeAsc(String videoId);
    List<Subtitle> findByVideoAndStartTimeBetween(Video video, Double start, Double end);
    List<Subtitle> findAllByVideo(Video video);
}
