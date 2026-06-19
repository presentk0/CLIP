package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.ai.OpenAIService;
import com.clip.server.quiz.ai.QuizFallbackService;
import com.clip.server.quiz.dto.request.QuizGenerateRequest;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.QuizCompleteResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizGenerateResponse;
import com.clip.server.quiz.entity.*;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.quiz.repository.QuizSessionWordRepository;
import com.clip.server.subtitle.entity.Subtitle;
import com.clip.server.subtitle.repository.SubtitleRepository;
import com.clip.server.user.dto.response.BadgeAwardResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.badge.BadgeType;
import com.clip.server.user.entity.badge.UserBadge;
import com.clip.server.user.entity.exp.ExpSourceType;
import com.clip.server.user.entity.learning.LearningHistory;
import com.clip.server.user.repository.LearningHistoryRepository;
import com.clip.server.user.repository.UserBadgeRepository;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.user.service.ExpLogService;
import com.clip.server.video.entity.Video;
import com.clip.server.word.entity.WordType;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class QuizProcessor {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizSessionWordRepository quizSessionWordRepository;
    private final QuizResultRepository quizResultRepository;
    private final SubtitleRepository subtitleRepository;
    private final UserRepository userRepository;
    private final CollectedWordRepository collectedWordRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    private final UserBadgeRepository userBadgeRepository;

    private final QuizService quizService;
    private final OpenAIService openAIService;
    private final QuizFeedbackGenerator quizFeedbackGenerator;
    private final QuizFallbackService quizFallbackService;
    private final ExpLogService expLogService;

    private static final double DENSITY_FACTOR = 0.8;
    private static final int VIDEO_COMPENSATION = 200;

    // ==================== 섹션 퀴즈 생성 ====================
    @Transactional
    public QuizGenerateResponse generateSectionQuizInternal(Long userId, Long sessionId, QuizGenerateRequest request) {


        QuizSession quizSession = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        Video video = quizSession.getVideo();
        // NPE 방지
        int duration = (video.getDuration() != null) ? video.getDuration() : 0;

        // 전체 섹션 수
        int totalSecCount = totalSections(duration);
        // 섹션별 퀴즈 수
        int quizPerSection = countQuizPerSection(duration);
        // 전체 학습 시간(보정값 적용)
        double calDuration = duration * DENSITY_FACTOR;
        int sectionNum = Math.max(request.getSectionNumber(), 1);

        // 섹션 시간 및 범위 계산
        double range = (double) calDuration / Math.max(totalSecCount, 1);
        double start = range * (sectionNum - 1);
        double end = range * sectionNum;

        // 해당 구간 단어 조회 및 우선순위(1~3순위) 정렬
        List<QuizSessionWord> candidates = quizSessionWordRepository.findWordsBySection(quizSession.getId(), start, end);

        // 전체 리스트를 무작위로 섞음
        Collections.shuffle(candidates);

        // 그 상태에서 우선순위대로 정렬 (셔플된 결과 내에서 등급순 정렬됨)
        candidates.sort(Comparator.comparingInt(this::getPriority));

        boolean hasUserWords = candidates.stream()
                .anyMatch(c -> c.getWordType() == WordType.COLLECT || c.getWordType() == WordType.POPUP);

        List<QuizWordRequest> quizWordRequests = candidates.stream()
                .map(this::mapToRequest)
                .collect(Collectors.toList());

        // 단어 부족 시 AI 보충
        if (quizWordRequests.size() < quizPerSection) {
            supplementWordsWithAI(quizWordRequests, video, start, end, quizPerSection);
        }


        // 중복 제거 후 필요한 개수만
        List<QuizWordRequest> finalWords = deduplicateAndLimit(quizWordRequests, quizPerSection);

        // 퀴즈 생성
        List<QuizDetailResponse> quizzes = createSectionQuizzes(
                quizSession.getId(), userId, finalWords, hasUserWords, request.getVideoId(), sectionNum);

        return mapToQuizGenerateResponse(quizSession, quizzes);
    }

    private void supplementWordsWithAI(List<QuizWordRequest> quizWordRequests, Video video,
                                       double start, double end, int quizPerSection) {
        int needCount = quizPerSection - quizWordRequests.size();

        List<Subtitle> sectionSubtitles = subtitleRepository.findByVideoAndStartTimeBetween(video, start, end);
        String combinedSubtitles = sectionSubtitles.stream()
                .map(Subtitle::getText)
                .collect(Collectors.joining(" "));

        String aiInputText = combinedSubtitles.isBlank() ? video.getTitle() : combinedSubtitles;

        log.info("### AI 단어 추출 요청 - 입력 텍스트 요약: {}...",
                aiInputText.substring(0, Math.min(aiInputText.length(), 50)));

        List<Map<String, String>> aiRecommended = Optional.ofNullable(
                openAIService.recommendImportantWords(aiInputText, needCount)
        ).orElse(Collections.emptyList());

        for (Map<String, String> rec : aiRecommended) {
            if (rec == null) continue;

            String word = rec.get("word");
            String meaning = rec.get("meaning");

            if (word == null || word.isBlank()) continue;

            boolean isDuplicate = quizWordRequests.stream()
                    .map(QuizWordRequest::getWord)
                    .filter(Objects::nonNull)
                    .anyMatch(existing -> existing.equalsIgnoreCase(word));

            if (!isDuplicate) {
                quizWordRequests.add(new QuizWordRequest(word, meaning != null ? meaning : "", "00:00"));
            }
        }
    }

    private List<QuizDetailResponse> createSectionQuizzes(Long sessionId, Long userId,
                                                          List<QuizWordRequest> finalWords,
                                                          boolean hasUserWords,
                                                          String videoId, int sectionNum) {
        List<QuizDetailResponse> quizzes = new ArrayList<>();

        if (finalWords.isEmpty()) {
            log.warn("videoId: {}, 섹션 {}에 사용할 단어가 없습니다.", videoId, sectionNum);
            return quizzes;
        }

        if (hasUserWords) {
            // [전략 A] 수집/호버 단어 존재: OX 1, 빈칸 1 보장 + 나머지 빈칸
            quizzes.add(quizService.createOXQuiz(sessionId, userId, finalWords.get(0)));
            if (finalWords.size() > 1) {
                quizzes.add(quizService.createBlankQuiz(sessionId, userId, finalWords.get(1)));
            }
            for (int i = 2; i < finalWords.size(); i++) {
                quizzes.add(quizService.createBlankQuiz(sessionId, userId, finalWords.get(i)));
            }
        } else {
            // [전략 B] 수집 단어 없음: 빈칸 1 + 나머지 OX
            quizzes.add(quizService.createBlankQuiz(sessionId, userId, finalWords.get(0)));
            for (int i = 1; i < finalWords.size(); i++) {
                quizzes.add(quizService.createOXQuiz(sessionId, userId, finalWords.get(i)));
            }
        }

        return quizzes;
    }

    // ==================== 매칭 퀴즈 생성 ====================
    @Transactional
    public QuizGenerateResponse generateMatchingQuizInternal(Long userId, Long sessionId, Video video) {

        QuizSession quizSession = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        List<QuizSessionWord> allWords = quizSessionWordRepository.findAllByQuizSession(quizSession);

        // 우선순위별 그룹 분리 및 셔플
        List<QuizSessionWord> prioritizedWords = prioritizeAndShuffleWords(allWords);

        List<QuizWordRequest> finalCandidates = prioritizedWords.stream()
                .map(this::mapToRequest)
                .collect(Collectors.toList());

        // 5개 미만인 경우 AI 보충
        if (finalCandidates.size() < 5) {
            supplementMatchingWordsWithAI(finalCandidates, video, 5);
        }

        // 중복 제거 후 5개 선택
        List<QuizWordRequest> finalWords = deduplicateAndLimit(finalCandidates, 5);

        if (finalWords.size() < 5) {
            log.warn("매칭 퀴즈 5개 확보 실패 - 현재 개수={}, videoId={}",
                    finalWords.size(), video.getVideoId());
        }

        // 퀴즈 생성 (AI 실패 시 Fallback)
        List<QuizDetailResponse> quizzes;
        try {
            quizzes = quizService.createMatchingQuiz(sessionId, userId, finalWords);
        } catch (Exception e) {
            log.warn("AI 매칭 퀴즈 생성 실패, Fallback 로직 가동", e);
            quizzes = quizFallbackService.createLocalMatchingQuizNewTx(sessionId, userId, finalWords);
        }

        return mapToQuizGenerateResponse(quizSession, quizzes);
    }

    private List<QuizSessionWord> prioritizeAndShuffleWords(List<QuizSessionWord> allWords) {
        List<QuizSessionWord> collectWords = allWords.stream()
                .filter(w -> w.getWordType() == WordType.COLLECT).collect(Collectors.toList());
        List<QuizSessionWord> popupWords = allWords.stream()
                .filter(w -> w.getWordType() == WordType.POPUP).collect(Collectors.toList());
        List<QuizSessionWord> systemWords = allWords.stream()
                .filter(w -> w.getWordType() == WordType.SYSTEM).collect(Collectors.toList());

        Collections.shuffle(collectWords);
        Collections.shuffle(popupWords);
        Collections.shuffle(systemWords);

        List<QuizSessionWord> result = new ArrayList<>();
        result.addAll(collectWords);
        result.addAll(popupWords);
        result.addAll(systemWords);
        return result;
    }

    private void supplementMatchingWordsWithAI(List<QuizWordRequest> candidates, Video video, int targetCount) {
        int needCount = targetCount - candidates.size();

        List<Subtitle> subtitles = subtitleRepository.findAllByVideo(video);
        StringBuilder sb = new StringBuilder();
        for (Subtitle s : subtitles) {
            sb.append(s.getText()).append(" ");
            if (sb.length() > 3000) {
                sb.append("...");
                break;
            }
        }

        String aiInput = sb.length() == 0 ? video.getTitle() : sb.toString();
        log.info("### [Matching Quiz AI] 자막 기반 단어 추천 요청 ({}자)", aiInput.length());

        try {
            List<Map<String, String>> aiRecommended = Optional.ofNullable(
                    openAIService.recommendImportantWords(aiInput, needCount)
            ).orElse(Collections.emptyList());

            for (Map<String, String> rec : aiRecommended) {
                if (rec == null) continue;
                String word = rec.get("word");
                String meaning = rec.get("meaning");
                if (word == null || word.isBlank()) continue;

                boolean notExists = candidates.stream()
                        .map(QuizWordRequest::getWord)
                        .filter(Objects::nonNull)
                        .noneMatch(existing -> existing.equalsIgnoreCase(word));

                if (notExists) {
                    candidates.add(new QuizWordRequest(word, meaning != null ? meaning : "", "00:00"));
                }
            }
        } catch (Exception e) {
            log.error("매칭 퀴즈 AI 단어 보충 실패", e);
        }
    }

    // ==================== 퀴즈 세션 완료 ====================

    @Transactional
    public QuizCompleteResponse completeQuizSession(Long userId, Long sessionId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuizSession quizSession = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        Video video = quizSession.getVideo();

        // 학습 이력 조회 또는 생성
        LearningHistory learningHistory = learningHistoryRepository.findByUserAndVideo(user, video)
                .orElseGet(() -> learningHistoryRepository.save(
                        LearningHistory.builder()
                                .user(user)
                                .video(video)
                                .lastAccessAt(LocalDateTime.now())
                                .build()
                ));

        // 수집한 단어 수
        Long wordCounts = collectedWordRepository
                .countByUserIdAndVideoVideoIdAndWordType(user.getId(), video.getVideoId(), WordType.COLLECT);

        // 학습 이력 업데이트
        learningHistory.updateCollectedWord(wordCounts);
        learningHistory.updateCompletionCount();
        learningHistory.updateTotalWatchTime(video.getDuration() != null ? video.getDuration() : 0);
        learningHistory.updateLastAccessAt();

        // 퀴즈 결과 집계
        List<QuizResult> quizResults = quizResultRepository.findByQuizSession(quizSession);
        int totalQuizCount = quizResults.size();
        int correctCount = (int) quizResults.stream()
                .filter(result -> Boolean.TRUE.equals(result.getIsCorrect()))
                .count();
        int wrongCount = totalQuizCount - correctCount;
        double accuracy = totalQuizCount > 0 ? ((double) correctCount / totalQuizCount) : 0.0;
        int baseQuizExp = quizResults.stream()
                .mapToInt(result -> result.getEarnedExp() != null ? result.getEarnedExp() : 0)
                .sum();

        // 배지 획득
        BadgeAwardResponse badgeAward = awardBadgeAndBonusExp(user, video, learningHistory.getCompletionCount());

        // 레벨업 체크용
        int oldLevel = user.getLevel();

        // 1. 퀴즈 정답 보상
        if(baseQuizExp>0) {
            String description = String.format("퀴즈 정답 (%d문제)", correctCount);
            expLogService.addExp(user, ExpSourceType.QUIZ_CORRECT, sessionId, description, baseQuizExp);
        }

        // 2. 영상 완주 보상
        expLogService.addExp(user, ExpSourceType.VIDEO_COMPLETE, sessionId, VIDEO_COMPENSATION);

        // 3. 배지 보상
        if (badgeAward.getBadgeType() != null && badgeAward.getBonusExp() > 0) {
            ExpSourceType badgeSource = mapBadgeToExpSource(badgeAward.getBadgeType());
            if (badgeSource != null) {
                expLogService.addExp(user, badgeSource, sessionId, badgeAward.getBonusExp());
            }
        }

        // 최종 경험치
        int totalEarnedExp = baseQuizExp + VIDEO_COMPENSATION + badgeAward.getBonusExp();

        int newLevel = user.getLevel();
        boolean levelUp = newLevel > oldLevel;

        // 세션 완료
        quizSession.complete(totalQuizCount, correctCount, wrongCount, totalEarnedExp);

        // 피드백 생성
        Optional<QuizType> mostWrongType = quizResultRepository.findMostWrongQuizType(sessionId);
        String feedback = quizFeedbackGenerator.generateFeedback(accuracy, video.getTitle(), mostWrongType);

        return QuizCompleteResponse.builder()
                .sessionId(sessionId)
                .totalQuizCount(totalQuizCount)
                .correctCount(correctCount)
                .wrongCount(wrongCount)
                .accuracy(accuracy)
                .earnedExp(totalEarnedExp)
                .levelUp(levelUp)
                .currentLevel(newLevel)
                .newBadge(toBadgeInfo(video.getVideoId(), badgeAward.getBadgeType()))
                .completedAt(LocalDateTime.now())
                .feedback(feedback)
                .build();
    }


    // ==================== 유틸리티 메서드 ====================

    // 헬퍼 메서드
    private ExpSourceType mapBadgeToExpSource(BadgeType badgeType) {
        return switch (badgeType) {
            case BRONZE -> ExpSourceType.BADGE_BRONZE;
            case SILVER -> ExpSourceType.BADGE_SILVER;
            case GOLD -> ExpSourceType.BADGE_GOLD;
            case NONE, COMPLETION -> null;
        };
    }

    private List<QuizWordRequest> deduplicateAndLimit(List<QuizWordRequest> requests, int limit) {
        return requests.stream()
                .filter(q -> q.getWord() != null && !q.getWord().isBlank())
                .collect(Collectors.toMap(
                        QuizWordRequest::getWord,
                        req -> req,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    private QuizWordRequest mapToRequest(QuizSessionWord word) {
        return new QuizWordRequest(
                word.getWord(),
                word.getTranslation(),
                formatTimestamp(word.getTimestamp())
        );
    }

    private String formatTimestamp(Double totalSeconds) {
        if (totalSeconds == null) return "00:00";
        int total = totalSeconds.intValue();
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private int countQuizPerSection(int duration) {
        return (duration > 1200) ? 4 : 3;
    }

    private int totalSections(int duration) {
        if (duration < 60) return 0;
        if (duration < 600) return 1;
        if (duration < 1200) return 2;
        return 3;
    }

    private int getPriority(QuizSessionWord word) {
        return switch (word.getWordType()) {
            case COLLECT -> 1;
            case POPUP -> 2;
            case SYSTEM -> 3;
        };
    }

    private QuizGenerateResponse mapToQuizGenerateResponse(QuizSession session, List<QuizDetailResponse> quizzes) {
        return QuizGenerateResponse.builder()
                .sessionId(session.getId())
                .sessionType(session.getSessionType())
                .videoId(session.getVideo().getVideoId())
                .totalQuizCount(session.getTotalQuizCount())
                .quizzes(quizzes)
                .build();
    }

    private BadgeAwardResponse awardBadgeAndBonusExp(User user, Video video, int currentCount) {
        BadgeType badgeType = null;
        int bonusExp = 0;

        if (currentCount == 1) {
            badgeType = BadgeType.BRONZE;
            bonusExp = 300;
        } else if (currentCount == 2) {
            badgeType = BadgeType.SILVER;
            bonusExp = 500;
        } else if (currentCount == 3) {
            badgeType = BadgeType.GOLD;
            bonusExp = 1000;
        }

        if (badgeType != null && !userBadgeRepository.existsByUserAndVideoAndBadgeType(user, video, badgeType)) {
            userBadgeRepository.save(UserBadge.builder()
                    .badgeType(badgeType)
                    .user(user)
                    .video(video)
                    .build());
        }

        return BadgeAwardResponse.builder()
                .badgeType(badgeType)
                .bonusExp(bonusExp)
                .build();
    }

    private QuizCompleteResponse.BadgeInfo toBadgeInfo(String videoId, BadgeType badgeType) {
        if (badgeType == null) return null;
        return QuizCompleteResponse.BadgeInfo.builder()
                .videoId(videoId)
                .badgeType(badgeType)
                .build();
    }
}
