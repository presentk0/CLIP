package com.clip.server.quiz.dto.response;

import com.clip.server.quiz.entity.QuizType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class QuizDetailResponse {

    private Long quizId;
    private QuizType quizType;

    private String content;  // 영어 예문 (OX는 전체 문장, 빈칸은 [ ] 포함 문장)
    private String translation; // 예문에 대한 한글 해석 (와이어프레임의 카드 중앙 하단)

    private String question; // 개구리 캐릭터가 던지는 질문 (예: "문장에 들어간 단어로 저게 맞을까?")

    private List<String> options; // 빈칸 채우기용 오답 포함 선택지 리스트 (4지 선다 등)

    private String answer; // 매칭 퀴즈 시 단어 뜻 전달을 위함

    private String videoTimeStamp; // 다시 듣기용 비디오 타임스탬프

    /**
     * 매칭 퀴즈가 아니면 answer를 null로 반환하여 정답 노출 방지
     */
    public String getAnswer() {
        if (quizType == QuizType.MATCHING) {
            return answer;
        }
        return null;
    }

}
