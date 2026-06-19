package com.clip.server.quiz.persistence;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizSessionWord;
import com.clip.server.quiz.entity.SessionStatus;
import com.clip.server.quiz.entity.SessionType;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.quiz.repository.QuizSessionWordRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.entity.VideoKeyWord;
import com.clip.server.video.repository.VideoKeyWordRepository;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.entity.WordType;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizTxService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizSessionWordRepository quizSessionWordRepository;
    private final CollectedWordRepository collectedWordRepository;
    private final VideoKeyWordRepository videoKeyWordRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QuizSession getOrCreateSession(User user, Video video, SessionType type) {

        return quizSessionRepository.findByUserAndVideoAndStatus(user, video, SessionStatus.IN_PROGRESS)
                .map(existingSession -> {
                    //  기존 세션이 있어도 새로 저장한 단어가 있으면 추가 동기화
                    syncNewlyCollectedWords(existingSession, user, video);
                    return existingSession;
                })
                .orElseGet(() -> {
                    try {

                        QuizSession newSession = QuizSession.builder()
                                .user(user)
                                .video(video)
                                .sessionType(type)
                                .build();

                        QuizSession saved = quizSessionRepository.saveAndFlush(newSession);

                        syncSessionWords(saved, user, video);

                        return saved;

                    } catch (DataIntegrityViolationException e) {

                        log.warn("### 세션 동시 생성 충돌 발생. 기존 세션 재조회");

                        return quizSessionRepository.findByUserAndVideoAndStatus(user, video, SessionStatus.IN_PROGRESS)
                                .orElseThrow(() ->
                                        new BusinessException(ErrorCode.SESSION_NOT_FOUND));
                    }
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

    private Double convertToSeconds(String timestamp) {
        if (timestamp == null || !timestamp.contains(":")) return 0.0;
        try {
            String[] parts = timestamp.split(":");
            return (double) (Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]));
        } catch (Exception e) {
            log.error("타임스탬프 변환 실패: {}", timestamp);
            return 0.0;
        }
    }

    /**
     *  기존 세션에 새로 저장된 단어만 추가 동기화
     */
    private void syncNewlyCollectedWords(QuizSession quizSession, User user, Video video) {
        // 1. 현재 세션에 이미 들어있는 단어 목록
        List<QuizSessionWord> existingWords = quizSessionWordRepository.findAllByQuizSession(quizSession);
        Set<String> existingWordSet = existingWords.stream()
                .map(w -> w.getWord().toLowerCase())
                .collect(Collectors.toSet());

        // 2. 사용자가 수집한 단어 중 세션에 없는 것만 추가
        List<CollectedWord> collected = collectedWordRepository.findAllByUserAndVideo(user, video);
        List<QuizSessionWord> newWords = new ArrayList<>();

        for (CollectedWord cw : collected) {
            if (!existingWordSet.contains(cw.getWord().toLowerCase())) {
                newWords.add(QuizSessionWord.builder()
                        .quizSession(quizSession)
                        .word(cw.getWord())
                        .sentence(cw.getSentence())
                        .translation(cw.getTranslation())
                        .wordType(cw.getWordType())
                        .timestamp(convertToSeconds(cw.getTimestamp()))
                        .build());
            }
        }

        if (!newWords.isEmpty()) {
            quizSessionWordRepository.saveAll(newWords);
            log.info("세션 {}에 새 수집 단어 {}개 추가됨", quizSession.getId(), newWords.size());
        }
    }
}
