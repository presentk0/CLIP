package com.clip.server.word.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.word.dto.request.CollectedWordRequest;
import com.clip.server.word.dto.response.CollectedWordResponse;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.repository.CollectedWordRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
    private UserRepository  userRepository;
    @Mock
    private VideoRepository videoRepository;
    @Mock
    private CollectedWordRepository collectedWordRepository;

    @InjectMocks
    private WordService wordService;

    @Test
    @DisplayName("단어 수집 성공 테스트")
    void collectWord_success() {
        // 1.Given
        CollectedWordRequest request = new CollectedWordRequest("v1","title1", "apple", "I eat an apple", "0:01", "사과");
        Video fakeVideo = Video.builder()
                .videoId("v1")
                .title("title1")
                .build();
        User fakeUser = User.builder()
                .name("홍갈동")
                .email("teat@gmail.com")
                .build();

        ReflectionTestUtils.setField(fakeUser, "id", 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(fakeUser));
        given(collectedWordRepository.existsByUserAndWord(any(), anyString())).willReturn(false);
        given(videoRepository.findById("v1")).willReturn(Optional.of(fakeVideo));

        // 2. When 서비스 로직 호출
        CollectedWordResponse response = wordService.save(request);

        // 3. Then
        assertThat(response.getTotalCollectedWords()).isNotNull();
        verify(collectedWordRepository, times(1)).save(any(CollectedWord.class));
    }

    @Test
    @DisplayName("수집된 단어가 중복되면 에러가 발생합니다.")
    void collectWord_duplicate_exception() {
        // 1. Given
        CollectedWordRequest request = new CollectedWordRequest("v1","title1", "apple", "I eat an apple", "0:01", "사과");
        Video fakeVideo = Video.builder()
                .videoId("v1")
                .title("title1")
                .build();
        User fakeUser = User.builder()
                .name("홍갈동")
                .email("teat@gmail.com")
                .build();
        ReflectionTestUtils.setField(fakeUser, "id", 1L);

        given(userRepository.findById(1L)).willReturn(Optional.of(fakeUser));
        // 중복 체크에서 true(이미 있음)를 반환하도록 설정
        given(collectedWordRepository.existsByUserAndWord(any(), anyString())).willReturn(true);

        // 2. When & Then
        assertThatThrownBy(() -> wordService.save(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("이미 수집한 단어");

    }

    @Test
    @DisplayName("유저를 찾지 못하면 에러가 발생합니다.")
    void user_notfound_exception() {
        // 1. Given
        CollectedWordRequest request = new CollectedWordRequest("v1", "title1", "apple", "...", "0:01", "사과");
        given(userRepository.findById(1L)).willReturn(Optional.empty());

        // 2. When & 3. Then
        assertThatThrownBy(() -> wordService.save(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다"); // ErrorCode에 설정한 메시지
    }
}
