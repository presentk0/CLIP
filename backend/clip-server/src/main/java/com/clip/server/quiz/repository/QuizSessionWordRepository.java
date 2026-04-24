package com.clip.server.quiz.repository;

import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizSessionWord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizSessionWordRepository extends JpaRepository<QuizSessionWord, Long> {
    @Query("SELECT qsw FROM QuizSessionWord qsw " +
            "WHERE qsw.quizSession.id = :sessionId " +
            "AND qsw.timestamp >= :start " +
            "AND qsw.timestamp < :end")
    List<QuizSessionWord> findWordsBySection(
            @Param("sessionId") Long sessionId,
            @Param("start") double start,
            @Param("end") double end
    );
    List<QuizSessionWord> findAllByQuizSession(QuizSession quizSession);
}
