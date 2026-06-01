package com.clip.server.video.controller;

import com.clip.server.video.dto.request.AdminVideoRequest;
import com.clip.server.video.service.AdminVideoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/videos")
@RequiredArgsConstructor
public class AdminVideoController {

    private final AdminVideoService adminVideoService;

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
        adminVideoService.registerBatchVideos(requests);
        return ResponseEntity.ok().build();
    }
}
