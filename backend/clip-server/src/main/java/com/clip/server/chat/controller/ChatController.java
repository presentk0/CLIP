package com.clip.server.chat.controller;

import com.clip.server.chat.dto.request.ChatRoomInitRequest;
import com.clip.server.chat.dto.response.*;
import com.clip.server.chat.service.*;
import com.clip.server.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
@Tag(name="AI 채팅 API", description = "AI 음성/텍스트 채팅 관련 API입니다.")
@Validated
public class ChatController {

    private final ChatWordService chatWordService;
    private final ChatScenarioService chatScenarioService;
    private final ChatRoomService chatRoomService;
    private final ChatMessageService chatMessageService;
    private final ChatReportService chatReportService;

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

    @Operation(summary = "채팅방 생성/초기화")
    @PostMapping("/rooms")
    public ResponseEntity <ApiResponse<ChatRoomInitResponse>> initChatRoom(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ChatRoomInitRequest request
            ) {
        ChatRoomInitResponse response = chatRoomService.initChatRoom(userId, request);

        String message = request.getIsNewStart() ? "AI 채팅방이 초기화 되었습니다. 대화를 시작해보세요." : "기존 AI 채팅방이 연결됩니다. 대화를 이어해보세요.";
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response, message));
    }

    @Operation(summary = "채팅 메시지 조회")
    @GetMapping("/rooms/{chatRoomId}/messages")
    public ApiResponse<ChatMessageListResponse> getChatMessage(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long chatRoomId
    ) {
        ChatMessageListResponse response = chatMessageService.getMessage(userId, chatRoomId);
        String message = response.getMessages().isEmpty()
                ? "아직 대화 내역이 없습니다. 새로운 대화를 시작해보세요!"
                : "과거 대화 내역 조회가 완료되었습니다. 기존 대화를 이어가세요.";
        return ApiResponse.success(response, message);
    }

    @Operation(summary = "채팅방 수동 종료")
    @PatchMapping("/rooms/{chatRoomId}/complete")
    public ApiResponse<ChatRoomCompleteResponse> completeChatRoom(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long chatRoomId
    ) {
        ChatRoomCompleteResponse response = chatRoomService.completeChatRoom(userId, chatRoomId);
        return ApiResponse.success(response, "채팅방이 종료되었습니다.");
    }

    @Operation(summary = "AI 대화 리포트 조회")
    @GetMapping("/rooms/{chatRoomId}/report")
    public ApiResponse<ChatReportResponse> getReport(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long chatRoomId
    ) {
        ChatReportResponse response = chatReportService.generateReport(userId, chatRoomId);
        return ApiResponse.success(
                response,
                "AI 대화 리포트 생성이 완료되었습니다. 고생하셨습니다!"
        );
    }

}
