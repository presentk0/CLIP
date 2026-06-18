package com.clip.server.admin.dashboard.stats.service;

import com.clip.server.admin.dashboard.stats.dto.request.ChatPatternStatRequest;
import com.clip.server.admin.dashboard.stats.dto.request.UserVideoStatRequest;
import com.clip.server.admin.dashboard.stats.dto.response.ChatPatternStatResponse;
import com.clip.server.admin.dashboard.stats.dto.response.UserVideoWordStatResponse;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.clip.server.chat.repository.ChatMessageRepository;
import com.clip.server.chat.repository.ChatRoomRepository;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.admin.dashboard.stats.dto.request.AiUsageStatRequest;
import com.clip.server.admin.dashboard.stats.dto.response.AiUsageStatResponse;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 어드민 통계 조회이므로 전역 가볍게 readOnly 설정
public class AdminStatsService {

    private final CollectedWordRepository collectedWordRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final QuizResultRepository quizResultRepository;
    private final ChatRoomRepository chatRoomRepository;

    /**
     * 유저-영상별 단어 수집 통계 목록 조회
     */
    public List<UserVideoWordStatResponse> getUserVideoWordStatistics(UserVideoStatRequest request) {
        log.info("어드민 지표 분석 - 유저-영상별 단어 수집 패턴 조회 시작");

        // 1. 기간 필터링 검색 조건 파싱 및 기본값 방어 (날짜가 없으면 최근 한 달로 세팅)
        LocalDateTime startDateTime = (request.getStartDate() != null)
                ? request.getStartDate().atStartOfDay()
                : LocalDateTime.now().minusMonths(1).with(LocalTime.MIN);

        LocalDateTime endDateTime = (request.getEndDate() != null)
                ? request.getEndDate().atTime(LocalTime.MAX)
                : LocalDateTime.now().with(LocalTime.MAX);

        // 2. Request DTO에 담긴 limit 값을 스프링 Pageable에 매핑 (과부하 방지)
        int limit = (request.getLimit() != null && request.getLimit() > 0) ? request.getLimit() : 50;
        Pageable pageable = PageRequest.of(0, limit);

        log.debug("통계 조회 기간: {} ~ {}, 조회 제한 수: {}", startDateTime, endDateTime, limit);

        // 3. 레포지토리 복합 쿼리 호출 후 결과 반환
        return collectedWordRepository.findUserVideoWordStatistics(startDateTime, endDateTime, pageable);
    }

    /**
     * AI API 호출 횟수 비용 통계 조회
     */
    public AiUsageStatResponse getAiUsageStatistics(AiUsageStatRequest request) {
        log.info("어드민 비용 분석 - AI API 호출 통계 집계 시작");

        // 1. 기간 방어 로직 (날짜 비어있을 시 기본값 세팅)
        LocalDateTime startDateTime = (request.getStartDate() != null)
                ? request.getStartDate().atStartOfDay()
                : LocalDateTime.now().minusMonths(1).with(LocalTime.MIN);

        LocalDateTime endDateTime = (request.getEndDate() != null)
                ? request.getEndDate().atTime(LocalTime.MAX)
                : LocalDateTime.now().with(LocalTime.MAX);

        // 2. 각각의 인덱스 태워서 개별 카운팅 쿼리 수행 (속도 극대화)
        Long chatCallCount = chatMessageRepository.countAiMessagesByPeriod(startDateTime, endDateTime);
        Long quizExplCount = quizResultRepository.countQuizExplanationsByPeriod(startDateTime, endDateTime);

        // 3. 총합 산출
        Long totalCallCount = chatCallCount + quizExplCount;

        log.debug("AI 통계 결과 - Total: {}, Chat: {}, Quiz: {}", totalCallCount, chatCallCount, quizExplCount);

        // 4. 프로젝트 표준 빌더 패턴으로 Response DTO 조립
        return AiUsageStatResponse.builder()
                .totalCallCount(totalCallCount)
                .chatBotCallCount(chatCallCount)
                .quizExplanationCount(quizExplCount)
                .build();
    }

    /**
     * AI 채팅 세션 사용 패턴 통계 조회
     */
    public ChatPatternStatResponse getChatPatternStatistics(ChatPatternStatRequest request) {
        log.info("어드민 행동 분석 - AI 채팅 세션 사용 패턴 집계 시작");

        // 1. 기간 방어 로직
        LocalDateTime startDateTime = (request.getStartDate() != null)
                ? request.getStartDate().atStartOfDay()
                : LocalDateTime.now().minusMonths(1).with(LocalTime.MIN);

        LocalDateTime endDateTime = (request.getEndDate() != null)
                ? request.getEndDate().atTime(LocalTime.MAX)
                : LocalDateTime.now().with(LocalTime.MAX);

        // 2. 대시보드 데이터 수집
        Long activeRooms = chatRoomRepository.countByStatusAndPeriod(
                com.clip.server.chat.entity.ChatRoomStatus.IN_PROGRESS, startDateTime, endDateTime
        );
        Long completedRooms = chatRoomRepository.countByStatusAndPeriod(
                com.clip.server.chat.entity.ChatRoomStatus.COMPLETED, startDateTime, endDateTime
        );
        Double avgTurns = chatMessageRepository.getAverageTurnsPerRoom(startDateTime, endDateTime);
        Double avgScore = chatMessageRepository.getAveragePronunciationScore(startDateTime, endDateTime);

        // 3. Null 데이터 방어 처리 (첫 데이터가 없으면 null 반환 가능성이 있음)
        avgTurns = (avgTurns != null) ? Math.round(avgTurns * 10) / 10.0 : 0.0; // 소수점 첫째짜리 반올림
        avgScore = (avgScore != null) ? Math.round(avgScore * 10) / 10.0 : 0.0;

        log.debug("채팅 패턴 집계 완료 - Active: {}, Completed: {}, AvgTurns: {}, AvgScore: {}",
                activeRooms, completedRooms, avgTurns, avgScore);

        return ChatPatternStatResponse.builder()
                .activeRoomsCount(activeRooms)
                .completedRoomsCount(completedRooms)
                .averageTurnsPerRoom(avgTurns)
                .averagePronunciationScore(avgScore)
                .build();
    }
}