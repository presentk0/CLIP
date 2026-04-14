package com.clip.server.word.contorller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.word.dto.request.CollectedWordRequest;
import com.clip.server.word.dto.response.CollectedWordResponse;
import com.clip.server.word.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "단어 API", description = "단어 수집 기능 관련 API입니다.")
public class WordController {

    private final WordService wordService;

    @Operation(summary = "단어 수집")
    @PostMapping("/words/collect")
    public ResponseEntity<ApiResponse<CollectedWordResponse>> collectWord(
            @Valid @RequestBody CollectedWordRequest collectedWordRequest) {
            CollectedWordResponse collectedWordResponse = wordService.save(collectedWordRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                            .body(ApiResponse.success(collectedWordResponse,"단어가 성공적으로 수집되었습니다."));
    }
}
