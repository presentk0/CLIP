package com.clip.server.video.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "VideoKeyWord")
public class VideoKeyWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(columnDefinition = "TEXT")
    private String sentence;

    @Column(length = 200)
    private String translation;

    @Column(length = 10)
    private Double timestamp; // "01:23" 형식

    @Builder
    public VideoKeyWord(Video video, String word, String sentence, String translation, Double timestamp) {
        this.video = video;
        this.word = word;
        this.sentence = sentence;
        this.translation = translation;
        this.timestamp = timestamp;
    }
}
