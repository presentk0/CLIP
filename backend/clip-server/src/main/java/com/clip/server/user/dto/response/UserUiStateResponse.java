package com.clip.server.user.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserUiStateResponse {
    
    private Boolean voiceConsentRead; // 음성 팝업 동의 여부
    private Boolean tutorialCompleted; // 튜토리얼 팝업 동의 여부
}
