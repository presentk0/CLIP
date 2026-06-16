package com.clip.server.report.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatReportService 단위 테스트")
class AiChatReportServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatReportRepository chatReportRepository;

    @InjectMocks
    private AiChatReportService aiChatReportService;

    private User mockUser;
    private ChatMessage mockMessage;
    private ChatRoom mockChatRoom;

    @BeforeEach
    void setUp() throws Exception {
        // 공통 사용자 mock
        mockUser = createInstance(User.class);
        ReflectionTestUtils.setField(mockUser, "id", 1L);
        ReflectionTestUtils.setField(mockUser, "email", "test@example.com");
        ReflectionTestUtils.setField(mockUser, "name", "테스트유저");

        // 공통 메시지 mock
        mockMessage = createInstance(ChatMessage.class);
        ReflectionTestUtils.setField(mockMessage, "id", 100L);

        // 공통 채팅방 mock
        mockChatRoom = createInstance(ChatRoom.class);
        ReflectionTestUtils.setField(mockChatRoom, "id", 200L);
    }

    // 메시지 신고 테스트
    @Nested
    @DisplayName("메시지 신고")
    class MessageReport {

        @Test
        @DisplayName("정상적으로 메시지를 신고할 수 있다")
        void postMessageReport_Success() throws Exception {
            // given
            ChatReportRequest request = createRequest(
                    ReportType.MESSAGE,
                    100L,
                    "부적절한 메시지입니다."
            );

            ChatReport savedReport = ChatReport.builder()
                    .user(mockUser)
                    .reportType(ReportType.MESSAGE)
                    .message(mockMessage)
                    .content(request.getContent())
                    .build();
            ReflectionTestUtils.setField(savedReport, "id", 1L);
            ReflectionTestUtils.setField(savedReport, "createdAt", LocalDateTime.now());

            given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
            given(chatMessageRepository.findById(100L)).willReturn(Optional.of(mockMessage));
            given(chatReportRepository.save(any(ChatReport.class))).willReturn(savedReport);

            // when
            AiChatReportResponse response = aiChatReportService.postAiChatReport(1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getReportId()).isEqualTo(1L);
            assertThat(response.getReportType()).isEqualTo("MESSAGE");
            assertThat(response.getCreatedAt()).isNotNull();

            verify(chatReportRepository).save(any(ChatReport.class));
            verify(chatRoomRepository, never()).findById(any());
        }

        @Test
        @DisplayName("존재하지 않는 메시지 ID로 신고하면 예외가 발생한다")
        void postMessageReport_MessageNotFound_Fail() throws Exception {
            // given
            ChatReportRequest request = createRequest(
                    ReportType.MESSAGE,
                    999L,
                    "신고합니다."
            );

            given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
            given(chatMessageRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> aiChatReportService.postAiChatReport(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MESSAGE_NOT_FOUND);

            verify(chatReportRepository, never()).save(any());
        }
    }

    // 시나리오 신고 테스트
    @Nested
    @DisplayName("시나리오 신고")
    class ScenarioReport {

        @Test
        @DisplayName("정상적으로 시나리오를 신고할 수 있다")
        void postScenarioReport_Success() throws Exception {
            // given
            ChatReportRequest request = createRequest(
                    ReportType.SCENARIO,
                    200L,
                    "부적절한 시나리오입니다."
            );

            ChatReport savedReport = ChatReport.builder()
                    .user(mockUser)
                    .reportType(ReportType.SCENARIO)
                    .chatRoom(mockChatRoom)
                    .content(request.getContent())
                    .build();
            ReflectionTestUtils.setField(savedReport, "id", 2L);
            ReflectionTestUtils.setField(savedReport, "createdAt", LocalDateTime.now());

            given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
            given(chatRoomRepository.findById(200L)).willReturn(Optional.of(mockChatRoom));
            given(chatReportRepository.save(any(ChatReport.class))).willReturn(savedReport);

            // when
            AiChatReportResponse response = aiChatReportService.postAiChatReport(1L, request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getReportId()).isEqualTo(2L);
            assertThat(response.getReportType()).isEqualTo("SCENARIO");
            assertThat(response.getCreatedAt()).isNotNull();

            verify(chatReportRepository).save(any(ChatReport.class));
            verify(chatMessageRepository, never()).findById(any());
        }

        @Test
        @DisplayName("존재하지 않는 채팅방 ID로 신고하면 예외가 발생한다")
        void postScenarioReport_ChatRoomNotFound_Fail() throws Exception {
            // given
            ChatReportRequest request = createRequest(
                    ReportType.SCENARIO,
                    999L,
                    "신고합니다."
            );

            given(userRepository.findById(1L)).willReturn(Optional.of(mockUser));
            given(chatRoomRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> aiChatReportService.postAiChatReport(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.CHAT_ROOM_NOT_FOUND);

            verify(chatReportRepository, never()).save(any());
        }
    }

    // 사용자 검증 테스트
    @Nested
    @DisplayName("사용자 검증")
    class UserValidation {

        @Test
        @DisplayName("존재하지 않는 사용자가 신고하면 예외가 발생한다")
        void postReport_UserNotFound_Fail() throws Exception {
            // given
            ChatReportRequest request = createRequest(
                    ReportType.MESSAGE,
                    100L,
                    "신고합니다."
            );

            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> aiChatReportService.postAiChatReport(999L, request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.USER_NOT_FOUND);

            // 사용자 조회 실패 시 메시지/채팅방 조회도 안 됨
            verify(chatMessageRepository, never()).findById(any());
            verify(chatRoomRepository, never()).findById(any());
            verify(chatReportRepository, never()).save(any());
        }
    }

    // 헬퍼 메서드
    /**
     * protected 생성자를 가진 클래스의 인스턴스를 리플렉션으로 생성
     */
    private <T> T createInstance(Class<T> clazz) throws Exception {
        Constructor<T> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    /**
     * ChatReportRequest 생성 (protected 생성자 우회)
     */
    private ChatReportRequest createRequest(ReportType type, Long targetId, String content) throws Exception {
        ChatReportRequest request = createInstance(ChatReportRequest.class);
        ReflectionTestUtils.setField(request, "reportType", type);
        ReflectionTestUtils.setField(request, "targetId", targetId);
        ReflectionTestUtils.setField(request, "content", content);
        return request;
    }
}