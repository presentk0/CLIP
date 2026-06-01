package com.clip.server.user.dto.request;

import com.clip.server.user.entity.preference.AbsoluteLevel;
import com.clip.server.user.entity.preference.DifficultyLevel;
import com.clip.server.user.entity.preference.LearningGoal;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class InitialPreferenceRequest {
    @NotNull
    private LearningGoal learningGoal; // 학습 목표
    @NotNull
    private DifficultyLevel difficultyLevel; // 학습 난이도(상대적 난이도)
    @NotNull
    private AbsoluteLevel absoluteLevel; // 사용자의 절대적 난이도
}
