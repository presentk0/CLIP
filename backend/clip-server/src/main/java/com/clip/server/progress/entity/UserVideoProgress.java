package com.clip.server.progress.entity;

import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;
import jakarta.persistence.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_video_progress")
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserVideoProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Builder.Default
    private double learnedTime = 0.0;       // 실제 누적 학습 시간
    @Builder.Default
    private double lastAddedStartTime = -1.0; // 중복 합산 방지 (A, B, C 문장 필터링)
    @Builder.Default
    private int lastQuizzedSection = 0;    // 마지막 퀴즈 출제 섹션 번호


    // 누적 시간 리셋 (반복 시청 시 사용)
    public void resetProgress() {
        this.learnedTime = 0.0;
        this.lastAddedStartTime = -1.0;
        this.lastQuizzedSection = 0;
    }

    // 학습 시간 추가
    public void addProgress(Double subtitleDuration, Double startTime) {
        this.learnedTime += subtitleDuration;
        this.lastAddedStartTime = startTime;
    }

    // 섹션 업데이트
    public void updateSection(int section) {
        this.lastQuizzedSection = section;
    }
}
