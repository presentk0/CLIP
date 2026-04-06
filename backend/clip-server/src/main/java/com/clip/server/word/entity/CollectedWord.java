package com.clip.server.word.entity;

import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "collected_word")
public class CollectedWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    // 사용자가 호버한 단어
    @Column(nullable = false, length = 100)
    private String word;

    // 단어 수집시 해당 단어가 포함된 예문
    @Column(nullable = false)
    private String sentence;

    // 해당 영상의 TimeStamp
    @Column(nullable = false, length = 10)
    private String timestamp;

    // 자막 번역
    @Column(nullable = false, length = 200)
    private String translation;

    // 단어 수집 시간
    @CreatedDate
    @Column(name = "collected_at", nullable = false, updatable = false)
    private LocalDateTime collectedAt;

    @Builder
    public CollectedWord(User user, Video video, String word, String sentence, String timestamp, String translation) {
        this.user = user;
        this.video = video;
        this.word = word;
        this.sentence = sentence;
        this.timestamp = timestamp;
        this.translation = translation;
    }
}
