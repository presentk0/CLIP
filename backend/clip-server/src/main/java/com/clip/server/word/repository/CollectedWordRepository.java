package com.clip.server.word.repository;

import com.clip.server.user.entity.User;
import com.clip.server.word.entity.CollectedWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface CollectedWordRepository extends JpaRepository<CollectedWord, Long> {
    boolean existsByUserAndWord(User user, String word);
    int countByUser(User user);
}
