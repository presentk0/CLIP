package com.clip.server.video.entity;

import com.clip.server.user.entity.preference.LearningGoal;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "video")
@EntityListeners(AuditingEntityListener.class)
public class Video {

    @Id
    @Column(name = "video_id", nullable = false, length = 20)
    private String videoId;

    @Column(nullable = false, length = 255)
    private String title; // 영상 제목

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl; // 썸네일  URL

    private Integer duration;   // 영상 길이

    @Column(name ="channel_name", length = 100)
    private String channelName; // 채널명

    @Column(name = "channel_profile_image_url", length = 500)
    private String channelProfileImageUrl; // 영상 프로필 URL

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_goal", length = 50)
    private LearningGoal learningGoal;

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 50)
    private VideoDifficulty estimatedDifficulty;

    @Column(name = "difficulty_score")
    private Double difficultyScore; // 난이도 점수 (디버깅/튜닝용)

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_source", length = 20)
    private DifficultySource difficultySource; // 라벨링 출처
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt; // 영상 등록일

    @Builder
    public Video(String videoId,
                 String title,
                 String thumbnailUrl,
                 Integer duration,
                 String channelName,
                 String channelProfileImageUrl,
                 LearningGoal learningGoal,
                 VideoDifficulty estimatedDifficulty,
                 Double difficultyScore,
                 DifficultySource difficultySource
    ) {
        this.videoId = videoId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
        this.channelName = channelName;
        this.channelProfileImageUrl = channelProfileImageUrl;
        this.learningGoal = learningGoal;
        this.estimatedDifficulty = estimatedDifficulty;
        this.difficultyScore = difficultyScore;
        this.difficultySource = difficultySource;
    }

    public void updateAutoDifficulty(VideoDifficulty level, Double score) {
        // 수동 라벨은 보호
        if (this.difficultySource == DifficultySource.MANUAL) {
            return;
        }
        this.estimatedDifficulty = level;
        this.difficultyScore = score;
        this.difficultySource = DifficultySource.AUTO;
    }

    public void updateManualLabel(LearningGoal goal, VideoDifficulty difficulty) {
        this.learningGoal = goal;
        this.estimatedDifficulty = difficulty;
        this.difficultySource = DifficultySource.MANUAL;
        // difficultyScore는 수동 라벨이므로 null 또는 그대로 유지
    }
}
