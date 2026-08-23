package com.clip.server.notice.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.notice.dto.response.DismissAllResponse;
import com.clip.server.notice.dto.response.DismissResponse;
import com.clip.server.notice.dto.response.UnreadCountResponse;
import com.clip.server.notice.entity.UserNoticeDismiss;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import com.clip.server.notice.dto.response.PopupResponse;
import com.clip.server.notice.entity.Notice;
import com.clip.server.notice.repository.NoticeRepository;
import com.clip.server.notice.repository.UserNoticeDismissRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserNoticeDismissRepository userNoticeDismissRepository;
    private final UserRepository userRepository;

    @Value("${clipzy.notice.base-url}")
    private String noticeBaseUrl;

    /**
     * 팝업 대상 공지 조회
     */
    public PopupResponse getPopup(Long userId) {

        Notice notice = noticeRepository.findPopupTarget(userId, LocalDateTime.now()).orElse(null);

        return mapToPopupResponse(notice);
    }

    /**
     * 미확인 공지 개수 조회 (배지용)
     */
    public UnreadCountResponse getUnreadCount(Long userId) {

        // 미확인 공지 개수
        long count = noticeRepository.countUnreadByUserId(userId, LocalDateTime.now());

        return UnreadCountResponse.builder()
                .unreadCount(count)
                .hasNew(count>0)
                .build();

    }

    /**
     * 공지 확인 처리
     */
    @Transactional
    public DismissResponse postDismiss(Long userId, Long noticeId) {

        // 1. 공지 존재 유무 확인
        Notice notice = noticeRepository.findById(noticeId).orElseThrow(()-> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        // 2. 공지 확인 이력 조회
        UserNoticeDismiss dismiss = userNoticeDismissRepository.findByUserIdAndNoticeId(userId, noticeId).orElse(null);

        if(dismiss !=null) {
            return mapToDismiss(dismiss);
        }

        User user = userRepository.getReferenceById(userId);
        UserNoticeDismiss newDismiss = UserNoticeDismiss.builder()
                .user(user)
                .notice(notice)
                .dontShowAgain(false)
                .build();

        UserNoticeDismiss savedDismiss = userNoticeDismissRepository.save(newDismiss);
        return mapToDismiss(savedDismiss);
    }

    @Transactional
    public DismissAllResponse postDismissAll(Long userId) {

        // 1. 활성 공지 중 미확인 공지 확인
        List<Notice> notices = noticeRepository.findActiveNotDismissed(userId, LocalDateTime.now());

        // 2. 미확인 공지가 없을 경우 null 반환
        if(notices.isEmpty()) {
            return DismissAllResponse.builder()
                    .dismissedCount(0L)
                    .dismissedAt(LocalDateTime.now())
                    .build();
        }

        // 3. User 프록시 가져오기
        User user = userRepository.getReferenceById(userId);

        // 4. 각 공지에 대해 UserNoticeDismiss 생성
        List<UserNoticeDismiss> dismisses = notices.stream()
                .map(notice -> UserNoticeDismiss.builder()
                        .user(user)
                        .notice(notice)
                        .dontShowAgain(false)
                        .build())
                .toList();

        // 5. 저장
        userNoticeDismissRepository.saveAll(dismisses);

        return DismissAllResponse.builder()
                .dismissedCount((long)dismisses.size())
                .dismissedAt(LocalDateTime.now())
                .build();
    }

    // ======================== 변환 메서드 ===========================
    private PopupResponse mapToPopupResponse(Notice notice) {

        // 팝업 공지 없음
        if(notice == null) {
            return PopupResponse.builder()
                    .hasPopup(false)
                    .notice(null)
                    .build();
        }

        return PopupResponse.builder()
                .hasPopup(true)
                .notice(mapToNoticeDetail(notice))
                .build();
    }

    private PopupResponse.NoticeDetail mapToNoticeDetail(Notice notice) {
        return PopupResponse.NoticeDetail.builder()
                        .id(notice.getId())
                        .title(notice.getTitle())
                        .summary(notice.getSummary())
                        .detailUrl(noticeBaseUrl + "/" + notice.getSlug() )
                        .createdAt(notice.getCreatedAt())
                        .build();
    }

    private DismissResponse mapToDismiss(UserNoticeDismiss dismiss) {
        return DismissResponse.builder()
                .noticeId(dismiss.getNotice().getId())
                .dismissedAt(dismiss.getDismissedAt())
                .build();
    }

}
