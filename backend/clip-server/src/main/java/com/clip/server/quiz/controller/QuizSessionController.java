package com.clip.server.quiz.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/quiz/sessions/generate")
@Tag(name = "단어 API", description = "퀴즈 생성 API입니다.")
@Validated
public class QuizSessionController {


}
