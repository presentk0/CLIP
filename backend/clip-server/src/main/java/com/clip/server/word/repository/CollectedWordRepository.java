package com.clip.server.word.repository;

import com.clip.server.admin.dashboard.stats.dto.response.UserVideoWordStatResponse;
import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.entity.WordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 오늘 수집한 단어 목록 조회 (1순위 후보)
     * - 정의서: 오늘 학습한 단어를 1순위로 후보 제시
     */
    @Query("SELECT cw FROM CollectedWord cw " +
            "WHERE cw.user.id = :userId " +
            "AND cw.collectedAt >= :startOfDay " +
            "AND cw.collectedAt < :endOfDay " +
            "ORDER BY cw.collectedAt DESC")
    List<CollectedWord> findTodayCollectedWords(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            Pageable pageable
    );

    /**
     * 오늘 외 수집 단어 목록 조회 (2순위 후보, 최신순)
     * 오늘 학습은 없지만 수집 단어 있을 때 2순위 추천
     */
    @Query("SELECT cw FROM CollectedWord cw " +
            "WHERE cw.user.id = :userId " +
            "AND cw.collectedAt < :startOfDay " +
            "ORDER BY cw.collectedAt DESC")
    List<CollectedWord> findCollectedWordsBeforeToday(
            @Param("userId") Long userId,
            @Param("startOfDay") LocalDateTime startOfDay,
            Pageable pageable
    );

    /**
     * 사용자의 단어 보유 여부 (신규 유저 차단용)
     */
    boolean existsByUserId(Long userId);


    // 관리자용
    @Query("SELECT new com.clip.server.admin.dashboard.stats.dto.response.UserVideoWordStatResponse(" +
            "u.id, u.name, v.videoId, v.title, COUNT(cw), " +
            "SUM(CASE WHEN cw.wordType = 'COLLECT' THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN cw.wordType = 'POPUP' THEN 1 ELSE 0 END)) " +
            "FROM CollectedWord cw " +
            "JOIN cw.user u " +
            "JOIN cw.video v " +
            "WHERE cw.collectedAt BETWEEN :startDateTime AND :endDateTime " +
            "GROUP BY u.id, u.name, v.videoId, v.title " +
            "ORDER BY COUNT(cw) DESC")
    List<UserVideoWordStatResponse> findUserVideoWordStatistics(
            @Param("startDateTime") LocalDateTime startDateTime,
            @Param("endDateTime") LocalDateTime endDateTime,
            Pageable pageable
    );
}
