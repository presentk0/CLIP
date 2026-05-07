package com.clip.server.user.repository;

import com.clip.server.user.entity.User;
import com.clip.server.user.entity.learning.LearningHistory;
import com.clip.server.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LearningHistoryRepository extends JpaRepository<LearningHistory, Long> {

    Optional<LearningHistory> findByUserAndVideo(User user, Video video);
}
