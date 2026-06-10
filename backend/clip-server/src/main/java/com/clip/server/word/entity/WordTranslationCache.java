package com.clip.server.word.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "word_translation_cache",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_word_meanings",
                columnNames = {"word", "meaningsHash"}
        ))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WordTranslationCache {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String word;

    @Column(nullable = false, length = 64)
    private String meaningsHash;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String meanings;  // JSON

    @Column(nullable = false, columnDefinition = "TEXT")
    private String translations;  // JSON

    @Column(nullable = false)
    private Long hitCount = 0L;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime lastAccessedAt;

    @Builder
    public WordTranslationCache(String word, String meaningsHash,
                                String meanings, String translations) {
        this.word = word;
        this.meaningsHash = meaningsHash;
        this.meanings = meanings;
        this.translations = translations;
        this.hitCount = 0L;
        this.createdAt = LocalDateTime.now();
        this.lastAccessedAt = LocalDateTime.now();
    }

}
