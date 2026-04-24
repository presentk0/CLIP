package com.clip.server.word.repository;

import com.clip.server.common.config.AuditingConfig;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.word.entity.CollectedWord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
@Import(AuditingConfig.class)
public class CollectedWordRepositoryTest {

    @Autowired
    private CollectedWordRepository collectedWordRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private VideoRepository videoRepository;

    @Test
    @DisplayName("딘아 수집 정보가 성공적으로 저장되어야 한다.")
    void saveCollectedWordTest() {
        // 1. Given: 테스트에 필요한 가짜 데이터 준비
        User user = User.builder()
                .email("123@gmail.com")
                .name("홍길동")
                .build();

        Video video = Video.builder()
                .videoId("avengers")
                .title("어벤져스")
                .build();

        userRepository.save(user);
        videoRepository.save(video);

        CollectedWord collectedWord = CollectedWord.builder()
                .user(user)
                .video(video)
                .word("apple")
                .sentence("I eat an apple.")
                .timestamp("00:15")
                .translation("나는 사과를 먹는다")
                .build();

        // 2. When: 실제 저장 수행
        CollectedWord saveWord = collectedWordRepository.save(collectedWord);

        // 3. Then: 저장된 데이터가 내가 넣은 것과 같은지 검증
        assertThat(saveWord.getId()).isNotNull();
        assertThat(saveWord.getWord()).isEqualTo("apple");
        assertThat(saveWord.getSentence()).isEqualTo("I eat an apple.");
    }

}
