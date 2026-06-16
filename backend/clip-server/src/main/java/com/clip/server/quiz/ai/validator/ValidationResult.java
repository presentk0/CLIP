package com.clip.server.quiz.ai.validator;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ValidationResult {
    private final boolean valid;
    private final String reason;  // 실패 사유 (재시도 시 프롬프트에 힌트 줄 수 있음)

    public static ValidationResult success() {
        return new ValidationResult(true, null);
    }

    public static ValidationResult fail(String reason) {
        return new ValidationResult(false, reason);
    }
}
