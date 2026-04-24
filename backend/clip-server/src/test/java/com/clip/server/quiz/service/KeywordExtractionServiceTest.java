package com.clip.server.quiz.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

    @Test
    @DisplayName("영문 자막에서 명사, 동사, 형용사 키워드를 성공적으로 추출한다")
    void extractKeywords_Success() {
        // given: 테스트용 영문 자막 문장
        String subtitleText = "The quick brown fox jumps over the lazy dog. We should implement a better strategy for the team.";

        // when: 키워드 추출 수행
        List<String> keywords = keywordExtractionService.extractKeywords(subtitleText);

        // then: 주요 학습 단어들이 포함되었는지 검증
        // 💡 팁: 만약 여기서 실패한다면, extractKeywords 내의 System.out.println 로그를 통해
        // 실제로 어떤 태그가 찍히고 있는지 확인이 필요합니다.
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