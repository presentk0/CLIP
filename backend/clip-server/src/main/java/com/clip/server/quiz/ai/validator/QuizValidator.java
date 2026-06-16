package com.clip.server.quiz.ai.validator;

import com.clip.server.quiz.dto.response.OpenAIQuizDataResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class QuizValidator {

    // ==================== OX 퀴즈 검증 ====================

    public ValidationResult validateOXQuiz(OpenAIQuizDataResponse quiz) {
        if (quiz == null) {
            return ValidationResult.fail("AI 응답이 null입니다");
        }
        if (isBlank(quiz.getContent())) {
            return ValidationResult.fail("content가 비어있습니다");
        }
        if (isBlank(quiz.getAnswer())) {
            return ValidationResult.fail("answer가 비어있습니다");
        }
        if (isBlank(quiz.getTranslation())) {
            return ValidationResult.fail("translation이 비어있습니다");
        }
        if (containsKorean(quiz.getContent())) {
            return ValidationResult.fail("content에 한글이 포함되어 있습니다. 영어 문장만 작성하세요");
        }
        return ValidationResult.success();
    }

    // ==================== 빈칸 퀴즈 검증 ====================

    public ValidationResult validateBlankQuiz(OpenAIQuizDataResponse quiz) {
        if (quiz == null) {
            return ValidationResult.fail("AI 응답이 null입니다");
        }
        if (isBlank(quiz.getContent()) || !quiz.getContent().contains("[ ]")) {
            return ValidationResult.fail("content에 [ ] 표시가 없습니다");
        }
        if (isBlank(quiz.getAnswer())) {
            return ValidationResult.fail("answer가 비어있습니다");
        }
        if (quiz.getOptions() == null || quiz.getOptions().size() != 4) {
            return ValidationResult.fail("options는 정확히 4개여야 합니다");
        }
        if (isBlank(quiz.getTranslation())) {
            return ValidationResult.fail("translation이 비어있습니다");
        }
        if (containsKorean(quiz.getContent())) {
            return ValidationResult.fail("content에 한글이 포함되어 있습니다");
        }
        if (quiz.getTranslation().length() > 80) {
            return ValidationResult.fail("translation이 너무 깁니다 (80자 초과)");
        }
        if (quiz.getTranslation().contains("~")) {
            return ValidationResult.fail("translation에 '~' 기호가 포함되어 있습니다");
        }
        if (isAnswerExposed(quiz)) {
            return ValidationResult.fail(
                    String.format("content에 정답 '%s'가 노출되어 있습니다. 정답 단어와 변형형(ed, ing, er)을 절대 포함하지 마세요",
                            quiz.getAnswer()));
        }
        return ValidationResult.success();
    }

    // ==================== 매칭 퀴즈 검증 ====================

    public ValidationResult validateMatchingQuiz(OpenAIQuizDataResponse quiz) {
        if (quiz == null) {
            return ValidationResult.fail("AI 응답이 null입니다");
        }
        if (isBlank(quiz.getWord())) {
            return ValidationResult.fail("word가 비어있습니다");
        }
        if (isBlank(quiz.getQuestion())) {
            return ValidationResult.fail("question이 비어있습니다");
        }
        if (isBlank(quiz.getAnswer())) {
            return ValidationResult.fail("answer가 비어있습니다");
        }
        return ValidationResult.success();
    }

    // ==================== 공통 유틸 ====================

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private boolean containsKorean(String s) {
        return s != null && s.matches(".*[가-힣].*");
    }

    private boolean isAnswerExposed(OpenAIQuizDataResponse quiz) {
        if (quiz.getContent() == null || quiz.getAnswer() == null) return false;

        String content = quiz.getContent().toLowerCase();
        String answer = quiz.getAnswer().toLowerCase();

        return content.contains(answer)
                || content.contains(answer + "ed")
                || content.contains(answer + "ing")
                || content.contains(answer + "er");
    }
}
