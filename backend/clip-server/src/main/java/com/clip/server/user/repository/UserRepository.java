package com.clip.server.user.repository;

import com.clip.server.auth.entity.OAuthProvider;
import com.clip.server.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 이메일로 사용자 조회
     */
    Optional<User> findByEmail(String email);

    /**
     * OAuth 제공자 + OAuth ID로 사용자 조회
     */
    Optional<User> findByOauthProviderAndOauthId(
            OAuthProvider oauthProvider,
            String oauthId
    );

    /**
     * 이메일 존재 여부 확인
     */
    boolean existsByEmail(String email);
}
