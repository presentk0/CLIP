package com.clip.server.user.entity.exp;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExpSourceType {

    QUIZ_CORRECT("퀴즈 정답"),
    VIDEO_COMPLETE("영상 학습 완료"),
    BADGE_BRONZE("마스터리 브론즈 획득"),
    BADGE_SILVER("마스터리 실버 획득"),
    BADGE_GOLD("마스터리 골드 획득");

    private final String defaultTitle;
}