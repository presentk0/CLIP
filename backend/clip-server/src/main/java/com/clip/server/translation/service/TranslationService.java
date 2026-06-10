package com.clip.server.translation.service;

import com.clip.server.common.exception.BusinessException;
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

        // 유저 정보 확인
        User user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(USER_NOT_FOUND));

        // 1. 방어 코드: 요청 데이터가 없으면 빈 응답 반환
        if (request.getSubtitleRequests() == null || request.getSubtitleRequests().isEmpty()) {
            return TranslationResponse.builder()
                    .translatedTexts(Collections.emptyList())
                    .build();
        }

        String videoId = request.getVideoId();

        // 2. L1: Redis에서 videoId로 조회
        List<String> cachedTranslations = translationCacheService.getVideoTranslations(videoId);
        if(cachedTranslations != null) {
            log.info("L1 cache hit for video: {}", videoId);
            return TranslationResponse.builder()
                    .translatedTexts(cachedTranslations)
                    .build();
        }

        // 3. L2: DB 조회 (subtitle 테이블)
        List<String> dbTranslations = subtitleRepository.findTranslationsByVideoId(videoId);
        if(!dbTranslations.isEmpty()) {
            log.info("L2 cache hit for video: {}", videoId);

            // 캐시 저장- L1 승격
            translationCacheService.putVideoTranslations(videoId, dbTranslations);
            return TranslationResponse.builder()
                    .translatedTexts(dbTranslations)
                    .build();
        }

        // 2. 번역할 텍스트만 리스트로 추출
        log.info("Cache miss - DeepL 호출 - videoId: {}", videoId);

        List<String> originalTexts = request.getSubtitleRequests().stream()
                .map(TranslationRequest.SubtitleDetail::getText)
                .toList();

        // 3. DeepL 번역 수행
        List<String> translatedList = translationClient.translateBatch(originalTexts);

        // 4. 번역 결과와 원본 정보를 함께 SubtitleService의 벌크 저장 로직으로 전달
        subtitleService.bulkSaveSubtitles(
                request.getVideoId(),
                request.getTitle(),
                request.getDuration(),
                request.getChannelName(),
                request.getThumbnailUrl(),
                request.getSubtitleRequests(),
                translatedList
        );

        // 5. Redis 저장
        translationCacheService.putVideoTranslations(videoId, translatedList);

        return TranslationResponse.builder()
                .translatedTexts(translatedList)
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
