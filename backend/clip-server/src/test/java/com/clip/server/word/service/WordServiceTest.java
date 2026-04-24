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
import com.clip.server.word.entity.WordType;
import com.clip.server.word.repository.CollectedWordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        // COLLECT 타입으로 저장 요청하는 기본 DTO
        request = new CollectedWordRequest("v1", "title1", "apple", "사과", "I eat an apple", "0:01", "나는 사과를 먹는다.", WordType.COLLECT);

        fakeUser = User.builder().build();
        ReflectionTestUtils.setField(fakeUser, "id", TEST_USER_ID);

        fakeVideo = Video.builder()
                .videoId("v1")
                .title("title1")
                .build();
    }

    @Test
    @DisplayName("단어 수집 성공 테스트 (기존 데이터 없음)")
    void collectWord_success() {
        // 1. given: 기존에 저장된 단어가 없는 상태(Optional.empty)
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(fakeUser));
        given(collectedWordRepository.findByUserIdAndVideo_VideoIdAndWord(anyLong(), anyString(), anyString()))
                .willReturn(Optional.empty());
        given(videoRepository.findById("v1")).willReturn(Optional.of(fakeVideo));

        // 2. when
        wordService.save(TEST_USER_ID, request);

        // 3. Then
        verify(collectedWordRepository, times(1)).save(any(CollectedWord.class));
    }

    @Test
    @DisplayName("이미 COLLECT된 단어를 또 COLLECT 하려고 하면 에러가 발생합니다.")
    void collectWord_duplicate_exception() {
        // 1. Given: 이미 COLLECT 타입으로 저장된 단어가 존재함
        CollectedWord alreadyCollectedWord = CollectedWord.builder()
                .wordType(WordType.COLLECT)
                .build();

        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(fakeUser));
        given(collectedWordRepository.findByUserIdAndVideo_VideoIdAndWord(anyLong(), anyString(), anyString()))
                .willReturn(Optional.of(alreadyCollectedWord));

        // 2. When & Then
        assertThatThrownBy(() -> wordService.save(TEST_USER_ID, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("videoId가 있으면 해당 영상의 'COLLECT' 단어만 필터링하여 조회한다")
    void getWords_withVideoId() {
        // 1. Given
        String videoId = "v123";
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("collectedAt").descending());
        Page<CollectedWord> wordPage = new PageImpl<>(List.of(), pageRequest, 0);

        // ✅ 레포지토리 메서드명과 인자(WordType.COLLECT)를 서비스와 일치시킴
        given(collectedWordRepository.findAllByUserIdAndVideo_VideoIdAndWordType(eq(TEST_USER_ID), eq(videoId), eq(WordType.COLLECT), any(Pageable.class)))
                .willReturn(wordPage);

        // 2. When
        wordService.getWords(TEST_USER_ID, 0, 10, videoId);

        // 3. Then
        verify(collectedWordRepository).findAllByUserIdAndVideo_VideoIdAndWordType(eq(TEST_USER_ID), eq(videoId), eq(WordType.COLLECT), any(Pageable.class));
    }

    @Test
    @DisplayName("이미 POPUP으로 저장된 단어를 COLLECT로 요청하면 타입이 업데이트된다.")
    void upgrade_popup_to_collect_success() {
        // 1. Given
        String videoId = "v1";
        String wordText = "apple";

        // 기존에 POPUP으로 저장되어 있던 단어 객체 (ID 100번 주입)
        CollectedWord existingPopupWord = CollectedWord.builder()
                .word(wordText)
                .wordType(WordType.POPUP)
                .user(fakeUser)
                .build();
        ReflectionTestUtils.setField(existingPopupWord, "id", 100L);

        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(fakeUser));
        given(collectedWordRepository.findByUserIdAndVideo_VideoIdAndWord(TEST_USER_ID, videoId, wordText))
                .willReturn(Optional.of(existingPopupWord));
        given(collectedWordRepository.countByUser(fakeUser)).willReturn(1);

        // 2. When
        CollectedWordResponse response = wordService.save(TEST_USER_ID, request);

        // 3. Then
        assertThat(response.getWordId()).isEqualTo(100L);
        assertThat(existingPopupWord.getWordType()).isEqualTo(WordType.COLLECT); // 타입 변경 확인
        assertThat(existingPopupWord.getSentence()).isEqualTo(request.getSentence()); // 문장 갱신 확인
    }
}