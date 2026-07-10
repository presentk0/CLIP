package com.clip.server.quiz.agent.dto;

import com.clip.server.user.entity.preference.AbsoluteLevel;
import com.clip.server.user.entity.preference.LearningGoal;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserLearningState {

    private Long userId;

    // 사용자 프로필
    private Integer userLevel;              // users.level
    private AbsoluteLevel absoluteLevel;     // 사용자 레벨
    private LearningGoal learningGoal;       // 사용자 학습 목표

    // 퀴즈 이력
    private Double recentAccuracy;          // 최근 30개 정답률 (0.0 ~ 1.0)
    private List<String> weakWords;         // 정답률 < 50% 단어
    private List<String> recentWrongWords;  // 최근 7일 오답 단어

    // 수집 활동
    private List<String> neverTestedWords;  // 수집했지만 미출제 단어
    private Integer totalCollectedWords;

    // 채팅 약점 (시그니처)
    private List<String> chatWeakExpressions; // "very like" 등

    // 활동성
    private Integer daysSinceLastQuiz;
}