package com.clip.server.user.repository;

import com.clip.server.user.entity.User;
import com.clip.server.user.entity.learning.LearningHistory;
import com.clip.server.video.entity.Video;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LearningHistoryRepository extends JpaRepository<LearningHistory, Long> {

    Optional<LearningHistory> findByUserAndVideo(User user, Video video);

    // 유저가 마지막으로 접근한 학습 이력을 영상 정보와 함께 한 건 가져옴
    @EntityGraph(attributePaths = {"video"})
    Optional<LearningHistory> findFirstByUserOrderByLastAccessAtDesc(User user);

    /**
     * 사용자가 시청한 영상 ID 목록 조회
     */
    @Query("SELECT lh.video.videoId FROM LearningHistory lh " +
            "WHERE lh.user.id = :userId")
    List<String> findVideoByUserId(@Param("userId") Long userId);

}
