package com.clip.server.quiz.repository;

import com.clip.server.quiz.entity.QuizResult;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {

    List<QuizResult> findByQuizSession(QuizSession quizSession);
    @Query("SELECT qr.quizType FROM QuizResult qr " +
            "WHERE qr.quizSession.id = :sessionId " +
            "AND qr.isCorrect = false " +
            "GROUP BY qr.quizType " +
            "ORDER BY COUNT(qr) DESC " +
            "LIMIT 1")
    Optional<QuizType> findMostWrongQuizType(@Param("sessionId") Long sessionId);
}
