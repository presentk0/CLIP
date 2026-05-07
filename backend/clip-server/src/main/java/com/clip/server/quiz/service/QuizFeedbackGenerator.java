package com.clip.server.quiz.service;

import com.clip.server.quiz.entity.QuizType;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Random;

@Component
public class QuizFeedbackGenerator {

    private final Random random = new Random();

    public String generateFeedback(double accuracy, String videoTitle, Optional<QuizType> mostWrongType) {
        String typeName = mostWrongType
                .map(QuizType::getDescription)
                .orElse("퀴즈");
        if (accuracy >= 0.8) return getWellDoneFeedback(accuracy, videoTitle);
        if (accuracy >= 0.6) return getNormalFeedback(videoTitle, typeName);
        if (accuracy >= 0.4) return getDifficultFeedback(typeName);
        return getOverwhelmingFeedback(typeName);
    }

    // 시나리오 1. 잘한 날 (80% 이상)
    private String getWellDoneFeedback(double accuracy, String videoTitle) {
        String[] slot1 = {
                "오늘 정확도가 좋아요!",
                videoTitle + " 주제의 단어들을 잘 맞혔어요!",
                "오늘 영상 깔끔하게 끝냈어요.",
                "정답률이 " + (int)(accuracy * 100) + "%네요! 정말 좋아요!",
                "이번 영상 정답률이 정말 높아요!"
        };
        String[] slot3 = {
                "한 단계 올려볼까요?",
                "비슷한 주제로 더 해볼까요?",
                "새 영상으로 가볼까요?",
                "조금 긴 영상도 해볼까요?",
                "다음 영상도 함께 가봐요!"
        };
        return pick(slot1) + " " + pick(slot3);
    }

    // 시나리오 2. 평범한 날 (60~80%)
    private String getNormalFeedback(String videoTitle, String typeName) {
        String[] slot1 = {
                "오늘도 한 영상을 잘 마무리하셨네요!",
                videoTitle + " 주제의 단어를 잘 맞혔어요.",
                "단어 수집도 꾸준히 이어가고 계시는군요!",
                "이번에도 잘 풀었어요!",
                "오늘 한 걸음 더 나아갔어요."
        };
        String[] slot2 = {
                typeName + " 문제가 조금 어려웠네요.",
                typeName + " 부분 다시 연습해보면 좋은 문제예요!",
                typeName + " 가 조금 까다로웠죠?",
                "긴 문장은 좀 더 연습해볼까요?",
                "오늘 놓친 문제들은 다음에 함께 만나봐요!"
        };
        String[] slot3 = {
                "다음 영상 추천드릴까요?",
                "비슷한 주제로 한 번 더 할까요?",
                "다음은 어떤 영상으로 갈까요?",
                "쭉 이어서 한 영상 더 해볼까요?",
                "오늘은 여기서 마무리할까요?"
        };
        // 축 2는 선택적으로 조합 (예: 50% 확률로 포함)
        return random.nextBoolean() ?
                pick(slot1) + " " + pick(slot2) + " " + pick(slot3) :
                pick(slot1) + " " + pick(slot3);
    }

    // 시나리오 3. 어려운 날 (40% 이상 60% 미만)
    private String getDifficultFeedback(String typeName) {
        String[] slot2 = {
                "꽤 쉽지 않은 도전이었네요.",
                "이 영상 만만치 않았지만, 잘 달려왔어요.",
                "괜찮아요, 천천히 가도 돼요.",
                "이번은 단어들이 좀 어려웠어요.",
                typeName + " 부분을 한 번 더 보면 분명 달라질 거예요."
        };
        String[] slot3 = {
                "비슷한 영상으로 다시 해볼까요?",
                "쉬운 영상부터 가볼까요?",
                "짧은 영상으로 가볍게 갈까요?",
                "오늘은 여기서 쉬어갈까요?",
                "같은 영상 한 번 더 볼까요?"
        };

        return pick(slot2) + " " + pick(slot3);
    }

    // 시나리오 4. 버거운 날 (40% 미만)
    private String getOverwhelmingFeedback(String typeName) {
        String[] slot2 = {
                "영상이 좀 어려우셨나요?",
                typeName + "에 성장이 필요해 보여요.",
                "한 발 물러서는 것도 방법이에요.",
                "어렵다고 느끼는 게 자연스러워요.",
                "여기까지 정말 잘 따라와 주셨네요."
        };
        String[] slot3 = {
                "난이도 한 단계 낮춰볼까요?",
                "같은 영상 한 번 더 보면 익숙해져요.",
                "다음은 조금 더 가벼운 마음으로 함께 살펴볼까요?",
                "함께 이 영상을 다시 도전해볼까요?",
                "다시 가볍게 시작해도 돼요."
        };

        return pick(slot2) + " " + pick(slot3);
    }
    private String pick(String[] slots) {
        return slots[random.nextInt(slots.length)];
    }
}
