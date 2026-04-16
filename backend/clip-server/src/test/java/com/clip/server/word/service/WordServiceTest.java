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

    // 테스트에서 공통으로 사용할 변수들
    private final Long TEST_USER_ID = 1L;
    private CollectedWordRequest request;
    private User fakeUser;
    private Video fakeVideo;

    @BeforeEach
    void setUp() {
        request = new CollectedWordRequest("v1", "title1", "apple", "I eat an apple", "0:01", "사과");

        fakeUser = User.builder()
                .name("홍길동")
                .email("test@gmail.com")
                .build();
        ReflectionTestUtils.setField(fakeUser, "id", TEST_USER_ID);

        fakeVideo = Video.builder()
                .videoId("v1")
                .title("title1")
                .build();
    }

    @Test
    @DisplayName("단어 수집 성공 테스트")
    void collectWord_success() {
        // 1. Given
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(fakeUser));
        given(collectedWordRepository.existsByUserAndWord(any(User.class), anyString())).willReturn(false);
        given(videoRepository.findById("v1")).willReturn(Optional.of(fakeVideo));

        // Mock 저장 객체 설정 (save 호출 시 가짜 엔티티 반환)
        CollectedWord fakeWord = CollectedWord.builder().build();
        given(collectedWordRepository.save(any(CollectedWord.class))).willReturn(fakeWord);

        // 2. When: 서비스 로직 호출 (userId를 첫 번째 인자로 전달)
        CollectedWordResponse response = wordService.save(TEST_USER_ID, request);

        // 3. Then
        assertThat(response).isNotNull();
        verify(collectedWordRepository, times(1)).save(any(CollectedWord.class));
        verify(userRepository, times(1)).findById(TEST_USER_ID);
    }

    @Test
    @DisplayName("수집된 단어가 중복되면 에러가 발생합니다.")
    void collectWord_duplicate_exception() {
        // 1. Given
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.of(fakeUser));
        // 중복 체크에서 true(이미 있음)를 반환하도록 설정
        given(collectedWordRepository.existsByUserAndWord(any(User.class), anyString())).willReturn(true);

        // 2. When & Then: userId와 함께 호출
        assertThatThrownBy(() -> wordService.save(TEST_USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 수집한 단어");
    }

    @Test
    @DisplayName("유저를 찾지 못하면 에러가 발생합니다.")
    void user_notfound_exception() {
        // 1. Given: 해당 ID의 유저가 없는 상황
        given(userRepository.findById(TEST_USER_ID)).willReturn(Optional.empty());

        // 2. When & 3. Then: userId와 함께 호출
        assertThatThrownBy(() -> wordService.save(TEST_USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("videoId가 없으면 사용자의 전체 수집 단어를 조회한다")
    void getWords_withoutVideoId() {
        // 1. Given
        Long userId = 1L;
        int page = 0, size = 10;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Video fakeVideo = Video.builder()
                .videoId("v123")
                .title("테스트 영상")
                .build();

        CollectedWord word = CollectedWord.builder()
                .word("apple")
                .video(fakeVideo)
                .build();

        Page<CollectedWord> wordPage = new PageImpl<>(List.of(word), pageRequest, 1);

        // Mock 설정
        given(collectedWordRepository.findAllByUserId(eq(userId), any(Pageable.class)))
                .willReturn(wordPage);

        // 2. When
        WordListResponse response = wordService.getWords(userId, page, size, null);

        // 3. Then
        assertThat(response.getWords().size()).isEqualTo(1);
        assertThat(response.getWords().get(0).getWord()).isEqualTo("apple");
        assertThat(response.getPagination().getTotalCount()).isEqualTo(1);

        verify(collectedWordRepository, times(1)).findAllByUserId(eq(userId), any(Pageable.class));
        verify(collectedWordRepository, never()).findAllByUserIdAndVideo_VideoId(anyLong(), anyString(), any());
    }

    @Test
    @DisplayName("videoId가 있으면 해당 영상의 단어만 필터링하여 조회한다")
    void getWords_withVideoId() {
        // 1. Given
        Long userId = 1L;
        String videoId = "v123";
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("createdAt").descending());

        Page<CollectedWord> wordPage = new PageImpl<>(List.of(), pageRequest, 0);

        given(collectedWordRepository.findAllByUserIdAndVideo_VideoId(eq(userId), eq(videoId), any(Pageable.class)))
                .willReturn(wordPage);

        // 2. When
        wordService.getWords(userId, 0, 10, videoId);

        // 3. Then
        verify(collectedWordRepository, times(1)).findAllByUserIdAndVideo_VideoId(eq(userId), eq(videoId), any(Pageable.class));
        verify(collectedWordRepository, never()).findAllByUserId(anyLong(), any());
    }
}