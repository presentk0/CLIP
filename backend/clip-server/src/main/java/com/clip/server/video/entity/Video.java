package com.clip.server.video.entity;

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
    private String title;

    @Column(name = "thumbnail_url", length = 500)
    private String url;

    // 영상 길이
    private Integer duration;

    @Column(name ="channel_name", length = 100)
    private String channelName;
    
    // 영상 등록일
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Builder
    public Video(String videoId, String title, String url, Integer duration, String channelName) {
        this.videoId = videoId;
        this.title = title;
        this.url = url;
        this.duration = duration;
        this.channelName = channelName;
    }
}
