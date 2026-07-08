package com.clip.server.user.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UpdateUiStateRequest {

    private Boolean voiceConsentRead; // 음성 팝업 동의 저장 요청
    private Boolean tutorialCompleted; // 튜토리얼 팝업 확인 저장 요청
}
