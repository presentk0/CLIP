package com.clip.server.translation.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.subtitle.repository.SubtitleRepository;
import com.clip.server.subtitle.service.SubtitleService;
import com.clip.server.translation.client.TranslationClient;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.translation.dto.request.WordTranslationRequest;
import com.clip.server.translation.dto.response.TranslationResponse;
import com.clip.server.translation.dto.response.WordTranslationResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.clip.server.common.exception.ErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TranslationService {

    private final TranslationClient translationClient;
    private final SubtitleService subtitleService;
    private final UserRepository userRepository;
    private final SubtitleRepository subtitleRepository;
    private final TranslationCacheService translationCacheService;

    /** 이중 캐시 구조
    * L1(Redis)-> L2(DB)-> DeepL API 번역
     */
    @Transactional
    public TranslationResponse translateAndSave(Long userId, TranslationRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        if (request.getSubtitleRequests() == null || request.getSubtitleRequests().isEmpty()) {
            return TranslationResponse.builder()
                    .translatedTexts(Collections.emptyList())
                    .build();
        }

        String videoId = request.getVideoId();
        int expectedSize = request.getSubtitleRequests().size();

        // L1: Redis 조회 + 검증
        List<String> cachedTranslations = translationCacheService.getVideoTranslations(videoId);
        if (isValidTranslationList(cachedTranslations, expectedSize)) {
            log.info("L1 cache hit - videoId: {}, size: {}", videoId, cachedTranslations.size());
            return buildResponse(cachedTranslations);
        }

        // L1 캐시 무효화 (사이즈 불일치 또는 빈값 포함)
        if (cachedTranslations != null) {
            log.warn("L1 cache invalid - evicting: videoId={}, size={}, expected={}",
                    videoId, cachedTranslations.size(), expectedSize);
            translationCacheService.evict(videoId);
        }

        // L2: DB 조회 + 검증
        List<String> dbTranslations = subtitleRepository.findTranslationsByVideoId(videoId);
        if (isValidTranslationList(dbTranslations, expectedSize)) {
            log.info("L2 cache hit - videoId: {}, size: {}", videoId, dbTranslations.size());
            translationCacheService.putVideoTranslations(videoId, dbTranslations);
            return buildResponse(dbTranslations);
        }

        // DB 데이터 부족/이상 → DeepL 재호출 필요
        log.info("Cache miss or invalid DB data - DeepL 호출 - videoId: {}, expected: {}, db_actual: {}",
                videoId, expectedSize,
                dbTranslations != null ? dbTranslations.size() : 0);

        // DeepL 호출
        List<String> originalTexts = request.getSubtitleRequests().stream()
                .map(TranslationRequest.SubtitleDetail::getText)
                .toList();

        List<String> translatedList = translationClient.translateBatch(originalTexts);

        // 검증
        if (!isValidTranslationList(translatedList, expectedSize)) {
            log.error("DeepL 응답 이상 - videoId: {}, expected: {}, actual: {}",
                    videoId, expectedSize,
                    translatedList != null ? translatedList.size() : 0);
            throw new BusinessException(ErrorCode.TRANSLATION_FAILED);
        }

        // DB 저장 (기존 데이터 업데이트 포함)
        subtitleService.bulkSaveSubtitles(
                videoId,
                request.getTitle(),
                request.getDuration(),
                request.getChannelName(),
                request.getThumbnailUrl(),
                request.getSubtitleRequests(),
                translatedList
        );

        // Redis 저장
        translationCacheService.putVideoTranslations(videoId, translatedList);

        return buildResponse(translatedList);
    }

    /**
     * 번역 리스트 유효성 검증
     */
    private boolean isValidTranslationList(List<String> translations, int expectedSize) {
        if (translations == null) return false;
        if (translations.size() != expectedSize) return false;
        return translations.stream()
                .allMatch(s -> s != null && !s.trim().isEmpty());
    }

    private TranslationResponse buildResponse(List<String> translations) {
        return TranslationResponse.builder()
                .translatedTexts(translations)
                .build();
    }


    // 단어 뜻 번역 메서드
    @Transactional
    public WordTranslationResponse translateWord(Long userId, WordTranslationRequest request) {

        // 1. 유저 검증
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(USER_NOT_FOUND);
        }

        // 2. 빈 입력 방어
        if (request.getMeanings() == null || request.getMeanings().isEmpty()) {
            log.warn("번역할 의미가 비어있음: userId={}, word={}", userId, request.getWord());
            return WordTranslationResponse.builder()
                    .word(request.getWord())
                    .translations(Collections.emptyList())
                    .build();
        }

        String word = request.getWord();
        List<String> meanings = request.getMeanings();

        // 3. 캐시 조회 (L1 → L2 자동)
        Optional<List<String>> cached = translationCacheService.getWordTranslations(word, meanings);
        if (cached.isPresent()) {
            log.info("Word cache hit: word={}", word);
            return WordTranslationResponse.builder()
                    .word(word)
                    .translations(cached.get())
                    .build();
        }

        // 4. Cache miss → DeepL 호출
        log.info("Word cache miss - DeepL 호출: userId={}, word={}, count={}",
                userId, word, meanings.size());

        List<String> translatedList = translationClient.translateBatch(meanings);

        // 5. L1 + L2 동시 저장
        translationCacheService.putWordTranslations(word, meanings, translatedList);

        log.info("단어 번역 완료 및 캐싱: word={}", word);

        return WordTranslationResponse.builder()
                .word(word)
                .translations(translatedList)
                .build();
    }
}
