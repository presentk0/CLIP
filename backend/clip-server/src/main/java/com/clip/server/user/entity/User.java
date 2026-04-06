package com.clip.server.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="user")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // TODO: Google OAuth 로그인 구현 시 추가 예정
    /*  @Enumerated(EnumType.STRING)
        @Column(name = "oauth_provider", nullable = false, length = 20)
        private OAuthProvider oauthProvider;

        @Column(name = "oauth_id", nullable = false, length = 100)
        private String oauthId;
    */
    @Column(nullable = false, length = 255)
    private String email;
    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "profile_image_url" length = 500)
    private String profileImageUrl;

    private Integer level = 1;
    private Integer exp = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 사용자 정보 수정 시간
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public User(String email, String name, String profileImageUrl, Integer level, Integer exp) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.level = level;
        this.exp = exp;
    }
}
