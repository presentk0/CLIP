package com.clip.server.word.repository;

import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.entity.WordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface CollectedWordRepository extends JpaRepository<CollectedWord, Long> {
    boolean existsByUserAndWord(User user, String word);
    int countByUser(User user);
    Page<CollectedWord> findAll(Pageable pageable);
    Page<CollectedWord> findAllByUserIdAndVideo_VideoIdAndWordType(Long userId, String videoId, WordType type, Pageable pageable);
    Page<CollectedWord> findAllByUserIdAndWordType(Long userId, WordType type, Pageable pageable);
    Optional<CollectedWord> findByUserIdAndVideo_VideoIdAndWord(Long userId, String videoId, String word);
}
