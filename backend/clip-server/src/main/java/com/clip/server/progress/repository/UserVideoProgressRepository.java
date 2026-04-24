package com.clip.server.progress.repository;

import com.clip.server.progress.entity.UserVideoProgress;
import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserVideoProgressRepository extends JpaRepository<UserVideoProgress, Long> {

    Optional<UserVideoProgress> findByUserAndVideo(User user, Video video);
}
