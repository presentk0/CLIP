package com.clip.server.word.service;

import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.common.response.ApiResponse;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.video.entity.Video;
import com.clip.server.video.repository.VideoRepository;
import com.clip.server.word.dto.request.CollectedWordRequest;
import com.clip.server.word.dto.response.CollectedWordResponse;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.repository.CollectedWordRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import static com.clip.server.common.exception.ErrorCode.USER_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class WordService {

    private final VideoRepository videoRepository;
    private final CollectedWordRepository collectedWordRepository;
    private final UserRepository userRepository;

    @Transactional
    public CollectedWordResponse collectWord(CollectedWordRequest collectedWordRequest) {
        // 1. 유저 정보 확인
        User user = userRepository.findById(1L).orElseThrow(()-> new BusinessException(USER_NOT_FOUND));

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

    public CollectedWordResponse mapToCollectedWordResponse(CollectedWord collectedWord, long totalCount) {
        return CollectedWordResponse.builder()
                .wordId(collectedWord.getId())
                .collectedAt(collectedWord.getCollectedAt())
                .totalCollectedWords(totalCount)
                .build();
    }
}
