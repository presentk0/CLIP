package com.clip.server.admin.dashboard.stats.controller;

import com.clip.server.video.dto.request.AdminVideoRequest;
import com.clip.server.admin.dashboard.stats.service.AdminVideoBatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/videos")
@RequiredArgsConstructor
public class AdminVideoController {

    private final AdminVideoService adminVideoService;
    private final AdminVideoBatchService adminVideoBatchService;

    /**
     * 관리자: 큐레이션 영상 등록 (수동 라벨링)
     */
    @PostMapping
    public ResponseEntity<Void> registerCuratedVideo(@RequestBody AdminVideoRequest request) {
        adminVideoService.registerCuratedVideo(request);
        return ResponseEntity.ok().build();
    }

    /**
     * 관리자: 영상 일괄 등록
     */
    @PostMapping("/batch")
    public ResponseEntity<Void> registerBatchVideos(@RequestBody List<AdminVideoRequest> requests) {
        adminVideoBatchService.registerBatchVideos(requests);
        return ResponseEntity.ok().build();
    }
}
