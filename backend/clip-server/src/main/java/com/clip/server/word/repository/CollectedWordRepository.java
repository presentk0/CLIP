package com.clip.server.word.repository;

import com.clip.server.word.entity.CollectedWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface CollectedWordRepository extends JpaRepository<CollectedWord, Long> {
}
