package com.clip.server.chat.dto.request;

import com.clip.server.chat.entity.AiGender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "AI 채팅방 초기화 요청")
public class ChatRoomInitRequest {

    @NotNull(message = "isNewStart는 필수입니다.")
    private Boolean isNewStart;

    // 이어하기인 경우 wordId, selectedScenario, aiGender 선택이 불필요하므로 nullable
    private Long wordId;

    @Size(max = 100, message = "시나리오는 100자 이하여야 합니다.")
    private String selectedScenario;

    private AiGender aiGender;
}
