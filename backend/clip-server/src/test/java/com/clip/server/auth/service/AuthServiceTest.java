package com.clip.server.auth.service;

import com.clip.server.auth.dto.response.GoogleUserInfo;
import com.clip.server.auth.dto.response.LoginResult;
import com.clip.server.auth.jwt.JwtProvider;
import com.clip.server.auth.entity.OAuthProvider;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.clip.server.user.entity.User;
import com.clip.server.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenService refreshTokenService;

    private User existingUser;
    private GoogleUserInfo googleUserInfo;
    private final String mockIdToken = "mock.google.id.token";
    private final String mockAccessToken = "mock.access.token";
    private final String mockRefreshToken = "mock.refresh.token";

    @BeforeEach
    void setUp() {
        // 기존 목업 유저 설정
        existingUser = User.builder()
                .email("test@clipzy.com")
                .name("기존유저")
                .profileImageUrl("http://old-image.png")
                .oauthProvider(OAuthProvider.GOOGLE)
                .oauthId("google_12345")
                .build();
        ReflectionTestUtils.setField(existingUser, "id", 1L);

        // 구글 인증 정보 목업 설정
        googleUserInfo = GoogleUserInfo.builder()
                .oauthId("google_12345")
                .email("test@clipzy.com")
                .name("구글새이름")
                .profileImageUrl("http://new-image.png")
                .build();
    }

    @Nested
    @DisplayName("구글 소셜 로그인 테스트")
    class GoogleLoginTest {

        @Test
        @DisplayName("기존 유저 로그인 시 프로필 정보가 업데이트되고 JWT 토큰이 발급된다")
        void googleLogin_ExistingUser_Success() {
            // given
            given(googleAuthService.verifyIdToken(mockIdToken)).willReturn(googleUserInfo);
            given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.GOOGLE, googleUserInfo.getOauthId()))
                    .willReturn(Optional.of(existingUser));
            given(jwtProvider.createAccessToken(existingUser.getId())).willReturn(mockAccessToken);
            given(jwtProvider.createRefreshToken(existingUser.getId())).willReturn(mockRefreshToken);

            // when
            LoginResult result = authService.googleLogin(mockIdToken);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(mockAccessToken);
            assertThat(result.getRefreshToken()).isEqualTo(mockRefreshToken);
            assertThat(result.isNewUser()).isFalse();
            assertThat(result.getUser().getName()).isEqualTo("구글새이름"); // 프로필 업데이트 확인
            assertThat(result.getUser().getProfileImageUrl()).isEqualTo("http://new-image.png");

            verify(refreshTokenService, times(1)).save(existingUser.getId(), mockRefreshToken);
        }

        @Test
        @DisplayName("신규 유저 로그인 시 자동 가입(DB 저장) 후 신규 유저 플래그가 True로 반환된다")
        void googleLogin_NewUser_Success() {
            // given
            given(googleAuthService.verifyIdToken(mockIdToken)).willReturn(googleUserInfo);
            given(userRepository.findByOauthProviderAndOauthId(OAuthProvider.GOOGLE, googleUserInfo.getOauthId()))
                    .willReturn(Optional.empty());

            User newUser = User.builder()
                    .email(googleUserInfo.getEmail())
                    .name(googleUserInfo.getName())
                    .profileImageUrl(googleUserInfo.getProfileImageUrl())
                    .oauthProvider(OAuthProvider.GOOGLE)
                    .oauthId(googleUserInfo.getOauthId())
                    .build();
            ReflectionTestUtils.setField(newUser, "id", 2L);

            given(userRepository.save(any(User.class))).willReturn(newUser);
            given(jwtProvider.createAccessToken(2L)).willReturn(mockAccessToken);
            given(jwtProvider.createRefreshToken(2L)).willReturn(mockRefreshToken);

            // when
            LoginResult result = authService.googleLogin(mockIdToken);

            // then
            assertThat(result).isNotNull();
            assertThat(result.isNewUser()).isTrue();
            assertThat(result.getUser().getId()).isEqualTo(2L);
            assertThat(result.getUser().getEmail()).isEqualTo("test@clipzy.com");

            verify(userRepository, times(1)).save(any(User.class));
            verify(refreshTokenService, times(1)).save(2L, mockRefreshToken);
        }
    }

    @Nested
    @DisplayName("토큰 재발급(Silent Refresh) 및 RTR 보안 테스트")
    class RefreshTokenRotationTest {

        @Test
        @DisplayName("유효한 Refresh Token으로 재발급 시 새로운 Access/Refresh 쌍이 회전발급(RTR)된다")
        void refresh_Success() {
            // given
            Long userId = 1L;
            given(jwtProvider.isExpired(mockRefreshToken)).willReturn(false);
            given(jwtProvider.validateRefreshToken(mockRefreshToken)).willReturn(true);
            given(jwtProvider.getUserId(mockRefreshToken)).willReturn(userId);
            given(refreshTokenService.isValid(userId, mockRefreshToken)).willReturn(true);
            given(userRepository.findById(userId)).willReturn(Optional.of(existingUser));

            String newAccess = "new.access.token";
            String newRefresh = "new.refresh.token";
            given(jwtProvider.createAccessToken(userId)).willReturn(newAccess);
            given(jwtProvider.createRefreshToken(userId)).willReturn(newRefresh);

            // when
            LoginResult result = authService.refresh(mockRefreshToken);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getAccessToken()).isEqualTo(newAccess);
            assertThat(result.getRefreshToken()).isEqualTo(newRefresh);

            // 신규 발급된 Refresh Token이 Redis에 다시 갱신 저장되었는지 검증
            verify(refreshTokenService, times(1)).save(userId, newRefresh);
        }

        @Test
        @DisplayName("만료된 Refresh Token일 경우 REFRESH_TOKEN_EXPIRED 예외를 던진다")
        void refresh_ExpiredToken_ThrowException() {
            // given
            given(jwtProvider.isExpired(mockRefreshToken)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> authService.refresh(mockRefreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_EXPIRED);

            verify(refreshTokenService, never()).save(any(), any());
        }

        @Test
        @DisplayName("만료 예외(ExpiredJwtException)가 터졌을 때 예외를 캐치하여 REFRESH_TOKEN_EXPIRED를 반환한다")
        void refresh_ExpiredJwtException_ThrowException() {
            // given
            given(jwtProvider.isExpired(mockRefreshToken)).willThrow(new ExpiredJwtException(null, null, "Expired"));

            // when & then
            assertThatThrownBy(() -> authService.refresh(mockRefreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_EXPIRED);
        }

        @Test
        @DisplayName("토큰 서명이 위조되었거나 타입이 안 맞으면 INVALID_TOKEN 예외를 던진다")
        void refresh_InvalidToken_ThrowException() {
            // given
            given(jwtProvider.isExpired(mockRefreshToken)).willReturn(false);
            given(jwtProvider.validateRefreshToken(mockRefreshToken)).willReturn(false); // 유효성 검증 실패

            // when & then
            assertThatThrownBy(() -> authService.refresh(mockRefreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TOKEN);
        }

        @Test
        @DisplayName("[RTR 보안] 클라이언트의 토큰이 Redis 세션과 다르면(탈취의심) 저장소를 즉시 파괴하고 MISMATCH 예외를 던진다")
        void refresh_TokenMismatch_TheftDetected_DestroySession() {
            // given
            Long userId = 1L;
            given(jwtProvider.isExpired(mockRefreshToken)).willReturn(false);
            given(jwtProvider.validateRefreshToken(mockRefreshToken)).willReturn(true);
            given(jwtProvider.getUserId(mockRefreshToken)).willReturn(userId);

            // Redis 토큰 불일치 목업
            given(refreshTokenService.isValid(userId, mockRefreshToken)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> authService.refresh(mockRefreshToken))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REFRESH_TOKEN_MISMATCH);

            // 대단히 중요한 검증: 해커의 불법 연장 방지를 위해 기존 Redis 리프레시 키를 날렸는지 검증
            verify(refreshTokenService, times(1)).delete(userId);
            verify(userRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("로그아웃 및 블랙리스트 처리 테스트")
    class LogoutTest {

        @Test
        @DisplayName("로그아웃 시 Redis 세션이 삭제되고 남은 유효시간만큼 Access Token이 블랙리스트에 등록된다")
        void logout_Success() {
            // given
            Long userId = 1L;
            long mockRemainingMs = 1800000L; // 30분 남음
            given(jwtProvider.getRemainingExpiration(mockAccessToken)).willReturn(mockRemainingMs);

            // when
            authService.logout(userId, mockAccessToken);

            // then
            // 1. 유저의 리프레시 세션 소멸 검증
            verify(refreshTokenService, times(1)).delete(userId);
            // 2. 남은 수명만큼 블랙리스트 무효화 등록 검증
            verify(refreshTokenService, times(1)).addToBlacklist(mockAccessToken, mockRemainingMs);
        }

        @Test
        @DisplayName("이미 만료된 Access Token이거나 파싱 예외 발생 시, 로그가 기록되며 안전하게 세션만 파괴하고 종료된다")
        void logout_WithException_SafeExit() {
            // given
            Long userId = 1L;
            given(jwtProvider.getRemainingExpiration(mockAccessToken))
                    .willThrow(new IllegalArgumentException("Invalid token format"));

            // when
            authService.logout(userId, mockAccessToken);

            // then
            // 블랙리스트 등록에 에러가 나더라도 전체 로그아웃 트랜잭션이 중단되지 않고 리프레시는 정상 삭제되어야 함
            verify(refreshTokenService, times(1)).delete(userId);
            verify(refreshTokenService, never()).addToBlacklist(eq(mockAccessToken), anyLong());
        }
    }
}