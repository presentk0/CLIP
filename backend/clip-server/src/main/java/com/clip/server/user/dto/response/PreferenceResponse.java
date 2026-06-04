package com.clip.server.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PreferenceResponse {

    private String learningGoal; // 학습 목표
    private String difficultyLevel; // 학습 난이도
    private String absoluteLevel; // 사용자 개인 난이도

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
