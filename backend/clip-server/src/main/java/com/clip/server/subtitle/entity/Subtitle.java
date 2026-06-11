package com.clip.server.subtitle.entity;

import com.clip.server.video.entity.Video;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "Subtitle")
@EntityListeners(AuditingEntityListener.class)
public class Subtitle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(nullable = false)
    private String text; // 자막 내용

    @Column(nullable = false)
    private String translation; // 자막 번역

    @Column(name = "start_time", nullable = false)
    private Double startTime; // 영상 시작 시각

    @Column(name = "end_time", nullable = false)
    private Double endTime; // 영상 종료 시각

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Subtitle(Video video, String text, String translation, Double startTime, Double endTime) {
        this.video = video;
        this.text = text;
        this.translation = translation;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * 번역 텍스트 업데이트
     */
    public void updateTranslation(String translation) {
        this.translation = translation;
    }
}
