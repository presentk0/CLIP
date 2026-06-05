package com.clip.server.chat.service;

import com.clip.server.chat.dto.response.ChatWordResponse;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

//@Slf4j
//@Service
//@RequiredArgsConstructor
//@Transactional(readOnly = true)
//public class ChatService {
//
//    private final CollectedWordRepository collectedWordRepository;
//    private final UserRepository userRepository;
//
//    private static final int MAX_USER_WORDS = 5;          // 사용자 단어 최대 5개
//    private static final int MAX_TOTAL_CANDIDATES = 10;   // 총 후보 최대 10개
//
//    public ChatWordResponse getCandidateWords(Long userId) {
//
//        // 1. 신규 유저 체크
//        User user= userRepository.findById(userId).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
//
//        if(!collectedWordRepository.existsByUserId(userId)) {
//            return buildEmptyResponse();
//        }
//
//        // 2. 오늘 시간 계산
//        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
//        LocalDateTime endOfDay = startOfDay.plusDays(1);
//
//        // 3. 오늘 수집한 단어 조회(1순위)
//        List<CollectedWord> todayWords = collectedWordRepository.findTodayCollectedWords(
//                userId,
//                startOfDay,
//                endOfDay,
//                PageRequest.of(0, MAX_USER_WORDS)
//        );
//
//        // 4. 남은 자리만큼 이전 수집 단어 조회 (2순위)
//        int remainingSlots = MAX_USER_WORDS - todayWords.size();
//        List<CollectedWord> pastWords = remainingSlots > 0
//                ? collectedWordRepository.findCollectedWordsBeforeToday(
//                userId,
//                startOfDay,
//                PageRequest.of(0, remainingSlots))
//                : List.of();
//
//
//    }
//
//    // 신규 사용자(수집 단어X) 경우
//    private ChatWordResponse buildEmptyResponse() {
//        return ChatWordResponse.builder()
//                .hasWords(false)
//                .todayWords(List.of())
//                .collectedWords(List.of())
//                .recommendedWords(List.of())
//                .summary(ChatWordResponse.Summary.builder()
//                        .totalCollected(0)
//                        .totalRecommended(0)
//                        .maxCandidates(MAX_CANDIDATES)
//                        .build())
//                .build();
//    }
//
//    private ChatWordResponse.Word toTodayWordDto()
//}
