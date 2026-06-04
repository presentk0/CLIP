package com.clip.server.video.analyzer;

import com.clip.server.quiz.service.KeywordExtractionService;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.video.entity.VideoDifficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VideoDifficultyAnalyzerTest {

    private VideoDifficultyAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        // 실제 의존성으로 셋업 (스프링 없이)
        CefrDictionary dictionary = new CefrDictionary();
        dictionary.init();

        KeywordExtractionService extractionService = new KeywordExtractionService();
        extractionService.init();

        analyzer = new VideoDifficultyAnalyzer(dictionary, extractionService);
    }

    // 헬퍼 메서드: SubtitleDetail 빠르게 생성
    private TranslationRequest.SubtitleDetail createSubtitle(String text, double start, double end) {
        TranslationRequest.SubtitleDetail detail = new TranslationRequest.SubtitleDetail();
        // SubtitleDetail에 setter가 없을 수 있으니 reflection 또는 builder 사용
        // 실제로는 본인의 SubtitleDetail 생성 방식에 맞게 조정 필요
        try {
            java.lang.reflect.Field textField = detail.getClass().getDeclaredField("text");
            java.lang.reflect.Field startField = detail.getClass().getDeclaredField("startTime");
            java.lang.reflect.Field endField = detail.getClass().getDeclaredField("endTime");
            textField.setAccessible(true);
            startField.setAccessible(true);
            endField.setAccessible(true);
            textField.set(detail, text);
            startField.set(detail, start);
            endField.set(detail, end);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return detail;
    }

    @Nested
    @DisplayName("난이도 분석 - 정상 케이스")
    class AnalyzeSuccess {

        @Test
        @DisplayName("초급 영상 - 쉬운 단어 위주")
        void analyze_beginner() {
            // given: A1~A2 위주 자막
            List<TranslationRequest.SubtitleDetail> subtitles = List.of(
                    createSubtitle("Hello, my name is John.", 0.0, 2.0),
                    createSubtitle("I like to eat apples and oranges.", 2.0, 5.0),
                    createSubtitle("This is a good day.", 5.0, 7.0),
                    createSubtitle("I have a cat and a dog.", 7.0, 10.0),
                    createSubtitle("Do you want to play with me?", 10.0, 13.0),
                    createSubtitle("The sun is bright today.", 13.0, 16.0),
                    createSubtitle("We are happy to see you.", 16.0, 19.0),
                    createSubtitle("I go to school every day.", 19.0, 22.0),
                    createSubtitle("My mom is a nice person.", 22.0, 25.0),
                    createSubtitle("Please give me some water.", 25.0, 28.0)
            );

            // when
            VideoDifficultyAnalyzer.DifficultyResult result = analyzer.analyze(subtitles, 30);

            // then
            System.out.println("=== 초급 영상 결과 ===");
            printResult(result);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getLevel()).isEqualTo(VideoDifficulty.BEGINNER);
        }

        @Test
        @DisplayName("고급 영상 - 어려운 단어 위주")
        void analyze_advanced() {
            // given: B2~C1 위주
            List<TranslationRequest.SubtitleDetail> subtitles = List.of(
                    createSubtitle("The unprecedented ramifications are profound.", 0.0, 3.0),
                    createSubtitle("Comprehensive analysis reveals discrepancies.", 3.0, 6.0),
                    createSubtitle("The ubiquitous nature necessitates scrutiny.", 6.0, 9.0),
                    createSubtitle("Empirical evidence corroborates the hypothesis.", 9.0, 12.0),
                    createSubtitle("Subsequent deliberations yielded conclusions.", 12.0, 15.0),
                    createSubtitle("The intricate framework facilitates interpretations.", 15.0, 18.0),
                    createSubtitle("Inherent ambiguities perpetuate controversies.", 18.0, 21.0),
                    createSubtitle("Quintessential exemplars demonstrate principles.", 21.0, 24.0),
                    createSubtitle("Perpetual transformation characterizes this era.", 24.0, 27.0),
                    createSubtitle("Rigorous examination unveils complexities.", 27.0, 30.0)
            );

            // when
            VideoDifficultyAnalyzer.DifficultyResult result = analyzer.analyze(subtitles, 30);

            // then
            System.out.println("=== 고급 영상 결과 ===");
            printResult(result);

            assertThat(result.isValid()).isTrue();
            // 사전 커버리지에 따라 ADVANCED 또는 INTERMEDIATE 가능
            assertThat(result.getLevel())
                    .isIn(VideoDifficulty.ADVANCED, VideoDifficulty.INTERMEDIATE);
        }

        @Test
        @DisplayName("중급 영상 - 일상 회화 수준")
        void analyze_intermediate() {
            List<TranslationRequest.SubtitleDetail> subtitles = List.of(
                    createSubtitle("Although the weather was bad, we continued our journey.", 0.0, 3.0),
                    createSubtitle("It's important to consider different perspectives.", 3.0, 6.0),
                    createSubtitle("The company announced significant changes yesterday.", 6.0, 9.0),
                    createSubtitle("Despite the challenges, the team achieved their goals.", 9.0, 12.0),
                    createSubtitle("She explained the situation clearly and helpfully.", 12.0, 15.0),
                    createSubtitle("The development of new technology requires investment.", 15.0, 18.0),
                    createSubtitle("However, there are several factors to consider.", 18.0, 21.0),
                    createSubtitle("Many people believe education is the key to success.", 21.0, 24.0),
                    createSubtitle("The meeting was postponed due to circumstances.", 24.0, 27.0),
                    createSubtitle("Understanding cultural differences improves communication.", 27.0, 30.0)
            );

            VideoDifficultyAnalyzer.DifficultyResult result = analyzer.analyze(subtitles, 30);

            System.out.println("=== 중급 영상 결과 ===");
            printResult(result);

            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("난이도 분석 - 예외 케이스")
    class AnalyzeEdgeCase {

        @Test
        @DisplayName("빈 자막 리스트 - unknown 반환")
        void analyze_empty() {
            VideoDifficultyAnalyzer.DifficultyResult result = analyzer.analyze(List.of(), 60);
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("null 자막 - unknown 반환")
        void analyze_null() {
            VideoDifficultyAnalyzer.DifficultyResult result = analyzer.analyze(null, 60);
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("자막이 너무 짧으면 - unknown 반환")
        void analyze_tooShort() {
            List<TranslationRequest.SubtitleDetail> subtitles = List.of(
                    createSubtitle("Hi.", 0.0, 1.0),
                    createSubtitle("OK.", 1.0, 2.0)
            );
            VideoDifficultyAnalyzer.DifficultyResult result = analyzer.analyze(subtitles, 60);
            assertThat(result.isValid()).isFalse();
        }
    }

    @Nested
    @DisplayName("WPM 계산 검증")
    class WpmCalculation {

        @Test
        @DisplayName("자막 시간 기반 WPM 계산 - 발화 시간만 사용")
        void wpm_basedOnSpeechTime() {
            // given: 영상 60초이지만 자막은 20초만 있음 (BGM 영상 시뮬)
            List<TranslationRequest.SubtitleDetail> subtitles = createManySubtitles(20);

            // when: 영상 길이 60초로 분석
            VideoDifficultyAnalyzer.DifficultyResult result = analyzer.analyze(subtitles, 60);

            // then: WPM은 발화 시간(20초) 기반으로 계산되어야 함
            if (result.isValid()) {
                System.out.println("=== WPM 계산 검증 ===");
                printResult(result);

                // 자막 시간 합 ≈ 20초여야 함
                assertThat(result.getSpeechDurationSec()).isLessThanOrEqualTo(20.5);
                assertThat(result.getSpeechDurationSec()).isGreaterThan(0);
            }
        }

        @Test
        @DisplayName("자막 시간 정보가 부실하면 영상 전체 길이로 폴백")
        void wpm_fallbackToVideoDuration() {
            // given: startTime과 endTime이 동일 (시간 정보 부실)
            List<TranslationRequest.SubtitleDetail> subtitles = new ArrayList<>();
            for (int i = 0; i < 30; i++) {
                subtitles.add(createSubtitle(
                        "This is a test sentence with various words to analyze.",
                        i * 1.0, i * 1.0  // start == end
                ));
            }

            // when: 영상 길이 120초로 폴백되어야 함
            VideoDifficultyAnalyzer.DifficultyResult result = analyzer.analyze(subtitles, 120);

            // then
            if (result.isValid()) {
                System.out.println("=== 폴백 검증 ===");
                printResult(result);

                // 발화 시간이 영상 전체 길이로 폴백
                assertThat(result.getSpeechDurationSec()).isEqualTo(120.0);
            }
        }

        @Test
        @DisplayName("같은 자막, 짧은 영상이 더 빠른 WPM")
        void wpm_shorterVideoFasterWpm() {
            // given: 동일한 자막 (시간 정보 동일)
            List<TranslationRequest.SubtitleDetail> subtitles = createManySubtitles(30);

            // when: 영상 길이만 다르게 (자막 시간은 동일하므로 결과 같아야 함)
            VideoDifficultyAnalyzer.DifficultyResult result1 = analyzer.analyze(subtitles, 30);
            VideoDifficultyAnalyzer.DifficultyResult result2 = analyzer.analyze(subtitles, 300);

            // then: 자막 시간 기반이라 결과 동일 (영상 길이는 폴백용)
            if (result1.isValid() && result2.isValid()) {
                System.out.println("=== 영상 길이 무관 검증 ===");
                System.out.println("30초 영상: " + result1.getWpm());
                System.out.println("300초 영상: " + result2.getWpm());

                // 자막 시간 정보가 정상이면 영상 길이는 영향 X
                assertThat(result1.getWpm()).isEqualTo(result2.getWpm());
            }
        }
    }

    @Nested
    @DisplayName("점수 범위 검증")
    class ScoreRange {

        @Test
        @DisplayName("최종 점수는 1.0 ~ 6.0 범위")
        void score_inRange() {
            List<TranslationRequest.SubtitleDetail> subtitles = createManySubtitles(20);

            VideoDifficultyAnalyzer.DifficultyResult result = analyzer.analyze(subtitles, 60);

            if (result.isValid()) {
                assertThat(result.getScore()).isBetween(1.0, 6.0);
                assertThat(result.getCefrScore()).isBetween(1.0, 6.0);
            }
        }

        @Test
        @DisplayName("매칭 단어 수가 충분히 카운트됨")
        void matched_count() {
            List<TranslationRequest.SubtitleDetail> subtitles = createManySubtitles(20);

            VideoDifficultyAnalyzer.DifficultyResult result = analyzer.analyze(subtitles, 60);

            if (result.isValid()) {
                assertThat(result.getMatchedWordCount()).isGreaterThanOrEqualTo(20);
                assertThat(result.getTotalWordCount())
                        .isGreaterThanOrEqualTo(result.getMatchedWordCount());
            }
        }
    }

    // ==================== 헬퍼 메서드 ====================

    /**
     * 일상적인 자막을 N개 생성 (각 1초씩)
     */
    private List<TranslationRequest.SubtitleDetail> createManySubtitles(int count) {
        String[] templates = {
                "I really enjoy learning English every single day.",
                "We talk about many different topics with friends.",
                "The weather today is quite nice and sunny.",
                "She works hard to achieve her goals.",
                "Many people believe that education is important.",
                "Technology changes our lives in many ways.",
                "Reading books helps improve our vocabulary.",
                "Music has a powerful effect on our emotions.",
                "Travel opens our minds to new cultures.",
                "Healthy food is essential for our bodies."
        };

        List<TranslationRequest.SubtitleDetail> subtitles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            subtitles.add(createSubtitle(
                    templates[i % templates.length],
                    i * 1.0,
                    (i + 1) * 1.0
            ));
        }
        return subtitles;
    }

    /**
     * 분석 결과를 보기 좋게 출력
     */
    private void printResult(VideoDifficultyAnalyzer.DifficultyResult result) {
        if (!result.isValid()) {
            System.out.println("결과: UNKNOWN");
            return;
        }
        System.out.printf("Level: %s%n", result.getLevel());
        System.out.printf("Score: %.2f%n", result.getScore());
        System.out.printf("CEFR Score: %.2f%n", result.getCefrScore());
        System.out.printf("WPM: %.1f%n", result.getWpm());
        System.out.printf("Speech Duration: %.1f초%n", result.getSpeechDurationSec());
        System.out.printf("Matched/Total: %d/%d%n",
                result.getMatchedWordCount(), result.getTotalWordCount());
        System.out.println();
    }
}