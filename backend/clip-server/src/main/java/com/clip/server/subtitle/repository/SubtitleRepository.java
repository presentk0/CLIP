package com.clip.server.subtitle.repository;

import com.clip.server.subtitle.entity.Subtitle;
import com.clip.server.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.util.Optional;

public interface SubtitleRepository extends JpaRepository<Subtitle, Long> {
    Optional<Subtitle> findByVideoAndStartTime(Video video, BigDecimal startTime);
}
