package com.clip.server.user.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.dto.response.UserProfileResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserProfileResponse showProfile(Long userId) {

        // 유저확인
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int nextLevelExp = user.calculateNextLevelExp();
        double progressPercentage = user.getProgressPercentage();

        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .level(user.getLevel())
                .nextLevelExp(nextLevelExp)
                .progressPercentage(progressPercentage)
                .createdAt(user.getCreatedAt())
                .build();
    }

}
