package com.clip.server.user.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.dto.request.UpdateUiStateRequest;
import com.clip.server.user.dto.response.UserUiStateResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserUiStateService {

    private final UserRepository userRepository;

    /**
     * UI 팝업 상태 조회 메서드
     */
    public UserUiStateResponse getUserUiState(Long userId) {

        // 1. 사용자 확인
        User user = userRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserUiStateResponse.builder()
                .tutorialCompleted(user.getTutorialCompleted())
                .voiceConsentRead(user.getVoiceConsentRead())
                .build();
    }

    /**
     * UI 팝업 상태 저장 메서드
     */
    @Transactional
    public void updateUserUiState(Long userId, UpdateUiStateRequest updateUiStateRequest) {

        // 1. 사용자 확인
        User user = userRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));

        boolean changed = false;

        // 2-1 음성 팝업 동의 값 체크: 요청값 True인지, 저장값 false인지
        if(Boolean.TRUE.equals(updateUiStateRequest.getVoiceConsentRead()) && !Boolean.TRUE.equals(user.getVoiceConsentRead())) {
            user.markVoiceConsentRead();
            changed= true;
            log.info("음성 동의 팝업 확인 저장. userId={}", userId);
        }

        // 2-2 튜토리얼 팝업 동의 값 체크: 요청값 True인지, 저장값 false인지
        if(Boolean.TRUE.equals(updateUiStateRequest.getTutorialCompleted()) && !Boolean.TRUE.equals(user.getTutorialCompleted())) {
            user.markTutorialCompleted();
            changed= true;
            log.info("튜토리얼 팝업 확인 저장. userId={}", userId);
        }
        
        // 3. 변경사항 있으면 값 저장
        if(changed) {
            userRepository.save(user);    
        } else {
            log.info("변경사항 없음. userId={}", userId);
        }
    }
}
