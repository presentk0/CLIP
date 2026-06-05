package com.clip.server.chat.controller;

import com.clip.server.chat.dto.response.ChatScenarioResponse;
import com.clip.server.chat.dto.response.ChatWordsResponse;
import com.clip.server.chat.service.ChatScenarioService;
import com.clip.server.chat.service.ChatWordService;
import com.clip.server.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
@Tag(name="AI 채팅 API", description = "AI 음성/텍스트 채팅 관련 API입니다.")
@Validated
public class ChatController {

    private final ChatWordService chatWordService;
    private final ChatScenarioService chatScenarioService;

    @Operation(summary = "AI 채팅 기능 단어 조회")
    @GetMapping("/words")
    public ApiResponse<ChatWordsResponse> getChatWords(
            @AuthenticationPrincipal Long userId) {
        ChatWordsResponse response = chatWordService.getCandidateWords(userId);

        String message = response.isHasWords() ?  "AI 채팅 단어 조회가 완료되었습니다." : "수집된 단어가 없어요. 추천 영상으로 단어를 먼저 모아볼까요?";
        return ApiResponse.success(response,  message);
    }

    @Operation(summary = "AI 채팅 시나리오 조회")
    @GetMapping("/scenarios")
    public ApiResponse<ChatScenarioResponse> getScenarios(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "시나리오를 생성할 타겟 영어 단어", example = "serendipity")
            @RequestParam("word")
            @NotBlank(message = "타겟 단어는 필수입니다.")
            @Size(max = 50, message = "단어는 50자 이하여야 합니다.")
            @Pattern(
                    regexp = "^[a-zA-Z]+([\\s\\-'][a-zA-Z]+)*$",
                    message = "영문 단어만 입력 가능합니다."
            )
            String word
    ) {
        ChatScenarioResponse response = chatScenarioService.getScenarios(userId, word);
        return ApiResponse.success(response, "AI 채팅 시나리오가 성공적으로 조회되었습니다.");
    }


}
