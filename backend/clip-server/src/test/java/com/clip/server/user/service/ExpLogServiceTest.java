package com.clip.server.user.service;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.exp.ExpLog;
import com.clip.server.user.entity.exp.ExpSourceType;
import com.clip.server.user.repository.ExpLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ExpLogServiceTest {

    @Mock
    private ExpLogRepository expLogRepository;
    @InjectMocks
    private ExpLogService expLogService;

    private User user;
    private final Long userId = 1L;


    @BeforeEach
    void setUp() {
        user = User.builder()
                .build();
        ReflectionTestUtils.setField(user, "id", userId);
        ReflectionTestUtils.setField(user, "exp", 0);
        ReflectionTestUtils.setField(user, "level", 5);
    }

    @Test
    @DisplayName("Exp 추가시 user.exp 갱신 및 로그가 저장된다.")
    void addExp_successCase() {
        // given & when
        expLogService.addExp(user, ExpSourceType.QUIZ_CORRECT, 99L, "퀴즈 정답(8문제)", 800);

        // then
        assertThat(user.getExp()).isEqualTo(800);

        ArgumentCaptor<ExpLog> captor = ArgumentCaptor.forClass(ExpLog.class);
        verify(expLogRepository, times(1)).save(captor.capture());

        ExpLog savedLog = captor.getValue();
        assertThat(savedLog.getUser()).isEqualTo(user);
        assertThat(savedLog.getSourceType()).isEqualTo(ExpSourceType.QUIZ_CORRECT);
        assertThat(savedLog.getSourceId()).isEqualTo(99L);
        assertThat(savedLog.getDescription()).isEqualTo("퀴즈 정답(8문제)");
        assertThat(savedLog.getAmount()).isEqualTo(800);
    }

    @Test
    @DisplayName("amount가 0이면 EXP 갱신과 로그 저장이 모두 일어나지 않는다")
    void addExp_amount_0_무시() {
        // given
        int originalExp = user.getExp();

        // when
        expLogService.addExp(user, ExpSourceType.QUIZ_CORRECT, 99L, null, 0);

        // then
        assertThat(user.getExp()).isEqualTo(originalExp);  // 변동 없음
        verify(expLogRepository, never()).save(any());      // 저장 안 됨
    }

    @Test
    @DisplayName("description 없이 호출 시에도 정상 저장된다")
    void addExp_description_없이() {
        // given: setUp에서 만든 user 사용 (exp=0)

        // when: description 인자 없는 오버로딩 메서드 호출
        expLogService.addExp(user, ExpSourceType.VIDEO_COMPLETE, 99L, 200);

        // then
        assertThat(user.getExp()).isEqualTo(200);  // 0 + 200 = 200

        ArgumentCaptor<ExpLog> captor = ArgumentCaptor.forClass(ExpLog.class);
        verify(expLogRepository).save(captor.capture());

        ExpLog savedLog = captor.getValue();
        assertThat(savedLog.getDescription()).isNull();
        // getTitle()은 sourceType의 defaultTitle 반환
        assertThat(savedLog.getTitle()).isEqualTo("영상 학습 완료");
    }
}
