package com.clip.server.word.entity;

import com.clip.server.common.converter.WordMeaningJsonConverter;
import com.clip.server.user.entity.User;
import com.clip.server.video.entity.Video;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "collected_word")
@EntityListeners(AuditingEntityListener.class)
public class CollectedWord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    // 사용자가 수집한 단어
    @Column(nullable = false, length = 100)
    private String word;

    // 수집한 단어 뜻
    @Convert(converter = WordMeaningJsonConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false, name = "meaning")
    private List<WordMeaning> meaningsByPos;

    // 단어 수집시 해당 단어가 포함된 예문
    @Column(nullable = false)
    private String sentence;

    // 해당 영상의 TimeStamp
    @Column(nullable = false, length = 10)
    private String timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 10)
    private WordType wordType; // 호버 단어 or 수집 단어 여부 판별

    // 자막(예문) 번역
    @Column(nullable = false, length = 200)
    private String translation;

    // 단어 수집 시간
    @CreatedDate
    @Column(name = "collected_at", nullable = false, updatable = false)
    private LocalDateTime collectedAt;

    @Builder
    public CollectedWord(User user, Video video, String word, String sentence, String timestamp, String translation, List<WordMeaning> meaningsByPos, WordType wordType) {
        this.user = user;
        this.video = video;
        this.word = word;
        this.sentence = sentence;
        this.timestamp = timestamp;
        this.translation = translation;
        this.meaningsByPos = meaningsByPos;
        this.wordType = wordType;
    }

    // POPUP 상태 단어를 COLLECT로 업그레이드
    public void updateToCollect(String sentence, String translation) {
        if(this.wordType == WordType.COLLECT) {
            return;
        }

        this.wordType = WordType.COLLECT;
        this.sentence = sentence;
        this.translation = translation;
        this.collectedAt = LocalDateTime.now();
    }
}
