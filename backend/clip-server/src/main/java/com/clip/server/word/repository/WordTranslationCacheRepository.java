package com.clip.server.word.repository;

import com.clip.server.word.entity.WordTranslationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface WordTranslationCacheRepository extends JpaRepository<WordTranslationCache, Long> {

    Optional<WordTranslationCache> findByWordAndMeaningsHash(
            String word, String meaningsHash);

    @Modifying
    @Query("UPDATE WordTranslationCache w SET " +
            "w.hitCount = w.hitCount + 1, " +
            "w.lastAccessedAt = :now " +
            "WHERE w.id = :id")
    void incrementHitCount(@Param("id") Long id, @Param("now") LocalDateTime now);
}
