package com.clip.server.user.entity.preference;

import com.clip.server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_preference")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "learning_goal", length = 50, nullable = false)
    private LearningGoal learningGoal; // 학습 목표

    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty_level", length = 20, nullable = false)
    private DifficultyLevel difficultyLevel; // 학습 난이도

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // 수정 시각

    @Builder
    public UserPreference(User user, LearningGoal learningGoal, DifficultyLevel difficultyLevel) {
        this.user = user;
        this.learningGoal = learningGoal;
        this.difficultyLevel = difficultyLevel;
    }

    // 학습 설정 업데이트
    public void update(LearningGoal goal, DifficultyLevel level) {
        this.learningGoal = goal;
        this.difficultyLevel = level;
    }

    // 학습 설정 부분 업데이트
    public void partialUpdate(LearningGoal goal, DifficultyLevel level) {
        if (goal != null) {
            this.learningGoal = goal;
        }
        if (level != null) {
            this.difficultyLevel = level;
        }
    }
}


