package com.clip.server.translation.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.subtitle.service.SubtitleService;
import com.clip.server.translation.client.TranslationClient;
import com.clip.server.translation.dto.request.TranslationRequest;
import com.clip.server.translation.dto.response.TranslationResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static com.clip.server.common.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final TranslationClient translationClient;
    private final SubtitleService subtitleService;
    private final UserRepository userRepository;

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

        // 2. 번역할 텍스트만 리스트로 추출
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

        return TranslationResponse.builder()
                .translatedTexts(translatedList)
                .build();
    }
}
