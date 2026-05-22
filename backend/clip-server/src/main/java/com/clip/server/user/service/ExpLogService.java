package com.clip.server.user.service;

import com.clip.server.user.entity.User;
import com.clip.server.user.entity.exp.ExpLog;
import com.clip.server.user.entity.exp.ExpSourceType;
import com.clip.server.user.repository.ExpLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ExpLogService {

    private final ExpLogRepository expLogRepository;

    //  경험치 추가 + 로그 저장 (description 없음 - 기본 제목 사용)
    public void addExp(User user, ExpSourceType sourceType, Long sourceId, int amount) {
        addExp(user, sourceType, sourceId, null, amount);
    }

    public void addExp(User user, ExpSourceType sourceType, Long sourceId, String description, int amount) {

        if(amount==0) {
            log.debug("경험치 0 - 로그 생략: userId={}, sourceType={}", user.getId(), sourceType);
            return;
        }

        // 사용자 경험치 갱신
        user.addExp(amount);

        // 로그 저장
        expLogRepository.save(ExpLog.builder()
                .user(user)
                .sourceType(sourceType)
                .sourceId(sourceId)
                .description(description)
                .amount(amount)
                .build());

        log.info("경험치 변동 기록: userId={}, type={}, amount={}",
                user.getId(), sourceType, amount);

    }
}
