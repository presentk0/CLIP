package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizGenerateRequest;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizGenerateResponse;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizSessionWord;
import com.clip.server.quiz.entity.SessionType;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.quiz.repository.QuizSessionWordRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.entity.VideoKeyWord;
import com.clip.server.video.repository.VideoKeyWordRepository;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.entity.WordType;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSessionService {

    private static final double DENSITY_FACTOR = 0.8;

    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final CollectedWordRepository collectedWordRepository;
    private final VideoKeyWordRepository videoKeyWordRepository;
    private final QuizSessionWordRepository quizSessionWordRepository;
    private final QuizService quizService;
    private final GeminiService geminiService;

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
        int totalQuizCount = calculateTotalQuizCount(video.getDuration());

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

        List<QuizDetailResponse> quizzes = new ArrayList<>();

        // 출제 전략 적용
        if (hasUserActionWords(candidates)) {
            // 수집/호버 단어 존재: OX 1, 빈칸 1 보장 + 나머지 랜덤
            quizzes.add(quizService.createOXQuiz(quizSession.getId(), user.getId(), mapToRequest(candidates.get(0))));
            if (candidates.size() > 1 && totalQuizCount > 1) {
                quizzes.add(quizService.createBlankQuiz(quizSession.getId(), user.getId(), mapToRequest(candidates.get(1))));
            }
            for (int i = 2; i < Math.min(candidates.size(), totalQuizCount); i++) {
                quizzes.add(quizService.createBlankQuiz(quizSession.getId(), user.getId(), mapToRequest(candidates.get(i))));
            }
        } else {
            // 수집 단어 없음: 빈칸 1 + 나머지 OX (SYSTEM 단어 위주)
            if (!candidates.isEmpty()) {
                quizzes.add(quizService.createBlankQuiz(quizSession.getId(), user.getId(), mapToRequest(candidates.get(0))));
                for (int i = 1; i < Math.min(candidates.size(), totalQuizCount); i++) {
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

        // 3순위 AI 판단: 5개 미만인 경우 Gemini 호출
        if (finalCandidates.size() < 5) {
            int needCount = 5 - finalCandidates.size();
            List<Map<String, String>> aiRecommended = geminiService.recommendImportantWords(video.getTitle(), needCount);
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
    public QuizGenerateResponse mapToQuizStartResponse(QuizSession quizSession, List<QuizDetailResponse> quizzes) {
        return QuizGenerateResponse.builder()
                .sessionId(quizSession.getId())
                .sessionType(quizSession.getSessionType())
                .videoId(quizSession.getVideo().getVideoId())
                .totalQuizCount(quizSession.getTotalQuizCount())
                .quizzes(quizzes)
                .build();
    }
}
