package com.clip.server.user.dto.request;

import com.clip.server.user.entity.preference.DifficultyLevel;
import com.clip.server.user.entity.preference.LearningGoal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UpdatePreferenceRequest {

    private LearningGoal learningGoal; // 학습 목표
    private DifficultyLevel difficultyLevel; // 학습 난이도
}
