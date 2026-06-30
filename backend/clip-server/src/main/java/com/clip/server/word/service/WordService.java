package com.clip.server.word.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
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
import com.clip.server.word.entity.WordMeaning;
import com.clip.server.word.entity.WordType;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.clip.server.common.exception.ErrorCode.USER_NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WordService {

    private final VideoRepository videoRepository;
    private final CollectedWordRepository collectedWordRepository;
    private final UserRepository userRepository;

    // 단어 수집 메서드
    @Transactional
    public CollectedWordResponse save(Long userId, CollectedWordRequest collectedWordRequest) {
        // 1. 유저 정보 확인
        User user = userRepository.findById(userId).orElseThrow(()-> new BusinessException(USER_NOT_FOUND));

        Optional<CollectedWord> existingWord = collectedWordRepository.findByUserIdAndVideo_VideoIdAndWord(
                userId,
                collectedWordRequest.getVideoId(),
                collectedWordRequest.getWord()
        );

        if(existingWord.isPresent()) {
            CollectedWord word = existingWord.get();
            // 기존에 호버한 단어를 수집하려고 하는 경우
            if(word.getWordType()==WordType.POPUP && collectedWordRequest.getWordType()==WordType.COLLECT) {
                word.updateToCollect(collectedWordRequest.getSentence(), collectedWordRequest.getTranslation());
                return mapToCollectedWordResponse(word, collectedWordRepository.countByUser(user));
            }
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
                .wordType(collectedWordRequest.getWordType())
                .word(collectedWordRequest.getWord())
                .meaningsByPos(convertToWordMeanings(collectedWordRequest.getMeaningsByPos()))
                .sentence(collectedWordRequest.getSentence())
                .timestamp(collectedWordRequest.getTimestamp())
                .translation(collectedWordRequest.getTranslation())
                .build();
        CollectedWord saved = collectedWordRepository.save(collectedWord);

        // 5. 총 저장 단어수 계산
        long totalCount = collectedWordRepository.countByUser(user);

        return mapToCollectedWordResponse(saved, totalCount);
    }

    // 사용자의 전체 수집 단어 조회 메서드
    public WordListResponse getWords(Long userId,
                                     int page,
                                     int size,
                                     String sort,
                                     String filter,
                                     String keyword
    ) {
        // 1. 페이징 파라미터 검증 (잘못된 값 방어)
        if (page < 0) page = 0;
        if (size <= 0 || size > 100) size = 20;

        // 2. 정렬 조건 변환 (String → Spring Sort 객체)
        Sort sortOption = convertSort(sort);
        PageRequest pageRequest = PageRequest.of(page, size, sortOption);

        // 3. '오늘 수집' 필터 처리
        //    - filter=today일 경우 오늘 00:00:00 이후 수집된 단어만 조회
        //    - filter=all일 경우 null로 두어 전체 조회
        LocalDateTime todayStart = "today".equalsIgnoreCase(filter)
                ? LocalDate.now().atStartOfDay()
                : null;

        // 4. 검색어 정제 (null 또는 공백이면 검색 조건에서 제외)
        String searchKeyword = (keyword != null && !keyword.isBlank())
                ? keyword.trim()
                : null;

        // 5. DB 조회 (Native Query - word + meaning 컬럼 LIKE 검색)
        Page<CollectedWord> wordPage = collectedWordRepository.searchMyWords(
                userId,
                WordType.COLLECT.name(),  // Native Query라 Enum → String 변환
                todayStart,
                searchKeyword,
                pageRequest
        );

        // 6. Entity → DTO 변환 (응답용 경량 객체로 매핑)
        List<WordResponse> words = wordPage.getContent().stream()
                .map(this::mapToWordResponse)
                .toList();

        // 7. 페이지네이션 정보 구성
        PaginationResponse paginationResponse = PaginationResponse.builder()
                .totalCount(wordPage.getTotalElements())
                .currentPage(wordPage.getNumber())
                .pageSize(wordPage.getSize())
                .totalPage(wordPage.getTotalPages())
                .build();

        // 8. 최종 응답 DTO 반환
        return WordListResponse.builder()
                .words(words)
                .pagination(paginationResponse)
                .build();
    }

    // Request DTO의 MeaningByPos → Entity의 WordMeaning 변환
    private List<WordMeaning> convertToWordMeanings(
            List<CollectedWordRequest.MeaningByPos> dtos
    ) {
        if (dtos == null) {
            return List.of();
        }
        return dtos.stream()
                .map(dto -> new WordMeaning(
                        dto.getPartOfSpeech(),
                        dto.getMeanings()
                ))
                .toList();
    }


    private Sort convertSort(String sort) {
        if (sort == null) return Sort.by("collected_at").descending();

        return switch (sort.toLowerCase()) {
            case "oldest"        -> Sort.by("collected_at").ascending();   // 오래된 순
            case "alphabet-asc"  -> Sort.by("word").ascending();           // 알파벳 A-Z
            case "alphabet-desc" -> Sort.by("word").descending();          // 알파벳 Z-A
            default              -> Sort.by("collected_at").descending();  // 최신 순 (기본값)
        };
    }

    // collectWord, totalCount -> CollectWordResponse 변환
    private CollectedWordResponse mapToCollectedWordResponse(CollectedWord collectedWord, long totalCount) {
        return CollectedWordResponse.builder()
                .wordId(collectedWord.getId())
                .collectedAt(collectedWord.getCollectedAt())
                .totalCollectedWords(totalCount)
                .build();
    }

    // collectWord -> WordResponse 변환
    private WordResponse mapToWordResponse(CollectedWord collectedWord) {
        // 품사별 뜻을 플랫 배열로 합치기
        List<String> meanings = collectedWord.getMeaningsByPos().stream()
                .flatMap(wm -> wm.getMeanings().stream())
                .toList();

        return WordResponse.builder()
                .id(collectedWord.getId())
                .word(collectedWord.getWord())
                .meanings(meanings)
                .build();
    }
}
