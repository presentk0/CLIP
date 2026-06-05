package com.clip.server.chat.controller;

import com.clip.server.chat.dto.response.ChatWordsResponse;
import com.clip.server.chat.service.ChatWordService;
import com.clip.server.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
@Tag(name="AI 채팅 API", description = "AI 음성/텍스트 채팅 관련 API입니다.")
@Validated
public class ChatController {

    private final ChatWordService chatWordService;

    @Operation(summary = "AI 채팅 기능 단어 조회")
    @GetMapping("words")
    public ApiResponse<ChatWordsResponse> getChatWords(
            @AuthenticationPrincipal Long userId) {
        ChatWordsResponse response = chatWordService.getCandidateWords(userId);

        String message = response.isHasWords() ?  "AI 채팅 단어 조회가 완료되었습니다." : "수집된 단어가 없어요. 추천 영상으로 단어를 먼저 모아볼까요?";
        return ApiResponse.success(response,  message);
    }


}
