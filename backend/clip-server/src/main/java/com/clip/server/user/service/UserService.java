package com.clip.server.user.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.entity.SessionStatus;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.user.dto.response.UserDashBoardResponse;
import com.clip.server.user.dto.response.UserGrowthResponse;
import com.clip.server.user.dto.response.UserProfileResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.badge.BadgeType;
import com.clip.server.user.entity.exp.ExpLog;
import com.clip.server.user.entity.learning.LearningHistory;
import com.clip.server.user.repository.ExpLogRepository;
import com.clip.server.user.repository.LearningHistoryRepository;
import com.clip.server.user.repository.UserBadgeRepository;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.util.ArrayList;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    private final CollectedWordRepository collectedWordRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final ExpLogRepository expLogRepository;
    private final UserBadgeRepository userBadgeRepository;
    private static final int WEEKS_TO_SHOW = 4;

    /*
    ** 메인페이지 사용자 기본 프로필 조회
     */
    public UserProfileResponse showProfile(Long userId) {

        // 유저확인
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int nextLevelExp = user.calculateNextLevelExp();
        double progressPercentage = user.getProgressPercentage();

        Optional<LearningHistory> latestHistoryOpt = learningHistoryRepository.findFirstByUserOrderByLastAccessAtDesc(user);

        UserProfileResponse.OngoingMastery ongoingMastery = null;

        if (latestHistoryOpt.isPresent()) {
            LearningHistory latestHistory = latestHistoryOpt.get();
            var video = latestHistory.getVideo(); // 패치 조인으로 가져온 영상 메타데이터

            // 해당 영상의 현재 뱃지 등급 조회 (.name() 활용 최적화 완료)
            String currentBadge = userBadgeRepository.findTopByUserIdAndVideo_VideoIdOrderByEarnedAtDesc(user.getId(), video.getVideoId())
                    .map(badge -> badge.getBadgeType().name())
                    .orElse("NONE"); // 뱃지가 아직 없으면 NONE 또는 기본값 처리

            // 초 단위 duration을 "MM:SS" 혹은 "HH:MM:SS" 문자열로 예쁘게 변환
            String formattedDuration = formatDuration(video.getDuration());

            ongoingMastery = UserProfileResponse.OngoingMastery.builder()
                    .videoId(video.getVideoId())
                    .videoTitle(video.getTitle())
                    .thumbnailUrl(video.getThumbnailUrl())
                    .videoDuration(formattedDuration)
                    .channelName(video.getChannelName())
                    .channelProfileImageUrl(video.getChannelProfileImageUrl())
                    .currentBadge(currentBadge)
                    .build();
        }

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .profileImageUrl(user.getProfileImageUrl())
                .level(user.getLevel())
                .exp(user.getExp())
                .nextLevelExp(nextLevelExp)
                .progressPercentage(progressPercentage)
                .createdAt(user.getCreatedAt().toLocalDate().toString()) // 가입일 포맷팅
                .ongoingMastery(ongoingMastery) // 조회 결과 바인딩 - 없으면 null 처리
                .build();
    }

    private String formatDuration(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /*
     ** 마이페이지 상단부 사용자 대시보드 조회
     */
    public UserDashBoardResponse showDashBoard(Long userId) {

        // 유저 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 유저 레벨 관련 정보
        UserDashBoardResponse.LevelInfo levelInfo = UserDashBoardResponse.LevelInfo.builder()
                .currentLevel(user.getLevel())
                .currentExp(user.getExp())
                .nextLevelExp(user.calculateNextLevelExp())
                .progressPercentage(user.getProgressPercentage())
                .build();

        // 총 수집 단어
        int totalWords = (int) collectedWordRepository.countByUser(user);

        // 정복한 영상 수
        int conqueredVideos = (int) quizSessionRepository
                .countDistinctVideoByUserIdAndStatus(user.getId(), SessionStatus.COMPLETED);

        UserDashBoardResponse.StatsInfo statsInfo = UserDashBoardResponse.StatsInfo.builder()
                .totalWords(totalWords)
                .conqueredVideos(conqueredVideos)
                .build();

        // 최신 로그 10개 조회
        List<ExpLog> recentLogs = expLogRepository.findTop10ByUserOrderByCreatedAtDesc(user);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM.dd");

        List<UserDashBoardResponse.ExpLogInfo> expLogInfos = recentLogs.stream()
                .map(log -> UserDashBoardResponse.ExpLogInfo.builder()
                        .date(log.getCreatedAt().format(formatter))
                        .title(log.getTitle())
                        .amount(log.getAmount())
                        .build())
                .toList();

        // 주간 출석 정보 조회
        UserDashBoardResponse.WeeklyAttendance weeklyAttendance = calculateWeeklyAttendance(userId);

        return UserDashBoardResponse.builder()
                .levelInfo(levelInfo)
                .stats(statsInfo)
                .expLogs(expLogInfos)
                .weeklyAttendance(weeklyAttendance)
                .build();
    }

    // 이번 주 출석 정보 계산 (월요일 ~ 일요일)
    private UserDashBoardResponse.WeeklyAttendance calculateWeeklyAttendance(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.with(DayOfWeek.MONDAY);

        String[] dayLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        List<UserDashBoardResponse.DailyAttendance> days = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            boolean attended = hasLearnedOnDate(userId, date);
            boolean isToday = date.equals(today);

            days.add(UserDashBoardResponse.DailyAttendance.builder()
                    .dayOfWeek(dayLabels[i])
                    .date(date.toString())
                    .attended(attended)
                    .isToday(isToday)
                    .build());
        }

        String todayLabel = dayLabels[today.getDayOfWeek().getValue() - 1];

        return UserDashBoardResponse.WeeklyAttendance.builder()
                .today(todayLabel)
                .days(days)
                .build();
    }

    /**
     * 사용자 성장 지표 조회
     */
    public UserGrowthResponse showGrowth(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int averageAccuracy = calculateAverageAccuracy(userId);
        int streak = calculateStreak(userId);
        UserGrowthResponse.WeeklyAccuracyInfo weeklyAccuracy = calculateWeeklyAccuracy(userId);

        return UserGrowthResponse.builder()
                .averageAccuracy(averageAccuracy)
                .streak(streak)
                .weeklyAccuracy(weeklyAccuracy)
                .build();
    }

    // ==================== 평균 정확도 ====================

    private int calculateAverageAccuracy(Long userId) {
        long totalQuizCount = quizSessionRepository.sumTotalQuizCount(userId);
        long correctQuizCount = quizSessionRepository.sumCorrectCount(userId);

        if (totalQuizCount == 0) return 0;

        return (int) Math.round((double) correctQuizCount / totalQuizCount * 100);
    }

    // ==================== 연속 학습 ====================

    private int calculateStreak(Long userId) {
        LocalDate today = LocalDate.now();
        int streak = 0;
        LocalDate targetDate = today;

        if (!hasLearnedOnDate(userId, targetDate)) {
            targetDate = targetDate.minusDays(1);
        }

        while (hasLearnedOnDate(userId, targetDate)) {
            streak++;
            targetDate = targetDate.minusDays(1);

            if (streak > 9999) break;
        }

        return streak;
    }

    private boolean hasLearnedOnDate(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

        return quizSessionRepository.existsCompletedQuizToday(userId, startOfDay, endOfDay)
                || collectedWordRepository.existsCollectedWordToday(userId, startOfDay, endOfDay);
    }

    // ==================== 주간 정확도 ====================

    private UserGrowthResponse.WeeklyAccuracyInfo calculateWeeklyAccuracy(Long userId) {
        List<UserGrowthResponse.WeeklyData> weeklyData = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int weeksAgo = WEEKS_TO_SHOW - 1; weeksAgo >= 0; weeksAgo--) {
            LocalDateTime weekStart = today.minusWeeks(weeksAgo).atStartOfDay();
            LocalDateTime weekEnd = today.minusWeeks(weeksAgo - 1).atStartOfDay();

            int accuracy = (int) Math.round(calculateAccuracyInRange(userId, weekStart, weekEnd));

            weeklyData.add(UserGrowthResponse.WeeklyData.builder()
                    .week(getWeekLabel(weeksAgo))
                    .accuracy(accuracy)
                    .build());
        }

        int thisWeek = weeklyData.get(weeklyData.size() - 1).getAccuracy();
        int lastWeek = weeklyData.size() >= 2
                ? weeklyData.get(weeklyData.size() - 2).getAccuracy()
                : 0;
        int growthRate = thisWeek - lastWeek;

        return UserGrowthResponse.WeeklyAccuracyInfo.builder()
                .thisWeek(thisWeek)
                .lastWeek(lastWeek)
                .growthRate(growthRate)
                .weeklyData(weeklyData)
                .build();
    }

    private double calculateAccuracyInRange(Long userId, LocalDateTime start, LocalDateTime end) {
        Long totalQuizCount = quizSessionRepository.sumTotalQuizCountBetween(userId, start, end);
        Long correctQuizCount = quizSessionRepository.sumCorrectCountBetween(userId, start, end);

        if (totalQuizCount == null || totalQuizCount == 0) return 0.0;

        return (double) correctQuizCount / totalQuizCount * 100;
    }

    private String getWeekLabel(int weeksAgo) {
        return switch (weeksAgo) {
            case 0 -> "이번 주";
            case 1 -> "저번 주";
            default -> weeksAgo + "주 전";
        };
    }
}
