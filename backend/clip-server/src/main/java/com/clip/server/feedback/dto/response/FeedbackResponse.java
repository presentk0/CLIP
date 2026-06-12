package com.clip.server.feedback.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FeedbackResponse {

    private Long feedbackId;
    private LocalDateTime submittedAt;
}
