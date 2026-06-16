package com.clip.server.chat.service;

import com.clip.server.chat.client.ChatReportAiClient;
import com.clip.server.chat.dto.ai.AiReportEvaluation;
import com.clip.server.chat.dto.response.ChatReportResponse;
import com.clip.server.chat.entity.ChatMessage;
import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.entity.SenderType;
import com.clip.server.chat.repository.ChatMessageRepository;
import com.clip.server.chat.repository.ChatRoomRepository;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.chat.entity.UserWeakness;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReportService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserWeaknessService userWeaknessService;
    private final ChatReportAiClient chatReportAiClient;

    /**
     * 대화 리포트 생성
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChatReportResponse generateReport(Long userId, Long chatRoomId) {
        log.info("리포트 생성 요청. userId={}, chatRoomId={}", userId, chatRoomId);

        // 1. 채팅방 조회 (word 함께)
        ChatRoom chatRoom = chatRoomRepository
                .findByIdAndUserIdWithWord(chatRoomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 모든 메시지 조회 (시간순)
        List<ChatMessage> allMessages = chatMessageRepository
                .findByChatRoomIdOrderByCreatedAtAsc(chatRoomId);

        // 3. 약점 조회
        List<UserWeakness> weaknesses = userWeaknessService
                .getWeaknessesByChatRoom(chatRoomId);

        // 4. 각 섹션 생성
        ChatReportResponse.Summary summary = buildSummary(chatRoom, allMessages);
        ChatReportResponse.WordUsage wordUsage = buildWordUsage(chatRoom, allMessages);
        ChatReportResponse.PronunciationScore pronunciationScore =
                buildPronunciationScore(allMessages);
        ChatReportResponse.ExpressionNaturalness expressionNaturalness =
                buildExpressionNaturalness(allMessages);

        // 5. LLM 종합 평가
        AiReportEvaluation aiEval = chatReportAiClient.evaluate(
                chatRoom, allMessages, weaknesses.size()
        );

        // 6. Overall + WeaknessAnalysis
        ChatReportResponse.Overall overall = ChatReportResponse.Overall.builder()
                .score(aiEval.getOverallScore())
                .goodPoints(aiEval.getGoodPoints())
                .improvePoints(aiEval.getImprovePoints())
                .build();

        ChatReportResponse.WeaknessAnalysis weaknessAnalysis =
                buildWeaknessAnalysis(weaknesses);

        // 7. 응답 조립
        // 7. 응답 조립
        return ChatReportResponse.builder()
                .chatRoomId(chatRoomId)
                .summary(summary)
                .wordUsage(wordUsage)
                .pronunciationScore(pronunciationScore)
                .expressionNaturalness(expressionNaturalness)
                .overall(overall)
                .weaknessAnalysis(weaknessAnalysis)
                .build();
    }

    // ============================================================
    // Private 메서드들 (각 섹션 빌더)
    // ============================================================

    /**
     * ① Summary 빌드
     */
    private ChatReportResponse.Summary buildSummary(
            ChatRoom chatRoom,
            List<ChatMessage> allMessages
    ) {
        // 총 마디 수 (USER 메시지 개수)
        int totalTurns = (int) allMessages.stream()
                .filter(m -> m.getSenderType() == SenderType.USER)
                .count();

        // 대화 시간 계산
        String duration = calculateDuration(chatRoom);

        // 입력 모드 판단 (USER 메시지 중 audio_url이 있으면 voice, 없으면 text)
        String inputMode = determineInputMode(allMessages);

        // 타겟 단어
        String targetWord = chatRoom.getWord() != null
                ? chatRoom.getWord().getWord()
                : "(없음)";

        return ChatReportResponse.Summary.builder()
                .targetWord(targetWord)
                .totalTurns(totalTurns)
                .duration(duration)
                .completedAt(chatRoom.getUpdatedAt())
                .inputMode(inputMode)
                .build();
    }

    /**
     * ② WordUsage 빌드 - 타겟 단어 사용 분석
     */
    private ChatReportResponse.WordUsage buildWordUsage(
            ChatRoom chatRoom,
            List<ChatMessage> allMessages
    ) {
        String targetWord = chatRoom.getWord() != null
                ? chatRoom.getWord().getWord()
                : null;

        if (targetWord == null) {
            return ChatReportResponse.WordUsage.builder()
                    .targetWord("(없음)")
                    .used(false)
                    .usageCount(0)
                    .usageContext(List.of())
                    .feedback("타겟 단어가 설정되지 않았습니다.")
                    .build();
        }

        String targetWordLower = targetWord.toLowerCase();

        // 사용자 메시지 중 타겟 단어가 들어간 것들
        List<ChatReportResponse.WordUsage.UsageContext> usageContexts = allMessages.stream()
                .filter(m -> m.getSenderType() == SenderType.USER)
                .filter(m -> m.getContent().toLowerCase().contains(targetWordLower))
                .map(m -> ChatReportResponse.WordUsage.UsageContext.builder()
                        .messageId(m.getId())
                        .userSentence(m.getContent())
                        .natural(Boolean.TRUE.equals(m.getIsNatural()))
                        .build())
                .toList();

        int usageCount = usageContexts.size();
        boolean used = usageCount > 0;

        String feedback = used
                ? String.format("타겟 단어를 자연스럽게 %d번 활용하셨어요!", usageCount)
                : "타겟 단어를 사용하지 못했어요. 다음에 활용해보세요!";

        return ChatReportResponse.WordUsage.builder()
                .targetWord(targetWord)
                .used(used)
                .usageCount(usageCount)
                .usageContext(usageContexts)
                .feedback(feedback)
                .build();
    }

    /**
     * ③ PronunciationScore 빌드 - 발음 평가 (음성 모드 한정)
     */
    private ChatReportResponse.PronunciationScore buildPronunciationScore(
            List<ChatMessage> allMessages
    ) {
        // 사용자 메시지 중 발음 점수가 있는 것들
        List<ChatMessage> scoredMessages = allMessages.stream()
                .filter(m -> m.getSenderType() == SenderType.USER)
                .filter(m -> m.getPronunciationScore() != null)
                .toList();

        // 발음 점수 없으면 TEXT 모드 → available: false
        if (scoredMessages.isEmpty()) {
            return ChatReportResponse.PronunciationScore.builder()
                    .available(false)
                    .overallScore(null)
                    .feedback(null)
                    .weakSentences(List.of())
                    .build();
        }

        // 평균 점수
        double avgScore = scoredMessages.stream()
                .mapToDouble(m -> m.getPronunciationScore().doubleValue())
                .average()
                .orElse(0.0);

        int overallScore = (int) Math.round(avgScore);

        // 약한 문장 (70점 미만)
        List<ChatReportResponse.PronunciationScore.WeakSentence> weakSentences = scoredMessages.stream()
                .filter(m -> m.getPronunciationScore().compareTo(BigDecimal.valueOf(70)) < 0)
                .map(m -> ChatReportResponse.PronunciationScore.WeakSentence.builder()
                        .messageId(m.getId())
                        .original(m.getContent())
                        .score(m.getPronunciationScore().intValue())
                        .issue("발음을 다시 연습해보세요")
                        .modelAudioUrl(null)  // TTS는 나중에 추가
                        .build())
                .toList();

        String feedback = overallScore >= 80
                ? "전반적으로 정확하게 들렸어요!"
                : "발음 연습을 조금 더 해보세요.";

        return ChatReportResponse.PronunciationScore.builder()
                .available(true)
                .overallScore(overallScore)
                .feedback(feedback)
                .weakSentences(weakSentences)
                .build();
    }

    /**
     * ④ ExpressionNaturalness 빌드 - 표현 자연스러움
     */
    private ChatReportResponse.ExpressionNaturalness buildExpressionNaturalness(
            List<ChatMessage> allMessages
    ) {
        // 사용자 메시지 중 평가된 것들
        List<ChatMessage> evaluatedMessages = allMessages.stream()
                .filter(m -> m.getSenderType() == SenderType.USER)
                .filter(m -> m.getIsNatural() != null)
                .toList();

        if (evaluatedMessages.isEmpty()) {
            return ChatReportResponse.ExpressionNaturalness.builder()
                    .overallScore(0)
                    .feedback("평가할 데이터가 부족합니다.")
                    .improvements(List.of())
                    .build();
        }

        // 자연스러움 비율로 점수 계산
        long naturalCount = evaluatedMessages.stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsNatural()))
                .count();
        int score = (int) Math.round((double) naturalCount / evaluatedMessages.size() * 100);

        // 개선 제안 (어색했던 메시지들)
        List<ChatReportResponse.ExpressionNaturalness.Improvement> improvements =
                evaluatedMessages.stream()
                        .filter(m -> Boolean.FALSE.equals(m.getIsNatural()))
                        .filter(m -> m.getRecommendedAlternative() != null)
                        .map(m -> ChatReportResponse.ExpressionNaturalness.Improvement.builder()
                                .messageId(m.getId())
                                .original(m.getContent())
                                .suggested(m.getRecommendedAlternative())
                                .explanation("더 자연스러운 표현이에요")
                                .modelAudioUrl(null)  // TTS 나중에 추가
                                .build())
                        .toList();

        String feedback = score >= 80
                ? "전반적으로 자연스러운 표현을 사용하셨어요!"
                : "더 자연스러운 표현을 익혀보세요.";

        return ChatReportResponse.ExpressionNaturalness.builder()
                .overallScore(score)
                .feedback(feedback)
                .improvements(improvements)
                .build();
    }

    /**
     * ⑥ WeaknessAnalysis 빌드
     */
    private ChatReportResponse.WeaknessAnalysis buildWeaknessAnalysis(
            List<UserWeakness> weaknesses
    ) {
        List<String> awkwardExpressions = weaknesses.stream()
                .map(UserWeakness::getWeakExpression)
                .toList();

        return ChatReportResponse.WeaknessAnalysis.builder()
                .awkwardExpressions(awkwardExpressions)
                .avoidedSituations(List.of())  // 힌트 기능 추가 시 사용
                .willBeUsedInNextScenario(!awkwardExpressions.isEmpty())
                .build();
    }

    // ============================================================
    // Helper 메서드들
    // ============================================================

    /**
     * 대화 시간 계산 ("HH:mm:ss" 형식)
     */
    private String calculateDuration(ChatRoom chatRoom) {
        LocalDateTime start = chatRoom.getCreatedAt();
        LocalDateTime end = chatRoom.getUpdatedAt();

        if (start == null || end == null) {
            return "00:00:00";
        }

        Duration duration = Duration.between(start, end);
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * 입력 모드 판단
     */
    private String determineInputMode(List<ChatMessage> allMessages) {
        boolean hasVoice = allMessages.stream()
                .filter(m -> m.getSenderType() == SenderType.USER)
                .anyMatch(m -> m.getAudioUrl() != null);
        return hasVoice ? "voice" : "text";
    }
}