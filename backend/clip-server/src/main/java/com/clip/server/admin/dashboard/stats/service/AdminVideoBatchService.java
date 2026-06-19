package com.clip.server.admin.dashboard.stats.service;

import com.clip.server.video.dto.request.AdminVideoRequest;
import com.clip.server.admin.dashboard.stats.controller.AdminVideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminVideoBatchService {

    private final AdminVideoService adminVideoService;  // 다른 Service 주입 → 프록시 호출됨

    @Transactional
    public void registerBatchVideos(List<AdminVideoRequest> requests) {
        int successCount = 0;
        int failCount = 0;

        for (AdminVideoRequest request : requests) {
            try {
                adminVideoService.registerCuratedVideo(request);  // 프록시 호출
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("영상 등록 실패: videoId={}", request.getVideoId(), e);
            }
        }

        log.info("일괄 등록 완료: 성공={}, 실패={}", successCount, failCount);
    }
}
