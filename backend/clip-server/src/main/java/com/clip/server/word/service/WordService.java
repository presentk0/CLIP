package com.clip.server.word.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.common.response.ApiResponse;
import com.clip.server.common.response.PaginationResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.word.dto.request.CollectedWordRequest;
import com.clip.server.word.dto.response.CollectedWordResponse;
import com.clip.server.word.dto.response.WordListResponse;
import com.clip.server.word.dto.response.WordResponse;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.repository.CollectedWordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.clip.server.common.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class WordService {

    private final VideoRepository videoRepository;
    private final CollectedWordRepository collectedWordRepository;
    private final UserRepository userRepository;

    // 단어 수집 메서드
    @Transactional
    public CollectedWordResponse save(Long userId, CollectedWordRequest collectedWordRequest) {
        // 1. 유저 정보 확인
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(USER_NOT_FOUND));

        // 2. 단어 중복 확인
        if(collectedWordRepository.existsByUserAndWord(user, collectedWordRequest.getWord())) {
           throw new BusinessException(ErrorCode.WORD_ALREADY_COLLECTED);
        }

        // 3. 비디오 정보 가져오기(없으면 생성)
        Video video = videoRepository.findById(collectedWordRequest.getVideoId())
                .orElseGet(() ->{
                    Video newVideo = Video.builder()
                            .videoId(collectedWordRequest.getVideoId())
                            .title(collectedWordRequest.getTitle())
                            .build();
                    return videoRepository.save(newVideo);
                        }
                );

        // 4. 단어 저장
        CollectedWord collectedWord = CollectedWord.builder()
                .user(user)
                .video(video)
                .word(collectedWordRequest.getWord())
                .sentence(collectedWordRequest.getSentence())
                .timestamp(collectedWordRequest.getTimestamp())
                .translation(collectedWordRequest.getTranslation())
                .build();
        collectedWordRepository.save(collectedWord);
        // 5. 총 저장 단어수 계산
        long totalCount = collectedWordRepository.countByUser(user);

        return mapToCollectedWordResponse(collectedWord, totalCount);
    }

    // 사용자의 전체 수집 단어 조회 메서드
    public WordListResponse getWords(Long userId, int page, int size, String videoId) {

        PageRequest pageRequest = PageRequest.of(page,size, Sort.by("collectedAt").descending());
        Page<CollectedWord> wordPage;

        // 1. Video 정보 확인
        if (videoId != null && !videoId.isBlank()) {
            wordPage = collectedWordRepository.findAllByUserIdAndVideo_VideoId(userId, videoId, pageRequest);
        } else {
            wordPage = collectedWordRepository.findAllByUserId(userId, pageRequest);
        }

        // Page-> WordResponse DTO 변환
        List<WordResponse> words = wordPage.getContent().stream()
                .map(this::mapToWordResponse)
                .toList();

        // Page-> PaginationResponse DTO 변환
        PaginationResponse paginationResponse = PaginationResponse.builder()
                .totalCount(wordPage.getTotalElements())
                .currentPage(wordPage.getNumber())
                .pageSize(wordPage.getSize())
                .totalPage(wordPage.getTotalPages())
                .build();

        // WordListResponse DTO 반환
        return WordListResponse.builder()
                .words(words)
                .pagination(paginationResponse)
                .build();
    }

    public CollectedWordResponse mapToCollectedWordResponse(CollectedWord collectedWord, long totalCount) {
        return CollectedWordResponse.builder()
                .wordId(collectedWord.getId())
                .collectedAt(collectedWord.getCollectedAt())
                .totalCollectedWords(totalCount)
                .build();
    }

    public WordResponse mapToWordResponse(CollectedWord collectedWord) {
        return WordResponse.builder()
                .id(collectedWord.getId())
                .videoId(collectedWord.getVideo().getVideoId())
                .word(collectedWord.getWord())
                .timestamp(collectedWord.getTimestamp())
                .translation(collectedWord.getTranslation())
                .timestamp(collectedWord.getTimestamp())
                .collectedAt(collectedWord.getCollectedAt())
                .build();
    }
}
