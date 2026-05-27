package com.clip.server.user.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.dto.request.InitialPreferenceRequest;
import com.clip.server.user.dto.request.UpdatePreferenceRequest;
import com.clip.server.user.dto.response.PreferenceResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.preference.UserPreference;
import com.clip.server.user.repository.UserPreferenceRepository;
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
public class UserPreferenceService {

    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    /**
     * 온보딩 - 초기 설정
     * - 모든 필드 필수, 이미 온보딩 했으면 덮어쓰기
     */
    @Transactional
    public PreferenceResponse postUserPreference(Long userId, InitialPreferenceRequest request) {

        // 유저 확인
        User user = userRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserPreference userPreference = userPreferenceRepository.findByUser(user)
                .orElseGet(() -> UserPreference.builder().user(user).build());

        userPreference.update(request.getLearningGoal(), request.getDifficultyLevel());

        userPreferenceRepository.save(userPreference);

        return mapToPreferResponse(userPreference);
    }

    /*
     * 학습 설정 수정 (부분 수정)
     * - 온보딩 완료된 유저만 가능
     * - 둘 다 null이면 에러
     */
    @Transactional
    public PreferenceResponse updateUserPreference(Long userId, UpdatePreferenceRequest request) {

        if(request.getLearningGoal()==null && request.getDifficultyLevel()==null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 학습 설정 확인
        UserPreference userPreference = userPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        userPreference.partialUpdate(request.getLearningGoal(), request.getDifficultyLevel());

        return mapToPreferResponse(userPreference);
    }

    public PreferenceResponse getUserPreference(Long userId) {

        // 학습 설정 확인
        UserPreference userPreference = userPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND));

        return mapToPreferResponse(userPreference);
    }

    private PreferenceResponse mapToPreferResponse(UserPreference userPreference) {
        return PreferenceResponse.builder()
                .difficultyLevel(String.valueOf(userPreference.getDifficultyLevel()))
                .learningGoal(String.valueOf(userPreference.getLearningGoal()))
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
