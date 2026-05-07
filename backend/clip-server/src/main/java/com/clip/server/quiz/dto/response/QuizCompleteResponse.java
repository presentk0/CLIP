package com.clip.server.quiz.dto.response;

import com.clip.server.user.entity.badge.BadgeType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class QuizCompleteResponse {

    private Long sessionId; // 세션 Id
    private Integer totalQuizCount; // 총 퀴즈 개수
    private Integer correctCount; // 정답 개수
    private Integer wrongCount; // 오답 개수
    private Double accuracy; // 퀴즈 정확도
    private Integer earnedExp; // 획득한 Exp
    private Boolean levelUp; // Level 업 여부
    private Integer currentLevel; // 현재 레벌
    private BadgeInfo newBadge; // 영상 배지
    private LocalDateTime completedAt; // 세션 만료 시각
    private String feedback; // 최종 정산 피드백

    @Builder
    @Getter
    public static class BadgeInfo {
        private String videoId;
        private BadgeType badgeType;
    }

}
