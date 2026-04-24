package com.clip.server.quiz.repository;

import com.clip.server.quiz.entity.QuizSessionWord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuizSessionWordRepository extends JpaRepository<QuizSessionWord, Long> {

}
