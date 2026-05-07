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
import com.clip.server.quiz.entity.SessionType;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.quiz.repository.QuizSessionWordRepository;
import com.clip.server.user.dto.response.BadgeAwardResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.badge.BadgeType;
import com.clip.server.user.entity.badge.UserBadge;
import com.clip.server.user.entity.learning.LearningHistory;
import com.clip.server.user.repository.LearningHistoryRepository;
import com.clip.server.user.repository.UserBadgeRepository;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.entity.VideoKeyWord;
import com.clip.server.video.repository.VideoKeyWordRepository;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.entity.WordType;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
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

    private final QuizService quizService;
    private final OpenAIService openAIService;
    private final QuizFeedbackGenerator quizFeedbackGenerator;


    /**
     * 중간 섹션 퀴즈 생성 (OX, 빈칸)
     */
    @Transactional
    public QuizGenerateResponse generateSectionQuiz(Long userId, QuizGenerateRequest request) {

        // User, Video 정보 확인
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Video video = videoRepository.findById(request.getVideoId()).orElseThrow(()-> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));

        // 세션 정보 확인 및 생성
        QuizSession quizSession = getOrCreateSession(user, video, request.getSessionType());

        // 전체 학습 시간(보정값 적용)
        double calDuration = video.getDuration() * DENSITY_FACTOR;
        // 전체 섹션 수
        int totalSecCount = totalSections(video.getDuration());
        // 섹션별 퀴즈 수
        int quizPerSection = countQuizPerSection(video.getDuration());
        // 섹션 시간 및 범위 계산
        double range = (double)calDuration/totalSecCount;
        double start = range * (request.getSectionNumber()-1);
        double end = range * request.getSectionNumber();

       // 해당 구간 단어 조회 및 우선순위(1~3순위) 정렬
        List<QuizSessionWord> candidates = quizSessionWordRepository.findWordsBySection(quizSession.getId(), start, end);
        // 전체 리스트를 무작위로 섞음
        Collections.shuffle(candidates);
        // 그 상태에서 우선순위대로 정렬 (셔플된 결과 내에서 등급순 정렬됨)
        candidates.sort(Comparator.comparingInt(this::getPriority));

        List<QuizWordRequest> quizWordRequests = candidates.stream()
                .map(this::mapToRequest)
                .collect(Collectors.toList());

        // 단어 부족 시 AI 보충
        if (quizWordRequests.size() < quizPerSection) {
            int needCount = quizPerSection - quizWordRequests.size();
            List<Map<String, String>> aiRecommended = openAIService.recommendImportantWords(video.getTitle(), needCount);
            for (Map<String, String> rec : aiRecommended) {
                quizWordRequests.add(new QuizWordRequest(rec.get("word"), rec.get("meaning"), "00:00"));
            }
        }

        // 중복 제거 후 필요한 개수만
        List<QuizWordRequest> finalWords = quizWordRequests.stream()
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
            // 수집/호버 단어 존재: OX 1, 빈칸 1 보장 + 나머지 랜덤
            quizzes.add(quizService.createOXQuiz(quizSession.getId(), user.getId(), mapToRequest(candidates.get(0))));
            if (candidates.size() > 1 && quizPerSection > 1) {
                quizzes.add(quizService.createBlankQuiz(quizSession.getId(), user.getId(), mapToRequest(candidates.get(1))));
            }
            for (int i = 2; i < Math.min(candidates.size(), quizPerSection); i++) {
                quizzes.add(quizService.createBlankQuiz(quizSession.getId(), user.getId(), mapToRequest(candidates.get(i))));
            }
        } else {
            // 수집 단어 없음: 빈칸 1 + 나머지 OX (SYSTEM 단어 위주)
            if (!candidates.isEmpty()) {
                quizzes.add(quizService.createBlankQuiz(quizSession.getId(), user.getId(), mapToRequest(candidates.get(0))));
                for (int i = 1; i < Math.min(candidates.size(), quizPerSection); i++) {
                    quizzes.add(quizService.createOXQuiz(quizSession.getId(), user.getId(), mapToRequest(candidates.get(i))));
                }
            } else {
                log.warn("섹션 {}에 사용할 단어가 없습니다.", request.getSectionNumber());
            }
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

        QuizSession quizSession = getOrCreateSession(user, video, request.getSessionType());

        List<QuizSessionWord> allWords = quizSessionWordRepository.findAllByQuizSession(quizSession);

        Collections.shuffle(allWords);
        allWords.sort(Comparator.comparingInt(this::getPriority));


        List<QuizWordRequest> finalCandidates = allWords.stream()
                .map(this::mapToRequest)
                .collect(Collectors.toList());

        // 3순위 AI 판단: 5개 미만인 경우 OpenAI 호출
        if (finalCandidates.size() < 5) {
            int needCount = 5 - finalCandidates.size();
            List<Map<String, String>> aiRecommended = openAIService.recommendImportantWords(video.getTitle(), needCount);
            for (Map<String, String> rec : aiRecommended) {
                finalCandidates.add(new QuizWordRequest(rec.get("word"), rec.get("meaning"), "00:00"));
            }
        }

        // 중복 단어 제거 후 5개 선택
        List<QuizWordRequest> finalFive = finalCandidates.stream()
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
        List<QuizDetailResponse> quizzes = quizService.createMatchingQuiz(quizSession.getId(), user.getId(), finalFive);

        return mapToQuizStartResponse(quizSession, quizzes);
    }

    private QuizSession getOrCreateSession(User user, Video video, SessionType type) {
        return quizSessionRepository.findByUserAndVideo(user, video)
                .orElseGet(() -> {
                    QuizSession newSession = QuizSession.builder()
                            .user(user)
                            .video(video)
                            .sessionType(type)
                            .totalQuizCount(calculateTotalQuizCount(video.getDuration()))
                            .build();
                    QuizSession savedSession = quizSessionRepository.save(newSession);

                    syncSessionWords(savedSession, user, video);

                    return savedSession;
                });
    }

    private void syncSessionWords(QuizSession quizSession, User user, Video video) {

        List<QuizSessionWord> sessionWords = new ArrayList<>();

        List<CollectedWord> collected = collectedWordRepository.findAllByUserAndVideo(user, video);
        for (CollectedWord cw : collected) {
            sessionWords.add(QuizSessionWord.builder()
                    .quizSession(quizSession)
                    .word(cw.getWord())
                    .sentence(cw.getSentence())
                    .translation(cw.getTranslation())
                    .wordType(cw.getWordType())
                    .timestamp(convertToSeconds(cw.getTimestamp()))
                    .build());
        }

        List<VideoKeyWord> systemKeyWords = videoKeyWordRepository.findAllByVideo(video);
        for(VideoKeyWord vk: systemKeyWords) {
            // 중복 방지
            boolean isDuplicate = sessionWords.stream()
                    .anyMatch(sw -> sw.getWord().equalsIgnoreCase(vk.getWord()));
            if(!isDuplicate) {
                sessionWords.add(QuizSessionWord.builder()
                        .quizSession(quizSession)
                        .word(vk.getWord())
                        .sentence(vk.getSentence())
                        .translation(vk.getTranslation())
                        .wordType(WordType.SYSTEM)
                        .timestamp(vk.getTimestamp())
                        .build());
            }
        }
        quizSessionWordRepository.saveAll(sessionWords);
        log.info("세션 {}에 대해 {}개의 단어 스냅샷 생성 완료", quizSession.getId(), sessionWords.size());
    }

    private boolean hasUserActionWords(List<QuizSessionWord> words) {
        return words.stream().anyMatch(w -> w.getWordType() == WordType.COLLECT || w.getWordType() == WordType.POPUP);
    }

    private QuizWordRequest mapToRequest(QuizSessionWord word) {
        return new QuizWordRequest(word.getWord(), word.getTranslation(), formatTimestamp(word.getTimestamp()));
    }

    private String formatTimestamp(Double totalSeconds) {
        if (totalSeconds == null) return "00:00";
        int total = totalSeconds.intValue();
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    private Double convertToSeconds(String timestamp) {
        String[] parts = timestamp.split(":");
        int minutes = Integer.parseInt(parts[0]);
        int seconds = Integer.parseInt(parts[1]);
        return (double) (minutes * 60 + seconds);
    }

    // 섹션당 퀴즈 수 계산
    private int calculateTotalQuizCount(int videoTotalDuration) {
        int totalQuizCount = 0;
        if(videoTotalDuration<60) {
            // 섹션 개수 0개
            totalQuizCount = 0;
        } else if (videoTotalDuration<600) {
            // 섹션 개수 1개, 섹션 최대 퀴즈 개수 3개
            totalQuizCount = 3;
        } else if (videoTotalDuration<1200) {
            // 섹션 개수 2개, 섹션 최대 퀴즈 개수 3개
            totalQuizCount = 6;
        }  else if (1200<=videoTotalDuration) {
            // 섹션 개수 3개, 섹션 최대 퀴즈 개수 4개, 매칭 퀴즈수 5개
            totalQuizCount = 12;
        }
        return totalQuizCount;
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
        learningHistory.updateTotalWatchTime(video.getDuration());
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
        Optional mostWrongType = quizResultRepository.findMostWrongQuizType(sessionId);

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
