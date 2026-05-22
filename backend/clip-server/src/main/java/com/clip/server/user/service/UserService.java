package com.clip.server.user.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.entity.SessionStatus;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.user.dto.response.UserDashBoardResponse;
import com.clip.server.user.dto.response.UserProfileResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.exp.ExpLog;
import com.clip.server.user.repository.ExpLogRepository;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final CollectedWordRepository collectedWordRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final ExpLogRepository expLogRepository;

    /*
    ** 메인페이지 사용자 기본 프로필 조회
     */
    public UserProfileResponse showProfile(Long userId) {

        // 유저확인
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int nextLevelExp = user.calculateNextLevelExp();
        double progressPercentage = user.getProgressPercentage();

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .level(user.getLevel())
                .nextLevelExp(nextLevelExp)
                .progressPercentage(progressPercentage)
                .createdAt(user.getCreatedAt())
                .build();
    }

    /*
     ** 마이페이지 상단부 사용자 대시보드 조회
     */
    public UserDashBoardResponse showDashBoard(Long userId) {

        // 유저 확인
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 유저 레벨 관련 정보
        UserDashBoardResponse.LevelInfo levelInfo = UserDashBoardResponse.LevelInfo.builder()
                .currentLevel(user.getLevel())
                .currentExp(user.getExp())
                .nextLevelExp(user.calculateNextLevelExp())
                .progressPercentage(user.getProgressPercentage())
                .build();

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();

        // 오늘 학습 여부(영상 퀴즈 풀이 완료 or 단어 수집)
        boolean isLearnedToday = quizSessionRepository.existsCompletedQuizToday(user.getId(),startOfDay, endOfDay) ||
                collectedWordRepository.existsCollectedWordToday(user.getId(),startOfDay,endOfDay);

        // 총 수집 단어
        int totalWords = (int) collectedWordRepository.countByUser(user);

        // 정복한 영상 수
        int conqueredVideos = (int) quizSessionRepository.countDistinctVideoByUserIdAndStatus(user.getId(), SessionStatus.COMPLETED);

        UserDashBoardResponse.StatsInfo statsInfo = UserDashBoardResponse.StatsInfo.builder()
                .isLearnedToday(isLearnedToday)
                .totalWords(totalWords)
                .conqueredVideos(conqueredVideos)
                .build();

        // 최신 로그 10개 조회
        List<ExpLog> recentLogs = expLogRepository.findTop10ByUserOrderByCreatedAtDesc(user);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM.dd");

        List<UserDashBoardResponse.ExpLogInfo> expLogInfos = recentLogs.stream()
                .map(log-> UserDashBoardResponse.ExpLogInfo.builder()
                        .date(log.getCreatedAt().format(formatter))
                        .title(log.getTitle())
                        .amount(log.getAmount())
                        .build())
                .toList();

        return UserDashBoardResponse.builder()
                .levelInfo(levelInfo)
                .stats(statsInfo)
                .expLogs(expLogInfos)
                .build();
    }

}
