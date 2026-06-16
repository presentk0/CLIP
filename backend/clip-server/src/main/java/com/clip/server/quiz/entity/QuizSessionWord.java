package com.clip.server.quiz.entity;

import com.clip.server.word.entity.WordType;
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
    // 해당 세션에서 퀴즈 제작을 위한 단어 목록을 미리 뽑아 놓은 클래스
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private QuizSession quizSession;

    @Column(nullable = false, length = 255)
    private String word;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sentence;

    @Column(nullable = false, length = 200)
    private String translation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WordType wordType; // 단어 타입: 수집, 호버, 자막 필터링 단어

    @Column(nullable = false)
    private Double timestamp; // 단어 발생 시점


    @Builder
    public QuizSessionWord(QuizSession quizSession, String word, String sentence, String translation, WordType wordType, Double timestamp) {
        this.quizSession = quizSession;
        this.word = word;
        this.sentence = sentence;
        this.translation = translation;
        this.wordType = wordType;
        this.timestamp = timestamp;
    }
}
