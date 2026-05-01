package com.clip.server.quiz.entity;

import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizType;
import com.clip.server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "quiz_result")
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession quizSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String word;

    @Enumerated(EnumType.STRING)
    @Column(name = "quiz_type", nullable = false)
    private QuizType quizType;

    @Column(columnDefinition = "TEXT")
    private String content; // 영어 예문 (I want to [ ] around...)

    @Column(length = 500)
    private String translation; // 예문에 대한 한글 해석

    @Column(columnDefinition = "TEXT")
    private String question; // 개구리 캐릭터의 질문 메시지

    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

    @Column(columnDefinition = "TEXT")
    private String explanation; // 뉘앙스 차이 등을 포함한 친절한 해설

    @Column(name = "user_answer")
    private String userAnswer; // 사용자 답변

    @Column(name = "is_correct")
    private Boolean isCorrect ; // 정답 여부

    @Column(name = "earned_exp")
    @ColumnDefault("0")
    private Integer earnedExp;

    @Column(name = "video_timestamp")
    private String videoTimestamp; // 다시 듣기용 타임 스탬프

    @Column(name = "correct_feedback")
    private String correctFeedback; // 정답시 피드백

    @Column(name = "wrong_feedback")
    private String wrongFeedback; // 오답시 피드백

    @CreatedDate
    @Column(name = "answered_at")
    private LocalDateTime answeredAt; // 답변 시각

    @Builder
    public QuizResult(QuizSession quizSession, User user, String word, QuizType quizType,
                      String question, String correctAnswer, String explanation, String videoTimestamp,
                      String content, String translation, String correctFeedback, String wrongFeedback) {
        this.quizSession = quizSession;
        this.user = user;
        this.word = word;
        this.quizType = quizType;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.videoTimestamp = videoTimestamp;
        this.content = content;
        this.translation = translation;
        this.correctFeedback = correctFeedback;
        this.wrongFeedback = wrongFeedback;
    }

    // 사용자가 퀴즈를 풀었을 때 호출
    public void submitAnswer(String userAnswer, boolean isCorrect, int earnedExp) {
        this.userAnswer = userAnswer;
        this.isCorrect = isCorrect;
        this.earnedExp = earnedExp;
        this.answeredAt = LocalDateTime.now();
    }
}