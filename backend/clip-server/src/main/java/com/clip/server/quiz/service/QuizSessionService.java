package com.clip.server.quiz.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.dto.response.QuizGenerateResponse;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizSessionWord;
import com.clip.server.quiz.entity.SessionType;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizSessionService {

    private static final double DENSITY_FACTOR = 0.8;

    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final CollectedWordRepository collectedWordRepository;

//    @Transactional
//    public QuizGenerateResponse quizGenerate(Long userId, String videoId, int sectionNumber, SessionType sessionType) {
//
//        // User, Video 정보 확인
//        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
//        Video video = videoRepository.findById(videoId).orElseThrow(()-> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
//
//        // 세션 정보 확인 및 생성
//        QuizSession quizSession = quizSessionRepository.findByUserAndVideo(user, video)
//                .orElseGet(()->{
//                 QuizSession newQuizSession = QuizSession.builder()
//                         .user(user)
//                         .video(video)
//                         .sessionType(sessionType)
//                         .totalQuizCount(calculateTotalQuizCount(video.getDuration())) //영상 길이별 전체 퀴즈 수 계산
//                         .build();
//                 return quizSessionRepository.save(newQuizSession);
//                });
//
//        // 전체 학습 시간(보정값 적용)
//        double calDuration = video.getDuration() * DENSITY_FACTOR;
//        // 전체 섹션 수
//        int totalSecCount = totalSections(video.getDuration());
//        // 섹션별 퀴즈 수
//        int totalQuizCount = calculateTotalQuizCount(video.getDuration());
//
//        // 섹션 시간 및 범위 계산
//        double range = (double)calDuration/totalSecCount;
//        double start = range * (sectionNumber-1);
//        double end = range * sectionNumber;
//
////        List<QuizSessionWord> candidates = sess
////        List<QuizDetailResponse> quizzes;
//
//
//
//
//        return mapToQuizStartResponse();
//    }

    private void syncSessionWords(QuizSession quizSession, User user, Video video) {
        List<CollectedWord> collectedWords = collectedWordRepository.findAllByUserAndVideo(user, video);

        List<QuizSessionWord> sessionWords = collectedWords.stream()
                .map(cw-> QuizSessionWord.builder()
                        .quizSession(quizSession)
                        .word(cw.getWord())
                        .sentence(cw.getSentence())
                        .translation(cw.getTranslation())
                        .wordType(cw.getWordType())
                        .timestamp(cw.getTimestamp())
                        .build())
                .collect(Collectors.toList());


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
            default -> 4;
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
