package com.clip.server.user.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserGrowthResponse {

    private int averageAccuracy;          // 평균 정확도
    private int streak;                       // 연속 학습 일수
    private WeeklyAccuracyInfo weeklyAccuracy; // 주간 정확도 정보

    @Getter
    @Builder
    public static class WeeklyAccuracyInfo {
        private int thisWeek;                 // 이번 주 정확도
        private int lastWeek;                 // 저번 주 정확도
        private int growthRate;               // 성장률 (this - last)
        private List<WeeklyData> weeklyData;  // 주차별 데이터
    }

    @Getter
    @Builder
    public static class WeeklyData {
        private String week;                  // "4주 전", "이번 주" 등
        private int accuracy;                 // 해당 주 정확도
    }
}