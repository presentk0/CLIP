package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizGenerateRequest;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.QuizCompleteResponse;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizGenerateResponse;
import com.clip.server.quiz.entity.QuizResult;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizSessionWord;
import com.clip.server.quiz.entity.QuizType;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.quiz.repository.QuizSessionWordRepository;
import com.clip.server.subtitle.entity.Subtitle;
import com.clip.server.subtitle.repository.SubtitleRepository;
import com.clip.server.user.dto.response.BadgeAwardResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.badge.BadgeType;
import com.clip.server.user.entity.badge.UserBadge;
import com.clip.server.user.entity.learning.LearningHistory;
import com.clip.server.user.repository.LearningHistoryRepository;
import com.clip.server.user.repository.UserBadgeRepository;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoKeyWordRepository;
import com.clip.server.video.repository.VideoRepository;
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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSessionService {

    private static final double DENSITY_FACTOR = 0.8;
    private static final int CORRECT_EXP = 100;
    // 영상 시청 완료 보상

    private static final int VIDEO_COMPENSATION = 200;

    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final CollectedWordRepository collectedWordRepository;
    private final VideoKeyWordRepository videoKeyWordRepository;
    private final QuizSessionWordRepository quizSessionWordRepository;
    private final QuizResultRepository quizResultRepository;
    private final LearningHistoryRepository learningHistoryRepository;
    private final UserBadgeRepository userBadgeRepository;
    private final SubtitleRepository subtitleRepository;

    private final QuizService quizService;
    private final OpenAIService openAIService;
    private final QuizFeedbackGenerator quizFeedbackGenerator;
    private final QuizFallbackService quizFallbackService;
    private final QuizTxService quizTxService;

    /**
     * 중간 섹션 퀴즈 생성 (OX, 빈칸)
     */
    @Transactional
    public QuizGenerateResponse generateSectionQuiz(Long userId, QuizGenerateRequest request) {

        // User, Video 정보 확인
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Video video = videoRepository.findById(request.getVideoId()).orElseThrow(()-> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        // 세션 정보 확인 및 생성
        QuizSession quizSession = quizTxService.getOrCreateSession(user, video, request.getSessionType());

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
            int needCount = quizPerSection - quizWordRequests.size();

            // 특정 시간대 자막 추출
            List<Subtitle> sectionSubtitles = subtitleRepository.findByVideoAndStartTimeBetween(video, start, end);
            String combinedSubtitles = sectionSubtitles.stream()
                    .map(Subtitle::getText)
                    .collect(Collectors.joining(" "));

            // 자막이 너무 비어있을 경우를 대비해 제목을 백업으로 활용
            String aiInputText = combinedSubtitles.isBlank() ? video.getTitle() : combinedSubtitles;

            String preview = Optional.ofNullable(aiInputText)
                    .filter(s -> !s.isBlank())
                    .map(s -> s.substring(0, Math.min(s.length(), 50)) + "...")
                    .orElse("[EMPTY]");

            log.info("### AI 단어 추출 요청 - 입력 텍스트 요약: {}", preview);

            List<Map<String, String>> aiRecommended =
                    Optional.ofNullable(
                            openAIService.recommendImportantWords(aiInputText, needCount)
                    ).orElse(Collections.emptyList());

            for (Map<String, String> rec : aiRecommended) {

                // null 데이터 방어
                if (rec == null) {
                    continue;
                }

                String word = rec.get("word");
                String meaning = rec.get("meaning");

                // word 없으면 스킵
                if (word == null || word.isBlank()) {
                    continue;
                }

                boolean isDuplicate = quizWordRequests.stream()
                        .map(QuizWordRequest::getWord)
                        .filter(Objects::nonNull)
                        .anyMatch(existing -> existing.equalsIgnoreCase(word));

                if (!isDuplicate) {
                    quizWordRequests.add(
                            new QuizWordRequest(
                                    word,
                                    meaning != null ? meaning : "",
                                    "00:00"
                            )
                    );
                }

            }
        }

        // 중복 제거 후 필요한 개수만
        List<QuizWordRequest> finalWords = quizWordRequests.stream()
                .filter(q -> q.getWord() != null && !q.getWord().isBlank())
                .collect(Collectors.toMap(
                        QuizWordRequest::getWord,
                        req -> req,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .limit(quizPerSection)
                .collect(Collectors.toList());

        List<QuizDetailResponse> quizzes = new ArrayList<>();

        // 출제 전략 적용
        if (!finalWords.isEmpty()) {
            if (hasUserWords) {
                // [전략 A] 수집/호버 단어 존재: OX 1, 빈칸 1 보장 + 나머지 빈칸
                quizzes.add(quizService.createOXQuiz(quizSession.getId(), user.getId(), finalWords.get(0)));
                if (finalWords.size() > 1) {
                    quizzes.add(quizService.createBlankQuiz(quizSession.getId(), user.getId(), finalWords.get(1)));
                }
                for (int i = 2; i < finalWords.size(); i++) {
                    quizzes.add(quizService.createBlankQuiz(quizSession.getId(), user.getId(), finalWords.get(i)));
                }
            } else {
                // [전략 B] 수집 단어 없음: 빈칸 1 + 나머지 OX (SYSTEM/AI 단어 위주)
                quizzes.add(quizService.createBlankQuiz(quizSession.getId(), user.getId(), finalWords.get(0)));
                for (int i = 1; i < finalWords.size(); i++) {
                    quizzes.add(quizService.createOXQuiz(quizSession.getId(), user.getId(), finalWords.get(i)));
                }
            }
        } else {
            log.warn("videoId: {}, 섹션 {}에 사용할 단어가 전혀 없습니다.", request.getVideoId(), request.getSectionNumber());
        }

        return mapToQuizStartResponse(quizSession, quizzes);
    }

    /**
     * 최종 매칭 퀴즈 생성
     */
    @Transactional
    public QuizGenerateResponse generateMatchingQuiz(Long userId, QuizGenerateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Video video = videoRepository.findById(request.getVideoId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        QuizSession quizSession = quizTxService.getOrCreateSession(user, video, request.getSessionType());

        List<QuizSessionWord> allWords =
                quizSessionWordRepository.findAllByQuizSession(quizSession);

        // 우선순위별 그룹 분리
        List<QuizSessionWord> collectWords = allWords.stream()
                .filter(w -> w.getWordType() == WordType.COLLECT)
                .collect(Collectors.toList());

        List<QuizSessionWord> popupWords = allWords.stream()
                .filter(w -> w.getWordType() == WordType.POPUP)
                .collect(Collectors.toList());

        List<QuizSessionWord> systemWords = allWords.stream()
                .filter(w -> w.getWordType() == WordType.SYSTEM)
                .collect(Collectors.toList());

        // 각 그룹 내부만 랜덤 셔플
        Collections.shuffle(collectWords);
        Collections.shuffle(popupWords);
        Collections.shuffle(systemWords);

        // 우선순위 순서대로 병합
        List<QuizSessionWord> prioritizedWords = new ArrayList<>();

        prioritizedWords.addAll(collectWords);
        prioritizedWords.addAll(popupWords);
        prioritizedWords.addAll(systemWords);

        // QuizWordRequest 변환
        List<QuizWordRequest> finalCandidates = prioritizedWords.stream()
                .map(this::mapToRequest)
                .collect(Collectors.toList());

        // 3순위 AI 판단: 5개 미만인 경우 OpenAI 호출
        if (finalCandidates.size() < 5) {
            int needCount = 5 - finalCandidates.size();

            // 1. 해당 영상의 모든 자막을 가져와 합칩니다.
            List<Subtitle> sampleSubtitles = subtitleRepository.findAllByVideo(video);

            StringBuilder sb = new StringBuilder();
            for (Subtitle s : sampleSubtitles) {
                sb.append(s.getText()).append(" ");
                // 글자수 3000자 제한
                if (sb.length() > 3000) {
                    sb.append("..."); // 내용이 더 있음을 표시
                    break;
                }
            }

            String aiInput = sb.length() == 0 ? video.getTitle() : sb.toString();

            log.info("### [Matching Quiz AI] 최적화된 자막(약 {}자) 기반 단어 추천 요청", aiInput.length());

            try {
                List<Map<String, String>> aiRecommended =
                        Optional.ofNullable(
                                openAIService.recommendImportantWords(aiInput, needCount)
                        ).orElse(Collections.emptyList());

                for (Map<String, String> rec : aiRecommended) {

                    if (rec == null) {
                        continue;
                    }

                    String word = rec.get("word");
                    String meaning = rec.get("meaning");

                    if (word == null || word.isBlank()) {
                        continue;
                    }

                    boolean notExists = finalCandidates.stream()
                            .map(QuizWordRequest::getWord)
                            .filter(Objects::nonNull)
                            .noneMatch(existing -> existing.equalsIgnoreCase(word));

                    if (notExists) {

                        finalCandidates.add(
                                new QuizWordRequest(
                                        word,
                                        meaning != null ? meaning : "",
                                        "00:00"
                                )
                        );
                    }
                }
            } catch (Exception e) {
                log.error("### 매칭 퀴즈 AI 단어 보충 실패", e);
            }
        }

        // 중복 단어 제거 후 5개 선택
        List<QuizWordRequest> finalWords = finalCandidates.stream()
                .filter(q -> q.getWord() != null && !q.getWord().isBlank())
                .collect(Collectors.toMap(
                        QuizWordRequest::getWord,
                        req -> req,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                .limit(5)
                .collect(Collectors.toList());

        if (finalWords.size() < 5) {
            log.warn(
                    "매칭 퀴즈 5개 확보 실패 - 현재 개수={}, videoId={}, sessionId={}",
                    finalWords.size(),
                    video.getVideoId(),
                    quizSession.getId()
            );
        }

        List<QuizDetailResponse> quizzes;
        try {
            // 1차 시도: AI를 통한 매칭 퀴즈 생성
            quizzes = quizService.createMatchingQuiz(quizSession.getId(), user.getId(), finalWords);
        } catch (Exception e) {
            // 2차 시도 (Fallback): AI 실패 시 서버 내부 데이터로 즉시 생성
            log.warn("AI 매칭 퀴즈 생성 실패, 자체 생성 로직(Local Fallback) 가동", e);
            quizzes = quizFallbackService.createLocalMatchingQuizNewTx(quizSession.getId(), user.getId(), finalWords);
        }

        return mapToQuizStartResponse(quizSession, quizzes);
    }

    private QuizWordRequest mapToRequest(QuizSessionWord word) {
        return new QuizWordRequest(word.getWord(), word.getTranslation(), formatTimestamp(word.getTimestamp()));
    }

    private String formatTimestamp(Double totalSeconds) {
        if (totalSeconds == null) return "00:00";
        int total = totalSeconds.intValue();
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    // 섹션당 퀴즈 개수 계산
    private int countQuizPerSection(int videoTotalDuration) {
        return (videoTotalDuration>1200) ? 4 : 3;
    }

    // 전체 섹션 수 계산
    private int totalSections(int videoTotalDuration) {
        if(videoTotalDuration<60) return 0;
        if(videoTotalDuration<600) return 1;
        if(videoTotalDuration<1200) return 2;
        return 3;
    }

    // 우선 순위 계산
    private int getPriority(QuizSessionWord word) {
        return switch(word.getWordType()) {
            case COLLECT -> 1;
            case POPUP -> 2;
            case SYSTEM -> 3;
        };
    }

    // QuizGenerateResponse 변환 메서드
    private QuizGenerateResponse mapToQuizStartResponse(QuizSession quizSession, List<QuizDetailResponse> quizzes) {
        return QuizGenerateResponse.builder()
                .sessionId(quizSession.getId())
                .sessionType(quizSession.getSessionType())
                .videoId(quizSession.getVideo().getVideoId())
                .totalQuizCount(quizSession.getTotalQuizCount())
                .quizzes(quizzes)
                .build();
    }

    // 퀴즈 세션 만료 메서드
    @Transactional
    public QuizCompleteResponse completeQuizSession(Long userId, Long sessionId) {

        // 1. 유저, 세션, 학습 이력 확인
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        QuizSession quizSession = quizSessionRepository.findById(sessionId)
                .orElseThrow(()-> new BusinessException(ErrorCode.SESSION_NOT_FOUND));

        Video video = quizSession.getVideo();

        LearningHistory learningHistory = learningHistoryRepository.findByUserAndVideo(user, video)
                .orElseGet(()->{
                    LearningHistory newLearningHistory = LearningHistory.builder()
                            .user(user)
                            .video(video)
                            .lastAccessAt(LocalDateTime.now())
                            .build();
                    learningHistoryRepository.save(newLearningHistory);
                    return newLearningHistory;
                });

        // 사용자가 해당 영상에서 수집한 단어 수 계산
        Long wordCounts = collectedWordRepository.countByUserIdAndVideoVideoIdAndWordType(user.getId(), video.getVideoId(), WordType.COLLECT);

        // 학습 이력 업데이트(단어수, 영상 학습 횟수, 영상 총 학습 시간, 영상 마지막 학습 시간)
        learningHistory.updateCollectedWord(wordCounts);
        learningHistory.updateCompletionCount();
        learningHistory.updateTotalWatchTime(
                video.getDuration() != null ? video.getDuration() : 0
        );
        learningHistory.updateLastAccessAt();

        // 퀴즈 결과 집계
        List<QuizResult> quizResults = quizResultRepository.findByQuizSession(quizSession);

        int totalQuizCount = quizResults.size();
        int correctCount = (int) quizResults.stream()
                .filter(result -> Boolean.TRUE.equals(result.getIsCorrect()))
                .count();
        int wrongCount = totalQuizCount-correctCount;
        double accuracy = totalQuizCount>0 ? ((double) correctCount/totalQuizCount) : 0.0;
        int baseQuizExp = quizResults.stream()
                .mapToInt(result -> result.getEarnedExp() != null ? result.getEarnedExp() : 0)
                .sum();

        // 배지 획득 로직
        BadgeAwardResponse badgeAwardResponse = awardBadgeAndBonusExp(user, video, learningHistory.getCompletionCount());

        // 최종 경험치 계산
        int totalEarnedExp = baseQuizExp + VIDEO_COMPENSATION + badgeAwardResponse.getBonusExp(); // 기본 퀴즈 보상 + 영상 보상 + 배지 획득시 보너스 EXP

        // 레벨업 로직 처리
        int oldLevel = user.getLevel();
        user.addExp(totalEarnedExp);
        int newLevel = user.getLevel();
        boolean levelUp = newLevel > oldLevel;

        // 세션 완료 상태 업데이트 IN_PROGRESS -> COMPLETED
        quizSession.complete(totalQuizCount, correctCount, wrongCount, totalEarnedExp);

        // 가장 많이 틀린 퀴즈 타입 추출 (예: "OX", "빈칸")
        Optional<QuizType> mostWrongType = quizResultRepository.findMostWrongQuizType(sessionId);

        // 2. 피드백 생성 (AI 호출 없이 즉시 생성!)
        String feedback = quizFeedbackGenerator.generateFeedback(
                accuracy,
                video.getTitle(),
                mostWrongType
        );

        return QuizCompleteResponse.builder()
                .sessionId(sessionId)
                .totalQuizCount(totalQuizCount)
                .correctCount(correctCount)
                .wrongCount(wrongCount)
                .accuracy(accuracy)
                .earnedExp(totalEarnedExp)
                .levelUp(levelUp)
                .currentLevel(newLevel)
                .newBadge(toBadgeInfo(video.getVideoId(), badgeAwardResponse.getBadgeType()))
                .completedAt(LocalDateTime.now())
                .feedback(feedback)
                .build();
    }

    private BadgeAwardResponse awardBadgeAndBonusExp(User user, Video video, int currentCount) {
        BadgeType badgeType = null;
        int bonusExp = 0;

        if(currentCount == 1) {
            badgeType = BadgeType.BRONZE;
            bonusExp = 300;
        } else if (currentCount == 2) {
            badgeType = BadgeType.SILVER;
            bonusExp = 500;
        } else if (currentCount == 3) {
            badgeType = BadgeType.GOLD;
            bonusExp = 1000;
        }

        if(badgeType !=null && !userBadgeRepository.existsByUserAndVideoAndBadgeType(user, video, badgeType)) {
            UserBadge userBadge = UserBadge.builder()
                    .badgeType(badgeType)
                    .user(user)
                    .video(video)
                    .build();
            userBadgeRepository.save(userBadge);
        }
        return BadgeAwardResponse.builder()
                .badgeType(badgeType)
                .bonusExp(bonusExp)
                .build();
    }

    private QuizCompleteResponse.BadgeInfo toBadgeInfo(String videoId, BadgeType badgeType) {
        if (badgeType == null) {
            return null;
        }
        return QuizCompleteResponse.BadgeInfo.builder()
                .videoId(videoId)
                .badgeType(badgeType)
                .build();
    }
}
