package com.clip.server.video.analyzer;

import com.clip.server.quiz.ai.KeywordExtractionService;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.video.entity.VideoDifficulty;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoDifficultyAnalyzer {

    private final CefrDictionary cefrDictionary;
    private final KeywordExtractionService extractionService;

    private static final int MIN_MATCHED_WORDS = 20;
    private static final double CEFR_WEIGHT = 0.75;
    private static final double WPM_WEIGHT = 0.25;

    public DifficultyResult analyze(List<TranslationRequest.SubtitleDetail> subtitles, int videoDurationSec) {
        if (subtitles == null || subtitles.isEmpty()) {
            return DifficultyResult.unknown();
        }

        // 1. 텍스트 합치고 토큰화
        String fullText = subtitles.stream()
                .map(TranslationRequest.SubtitleDetail::getText)
                .collect(Collectors.joining(" "));

        List<String> tokens = extractionService.tokenizeForDifficulty(fullText);
        if (tokens.isEmpty()) {
            return DifficultyResult.unknown();
        }

        // 2. CEFR 분포
        Map<CefrLevel, Integer> distribution = new EnumMap<>(CefrLevel.class);
        int matchedCount = 0;

        for (String token : tokens) {
            CefrLevel level = cefrDictionary.lookup(token);
            if (level != null) {
                distribution.merge(level, 1, Integer::sum);
                matchedCount++;
            }
        }

        if (matchedCount < MIN_MATCHED_WORDS) {
            log.warn("CEFR 매칭 부족: {} (전체: {})", matchedCount, tokens.size());
            return DifficultyResult.unknown();
        }

        // 3. CEFR 점수
        double cefrScore = calculateCefrScore(distribution, matchedCount);

        // 4. WPM 계산 (자막 시간 기반)
        double speechSec = calculateSpeechDuration(subtitles, videoDurationSec);
        double wpm = speechSec > 0 ? tokens.size() / (speechSec / 60.0) : 0;
        double wpmScore = normalizeWpm(wpm);

        // 5. 최종 점수
        double finalScore = cefrScore * CEFR_WEIGHT + wpmScore * WPM_WEIGHT;
        VideoDifficulty level = toDifficultyLevel(finalScore);

        log.info("난이도 분석 - cefrScore: {}, speechSec: {}, wpm: {}, final: {}, level: {}",
                String.format("%.2f", cefrScore),
                String.format("%.1f", speechSec),
                String.format("%.1f", wpm),
                String.format("%.2f", finalScore),
                level);

        return DifficultyResult.builder()
                .level(level)
                .score(finalScore)
                .cefrScore(cefrScore)
                .wpm(wpm)
                .speechDurationSec(speechSec)
                .matchedWordCount(matchedCount)
                .totalWordCount(tokens.size())
                .build();
    }

    /**
     * 실제 발화 시간 계산 (자막의 endTime - startTime 합산)
     * - 자막 데이터 부실 시 영상 전체 길이로 폴백
     */
    private double calculateSpeechDuration(List<TranslationRequest.SubtitleDetail> subtitles, int fallbackSec) {
        double totalSpeech = subtitles.stream()
                .filter(s -> s.getStartTime() != null && s.getEndTime() != null)
                .mapToDouble(s -> Math.max(0, s.getEndTime() - s.getStartTime()))
                .sum();

        // 자막 시간 정보가 부실하면 영상 전체 길이 사용
        if (totalSpeech < 1.0) {
            log.warn("자막 시간 정보 부실 - 영상 전체 길이로 폴백: {}초", fallbackSec);
            return fallbackSec;
        }

        return totalSpeech;
    }

    private double calculateCefrScore(Map<CefrLevel, Integer> dist, int total) {
        double sum = 0;
        for (Map.Entry<CefrLevel, Integer> entry : dist.entrySet()) {
            sum += entry.getKey().getScore() * entry.getValue();
        }
        return sum / total;
    }

    private double normalizeWpm(double wpm) {
        if (wpm < 120) return 1.5; // 매우 느림
        if (wpm < 140) return 2.5; // 느림
        if (wpm < 160) return 3.5; // 보통
        if (wpm < 180) return 4.5; // 빠름
        return 5.5;                // 매우 빠름
    }

    private VideoDifficulty toDifficultyLevel(double score) {
        if (score < 2.5) return VideoDifficulty.BEGINNER;
        if (score < 4.0) return VideoDifficulty.INTERMEDIATE;
        return VideoDifficulty.ADVANCED;
    }

    @Getter
    @Builder
    public static class DifficultyResult {
        private VideoDifficulty level;
        private double score;
        private double cefrScore;
        private double wpm;
        private double speechDurationSec;
        private int matchedWordCount;
        private int totalWordCount;

        public static DifficultyResult unknown() {
            return DifficultyResult.builder().level(null).score(0).build();
        }

        public boolean isValid() {
            return level != null;
        }
    }
}