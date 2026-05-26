package com.clip.server.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserDashBoardResponse {

    private LevelInfo levelInfo;
    private StatsInfo stats;
    private List<ExpLogInfo> expLogs;
    private WeeklyAttendance weeklyAttendance;

    @Getter
    @Builder
    public static class LevelInfo {
        private int currentLevel; // 현재 레벨
        private int currentExp; // 현재 Exp
        private int nextLevelExp; // 다음 레벨까지 필요한 Exp
        private double progressPercentage; // 다음 레벨까지 진행률
    }

    @Getter
    @Builder
    public static class StatsInfo {
        private int totalWords; // 총 수집 단어
        private int conqueredVideos; // 완료된 영상 수
    }

    @Getter
    @Builder
    public static class ExpLogInfo {
        private String date; // 로그 획득 날짜
        private String title; // 로그 획득 타이틀
        private int amount; // 획득한 exp
    }

    @Getter
    @Builder
    public static class WeeklyAttendance {
        private String today;
        private List<DailyAttendance> days;
    }

    @Getter
    @Builder
    public static class DailyAttendance {
        private String dayOfWeek;
        private String date;
        private boolean attended;
        private boolean isToday;
    }

}
