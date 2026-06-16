package com.clip.server.quiz.ai;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.quiz.dto.request.QuizWordRequest;
import com.clip.server.quiz.dto.response.QuizDetailResponse;
import com.clip.server.quiz.entity.QuizResult;
import com.clip.server.quiz.entity.QuizSession;
import com.clip.server.quiz.entity.QuizType;
import com.clip.server.quiz.repository.QuizResultRepository;
import com.clip.server.quiz.repository.QuizSessionRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizFallbackService {

    private final QuizResultRepository quizResultRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<QuizDetailResponse> createLocalMatchingQuiz(Long sessionId, Long userId, List<QuizWordRequest> requests) {

        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("### 매칭 퀴즈 Fallback 생성 (단어 수: {})", requests.size());

        return requests.stream().map(req -> {
            String word = req.getWord();
            String meaning = req.getMeaning();
            String safeMeaning = (meaning != null && !meaning.isBlank()) ? meaning : "뜻";

            // 랜덤 예문 생성
            int templateIndex = random.nextInt(SENTENCE_TEMPLATES.size());
            String content = String.format(SENTENCE_TEMPLATES.get(templateIndex), word);
            String translation = String.format(TRANSLATION_TEMPLATES.get(templateIndex), safeMeaning);

            QuizResult result = QuizResult.builder()
                    .quizSession(session)
                    .user(user)
                    .word(word)
                    .quizType(QuizType.MATCHING)
                    .content(content)
                    .translation(translation)
                    .question(word)
                    .correctAnswer(shortenMeaning(safeMeaning))
                    .explanation(String.format("'%s'는 '%s'라는 의미예요.", word, safeMeaning))
                    .videoTimestamp(req.getVideoTimeStamp())
                    .correctFeedback(getRandomFeedback(CORRECT_FEEDBACKS))
                    .wrongFeedback(getRandomFeedback(WRONG_FEEDBACKS))
                    .build();

            QuizResult saved = quizResultRepository.save(result);

            return QuizDetailResponse.builder()
                    .quizId(saved.getId())
                    .quizType(saved.getQuizType())
                    .content(saved.getContent())
                    .translation(saved.getTranslation())
                    .question(saved.getQuestion())
                    .answer(saved.getCorrectAnswer())
                    .videoTimeStamp(saved.getVideoTimestamp())
                    .build();
        }).collect(Collectors.toList());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<QuizDetailResponse> createLocalMatchingQuizNewTx(Long sessionId, Long userId, List<QuizWordRequest> requests) {
        return createLocalMatchingQuiz(sessionId, userId, requests);
    }

    // ==================== 템플릿 데이터 ====================

    private static final List<String> SENTENCE_TEMPLATES = List.of(
            "I want to %s around the world.",
            "She decided to %s her skills.",
            "We need to %s this problem.",
            "They plan to %s next month.",
            "He loves to %s in his free time.",
            "It's important to %s every day.",
            "You should %s before deciding.",
            "Let's %s together this weekend.",
            "I'm trying to %s my English.",
            "The team will %s the project soon."
    );

    private static final List<String> TRANSLATION_TEMPLATES = List.of(
            "나는 전 세계를 %s하고 싶어요.",
            "그녀는 자신의 실력을 %s하기로 했어요.",
            "우리는 이 문제를 %s해야 해요.",
            "그들은 다음 달에 %s할 계획이에요.",
            "그는 여가 시간에 %s하는 것을 좋아해요.",
            "매일 %s하는 것이 중요해요.",
            "결정 전에 %s해야 해요.",
            "이번 주말에 함께 %s해요.",
            "나는 영어를 %s하려고 노력 중이에요.",
            "팀은 곧 프로젝트를 %s할 거예요."
    );

    private static final List<String> SIMILAR_WORDS = List.of(
            "go", "move", "run", "walk", "come", "leave", "stay",
            "start", "stop", "get", "make", "take", "find", "try"
    );

    private static final List<String> CORRECT_FEEDBACKS = List.of(
            "맞았어요!", "정답이에요.", "잘 찾으셨어요.", "좋아요!",
            "문맥 잘 보셨어요.", "정확해요!", "훌륭해요!"
    );

    private static final List<String> WRONG_FEEDBACKS = List.of(
            "괜찮아요. 다음에 다시 만날 거예요.",
            "조금 헷갈릴 수 있어요.",
            "다시 한 번 볼까요?",
            "이 부분이 까다로워요.",
            "괜찮아요, 천천히 가도 돼요.",
            "비슷한 단어라 헷갈릴 수 있어요."
    );

    // ==================== OX 퀴즈 Fallback ====================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QuizDetailResponse createLocalOXQuiz(Long sessionId, Long userId, QuizWordRequest request) {

        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("### OX 퀴즈 Fallback 생성 - word: {}", request.getWord());

        String word = request.getWord();
        String meaning = request.getMeaning();

        String safeMeaning = (meaning != null && !meaning.isBlank()) ? meaning : word;

        boolean isCorrect = random.nextBoolean();
        String similarWord = getRandomSimilarWord(word);

        // 랜덤 템플릿 선택
        int templateIndex = random.nextInt(SENTENCE_TEMPLATES.size());
        String sentenceTemplate = SENTENCE_TEMPLATES.get(templateIndex);
        String translationTemplate = TRANSLATION_TEMPLATES.get(templateIndex);

        String content;
        String translation;
        String answer;
        String explanation;

        if (isCorrect) {
            content = String.format(sentenceTemplate, word);
            translation = String.format(translationTemplate, meaning);
            answer = "O";
            explanation = String.format("'%s'는 '%s'라는 의미로, 이 문맥에 잘 맞아요.", word, safeMeaning);
        } else {
            content = String.format(sentenceTemplate, similarWord);
            translation = String.format(translationTemplate, meaning);
            answer = "X";
            explanation = String.format("이 문맥에서는 '%s'가 아니라 '%s'가 맞아요. '%s'는 '%s'라는 뜻이에요.",
                    similarWord, word, word, meaning);
        }

        QuizResult result = QuizResult.builder()
                .quizSession(session)
                .user(user)
                .word(word)
                .quizType(QuizType.OX)
                .content(content)
                .translation(translation)
                .question(String.format("'%s'가 문맥에 맞을까요?", isCorrect ? word : similarWord))
                .correctAnswer(answer)
                .explanation(explanation)
                .videoTimestamp(request.getVideoTimeStamp())
                .correctFeedback(getRandomFeedback(CORRECT_FEEDBACKS))
                .wrongFeedback(getRandomFeedback(WRONG_FEEDBACKS))
                .build();

        QuizResult saved = quizResultRepository.save(result);

        return QuizDetailResponse.builder()
                .quizId(saved.getId())
                .quizType(saved.getQuizType())
                .content(saved.getContent())
                .translation(saved.getTranslation())
                .question(saved.getQuestion())
                .answer(saved.getCorrectAnswer())
                .videoTimeStamp(saved.getVideoTimestamp())
                .options(null)
                .build();
    }

    // ==================== 빈칸 퀴즈 Fallback ====================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public QuizDetailResponse createLocalBlankQuiz(Long sessionId, Long userId, QuizWordRequest request) {

        QuizSession session = quizSessionRepository.findById(sessionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SESSION_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("### 빈칸 퀴즈 Fallback 생성 - word: {}", request.getWord());

        String word = request.getWord();
        String meaning = request.getMeaning();

        String safeMeaning = (meaning != null && !meaning.isBlank()) ? meaning : word;

        // 랜덤 템플릿 선택
        int templateIndex = random.nextInt(SENTENCE_TEMPLATES.size());
        String sentenceTemplate = SENTENCE_TEMPLATES.get(templateIndex);
        String translationTemplate = TRANSLATION_TEMPLATES.get(templateIndex);

        // [ ] 형태로 빈칸 만들기
        String content = sentenceTemplate.replace("%s", "[ ]");
        String translation = String.format(translationTemplate, safeMeaning);

        // 4지선다 생성
        List<String> options = generateOptions(word, 4);

        QuizResult result = QuizResult.builder()
                .quizSession(session)
                .user(user)
                .word(word)
                .quizType(QuizType.BLANK)
                .content(content)
                .translation(translation)
                .question("빈칸에 들어갈 단어는 무엇일까요?")
                .correctAnswer(word)
                .explanation(String.format("'%s'는 '%s'라는 뜻으로, 이 문장에 가장 잘 어울려요.", word, meaning))
                .videoTimestamp(request.getVideoTimeStamp())
                .correctFeedback(getRandomFeedback(CORRECT_FEEDBACKS))
                .wrongFeedback(getRandomFeedback(WRONG_FEEDBACKS))
                .build();

        QuizResult saved = quizResultRepository.save(result);

        return QuizDetailResponse.builder()
                .quizId(saved.getId())
                .quizType(saved.getQuizType())
                .content(saved.getContent())
                .translation(saved.getTranslation())
                .question(saved.getQuestion())
                .answer(saved.getCorrectAnswer())
                .videoTimeStamp(saved.getVideoTimestamp())
                .options(options)
                .build();
    }

    // ==================== 유틸리티 메서드 ====================

    private String getRandomSimilarWord(String excludeWord) {
        List<String> candidates = new ArrayList<>(SIMILAR_WORDS);
        candidates.removeIf(w -> w.equalsIgnoreCase(excludeWord));
        if (candidates.isEmpty()) return "go";
        return candidates.get(random.nextInt(candidates.size()));
    }

    private List<String> generateOptions(String correctAnswer, int count) {
        List<String> options = new ArrayList<>();
        options.add(correctAnswer);

        Set<String> used = new HashSet<>();
        used.add(correctAnswer.toLowerCase());

        for (String word : SIMILAR_WORDS) {
            if (options.size() >= count) break;
            if (!used.contains(word.toLowerCase())) {
                options.add(word);
                used.add(word.toLowerCase());
            }
        }

        Collections.shuffle(options);
        return options;
    }

    private String shortenMeaning(String meaning) {
        if (meaning == null || meaning.isBlank()) return "뜻";
        if (meaning.length() <= 5) return meaning;

        String shortened = meaning.split("[,\\(]")[0].trim();
        if (shortened.length() > 5) {
            shortened = shortened.substring(0, 5);
        }
        return shortened;
    }

    private String getRandomFeedback(List<String> feedbacks) {
        return feedbacks.get(random.nextInt(feedbacks.size()));
    }

}
