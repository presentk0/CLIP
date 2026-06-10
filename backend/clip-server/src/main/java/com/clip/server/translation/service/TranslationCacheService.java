package com.clip.server.translation.service;

import com.clip.server.word.entity.WordTranslationCache;
import com.clip.server.word.repository.WordTranslationCacheRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final WordTranslationCacheRepository wordCacheRepository;
    private final ObjectMapper objectMapper;

    // ===== 자막 번역 =====
    private static final String VIDEO_PREFIX = "video:translations:"; // 키 접두사
    private static final Duration TTL = Duration.ofDays(30); // 유효시간

    // ===== 단어 번역 =====
    private static final String WORD_PREFIX = "word:translation:";
    private static final Duration WORD_TTL = Duration.ofDays(90);

    // ====================== 자막 번역 캐시 메서드 =============================
    // 조회 메서드
    public List<String> getVideoTranslations(String videoId) {
        return (List<String>) redisTemplate.opsForValue()
                .get(VIDEO_PREFIX+videoId);
    }

    // 저장 메서드
    public void putVideoTranslations(String videoId, List<String> translations) {
        redisTemplate.opsForValue().set(
                VIDEO_PREFIX+videoId,
                translations,
                TTL
        );
    }

    /**
    * 삭제 메서드
    * - 자막에 오류가 있어서 다시 번역하고 싶을 때
    * - 영상이 업데이트되어서 캐시를 무효화하고 싶을 때
     */
    public void evict(String videoId) {
        redisTemplate.delete(VIDEO_PREFIX + videoId);
    }

    // ====================== 단어 번역 캐시 메서드 =============================
    /**
     * L1 (Redis) 조회
     */
    @SuppressWarnings("unchecked")
    public List<String> getWordTranslationsFromRedis(String word, List<String> meanings) {
        String key = generateWordKey(word, meanings);
        return (List<String>) redisTemplate.opsForValue().get(key);
    }

    /**
     * L2 (DB) 조회
     */
    public Optional<List<String>> getWordTranslationsFromDb(String word, List<String> meanings) {
        String hash = hashMeanings(meanings);

        return wordCacheRepository.findByWordAndMeaningsHash(word, hash)
                .map(cache -> {
                    // 비동기 hit count 증가 (선택)
                    wordCacheRepository.incrementHitCount(cache.getId(), LocalDateTime.now());
                    return parseTranslations(cache.getTranslations());
                });
    }

    /**
     * L1 + L2 동시 저장
     */
    @Transactional
    public void putWordTranslations(String word, List<String> meanings, List<String> translations) {
        String hash = hashMeanings(meanings);

        // L1: Redis 저장
        String key = generateWordKey(word, meanings);
        redisTemplate.opsForValue().set(key, translations, WORD_TTL);

        // L2: DB 저장 (중복 방지)
        if (!wordCacheRepository.findByWordAndMeaningsHash(word, hash).isPresent()) {
            try {
                wordCacheRepository.save(WordTranslationCache.builder()
                        .word(word)
                        .meaningsHash(hash)
                        .meanings(toJson(meanings))
                        .translations(toJson(translations))
                        .build());
            } catch (DataIntegrityViolationException e) {
                log.debug("Duplicate word cache entry, ignoring: {}", word);
            }
        }
    }

    /**
     * L1 → L2 → null 순서로 조회
     */
    public Optional<List<String>> getWordTranslations(String word, List<String> meanings) {
        // L1
        List<String> fromRedis = getWordTranslationsFromRedis(word, meanings);
        if (fromRedis != null) {
            log.debug("Word L1 hit: {}", word);
            return Optional.of(fromRedis);
        }

        // L2
        Optional<List<String>> fromDb = getWordTranslationsFromDb(word, meanings);
        if (fromDb.isPresent()) {
            log.debug("Word L2 hit: {}", word);
            // L1으로 승격
            String key = generateWordKey(word, meanings);
            redisTemplate.opsForValue().set(key, fromDb.get(), WORD_TTL);
        }

        return fromDb;
    }

    // ===== Helper =====

    private String generateWordKey(String word, List<String> meanings) {
        return WORD_PREFIX + word.toLowerCase() + ":" + hashMeanings(meanings);
    }

    private String hashMeanings(List<String> meanings) {
        // 정렬해서 순서 무관하게
        List<String> sorted = meanings.stream().sorted().toList();
        String joined = String.join("|", sorted);
        return DigestUtils.md5DigestAsHex(joined.getBytes(StandardCharsets.UTF_8));
    }

    private String toJson(List<String> list) {
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> parseTranslations(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
