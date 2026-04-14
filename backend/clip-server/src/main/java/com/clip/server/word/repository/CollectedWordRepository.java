package com.clip.server.word.repository;

import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import com.clip.server.word.entity.CollectedWord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

public interface CollectedWordRepository extends JpaRepository<CollectedWord, Long> {
    boolean existsByUserAndWord(User user, String word);
    int countByUser(User user);
    Page<CollectedWord> findAll(Pageable pageable);
    Page findAllByUserIdAndVideoId(Long userId, String videoId, Pageable pageable);
    Page findAllByUserId(Long userId, Pageable pageable);
}
