package com.clip.server.word.repository;

import com.clip.server.quiz.entity.QuizSessionWord;
import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.entity.WordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CollectedWordRepository extends JpaRepository<CollectedWord, Long> {
    long countByUser(User user);
    Page<CollectedWord> findAll(Pageable pageable);
    Page<CollectedWord> findAllByUserIdAndVideo_VideoIdAndWordType(Long userId, String videoId, WordType type, Pageable pageable);
    Page<CollectedWord> findAllByUserIdAndWordType(Long userId, WordType type, Pageable pageable);
    Optional<CollectedWord> findByUserIdAndVideo_VideoIdAndWord(Long userId, String videoId, String word);
    List<CollectedWord> findAllByUserAndVideo(User user, Video video);
    Long countByUserIdAndVideoVideoIdAndWordType(
            Long userId,
            String videoId,
            WordType wordType
    );

    @Query("SELECT COUNT(cw) > 0 FROM CollectedWord cw " +
            "WHERE cw.user.id = :userId " +
            "AND cw.collectedAt >= :startOfDay " +
            "AND cw.collectedAt < :endOfDay")
    boolean existsCollectedWordToday(@Param("userId") Long userId,
                                     @Param("startOfDay") LocalDateTime stateOfDay,
                                     @Param("endOfDay")LocalDateTime endOfDay
                                    );


}
