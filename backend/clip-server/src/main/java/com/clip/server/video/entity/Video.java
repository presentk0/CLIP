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
    private String title; // 영상 제목

    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl; // 썸네일  URL

    private Integer duration;   // 영상 길이

    @Column(name ="channel_name", length = 100)
    private String channelName; // 채널명

    @Column(name = "channel_profile_image_url", length = 500)
    private String channelProfileImageUrl; // 영상 프로필 URL
    
    @CreatedDate
    @Column(name = "created_at")
    private LocalDateTime createdAt; // 영상 등록일

    @Builder
    public Video(String videoId, String title, String thumbnailUrl, Integer duration, String channelName, String channelProfileImageUrl) {
        this.videoId = videoId;
        this.title = title;
        this.thumbnailUrl = thumbnailUrl;
        this.duration = duration;
        this.channelName = channelName;
        this.channelProfileImageUrl = channelProfileImageUrl;
    }
}
