package com.clip.server.user.repository;

import com.clip.server.user.entity.User;
import com.clip.server.user.entity.preference.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    // 사용자의 학습 설정 조회
    Optional<UserPreference> findByUser(User user);
    Optional<UserPreference> findByUserId(Long userId);
    // 사용자의 학습 설정 존재 여부 확인
    boolean existsByUserId(Long userId);
}
