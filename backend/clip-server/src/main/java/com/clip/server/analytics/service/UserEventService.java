package com.clip.server.analytics.service;

import com.clip.server.analytics.entity.UserEvent;
import com.clip.server.analytics.repository.UserEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventService {

    private final UserEventRepository userEventRepository;

    /**
     * 비동기로 이벤트 저장 - 응답 속도 영향 X
     */
    @Async("eventTaskExecutor")
    public void saveAsync(UserEvent event) {
        try {
            userEventRepository.save(event);
        } catch (Exception e) {
            log.warn("유저 이벤트 저장 실패: {}", e.getMessage());
        }
    }
}