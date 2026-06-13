package com.clip.server.subtitle.repository;

import com.clip.server.subtitle.entity.Subtitle;
import com.clip.server.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface SubtitleRepository extends JpaRepository<Subtitle, Long> {
    Optional<Subtitle> findByVideoAndStartTimeAndText(Video video, Double startTime, String text);
    List<Subtitle> findByVideo_VideoIdOrderByStartTimeAsc(String videoId);
    List<Subtitle> findByVideoAndStartTimeBetween(Video video, Double start, Double end);
    List<Subtitle> findAllByVideo(Video video);

    /**
     * 영상의 번역 자막 조회 (NULL/빈값 제외)
     */
    @Query("SELECT s.translation FROM Subtitle s " +
            "WHERE s.video.videoId = :videoId " +
            "AND s.translation IS NOT NULL " +
            "AND s.translation <> '' " +
            "ORDER BY s.startTime ASC")
    List<String> findTranslationsByVideoId(@Param("videoId") String videoId);
}
