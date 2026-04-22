package com.clip.server.quiz.dto.response;

import com.clip.server.quiz.entity.SessionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuizStartResponse {

    private Long sessionId; // 세션 Id
    private String videoId; // 영상 Id
    private SessionType sessionType; // 퀴즈 타입(일반, 복습)
    private int totalQuizCount; // 퀴즈 개수
    private LocalDateTime startAt; // 퀴즈 시작 시간
}
