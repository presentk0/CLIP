package com.clip.server.notice.service;

import com.clip.server.notice.dto.response.DismissAllResponse;
import com.clip.server.notice.dto.response.DismissResponse;
import com.clip.server.notice.dto.response.PopupResponse;
import com.clip.server.notice.dto.response.UnreadCountResponse;
import com.clip.server.notice.entity.Notice;
import com.clip.server.notice.entity.NoticeStatus;
import com.clip.server.notice.entity.UserNoticeDismiss;
import com.clip.server.notice.repository.NoticeRepository;
import com.clip.server.notice.repository.UserNoticeDismissRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.clip.server.common.exception.BusinessException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NoticeService 단위 테스트")
class NoticeServiceTest {

    @InjectMocks
    private NoticeService noticeService;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private UserNoticeDismissRepository userNoticeDismissRepository;

    @Mock
    private UserRepository userRepository;

    private static final String BASE_URL = "https://slashpage.com/clipzy";

    @Test
    @DisplayName("팝업 대상 공지가 있으면 hasPopup=true를 반환한다")
    void getPopup_success() {
        // given
        Long userId = 1L;
        Notice notice = Notice.builder()
                .slug("test-slug")
                .title("테스트 공지")
                .summary("테스트 요약")
                .isPopup(true)
                .priority(10)
                .startAt(LocalDateTime.now())
                .status(NoticeStatus.PUBLISHED)
                .build();

        ReflectionTestUtils.setField(notice, "id", 1L);
        ReflectionTestUtils.setField(notice, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(noticeService, "noticeBaseUrl", BASE_URL);

        when(noticeRepository.findPopupTarget(anyLong(), any(LocalDateTime.class)))
                .thenReturn(Optional.of(notice));

        // when
        PopupResponse response = noticeService.getPopup(userId);

        // then
        assertThat(response.getHasPopup()).isTrue();
        assertThat(response.getNotice()).isNotNull();
        assertThat(response.getNotice().getTitle()).isEqualTo("테스트 공지");
        assertThat(response.getNotice().getDetailUrl())
                .isEqualTo("https://slashpage.com/clipzy/test-slug");
    }

    @Test
    @DisplayName("팝업 대상 공지가 없으면 hasPopup=false를 반환한다")
    void getPopup_returnsFalse_whenNoNotice() {
        // given
        Long userId = 1L;
        when(noticeRepository.findPopupTarget(anyLong(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // when
        PopupResponse response = noticeService.getPopup(userId);

        // then
        assertThat(response.getHasPopup()).isFalse();
        assertThat(response.getNotice()).isNull();
    }

    @Test
    @DisplayName("미확인 공지가 있으면 hasNew=true, unreadCount를 반환한다")
    void getUnreadCount_success() {
        // given
        Long userId = 1L;
        when(noticeRepository.countUnreadByUserId(anyLong(), any(LocalDateTime.class)))
                .thenReturn(3L);

        // when
        UnreadCountResponse response = noticeService.getUnreadCount(userId);

        // then
        assertThat(response.getUnreadCount()).isEqualTo(3L);
        assertThat(response.getHasNew()).isTrue();
    }

    @Test
    @DisplayName("미확인 공지가 없으면 hasNew=false, unreadCount=0을 반환한다")
    void getUnreadCount_returnsZero() {
        // given
        Long userId = 1L;
        when(noticeRepository.countUnreadByUserId(anyLong(), any(LocalDateTime.class)))
                .thenReturn(0L);

        // when
        UnreadCountResponse response = noticeService.getUnreadCount(userId);

        // then
        assertThat(response.getUnreadCount()).isEqualTo(0L);
        assertThat(response.getHasNew()).isFalse();
    }

    @Test
    @DisplayName("공지를 처음 dismiss하면 새로 저장하고 응답을 반환한다")
    void postDismiss_success() {
        // given
        Long userId = 1L;
        Long noticeId = 1L;

        Notice notice = Notice.builder()
                .slug("test-slug")
                .title("테스트 공지")
                .summary("테스트 요약")
                .isPopup(true)
                .priority(10)
                .startAt(LocalDateTime.now())
                .status(NoticeStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(notice, "id", noticeId);

        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        when(noticeRepository.findById(noticeId)).thenReturn(Optional.of(notice));
        when(userNoticeDismissRepository.findByUserIdAndNoticeId(userId, noticeId))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        UserNoticeDismiss dismiss = UserNoticeDismiss.builder()
                .user(user)
                .notice(notice)
                .dontShowAgain(false)
                .build();
        ReflectionTestUtils.setField(dismiss, "dismissedAt", LocalDateTime.now());

        when(userNoticeDismissRepository.save(any(UserNoticeDismiss.class)))
                .thenReturn(dismiss);

        // when
        DismissResponse response = noticeService.postDismiss(userId, noticeId);

        // then
        assertThat(response.getNoticeId()).isEqualTo(noticeId);
        assertThat(response.getDismissedAt()).isNotNull();

        verify(userNoticeDismissRepository, times(1)).save(any(UserNoticeDismiss.class));
    }

    @Test
    @DisplayName("이미 dismiss한 공지 재요청 시 기존 반환")
    void postDismiss_returnsExisting_whenAlreadyDismissed() {
        // given
        Long userId = 1L;
        Long noticeId = 1L;

        Notice notice = Notice.builder()
                .slug("test-slug")
                .title("테스트 공지")
                .summary("테스트 요약")
                .isPopup(true)
                .priority(10)
                .startAt(LocalDateTime.now())
                .status(NoticeStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(notice, "id", noticeId);

        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        UserNoticeDismiss existingDismiss = UserNoticeDismiss.builder()
                .user(user)
                .notice(notice)
                .dontShowAgain(false)
                .build();
        ReflectionTestUtils.setField(existingDismiss, "dismissedAt", LocalDateTime.now());

        when(noticeRepository.findById(noticeId)).thenReturn(Optional.of(notice));
        when(userNoticeDismissRepository.findByUserIdAndNoticeId(userId, noticeId)).thenReturn(Optional.of(existingDismiss));

        // when
        DismissResponse response = noticeService.postDismiss(userId, noticeId);

        // then
        assertThat(response.getNoticeId()).isEqualTo(noticeId);
        assertThat(response.getDismissedAt()).isNotNull();

        verify(userNoticeDismissRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 공지 dismiss 시 예외가 발생한다")
    void postDismiss_throwsException_whenNoticeNotFound() {
        // given
        Long userId = 1L;
        Long noticeId = 999L;

        when(noticeRepository.findById(noticeId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(()->noticeService.postDismiss(userId, noticeId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("공지를 찾을 수 없습니다.");;
    }

    @Test
    @DisplayName("dismiss할 공지가 없으면 0을 반환한다")
    void postDismissAll_returnsZero_whenNothingToDismiss() {
        // given
        Long userId = 1L;

        when(noticeRepository.findActiveNotDismissed(anyLong(), any(LocalDateTime.class))).thenReturn(List.of());

        // when
        DismissAllResponse response = noticeService.postDismissAll(userId);

        // then
        assertThat(response.getDismissedCount()).isEqualTo(0L);
        assertThat(response.getDismissedAt()).isNotNull();

        // saveAll이 호출 안 됐음을 검증
        verify(userNoticeDismissRepository, never()).saveAll(anyList());

    }

    @Test
    @DisplayName("활성 공지 여러 개를 한 번에 dismiss 처리한다")
    void postDismissAll_success() {
        // given
        Long userId = 1L;
        Long notice1Id = 1L;
        Long notice2Id = 2L;
        Long notice3Id = 3L;

        Notice notice1 = Notice.builder()
                .slug("test-slug")
                .title("테스트 공지")
                .summary("테스트 요약")
                .isPopup(true)
                .priority(10)
                .startAt(LocalDateTime.now())
                .status(NoticeStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(notice1, "id", notice1Id);

        Notice notice2 = Notice.builder()
                .slug("test-slug")
                .title("테스트 공지")
                .summary("테스트 요약")
                .isPopup(true)
                .priority(10)
                .startAt(LocalDateTime.now())
                .status(NoticeStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(notice2, "id", notice2Id);

        Notice notice3 = Notice.builder()
                .slug("test-slug")
                .title("테스트 공지")
                .summary("테스트 요약")
                .isPopup(true)
                .priority(10)
                .startAt(LocalDateTime.now())
                .status(NoticeStatus.PUBLISHED)
                .build();
        ReflectionTestUtils.setField(notice3, "id", notice3Id);

        List<Notice> notices = List.of(notice1, notice2, notice3);

        User user = User.builder().build();
        ReflectionTestUtils.setField(user, "id", userId);

        when(noticeRepository.findActiveNotDismissed(anyLong(), any(LocalDateTime.class))).thenReturn(notices);
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(userNoticeDismissRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        DismissAllResponse response = noticeService.postDismissAll(userId);

        // then
        assertThat(response.getDismissedCount()).isEqualTo(3L);
        assertThat(response.getDismissedAt()).isNotNull();

        verify(userNoticeDismissRepository, times(1)).saveAll(anyList());

    }
}