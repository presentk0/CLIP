package com.clip.server.user.service;

import com.clip.server.user.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSessionService {

    private final UserSessionRepository userSessionRepository;

    /**
     * 마지막 활동 시각 갱신
     * - 최근 30분 이내 세션만 갱신
     */
    @Transactional
    public void updateUserActivity(Long userId) {
        userSessionRepository.findFirstByUser_IdOrderByLoginAtDesc(userId)
                .ifPresent(session -> {
                    if (session.getLastActivityAt().isAfter(LocalDateTime.now().minusMinutes(30))) {
                        session.updateLastActivity();
                        // JPA 영속성 컨텍스트가 자동으로 변경 감지 (save 불필요)
                    }
                });
    }
}