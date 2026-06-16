package com.clip.server.quiz.service;

import com.clip.server.quiz.ai.KeywordExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KeywordExtractionServiceTest {

    private KeywordExtractionService keywordExtractionService;

    @BeforeEach
    void setUp() {
        // 스프링 컨텍스트를 띄우지 않고 직접 객체를 생성합니다.
        keywordExtractionService = new KeywordExtractionService();
        // @PostConstruct 메서드를 수동으로 호출하여 모델 파일을 로드합니다.
        keywordExtractionService.init();
    }

    // ==================== 기존: extractKeywords (퀴즈 키워드 추출) ====================
    @Nested
    @DisplayName("extractKeywords - 퀴즈용 키워드 추출")
    class ExtractKeywords {

        @Test
        @DisplayName("영문 자막에서 명사, 동사, 형용사 키워드를 성공적으로 추출한다")
        void extractKeywords_Success() {
            // given: 테스트용 영문 자막 문장
            String subtitleText = "The quick brown fox jumps over the lazy dog. We should implement a better strategy for the team.";

            // when: 키워드 추출 수행
            List<String> keywords = keywordExtractionService.extractKeywords(subtitleText);

            // then: 주요 학습 단어들이 포함되었는지 검증
            assertThat(keywords).as("추출된 키워드가 비어있지 않아야 합니다.").isNotEmpty();

            // 포함 여부 확인
            assertThat(keywords).containsAnyOf("fox", "jumps", "implement", "strategy", "better");

            // 불용어 및 짧은 단어 제외 확인
            assertThat(keywords).doesNotContain("the", "we", "a", "for");

            System.out.println("최종 추출된 키워드: " + keywords);
        }

        @Test
        @DisplayName("빈 문자열이나 null이 입력되면 빈 리스트를 반환한다")
        void extractKeywords_EmptyOrNull() {
            // when & then
            assertThat(keywordExtractionService.extractKeywords("")).isEmpty();
            assertThat(keywordExtractionService.extractKeywords(null)).isEmpty();
            assertThat(keywordExtractionService.extractKeywords("   ")).isEmpty();
        }

        @Test
        @DisplayName("특수 기호나 숫자가 포함된 단어는 필터링 로직에 의해 제외된다")
        void extractKeywords_FilterNonAlphabetic() {
            // given
            String text = "Look at this! It's amazing. Error code 12345 occurred.";

            // when
            List<String> keywords = keywordExtractionService.extractKeywords(text);

            // then
            assertThat(keywords).contains("amazing");
            assertThat(keywords).doesNotContain("look!", "12345");
        }

        @Test
        @DisplayName("중복된 단어는 Set을 통해 제거되어 하나만 반환된다")
        void extractKeywords_Deduplication() {
            // given
            String text = "Practice makes perfect. Practice is important.";

            // when
            List<String> keywords = keywordExtractionService.extractKeywords(text);

            // then
            long practiceCount = keywords.stream().filter(w -> w.equals("practice")).count();
            assertThat(practiceCount).isEqualTo(1);
        }
    }

    // ==================== 신규: tokenizeForDifficulty (난이도 분석용) ====================
    @Nested
    @DisplayName("tokenizeForDifficulty - 난이도 분석용 토큰화")
    class TokenizeForDifficulty {

        @Test
        @DisplayName("영문 자막에서 의미있는 단어들을 추출한다")
        void tokenizeForDifficulty_Success() {
            // given
            String text = "I really enjoy learning English every single day with my friends.";

            // when
            List<String> tokens = keywordExtractionService.tokenizeForDifficulty(text);

            // then
            assertThat(tokens).as("토큰이 비어있지 않아야 합니다.").isNotEmpty();
            assertThat(tokens).contains("learning", "english", "day");

            System.out.println("난이도 분석용 토큰: " + tokens);
        }

        @Test
        @DisplayName("중복 단어를 허용한다 (분포 계산을 위해)")
        void tokenizeForDifficulty_AllowDuplicates() {
            // given - 명사/형용사 반복으로 변경
            String text = "I have a happy cat. The happy cat plays. A happy cat sleeps.";

            // when
            List<String> tokens = keywordExtractionService.tokenizeForDifficulty(text);

            // then: "happy"와 "cat"이 여러 번 나와야 함
            long happyCount = tokens.stream().filter(t -> t.equals("happy")).count();
            long catCount = tokens.stream().filter(t -> t.equals("cat")).count();

            assertThat(happyCount).as("'happy'는 3번 나와야 합니다.").isEqualTo(3);
            assertThat(catCount).as("'cat'은 3번 나와야 합니다.").isEqualTo(3);

            System.out.println("중복 허용 토큰: " + tokens);
        }

        @Test
        @DisplayName("불용어도 포함한다 (extractKeywords와의 차이)")
        void tokenizeForDifficulty_IncludeStopWords() {
            // given
            String text = "I am going to the store with my friend.";

            // when
            List<String> tokens = keywordExtractionService.tokenizeForDifficulty(text);

            // then: 불용어 일부도 포함되어야 함 (난이도 분석엔 필요)
            assertThat(tokens).containsAnyOf("the", "to", "with", "am");

            System.out.println("불용어 포함 토큰: " + tokens);
        }

        @Test
        @DisplayName("빈 문자열이나 null이 입력되면 빈 리스트를 반환한다")
        void tokenizeForDifficulty_EmptyOrNull() {
            // when & then
            assertThat(keywordExtractionService.tokenizeForDifficulty("")).isEmpty();
            assertThat(keywordExtractionService.tokenizeForDifficulty(null)).isEmpty();
            assertThat(keywordExtractionService.tokenizeForDifficulty("   ")).isEmpty();
        }

        @Test
        @DisplayName("숫자/특수문자는 제외된다")
        void tokenizeForDifficulty_FilterNonAlphabetic() {
            // given
            String text = "Hello world! 12345 @#$ test.";

            // when
            List<String> tokens = keywordExtractionService.tokenizeForDifficulty(text);

            // then
            assertThat(tokens).contains("hello", "world", "test");
            assertThat(tokens).doesNotContain("12345", "@#$", "!", ".");
        }

        @Test
        @DisplayName("extractKeywords보다 더 많은 단어를 포함한다")
        void tokenizeForDifficulty_MoreInclusiveThanExtract() {
            // given
            String text = "The quick brown fox jumps over the lazy dog quickly and silently.";

            // when
            List<String> keywords = keywordExtractionService.extractKeywords(text);
            List<String> analyzeTokens = keywordExtractionService.tokenizeForDifficulty(text);

            // then: 분석용이 더 많은 단어를 포함 (불용어 등)
            assertThat(analyzeTokens.size())
                    .as("난이도 분석용 토큰이 키워드보다 많아야 합니다.")
                    .isGreaterThan(keywords.size());

            System.out.println("extractKeywords (" + keywords.size() + "): " + keywords);
            System.out.println("tokenizeForDifficulty (" + analyzeTokens.size() + "): " + analyzeTokens);
        }
    }
}