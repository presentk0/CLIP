package com.clip.server.user.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.user.dto.response.BadgeInfoResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.entity.badge.BadgeType;
import com.clip.server.user.entity.badge.UserBadge;
import com.clip.server.user.repository.UserBadgeRepository;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.clip.server.common.exception.ErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BadgeService {

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final UserBadgeRepository userBadgeRepository;

    public BadgeInfoResponse getBadge(Long userId, String videoId) {

        // 유저 정보 확인
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        // 영상 정보 확인, 없어도 에러 X
        Optional<Video> videoOptional = videoRepository.findById(videoId);

        // 영상 정보가 없을 경우 기본값 반환
        if(videoOptional.isEmpty()) {
            return BadgeInfoResponse.builder()
                    .videoId(videoId)
                    .videoTitle("Unknown Video")
                    .currentBadge(BadgeType.NONE)
                    .nextBadge(BadgeType.BRONZE)
                    .earnedBadges(new ArrayList<>())
                    .build();
        }

        Video video = videoOptional.get();
        List<UserBadge> earnedBadges = userBadgeRepository.findAllByUserAndVideo(user, video);

        BadgeType currentMax = earnedBadges.stream()
                .map(UserBadge::getBadgeType)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(null);

        BadgeType nextBadge = getNextBadge(currentMax);

        // 3. BadgeInfoResponse.BadgeDetail 리스트로 변환
        List<BadgeInfoResponse.BadgeDetail> badgeDetails = earnedBadges.stream()
                .map(ub -> BadgeInfoResponse.BadgeDetail.builder()
                        .badgeType(ub.getBadgeType())
                        .earnedAt(ub.getEarnedAt())
                        .build())
                .collect(Collectors.toList());

        return BadgeInfoResponse.builder()
                .videoId(video.getVideoId())
                .videoTitle(video.getTitle())
                .currentBadge(currentMax)
                .nextBadge(nextBadge)
                .earnedBadges(badgeDetails)
                .build();
    }

    private BadgeType getNextBadge(BadgeType current) {
        if (current == null) return BadgeType.BRONZE;
        return switch (current) {
            case NONE -> BadgeType.BRONZE;
            case BRONZE -> BadgeType.SILVER;
            case SILVER -> BadgeType.GOLD;
            case GOLD -> BadgeType.COMPLETION;
            case COMPLETION -> null;
        };
    }
}
