package com.clip.server.video.analyzer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CefrDictionary {

    private Map<String, CefrLevel> dictionary = Collections.emptyMap();

    @PostConstruct
    public void init() {
        try (InputStream is = new ClassPathResource("cefr/cefr-vocabulary.json").getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> raw = mapper.readValue(is, new TypeReference<>() {});

            Map<String, CefrLevel> map = new HashMap<>();
            raw.forEach((word, level) -> {
                try {
                    map.put(word.toLowerCase().trim(), CefrLevel.valueOf(level.toUpperCase().trim()));
                } catch (IllegalArgumentException e) {
                    log.warn("알 수 없는 CEFR 레벨: {} -> {}", word, level);
                }
            });
            this.dictionary = map;
            log.info("CEFR 사전 로드 완료: {} 단어", dictionary.size());
        } catch (Exception e) {
            log.error("CEFR 사전 로드 실패", e);
        }
    }

    /**
     * 단어의 CEFR 레벨 조회 (변형형 처리 포함)
     */
    public CefrLevel lookup(String word) {
        if (word == null || word.isBlank()) return null;
        String key = word.toLowerCase().trim();

        // 1차: 그대로 매칭
        CefrLevel level = dictionary.get(key);
        if (level != null) return level;

        // 2차: 간단한 변형 처리 (Lemmatizer 없는 대체용)
        return lookupWithVariations(key);
    }

    /**
     * Lemmatizer 없이 간단한 변형형 처리
     * - 복수형, -ing, -ed, -ly 등 처리
     */
    private CefrLevel lookupWithVariations(String word) {
        // 복수형 -s, -es 제거
        if (word.endsWith("s") && word.length() > 3) {
            CefrLevel level = dictionary.get(word.substring(0, word.length() - 1));
            if (level != null) return level;
            if (word.endsWith("es") && word.length() > 4) {
                level = dictionary.get(word.substring(0, word.length() - 2));
                if (level != null) return level;
            }
            // -ies → -y (studies → study)
            if (word.endsWith("ies") && word.length() > 4) {
                level = dictionary.get(word.substring(0, word.length() - 3) + "y");
                if (level != null) return level;
            }
        }

        // -ing 제거
        if (word.endsWith("ing") && word.length() > 5) {
            String stem = word.substring(0, word.length() - 3);
            CefrLevel level = dictionary.get(stem);
            if (level != null) return level;
            // 자음 중복 제거 (running → runn → run)
            if (stem.length() > 2 && stem.charAt(stem.length() - 1) == stem.charAt(stem.length() - 2)) {
                level = dictionary.get(stem.substring(0, stem.length() - 1));
                if (level != null) return level;
            }
            // -e 추가 (making → mak → make)
            level = dictionary.get(stem + "e");
            if (level != null) return level;
        }

        // -ed 제거
        if (word.endsWith("ed") && word.length() > 4) {
            String stem = word.substring(0, word.length() - 2);
            CefrLevel level = dictionary.get(stem);
            if (level != null) return level;
            level = dictionary.get(stem + "e"); // moved → mov → move
            if (level != null) return level;
            // -ied → -y (studied → study)
            if (word.endsWith("ied") && word.length() > 4) {
                level = dictionary.get(word.substring(0, word.length() - 3) + "y");
                if (level != null) return level;
            }
        }

        // -ly 제거 (부사 → 형용사)
        if (word.endsWith("ly") && word.length() > 4) {
            CefrLevel level = dictionary.get(word.substring(0, word.length() - 2));
            if (level != null) return level;
        }

        // -er, -est (비교/최상급)
        if (word.endsWith("er") && word.length() > 4) {
            CefrLevel level = dictionary.get(word.substring(0, word.length() - 2));
            if (level != null) return level;
        }
        if (word.endsWith("est") && word.length() > 5) {
            CefrLevel level = dictionary.get(word.substring(0, word.length() - 3));
            if (level != null) return level;
        }

        return null;
    }

    public int size() {
        return dictionary.size();
    }
}
