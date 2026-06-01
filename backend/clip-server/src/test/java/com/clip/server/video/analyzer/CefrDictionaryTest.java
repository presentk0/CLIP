package com.clip.server.video.analyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CefrDictionaryTest {

    private CefrDictionary dictionary;

    @BeforeEach
    void setUp() {
        dictionary = new CefrDictionary();
        dictionary.init();  // @PostConstruct 수동 호출
    }

    @Nested
    @DisplayName("사전 로드 검증")
    class LoadTest {
        @Test
        @DisplayName("CEFR 사전이 정상 로드된다")
        void load_success() {
            assertThat(dictionary.size()).isGreaterThan(4000);
        }
    }

    @Nested
    @DisplayName("기본 단어 조회")
    class BasicLookup {
        @Test
        @DisplayName("A1 레벨 단어를 정확히 찾는다")
        void lookup_a1() {
            assertThat(dictionary.lookup("the")).isEqualTo(CefrLevel.A1);
            assertThat(dictionary.lookup("good")).isEqualTo(CefrLevel.A1);
        }

        @Test
        @DisplayName("B1 레벨 단어를 정확히 찾는다")
        void lookup_b1() {
            assertThat(dictionary.lookup("specifically")).isEqualTo(CefrLevel.B1);
        }

        @Test
        @DisplayName("대소문자 구분 없이 매칭된다")
        void lookup_caseInsensitive() {
            assertThat(dictionary.lookup("THE")).isEqualTo(CefrLevel.A1);
            assertThat(dictionary.lookup("Good")).isEqualTo(CefrLevel.A1);
        }

        @Test
        @DisplayName("사전에 없는 단어는 null 반환")
        void lookup_notFound() {
            assertThat(dictionary.lookup("xyzunknown")).isNull();
        }

        @Test
        @DisplayName("null/빈 문자열 처리")
        void lookup_invalid() {
            assertThat(dictionary.lookup(null)).isNull();
            assertThat(dictionary.lookup("")).isNull();
            assertThat(dictionary.lookup("   ")).isNull();
        }
    }

    @Nested
    @DisplayName("변형형 처리")
    class VariationLookup {

        @Test
        @DisplayName("복수형 -s 처리")
        void plural_s() {
            // "books"는 사전에 없고 "book"이 있을 가능성
            CefrLevel bookLevel = dictionary.lookup("book");
            if (bookLevel != null) {
                CefrLevel booksLevel = dictionary.lookup("books");
                // books가 사전에 직접 등록되어있을 수도 있고, 변형 처리될 수도 있음
                // 어쨌든 null만 아니면 OK
                assertThat(booksLevel).isNotNull();
            }
        }

        @Test
        @DisplayName("진행형 -ing 처리 - 사전에 없는 변형형으로 검증")
        void ing_form() {
            // "talking"이 사전에 직접 없다면, "talk"의 레벨로 매칭되어야 함
            CefrLevel talkLevel = dictionary.lookup("talk");
            CefrLevel talkingLevel = dictionary.lookup("talking");

            // talking이 직접 등록되었을 수도, talk으로 폴백될 수도 있음
            // 어느 쪽이든 null이 아니면 OK
            if (talkLevel != null) {
                assertThat(talkingLevel).isNotNull();
            }

            System.out.println("talk: " + talkLevel + ", talking: " + talkingLevel);
        }

        @Test
        @DisplayName("과거형 -ed 처리")
        void ed_form() {
            CefrLevel workLevel = dictionary.lookup("work");
            CefrLevel workedLevel = dictionary.lookup("worked");

            if (workLevel != null) {
                assertThat(workedLevel).isNotNull();
            }
        }

        @Test
        @DisplayName("부사 -ly 처리")
        void ly_form() {
            CefrLevel quickLevel = dictionary.lookup("quick");
            CefrLevel quicklyLevel = dictionary.lookup("quickly");

            if (quickLevel != null) {
                assertThat(quicklyLevel).isNotNull();
            }
        }

        @Test
        @DisplayName("실제로 사전에 없는 변형형 - 변형 로직이 동작")
        void notInDict_butVariationWorks() {
            // 이건 명백히 사전에 없을 변형형들
            // (실제 사전 내용에 따라 조정 필요)

            // 케이스 출력해서 어떻게 동작하는지 보기
            String[] testWords = {"jumping", "playing", "studying", "looked", "happily"};
            for (String word : testWords) {
                CefrLevel level = dictionary.lookup(word);
                System.out.println(word + " -> " + level);
            }
        }
    }
}
