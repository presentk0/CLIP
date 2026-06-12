package com.clip.server.feedback.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FeedbackRequest {

    @NotNull(message = "만족도 점수는 필수입니다.")
    @Min(value = 1, message = "만족도는 1 이상이어야 합니다.")
    @Max(value = 5, message = "만족도는 5 이하여야 합니다.")
    private Integer satisfactionScore;

    @Size(max = 1000, message = "좋았던 점은 1000자 이하여야 합니다.")
    private String goodPoint;

    @Size(max = 1000, message = "불편했던 점은 1000자 이하여야 합니다.")
    private String improvePoint;
}
