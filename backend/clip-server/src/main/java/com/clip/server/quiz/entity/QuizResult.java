package com.clip.server.quiz.entity;

import com.clip.server.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quiz_result")
public class QuizResult {
    // 퀴즈 문제가 저장되는 클래스
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession quizSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 사용자_Id

    @Column(nullable = false)
    private String word; // 퀴즈 단어

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuizType quizType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String question; // 퀴즈 문제

    @Column(nullable = false)
    private String correctAnswer; // 퀴즈 정답

    private String userAnswer; // 사용자 답변

    private boolean isCorrect; // 정답 여부

    private int earnedExp; // 획득 경험치

    private LocalDateTime answeredAt; // 답변 시각

    @Column(columnDefinition = "TEXT")
    private String explanation; // AI가 만든 해설

    private String videoTimestamp; //다시 듣기용 영상 다임 스탬프

    @Builder
    public QuizResult(QuizSession quizSession, User user, String word, QuizType quizType,
                      String question, String correctAnswer, String explanation, String videoTimestamp) {
        this.quizSession = quizSession;
        this.user = user;
        this.word = word;
        this.quizType = quizType;
        this.question = question;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.videoTimestamp = videoTimestamp;
    }

    // 사용자가 퀴즈를 풀었을 때 호출
    public void submitAnswer(String userAnswer, boolean isCorrect, int earnedExp) {
        this.userAnswer = userAnswer;
        this.isCorrect = isCorrect;
        this.earnedExp = earnedExp;
        this.answeredAt = LocalDateTime.now();
    }

}
