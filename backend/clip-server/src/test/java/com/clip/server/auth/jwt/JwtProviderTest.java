package com.clip.server.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * [JwtProviderTest]
 * 외부 DB나 스프링 컨텍스트 로딩 없이 순수 JUnit 5와 AssertJ만으로
 * JWT의 생성, 만료 계산, 서명 무결성, 타입 오용 방지 로직을 아주 고속으로 검증합니다.
 */
class JwtProviderTest {

    private JwtProvider jwtProvider;

    // 서명 암호화에 안전한 최소 32바이트 이상의 임시 대칭키 설정
    private final String testSecretKey = "vmxkdlaltmxjsmsepdlstksmsdlfrjsmsepdlstksmsdlf";

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();

        ReflectionTestUtils.setField(jwtProvider, "secretKeyPlain", testSecretKey);
        ReflectionTestUtils.setField(jwtProvider, "accessTokenExpirationTime", 3600000L); // 1시간
        ReflectionTestUtils.setField(jwtProvider, "refreshTokenExpirationTime", 1209600000L); // 14일

        // PostConstruct 수동 가동 (SecretKey 객체 빌드)
        jwtProvider.init();
    }

    @Nested
    @DisplayName("Access/Refresh 토큰 발행 및 클레임 파싱 테스트")
    class TokenCreationAndParsingTest {

        @Test
        @DisplayName("Access Token 생성 성공 시 페이로드에 올바른 userId(Subject)와 'access' 타입이 주입된다")
        void createAccessToken_Success() {
            // given
            Long userId = 999L;

            // when
            String accessToken = jwtProvider.createAccessToken(userId);

            // then
            assertThat(accessToken).isNotNull();
            assertThat(jwtProvider.getUserId(accessToken)).isEqualTo(userId);
            assertThat(jwtProvider.validateAccessToken(accessToken)).isTrue();

            // 💡 오용 방지 검증: Access Token은 Refresh Token 검증을 절대로 통과할 수 없어야 함
            assertThat(jwtProvider.validateRefreshToken(accessToken)).isFalse();
        }

        @Test
        @DisplayName("Refresh Token 생성 성공 시 페이로드에 올바른 userId(Subject)와 'refresh' 타입이 주입된다")
        void createRefreshToken_Success() {
            // given
            Long userId = 777L;

            // when
            String refreshToken = jwtProvider.createRefreshToken(userId);

            // then
            assertThat(refreshToken).isNotNull();
            assertThat(jwtProvider.getUserId(refreshToken)).isEqualTo(userId);
            assertThat(jwtProvider.validateRefreshToken(refreshToken)).isTrue();

            // Refresh Token은 Access Token 검증 필터를 절대로 통과할 수 없어야 함
            assertThat(jwtProvider.validateAccessToken(refreshToken)).isFalse();
        }
    }

    @Nested
    @DisplayName("보안 이상 징후 감지 및 유효 수명 테스트")
    class TokenSecurityAndLifespanTest {

        @Test
        @DisplayName("서명이 위조되거나 임의 변조된 토큰은 검증 단계에서 무조건 false를 반환한다")
        void validateToken_TamperedSignature_ReturnFalse() {
            // given
            Long userId = 123L;
            String originalToken = jwtProvider.createAccessToken(userId);
            String tamperedToken = originalToken + "tamperedExtraData"; // 임의 서명 파괴

            // when
            boolean isValid = jwtProvider.validateAccessToken(tamperedToken);

            // then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("만료 시간이 초과된 토큰은 isExpired가 true를 반환하고 검증을 통과하지 못한다")
        void isExpired_WithExpiredToken_ReturnTrue() {
            // given: 만료 시간이 음수(-1시간)인 가짜 만료 토큰 생성기 빌드
            JwtProvider expiredProvider = new JwtProvider();
            ReflectionTestUtils.setField(expiredProvider, "secretKeyPlain", testSecretKey);
            ReflectionTestUtils.setField(expiredProvider, "accessTokenExpirationTime", -3600000L); // 과거로 설정
            ReflectionTestUtils.setField(expiredProvider, "refreshTokenExpirationTime", 1209600000L);
            expiredProvider.init();

            String expiredToken = expiredProvider.createAccessToken(123L);

            // when & then
            assertThat(jwtProvider.isExpired(expiredToken)).isTrue();
            assertThat(jwtProvider.validateAccessToken(expiredToken)).isFalse();
        }

        @Test
        @DisplayName("블랙리스트 차단을 위해 토큰의 남은 만료 시간(ms)을 정확히 계산하여 양수로 반환한다")
        void getRemainingExpiration_Success() {
            // given
            Long userId = 123L;
            String token = jwtProvider.createAccessToken(userId);

            // when
            long remainingMs = jwtProvider.getRemainingExpiration(token);

            // then
            // 1시간(3,600,000ms) 유효 시간 직후이므로, 남은 시간은 0초 이상 1시간 이내여야 함
            assertThat(remainingMs).isGreaterThan(0);
            assertThat(remainingMs).isLessThanOrEqualTo(3600000L);
        }
    }
}