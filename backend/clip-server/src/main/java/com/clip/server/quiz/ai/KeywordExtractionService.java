package com.clip.server.quiz.ai;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class KeywordExtractionService {

    private TokenizerME tokenizer;
    private POSTaggerME posTagger;

    /**
     * 서버 기동 시 OpenNLP 모델을 로드합니다.
     * 모델 파일은 src/main/resources/models/ 폴더에 위치
     */
    @PostConstruct
    public void init() {
        try {
            // 1. Tokenizer 모델 로드 (단어 분리용)
            InputStream tokenModelIn = new ClassPathResource("models/en-token.bin").getInputStream();
            tokenizer = new TokenizerME(new TokenizerModel(tokenModelIn));

            // 2. POS Tag 모델 로드 (품사 판별용)
            InputStream posModelIn = new ClassPathResource("models/en-pos-maxent.bin").getInputStream();
            posTagger = new POSTaggerME(new POSModel(posModelIn));

            log.info("OpenNLP 모델 로드 완료 (en-token.bin, en-pos-maxent.bin)");
        } catch (Exception e) {
            log.error("OpenNLP 모델 로드 실패: {}. src/main/resources/models/ 폴더 내 파일명과 경로를 확인하세요.", e.getMessage());
        }
    }

    /**
     * 영문 자막 텍스트에서 핵심 단어(명사, 동사, 형용사)를 추출합니다.
     */
    public List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty() || tokenizer == null || posTagger == null) {
            return List.of();
        }

        // 1. 문장을 단어 단위로 분리 (Tokenize)
        String[] tokens = tokenizer.tokenize(text);

        // 2. 단어별 품사 태깅 (POS Tagging)
        String[] tags = posTagger.tag(tokens);

        Set<String> keywords = new HashSet<>();

        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i].toLowerCase();
            // 태그 체계의 다양성을 고려하여 대문자로 통일 후 비교
            String tag = tags[i].toUpperCase();

            // 학습 가치가 있는 품사인지 필터링
            if (isLearningValue(word, tag)) {
                keywords.add(word);
            }
        }

        return new ArrayList<>(keywords);
    }

    /**
     * 특정 단어가 학습할 가치가 있는 품사인지 판단합니다.
     */
    private boolean isLearningValue(String word, String tag) {
        // 단어 길이가 3자 미만이거나 순수 알파벳이 아니면 제외
        if (word.length() < 3 || !word.matches("[a-zA-Z]+")) {
            return false;
        }

        /**
         * 다양한 OpenNLP 모델의 태그 체계를 모두 지원합니다.
         * 1. Penn Treebank: NN(명사), VB(동사), JJ(형용사) 계열
         * 2. UD (Universal Dependencies): NOUN, VERB, ADJ, PROPN(고유명사)
         */
        return tag.startsWith("NN") || tag.startsWith("VB") || tag.startsWith("JJ") ||
                tag.contains("NOUN") || tag.contains("VERB") || tag.contains("ADJ") || tag.contains("PROPN");
    }

    /**
     * 난이도 분석용 - 의미있는 단어를 모두 추출 (중복 허용, 분포 계산용)
     * extractKeywords()와 달리:
     * - 중복 제거 X (빈도 계산해야 하므로)
     * - 더 많은 품사 포함 (부사, 전치사 등)
     */
    public List<String> tokenizeForDifficulty(String text) {
        if (text == null || text.trim().isEmpty() || tokenizer == null || posTagger == null) {
            return List.of();
        }

        String[] tokens = tokenizer.tokenize(text);
        String[] tags = posTagger.tag(tokens);

        List<String> result = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            String word = tokens[i].toLowerCase();
            String tag = tags[i].toUpperCase();

            if (isAnalyzable(word, tag)) {
                result.add(word);
            }
        }
        return result;
    }

    /**
     * 난이도 분석에 사용할 단어인지 판단 (extractKeywords보다 범위 넓음)
     */
    private boolean isAnalyzable(String word, String tag) {
        if (word.length() < 2 || !word.matches("[a-zA-Z]+")) {
            return false;
        }

        // 의미있는 품사: 명사, 동사, 형용사, 부사, 전치사, 접속사, 한정사, 대명사, 조동사
        return tag.startsWith("NN") || tag.startsWith("VB") || tag.startsWith("JJ") ||
                tag.startsWith("RB") || tag.startsWith("IN") || tag.startsWith("CC") ||
                tag.startsWith("DT") || tag.startsWith("PRP") || tag.startsWith("MD") ||
                tag.contains("NOUN") || tag.contains("VERB") || tag.contains("ADJ") ||
                tag.contains("ADV") || tag.contains("ADP") || tag.contains("CCONJ");
    }
}