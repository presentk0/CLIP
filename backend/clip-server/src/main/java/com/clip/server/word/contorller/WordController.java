package com.clip.server.word.contorller;

import com.clip.server.common.response.ApiResponse;
import com.clip.server.word.dto.request.CollectedWordRequest;
import com.clip.server.word.dto.response.CollectedWordResponse;
import com.clip.server.word.dto.response.WordListResponse;
import com.clip.server.word.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/words")
@Tag(name = "단어 API", description = "단어 수집 기능 관련 API입니다.")
@Validated
public class WordController {

    private final WordService wordService;

    @Operation(summary = "단어 수집")
    @PostMapping("/collect")
    public ResponseEntity<ApiResponse<CollectedWordResponse>> collectWord(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CollectedWordRequest collectedWordRequest
    ) {
            CollectedWordResponse collectedWordResponse = wordService.save(userId, collectedWordRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                            .body(ApiResponse.success(collectedWordResponse,"단어가 성공적으로 수집되었습니다."));
    }

    @Operation(summary = "수집된 단어 조회")
    @GetMapping("/my-collection")
    public ApiResponse<WordListResponse> getMyCollection(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(defaultValue = "all") String filter,
            @RequestParam(required = false) String keyword
    ) {
        WordListResponse wordListResponse = wordService.getWords(userId,  page, size, sort, filter, keyword);
        return ApiResponse.success(wordListResponse,"단어장이 성공적으로 조회됐습니다.");
    }
}
