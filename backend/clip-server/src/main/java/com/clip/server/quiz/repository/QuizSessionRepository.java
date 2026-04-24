package com.clip.server.quiz.repository;

import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizSessionRepository extends JpaRepository<QuizSession, Long> {

    Optional<QuizSession> findByUserAndVideo(User user, Video video);
}
