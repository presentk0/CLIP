package com.clip.server.report.service;

import com.clip.server.chat.dto.response.ChatReportResponse;
import com.clip.server.chat.entity.ChatMessage;
import com.clip.server.chat.entity.ChatRoom;
import com.clip.server.chat.repository.ChatMessageRepository;
import com.clip.server.chat.repository.ChatRoomRepository;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.report.dto.AiChatReportResponse;
import com.clip.server.report.dto.ChatReportRequest;
import com.clip.server.report.entity.ChatReport;
import com.clip.server.report.entity.ReportType;
import com.clip.server.report.repository.ChatReportRepository;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatReportService {

    private final UserRepository userRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatReportRepository chatReportRepository;

    @Transactional
    public AiChatReportResponse postAiChatReport(Long userId, ChatReportRequest chatReportRequest) {

        log.info("신고 작성 요청. userId={}, type={}, targetId={}",userId, chatReportRequest.getReportType(), chatReportRequest.getTargetId());

        // 1. 사용자 조회
        User user = userRepository.findById(userId).orElseThrow(()->new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 신고 타입에 따라 대상 조회 + 엔티티 생성
        ChatReport chatReport;

        if(chatReportRequest.getReportType() == ReportType.MESSAGE) {
            // 채팅 메시지 신고
            ChatMessage chatMessage = chatMessageRepository.findById(chatReportRequest.getTargetId())
                    .orElseThrow(()-> new BusinessException(ErrorCode.MESSAGE_NOT_FOUND));
            chatReport = ChatReport.builder()
                    .user(user)
                    .reportType(ReportType.MESSAGE)
                    .message(chatMessage)
                    .content(chatReportRequest.getContent())
                    .build();
        } else {
            // 채팅 시나리오 신고
            ChatRoom chatRoom = chatRoomRepository.findById(chatReportRequest.getTargetId())
                    .orElseThrow(()-> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

            chatReport = ChatReport.builder()
                    .user(user)
                    .reportType(ReportType.SCENARIO)
                    .chatRoom(chatRoom)
                    .content(chatReportRequest.getContent())
                    .build();
        }

        // 3. 저장
        ChatReport saved = chatReportRepository.save(chatReport);

        return mapToAiChatReportResponse(saved);
    }

    private AiChatReportResponse mapToAiChatReportResponse(ChatReport chatReport) {
        return AiChatReportResponse.builder()
                .reportId(chatReport.getId())
                .reportType(String.valueOf(chatReport.getReportType()))
                .createdAt(chatReport.getCreatedAt())
                .build();
    }
}
