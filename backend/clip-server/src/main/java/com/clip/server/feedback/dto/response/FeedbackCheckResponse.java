package com.clip.server.feedback.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeedbackCheckResponse {

    private final Boolean isSubmitted; // 피드백 제출 여부
}
