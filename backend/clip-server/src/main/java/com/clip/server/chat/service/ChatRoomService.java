package com.clip.server.chat.service;

import com.clip.server.chat.dto.request.ChatRoomInitRequest;
import com.clip.server.chat.dto.response.ChatRoomCompleteResponse;
import com.clip.server.chat.dto.response.ChatRoomInitResponse;
import com.clip.server.chat.dto.response.ChatRoomTargetWordResponse;
import com.clip.server.chat.dto.response.ChatWordsResponse;
import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.entity.ChatRoomStatus;
import com.clip.server.chat.repository.ChatMessageRepository;
import com.clip.server.chat.repository.ChatRoomRepository;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import com.clip.server.word.entity.CollectedWord;
import com.clip.server.word.entity.WordMeaning;
import com.clip.server.word.repository.CollectedWordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final CollectedWordRepository collectedWordRepository;

    /**
     * AI 채팅방 초기화 (새로 시작 or 이어하기)
     *
     * - 사용자당 IN_PROGRESS 방은 최대 1개만 존재
     * - 새로 시작: 기존 IN_PROGRESS 방을 COMPLETED 처리 후 새 방 생성
     * - 이어하기: 기존 IN_PROGRESS 방 그대로 사용
     */
    @Transactional
    public ChatRoomInitResponse initChatRoom(Long userId, ChatRoomInitRequest request) {
        log.info("채팅방 초기화 요청. userId={}, isNewStart={}", userId, request.getIsNewStart());

        // 기존 채팅방 조회
        Optional<ChatRoom> existingChatRoom = chatRoomRepository
                .findActiveRoomForUpdate(userId, ChatRoomStatus.IN_PROGRESS);

        if (Boolean.TRUE.equals(request.getIsNewStart())) {
            return handleNewStart(userId, request, existingChatRoom);
        } else {
            return handleResume(existingChatRoom);
        }
    }

    /**
     * 새로 시작 처리
     * - 기존 진행 중인 방이 있으면 COMPLETED 처리
     * - 새 방 생성 후 반환
     */
    private ChatRoomInitResponse handleNewStart(
            Long userId,
            ChatRoomInitRequest request,
            Optional<ChatRoom> existingRoom
    ) {
        // 1. 새로 시작 시 필수 필드 검증
        validateNewStartFields(request);

        // 2. 기존 방이 있으면 완료 처리
        existingRoom.ifPresent(room -> {
            room.complete();
            log.info("기존 채팅방 완료 처리. chatRoomId={}", room.getId());
        });

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. 단어 조회 + 소유권 검증
        CollectedWord word = collectedWordRepository.findById(request.getWordId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORD_NOT_FOUND));

        if (!word.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.WORD_NOT_OWNED);
        }

        // 5. 새 채팅방 생성
        ChatRoom newRoom = ChatRoom.builder()
                .user(user)
                .word(word)
                .scenarioTitle(request.getScenarioTitle())
                .scenarioGoal(request.getScenarioGoal())
                .scenarioSituation(request.getScenarioSituation())
                .aiGender(request.getAiGender())
                .build();

        ChatRoom saved = chatRoomRepository.save(newRoom);
        log.info("새 채팅방 생성 완료. chatRoomId={}", saved.getId());

        // 6. 응답 (시나리오 정보 포함)
        return ChatRoomInitResponse.builder()
                .chatRoomId(saved.getId())
                .hasPreviousMessages(false)
                .scenarioTitle(saved.getScenarioTitle())
                .scenarioGoal(saved.getScenarioGoal())
                .scenarioSituation(saved.getScenarioSituation())
                .build();
    }


    /**
     * 이어하기 처리
     * - 진행 중인 방이 없으면 에러
     */
    private ChatRoomInitResponse handleResume(Optional<ChatRoom> existingRoom) {
        ChatRoom room = existingRoom
                .orElseThrow(() -> new BusinessException(ErrorCode.NO_RESUMABLE_CHAT_ROOM));

        boolean hasPreviousMessages = chatMessageRepository.existsByChatRoomId(room.getId());

        log.info("채팅방 이어하기. chatRoomId={}, hasPreviousMessages={}",
                room.getId(), hasPreviousMessages);

        // 이어하기 시에도 시나리오 정보 포함 → 클라이언트가 화면 새로고침해도 시나리오 박스 그릴 수 있음
        return ChatRoomInitResponse.builder()
                .chatRoomId(room.getId())
                .hasPreviousMessages(hasPreviousMessages)
                .scenarioTitle(room.getScenarioTitle())
                .scenarioGoal(room.getScenarioGoal())
                .scenarioSituation(room.getScenarioSituation())
                .build();
    }

    /**
     * 새로 시작 시 필수 필드 검증
     */
    private void validateNewStartFields(ChatRoomInitRequest request) {
        if (request.getWordId() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "wordId는 필수입니다.");
        }
        if (request.getScenarioTitle() == null || request.getScenarioTitle().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "시나리오 제목은 필수입니다.");
        }
        if (request.getScenarioGoal() == null || request.getScenarioGoal().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "시나리오 미션(goal)은 필수입니다.");
        }
        if (request.getScenarioSituation() == null || request.getScenarioSituation().isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "시나리오 상황(situation)은 필수입니다.");
        }
        if (request.getAiGender() == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "AI 성별은 필수입니다.");
        }
    }

    @Transactional
    public ChatRoomCompleteResponse completeChatRoom(Long userId, Long chatRoomId) {
        log.info("채팅방 종료 요청. userId={}, chatRoomId={}", userId, chatRoomId);

        // 1. 채팅방 조회 + 소유권 검증
        ChatRoom chatRoom = chatRoomRepository.findByIdAndUserId(chatRoomId, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 2. 이미 종료된 방인지 체크
        if (chatRoom.getStatus() == ChatRoomStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.CHAT_ROOM_COMPLETED);
        }

        // 3. 종료 처리
        chatRoom.complete();

        log.info("채팅방 종료 완료. chatRoomId={}", chatRoom.getId());

        return ChatRoomCompleteResponse.builder()
                .chatRoomId(chatRoom.getId())
                .completedAt(chatRoom.getUpdatedAt())
                .build();
    }

    /**
     * AI 채팅 선택 단어 조회
     */
     public ChatRoomTargetWordResponse getChatWord(Long userId, Long chatRoomId) {
         log.info("AI 채팅 학습 단어 조회. userId={}, chatRoomId={}", userId, chatRoomId) ;

         // 1. 채팅방 조회
         ChatRoom chatRoom = chatRoomRepository.findByIdAndUserWithWord(chatRoomId, userId)
                 .orElseThrow(()-> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

         // 2. AI 채팅 선택 단어 여부 존재 확인
         CollectedWord word = chatRoom.getWord();
         if(word == null) {
             log.warn("채팅방에 학습 단어가 없습니다. chatRoomId={}", chatRoomId);
             throw new BusinessException(ErrorCode.WORD_NOT_FOUND);
         }

        return mapToChatRoomTargetWordResponse(word);
     }

     // CollectedWord -> ChatRoomTargetWordResponse 변환
     private ChatRoomTargetWordResponse mapToChatRoomTargetWordResponse(CollectedWord word) {
         return ChatRoomTargetWordResponse.builder()
                 .wordId(word.getId())
                 .word(word.getWord())
                 .meanings(extractMeanings(word.getMeaningsByPos()))
                 .build();
     }

     /**
     * 품사별 의미를 단순 리스트로 변환    { partOfSpeech: "동사", meanings: ["기부하다", "기증하다"] } -> ["기부하다", "기증하다", "기부"]
     */
     private List<String> extractMeanings(List<WordMeaning> meaningsByPos) {
         if(meaningsByPos == null && meaningsByPos.isEmpty()) {
             return new ArrayList<>();
         }

         List<String> result = new ArrayList<>();
         for(WordMeaning wm : meaningsByPos) {
             if(wm.getMeanings() !=null) {
                 result.addAll(wm.getMeanings());
             }
         }
         return result;
     }
}
