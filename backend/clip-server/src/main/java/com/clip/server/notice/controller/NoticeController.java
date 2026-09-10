package com.clip.server.notice.controller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.notice.dto.response.DismissAllResponse;
import com.clip.server.notice.dto.response.DismissResponse;
import com.clip.server.notice.dto.response.PopupResponse;
import com.clip.server.notice.dto.response.UnreadCountResponse;
import com.clip.server.notice.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notices")
@Tag(name = "공지사항 API", description = "사용자에게 표시되는 공지사항 팝업 및 배지 관련 API입니다.")
@Validated
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "사용자에게 보여줄 공지 조회")
    @GetMapping("/popup")
    public ApiResponse<PopupResponse> getPopup(
            @AuthenticationPrincipal Long userId
    ) {
        PopupResponse response = noticeService.getPopup(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "미확인 공지 조회")
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> getUnreadCount(
            @AuthenticationPrincipal Long userId
    ) {
        UnreadCountResponse response = noticeService.getUnreadCount(userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "팝업 닫기 처리")
    @PostMapping("/{id}/dismiss")
    public ResponseEntity<ApiResponse<DismissResponse>> postDismiss(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id
    ) {
        DismissResponse response = noticeService.postDismiss(userId, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "전체 공지 확인 처리")
    @PostMapping("/dismiss-all")
    public ResponseEntity<ApiResponse<DismissAllResponse>> postDismissAll(
            @AuthenticationPrincipal Long userId
    ) {
        DismissAllResponse response = noticeService.postDismissAll(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

}
