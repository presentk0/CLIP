package com.clip.server.feedback.repository;

import com.clip.server.feedback.entity.UserFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFeedbackRepository extends JpaRepository<UserFeedback, Long> {

    boolean existsByUserId(Long userId); // 사용자 피드백 제출 여부 체크
}
