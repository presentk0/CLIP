package com.clip.server.user.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
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

    @Column(nullable = false, length = 20)
    private String oauth_provider;

    @Column(nullable = false, length = 100)
    private String oauth_id;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(nullable = false, length = 20)
    private String name;

    @Column(name = "profile_image_url",  length = 500)
    private String profileImageUrl;

    private Integer level = 0;

    private Integer exp = 0;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 사용자 정보 수정 시간
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public User(String email, String name, String profileImageUrl) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.level = 0;
        this.exp = 0;
    }

    public void addExp(int amount) {
        if(this.exp==null) {
            this.exp = 0;
        }
        this.exp += amount;

        while (this.exp >= getThreshold(this.level + 1)) {
            this.level++;
        }
    }

    /**
     * L 레벨에 도달하기 위해 필요한 총 누적 경험치 계산
     * 공식: 1500L + 180L(L-1)
     */
    private int getThreshold(int L) {
        if (L <= 0) return 0;
        return (1500 * L) + (180 * L * (L - 1));
    }

}
