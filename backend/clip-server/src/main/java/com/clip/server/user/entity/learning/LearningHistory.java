package com.clip.server.user.entity.learning;

import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="learn_history")
@EntityListeners(AuditingEntityListener.class)
public class LearningHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id")
    private Video video;

    @Column(name = "total_watch_time")
    private int totalWatchTime; // 총 시청 시간(초)

    @Column(name = "words_collected")
    private long collectedWords; // 수집한 단어 수

    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessAt; // 마지막 접근 시각

    @Column(name = "completion_count")
    private int completionCount = 0;

    @Builder
    public LearningHistory(User user, Video video, LocalDateTime lastAccessAt) {
        this.user = user;
        this.video = video;
        this.totalWatchTime = 0;
        this.collectedWords = 0;
        this.lastAccessAt = lastAccessAt;
    }

    public void updateCollectedWord(Long totalWordCount) {
        this.collectedWords = totalWordCount;
    }

    public void updateCompletionCount() {
        this.completionCount++;
    }

    public void updateLastAccessAt() {
        this.lastAccessAt = LocalDateTime.now();
    }

    public void updateTotalWatchTime(int seconds) {
        this.totalWatchTime += seconds;
    }
}
