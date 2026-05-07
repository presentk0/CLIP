package com.clip.server.translation.controller;
import com.clip.server.common.response.ApiResponse;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.translation.dto.response.TranslationResponse;
import com.clip.server.translation.service.TranslationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/translate")
@RequiredArgsConstructor
@Tag(name = "자막 번역 API", description = "전체 자막을 번역하는 API입니다.")
@Validated
public class TranslationController {

    private final TranslationService translationService;

    @Operation(summary = "영상 전체 자막 번역")
    @PostMapping("/subtitles")
    public ResponseEntity<ApiResponse<TranslationResponse>> translateSubtitles(
            @RequestBody TranslationRequest request) {
        Long tempUserId = 1L;
        TranslationResponse response = translationService.translateAndSave(tempUserId, request);

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "해당 영상의 전체 자막이 성공적으로 번역되었습니다."
        ));
    }
}
