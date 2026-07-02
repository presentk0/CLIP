package com.clip.server.feedback.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.feedback.dto.request.FeedbackRequest;
import com.clip.server.feedback.dto.response.FeedbackCheckResponse;
import com.clip.server.feedback.dto.response.FeedbackResponse;
import com.clip.server.feedback.entity.UserFeedback;
import com.clip.server.feedback.repository.UserFeedbackRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final UserFeedbackRepository userFeedbackRepository;
    private final UserRepository userRepository;

    @Transactional
    public FeedbackResponse postFeedBack(Long userId, FeedbackRequest feedbackRequest) {
        log.info("피드백 제출. userId={}, score={}", userId, feedbackRequest.getSatisfactionScore());

        // 유저 확인
        User user = userRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 사용자 피드백 저장
        UserFeedback userFeedback = UserFeedback.builder()
                .user(user)
                .satisfactionScore(feedbackRequest.getSatisfactionScore())
                .goodPoint(feedbackRequest.getGoodPoint())
                .improvePoint(feedbackRequest.getImprovePoint())
                .build();
        UserFeedback saved= userFeedbackRepository.saveAndFlush(userFeedback);
        log.info("피드백 저장 완료. feedbackId={}", saved.getId());

        return FeedbackResponse.builder()
                .feedbackId(saved.getId())
                .submittedAt(saved.getCreatedAt() != null ? saved.getCreatedAt() : LocalDateTime.now())
                .build();
    }

    public FeedbackCheckResponse getFeedBackCheck(Long userId) {
        log.info("피드백 제출 여부 체크. userId={}", userId);

        // 유저 확인
        if(!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        boolean isSubmitted = userFeedbackRepository.existsByUserId(userId);

        return FeedbackCheckResponse.builder()
                .hasSubmittedFeedback(isSubmitted)
                .build();
    }
}
