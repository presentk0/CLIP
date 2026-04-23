package com.clip.server.quiz.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "quiz_session_word")
@EntityListeners(AuditingEntityListener.class)
public class QuizSessionWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private QuizSession quizSession;

    @Column(nullable = false)
    private String word;
    @Column(nullable = false)
    private String sentence;
    @Column(nullable = false, length = 200)
    private String translation;

    @Builder
    public QuizSessionWord(QuizSession quizSession, String word, String sentence, String translation) {
        this.quizSession = quizSession;
        this.word = word;
        this.sentence = sentence;
        this.translation = translation;
    }
}
