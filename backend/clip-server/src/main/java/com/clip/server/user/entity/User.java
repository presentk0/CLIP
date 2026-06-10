package com.clip.server.user.entity;

import com.clip.server.auth.entity.OAuthProvider;
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
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_oauth",
                        columnNames = {"oauth_provider", "oauth_id"}
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false, length = 20)
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_id", nullable = false, length = 100)
    private String oauthId;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "profile_image_url",  length = 500)
    private String profileImageUrl;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private Integer exp;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 사용자 정보 수정 시간
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public User(String email, String name, String profileImageUrl, OAuthProvider oauthProvider, String oauthId) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.level = 0;
        this.exp = 0;
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
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

    /**
     * 프로필 정보 업데이트 (재로그인 시 이름/사진 동기화)
     */
    public void updateProfile(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    /**
     * 다음 레벨업에 필요한 총 경험치
     * 예: level=0, exp=1300이면 1500 반환
     */
    public int calculateNextLevelExp() {
        return getThreshold(this.level + 1);
    }


    public double getProgressPercentage() {
        // 1. 현재 레벨을 시작하기 위해 필요했던 총 누적 경험치
        int currentLevelStartExp = getThreshold(this.level);
        // 2. 다음 레벨로 가기 위해 필요한 총 누적 경험치
        int nextLevelRequiredExp = getThreshold(this.level + 1);

        // 3. 현재 레벨 구간에서의 총 요구 경험치 (분모)
        int totalExpInThisLevel = nextLevelRequiredExp - currentLevelStartExp;

        // 4. 현재 유저가 '현재 레벨'에서 순수하게 쌓은 경험치 (분자)
        int currentExpInThisLevel = this.exp - currentLevelStartExp;

        // 예외 방지: 분모가 0이거나 유저 경험치가 정상 범위를 벗어난 경우 0% 반환
        if (totalExpInThisLevel <= 0 || currentExpInThisLevel < 0) {
            return 0.0;
        }

        // 5. 퍼센티지 계산
        double percentage = ((double) currentExpInThisLevel / totalExpInThisLevel) * 100;

        // 소수점 첫째 자리까지 반올림
        return Math.round(percentage * 10) / 10.0;
    }
}
