package com.clip.server.user.repository;

import com.clip.server.user.entity.User;
import com.clip.server.user.entity.exp.ExpLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpLogRepository extends JpaRepository<ExpLog, Long> {

    // 최신순으로 정렬해서 상위 10개만 가져오기
    List<ExpLog> findTop10ByUserOrderByCreatedAtDesc(User user);
}
