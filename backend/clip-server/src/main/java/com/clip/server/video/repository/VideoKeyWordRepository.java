package com.clip.server.video.repository;

import com.clip.server.video.entity.Video;
import com.clip.server.video.entity.VideoKeyWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VideoKeyWordRepository extends JpaRepository<VideoKeyWord, Long> {
    List<VideoKeyWord> findAllByVideo(Video video);
}
