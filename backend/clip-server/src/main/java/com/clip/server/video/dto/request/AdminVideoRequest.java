package com.clip.server.video.dto.request;

import com.clip.server.user.entity.preference.LearningGoal;
import com.clip.server.video.entity.VideoDifficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AdminVideoRequest {

    @NotBlank(message = "videoId는 필수입니다.")
    private String videoId;

    @NotNull(message = "학습 목표는 필수입니다.")
    private LearningGoal learningGoal;

    @NotNull(message = "난이도는 필수입니다.")
    private VideoDifficulty difficulty;
}
