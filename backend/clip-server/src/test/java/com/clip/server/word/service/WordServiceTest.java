package com.clip.server.word.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.word.dto.request.CollectedWordRequest;
import com.clip.server.word.dto.response.CollectedWordResponse;
import com.clip.server.word.dto.response.WordListResponse;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.entity.WordMeaning;
import com.clip.server.word.entity.WordType;
import com.clip.server.word.repository.CollectedWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WordServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private CollectedWordRepository collectedWordRepository;

    @InjectMocks
    private WordService wordService;

    private final Long TEST_USER_ID = 1L;
    private CollectedWordRequest request;
    private User fakeUser;
    private Video fakeVideo;

    @BeforeEach
    void setUp() {
        CollectedWordRequest.MeaningByPos meaning = new CollectedWordRequest.MeaningByPos(
                "명사",
                List.of("사과", "사과나무")
        );

        request = new CollectedWordRequest(
                "v1", "title1", "apple",
                List.of(meaning),
                "I eat an apple", "0:01",
                "나는 사과를 먹는다.",
                WordType.COLLECT
        );

        fakeUser = User.builder().build();
        ReflectionTestUtils.setField(fakeUser, "id", TEST_USER_ID);

        fakeVideo = Video.builder()
                .videoId("v1")
                .title("title1")
                .build();
    }

    // ============================================
    // 단어 수집 (save) - 변경 없음
    // ============================================

    @Test
    @DisplayName("단어 수집 성공 테스트 (기존 데이터 없음)")
    void collectWord_success() {
        // 1. Given
        given(userRepository.findById(TEST_USER_ID))
                .willReturn(Optional.of(fakeUser));
        given(collectedWordRepository.findByUserIdAndVideo_VideoIdAndWord(
                anyLong(), anyString(), anyString()))
                .willReturn(Optional.empty());
        given(videoRepository.findById("v1"))
                .willReturn(Optional.of(fakeVideo));

        CollectedWord fakeSavedWord = CollectedWord.builder()
                .word("effort")
                .meaningsByPos(List.of(
                        new WordMeaning("명사", List.of("노력", "수고"))
                ))
                .build();
        given(collectedWordRepository.save(any(CollectedWord.class)))
                .willReturn(fakeSavedWord);

        // 2. When
        wordService.save(TEST_USER_ID, request);

        // 3. Then
        verify(collectedWordRepository, times(1)).save(any(CollectedWord.class));
    }

    @Test
    @DisplayName("이미 COLLECT된 단어를 또 COLLECT 하려고 하면 에러가 발생합니다.")
    void collectWord_duplicate_exception() {
        // 1. Given
        CollectedWord alreadyCollectedWord = CollectedWord.builder()
                .wordType(WordType.COLLECT)
                .meaningsByPos(List.of(
                        new WordMeaning("명사", List.of("사과"))
                ))
                .build();

        given(userRepository.findById(TEST_USER_ID))
                .willReturn(Optional.of(fakeUser));
        given(collectedWordRepository.findByUserIdAndVideo_VideoIdAndWord(
                anyLong(), anyString(), anyString()))
                .willReturn(Optional.of(alreadyCollectedWord));

        // 2. When & Then
        assertThatThrownBy(() -> wordService.save(TEST_USER_ID, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("이미 POPUP으로 저장된 단어를 COLLECT로 요청하면 타입이 업데이트된다.")
    void upgrade_popup_to_collect_success() {
        // 1. Given
        String videoId = "v1";
        String wordText = "apple";

        CollectedWord existingPopupWord = CollectedWord.builder()
                .word(wordText)
                .wordType(WordType.POPUP)
                .user(fakeUser)
                .meaningsByPos(List.of(
                        new WordMeaning("명사", List.of("사과"))
                ))
                .build();
        ReflectionTestUtils.setField(existingPopupWord, "id", 100L);

        given(userRepository.findById(TEST_USER_ID))
                .willReturn(Optional.of(fakeUser));
        given(collectedWordRepository.findByUserIdAndVideo_VideoIdAndWord(
                TEST_USER_ID, videoId, wordText))
                .willReturn(Optional.of(existingPopupWord));
        given(collectedWordRepository.countByUser(fakeUser)).willReturn(1L);

        // 2. When
        CollectedWordResponse response = wordService.save(TEST_USER_ID, request);

        // 3. Then
        assertThat(response.getWordId()).isEqualTo(100L);
        assertThat(existingPopupWord.getWordType()).isEqualTo(WordType.COLLECT);
        assertThat(existingPopupWord.getSentence()).isEqualTo(request.getSentence());
    }

    // ============================================
    //  단어장 조회 (getWords) - 수정/추가
    // ============================================

    @Test
    @DisplayName("단어장 조회 - 기본 파라미터(최신순/전체)로 조회 성공")
    void getWords_default_success() {
        // 1. Given
        CollectedWord word = CollectedWord.builder()
                .word("apple")
                .meaningsByPos(List.of(
                        new WordMeaning("명사", List.of("사과", "사과나무"))
                ))
                .video(fakeVideo)
                .user(fakeUser)
                .wordType(WordType.COLLECT)
                .build();
        ReflectionTestUtils.setField(word, "id", 1L);

        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("collected_at").descending());
        Page<CollectedWord> wordPage = new PageImpl<>(List.of(word), pageRequest, 1);

        given(collectedWordRepository.searchMyWords(
                eq(TEST_USER_ID),
                eq(WordType.COLLECT.name()),
                isNull(),       // filter=all → todayStart는 null
                isNull(),       // keyword 없음
                any(Pageable.class)
        )).willReturn(wordPage);

        // 2. When
        WordListResponse response = wordService.getWords(
                TEST_USER_ID, 0, 10, "latest", "all", null
        );

        // 3. Then
        assertThat(response.getWords()).hasSize(1);
        assertThat(response.getWords().get(0).getWord()).isEqualTo("apple");
        assertThat(response.getWords().get(0).getMeanings()).containsExactly("사과", "사과나무");
        assertThat(response.getPagination().getTotalCount()).isEqualTo(1L);

        verify(collectedWordRepository).searchMyWords(
                eq(TEST_USER_ID),
                eq(WordType.COLLECT.name()),
                isNull(),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("단어장 조회 - 검색어(keyword)가 있으면 trim하여 전달한다")
    void getWords_withKeyword_success() {
        // 1. Given
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("collected_at").descending());
        Page<CollectedWord> wordPage = new PageImpl<>(List.of(), pageRequest, 0);

        given(collectedWordRepository.searchMyWords(
                eq(TEST_USER_ID),
                eq(WordType.COLLECT.name()),
                isNull(),
                eq("appre"),    // trim된 값
                any(Pageable.class)
        )).willReturn(wordPage);

        // 2. When
        wordService.getWords(TEST_USER_ID, 0, 10, "latest", "all", "  appre  ");

        // 3. Then
        verify(collectedWordRepository).searchMyWords(
                eq(TEST_USER_ID),
                eq(WordType.COLLECT.name()),
                isNull(),
                eq("appre"),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("단어장 조회 - 빈 문자열 검색어는 null로 변환되어 전달된다")
    void getWords_emptyKeyword_convertedToNull() {
        // 1. Given
        Page<CollectedWord> wordPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(collectedWordRepository.searchMyWords(
                anyLong(), anyString(), any(), any(), any(Pageable.class)
        )).willReturn(wordPage);

        // 2. When
        wordService.getWords(TEST_USER_ID, 0, 10, "latest", "all", "   ");

        // 3. Then
        verify(collectedWordRepository).searchMyWords(
                eq(TEST_USER_ID),
                eq(WordType.COLLECT.name()),
                isNull(),
                isNull(),      // 빈 문자열 → null
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("단어장 조회 - filter=today이면 오늘 시작 시간이 전달된다")
    void getWords_todayFilter_success() {
        // 1. Given
        Page<CollectedWord> wordPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(collectedWordRepository.searchMyWords(
                anyLong(), anyString(), any(), any(), any(Pageable.class)
        )).willReturn(wordPage);

        // 2. When
        wordService.getWords(TEST_USER_ID, 0, 10, "latest", "today", null);

        // 3. Then - todayStart 파라미터 캡처해서 검증
        ArgumentCaptor<LocalDateTime> todayStartCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(collectedWordRepository).searchMyWords(
                eq(TEST_USER_ID),
                eq(WordType.COLLECT.name()),
                todayStartCaptor.capture(),
                isNull(),
                any(Pageable.class)
        );

        LocalDateTime capturedTodayStart = todayStartCaptor.getValue();
        assertThat(capturedTodayStart).isNotNull();
        assertThat(capturedTodayStart.getHour()).isEqualTo(0);
        assertThat(capturedTodayStart.getMinute()).isEqualTo(0);
        assertThat(capturedTodayStart.getSecond()).isEqualTo(0);
    }

    @Test
    @DisplayName("단어장 조회 - sort=alphabet-asc이면 word 오름차순 정렬로 조회한다")
    void getWords_sortAlphabetAsc_success() {
        // 1. Given
        Page<CollectedWord> wordPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(collectedWordRepository.searchMyWords(
                anyLong(), anyString(), any(), any(), any(Pageable.class)
        )).willReturn(wordPage);

        // 2. When
        wordService.getWords(TEST_USER_ID, 0, 10, "alphabet-asc", "all", null);

        // 3. Then - Pageable 캡처해서 정렬 조건 검증
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(collectedWordRepository).searchMyWords(
                anyLong(), anyString(), any(), any(), pageableCaptor.capture()
        );

        Sort capturedSort = pageableCaptor.getValue().getSort();
        Sort.Order order = capturedSort.getOrderFor("word");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("단어장 조회 - sort=alphabet-desc이면 word 내림차순 정렬로 조회한다")
    void getWords_sortAlphabetDesc_success() {
        // 1. Given
        Page<CollectedWord> wordPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(collectedWordRepository.searchMyWords(
                anyLong(), anyString(), any(), any(), any(Pageable.class)
        )).willReturn(wordPage);

        // 2. When
        wordService.getWords(TEST_USER_ID, 0, 10, "alphabet-desc", "all", null);

        // 3. Then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(collectedWordRepository).searchMyWords(
                anyLong(), anyString(), any(), any(), pageableCaptor.capture()
        );

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("word");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    @DisplayName("단어장 조회 - sort=oldest이면 collected_at 오름차순 정렬로 조회한다")
    void getWords_sortOldest_success() {
        // 1. Given
        Page<CollectedWord> wordPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(collectedWordRepository.searchMyWords(
                anyLong(), anyString(), any(), any(), any(Pageable.class)
        )).willReturn(wordPage);

        // 2. When
        wordService.getWords(TEST_USER_ID, 0, 10, "oldest", "all", null);

        // 3. Then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(collectedWordRepository).searchMyWords(
                anyLong(), anyString(), any(), any(), pageableCaptor.capture()
        );

        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("collected_at");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.ASC);
    }

    @Test
    @DisplayName("단어장 조회 - 잘못된 페이지 번호(음수)는 0으로 보정된다")
    void getWords_invalidPage_correctedToZero() {
        // 1. Given
        Page<CollectedWord> wordPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(collectedWordRepository.searchMyWords(
                anyLong(), anyString(), any(), any(), any(Pageable.class)
        )).willReturn(wordPage);

        // 2. When
        wordService.getWords(TEST_USER_ID, -1, 10, "latest", "all", null);

        // 3. Then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(collectedWordRepository).searchMyWords(
                anyLong(), anyString(), any(), any(), pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    @DisplayName("단어장 조회 - 잘못된 size(0 이하 또는 100 초과)는 20으로 보정된다")
    void getWords_invalidSize_correctedToDefault() {
        // 1. Given
        Page<CollectedWord> wordPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

        given(collectedWordRepository.searchMyWords(
                anyLong(), anyString(), any(), any(), any(Pageable.class)
        )).willReturn(wordPage);

        // 2. When
        wordService.getWords(TEST_USER_ID, 0, 0, "latest", "all", null);

        // 3. Then
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(collectedWordRepository).searchMyWords(
                anyLong(), anyString(), any(), any(), pageableCaptor.capture()
        );

        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("단어장 조회 - 품사별로 그룹화된 뜻이 평탄화되어 반환된다")
    void getWords_meaningsFlattened() {
        // 1. Given - 여러 품사를 가진 단어
        CollectedWord word = CollectedWord.builder()
                .word("light")
                .meaningsByPos(List.of(
                        new WordMeaning("명사", List.of("빛", "조명")),
                        new WordMeaning("형용사", List.of("가벼운")),
                        new WordMeaning("동사", List.of("불을 켜다"))
                ))
                .video(fakeVideo)
                .user(fakeUser)
                .wordType(WordType.COLLECT)
                .build();
        ReflectionTestUtils.setField(word, "id", 2L);

        Page<CollectedWord> wordPage = new PageImpl<>(
                List.of(word), PageRequest.of(0, 10), 1
        );

        given(collectedWordRepository.searchMyWords(
                anyLong(), anyString(), any(), any(), any(Pageable.class)
        )).willReturn(wordPage);

        // 2. When
        WordListResponse response = wordService.getWords(
                TEST_USER_ID, 0, 10, "latest", "all", null
        );

        // 3. Then - 모든 품사의 뜻이 평탄화되어 단일 List로 반환
        assertThat(response.getWords()).hasSize(1);
        assertThat(response.getWords().get(0).getMeanings())
                .containsExactly("빛", "조명", "가벼운", "불을 켜다");
    }

    @Test
    @DisplayName("단어장 조회 - 결과가 없으면 빈 리스트와 0건의 페이지네이션이 반환된다")
    void getWords_emptyResult() {
        // 1. Given
        Page<CollectedWord> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(collectedWordRepository.searchMyWords(
                anyLong(), anyString(), any(), any(), any(Pageable.class)
        )).willReturn(emptyPage);

        // 2. When
        WordListResponse response = wordService.getWords(
                TEST_USER_ID, 0, 10, "latest", "all", "없는단어"
        );

        // 3. Then
        assertThat(response.getWords()).isEmpty();
        assertThat(response.getPagination().getTotalCount()).isEqualTo(0L);
        assertThat(response.getPagination().getTotalPage()).isEqualTo(0);
    }
}