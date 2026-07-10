package com.clip.server.quiz.repository;

import com.clip.server.quiz.entity.QuizResult;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
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

    // ==================== Agent용 쿼리 ====================

    // 최근 30개 퀴즈 정답률
    @Query(value = """
    SELECT AVG(CASE WHEN is_correct = true THEN 1.0 ELSE 0.0 END)
    FROM (
        SELECT is_correct FROM quiz_result 
        WHERE user_id = :userId AND answered_at IS NOT NULL
        ORDER BY answered_at DESC LIMIT 30
    ) recent
    """, nativeQuery = true)
    Double findRecentAccuracy(@Param("userId") Long userId);

    // 취약 단어 (정답률 50% 미만, 2회 이상 출제)
    @Query(value = """
    SELECT word FROM (
        SELECT word, 
               AVG(CASE WHEN is_correct = true THEN 1.0 ELSE 0.0 END) AS acc,
               COUNT(*) AS cnt
        FROM quiz_result 
        WHERE user_id = :userId AND answered_at IS NOT NULL
        GROUP BY word 
        HAVING COUNT(*) >= 2 AND AVG(CASE WHEN is_correct = true THEN 1.0 ELSE 0.0 END) < 0.5
    ) t
    """, nativeQuery = true)
    List<String> findWeakWords(@Param("userId") Long userId);

    // 최근 7일 오답 단어
    @Query("""
    SELECT DISTINCT qr.word FROM QuizResult qr
    WHERE qr.user.id = :userId 
      AND qr.isCorrect = false 
      AND qr.answeredAt > :since
    """)
    List<String> findRecentWrongWords(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since
    );

    // 마지막 퀴즈 이후 경과일
    @Query(value = """
    SELECT DATEDIFF(NOW(), MAX(answered_at))
    FROM quiz_result WHERE user_id = :userId
    """, nativeQuery = true)
    Integer findDaysSinceLastQuiz(@Param("userId") Long userId);


    // ============================== 관리자용 =============================
    // 해설(explanation) 필드가 채워진 행(AI를 호출했다는 증거)만 카운트
    @Query("SELECT COUNT(q) FROM QuizResult q " +
            "WHERE q.explanation IS NOT NULL " +
            "AND q.answeredAt BETWEEN :start AND :end")
    Long countQuizExplanationsByPeriod(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

