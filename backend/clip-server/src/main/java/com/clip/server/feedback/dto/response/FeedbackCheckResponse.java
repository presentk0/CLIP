package com.clip.server.feedback.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FeedbackCheckResponse {

    private final Boolean hasSubmittedFeedback; // 피드백 제출 여부
}
