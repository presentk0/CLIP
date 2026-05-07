package com.clip.server.user.repository;

import com.clip.server.user.entity.User;
import com.clip.server.user.entity.badge.BadgeType;
import com.clip.server.user.entity.badge.UserBadge;
import com.clip.server.video.entity.Video;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {

    Boolean existsByUserAndVideoAndBadgeType(User user, Video video, BadgeType badgeType);
    List<UserBadge> findAllByUserAndVideo(User user, Video video);
}
