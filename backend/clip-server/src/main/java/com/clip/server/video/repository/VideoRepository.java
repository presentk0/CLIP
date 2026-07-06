package com.clip.server.video.repository;

import com.clip.server.user.entity.preference.LearningGoal;
import com.clip.server.video.entity.VideoDifficulty;
import com.clip.server.video.entity.Video;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VideoRepository extends JpaRepository<Video, String> {

    /**
     * 유저의 학습 목표와 절대 난이도가 일치하고, 이미 본 영상은 제외하여 추천 리스트를 가져옴
     */
    @Query("SELECT v FROM Video v " +
            "WHERE (:learningGoal IS NULL OR v.learningGoal = :learningGoal) " +
            "AND v.difficultyLevel IN :recommendationRange " +
            "AND v.videoId NOT IN :watchedVideoIds")
    List<Video> findRecommendedVideos(
            @Param("learningGoal") LearningGoal learningGoal,
            @Param("recommendationRange") List<VideoDifficulty> recommendationRange,
            @Param("watchedVideoIds") List<String> watchedVideoIds,
            Pageable pageable
    );
}
