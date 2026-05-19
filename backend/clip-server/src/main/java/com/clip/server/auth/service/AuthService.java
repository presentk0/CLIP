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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [AuthService]
 * 인증 관련 핵심 비즈니스 로직
 * - 구글 로그인 처리
 * - 회원가입 (자동)
 * - 토큰 재발급
 * - 로그아웃
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final GoogleAuthService googleAuthService;
    private final JwtProvider jwtProvider;
    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    /**
     * 구글 로그인 (회원가입 자동 처리)
     *
     * @param idToken 구글 ID Token
     * @return 로그인 결과 (토큰 + 사용자 정보)
     */
    @Transactional
    public LoginResult googleLogin(String idToken) {
        // 1. 구글 ID Token 검증 + 사용자 정보 추출
        GoogleUserInfo googleInfo = googleAuthService.verifyIdToken(idToken);

        // 2. 사용자 조회 또는 생성
        boolean[] isNewUserFlag = {false};
        User user = userRepository
                .findByOauthProviderAndOauthId(OAuthProvider.GOOGLE, googleInfo.getOauthId())
                .map(existingUser -> {
                    // 기존 사용자: 프로필 정보 업데이트
                    existingUser.updateProfile(googleInfo.getName(), googleInfo.getProfileImageUrl());
                    log.info("기존 사용자 로그인: userId={}, email={}",
                            existingUser.getId(), existingUser.getEmail());
                    return existingUser;
                })
                .orElseGet(() -> {
                    // 신규 사용자: 회원가입 처리
                    isNewUserFlag[0] = true;
                    User newUser = User.builder()
                            .email(googleInfo.getEmail())
                            .name(googleInfo.getName())
                            .profileImageUrl(googleInfo.getProfileImageUrl())
                            .oauthProvider(OAuthProvider.GOOGLE)
                            .oauthId(googleInfo.getOauthId())
                            .build();
                    User saved = userRepository.save(newUser);
                    log.info("신규 사용자 가입: userId={}, email={}",
                            saved.getId(), saved.getEmail());
                    return saved;
                });

        // 3. JWT 토큰 발급
        String accessToken = jwtProvider.createAccessToken(user.getId());
        String refreshToken = jwtProvider.createRefreshToken(user.getId());

        // 4. Refresh Token Redis 저장
        refreshTokenService.save(user.getId(), refreshToken);

        // 5. 결과 반환
        return LoginResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(user)
                .isNewUser(isNewUserFlag[0])
                .build();
    }

    /**
     * Access Token 재발급 (Refresh Token Rotation)
     *
     * @param refreshToken 클라이언트가 보낸 Refresh Token
     * @return 새로 발급된 토큰들
     */
    /**
     * 토큰 재발급 (예외 구분 처리)
     */
    @Transactional(readOnly = true)
    public LoginResult refresh(String refreshToken) {
        // 1. Refresh Token 검증 (만료 vs 유효하지 않음 구분)
        Long userId;
        try {
            // 만료된 토큰인지 먼저 체크
            if (jwtProvider.isExpired(refreshToken)) {
                throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
            }

            // 유효성 검증 (서명, 타입 등)
            if (!jwtProvider.validateRefreshToken(refreshToken)) {
                throw new BusinessException(ErrorCode.INVALID_TOKEN);
            }

            userId = jwtProvider.getUserId(refreshToken);

        } catch (ExpiredJwtException e) {
            // jwt 라이브러리가 던지는 만료 예외
            log.error("Refresh Token 만료: {}", e.getMessage());
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (BusinessException e) {
            // 위에서 던진 커스텀 예외는 그대로 전달
            throw e;
        } catch (Exception e) {
            // 그 외 (서명 오류, 형식 오류 등)
            log.error("Refresh Token 검증 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 2. Redis 저장 토큰과 비교 (재사용 감지)
        if (!refreshTokenService.isValid(userId, refreshToken)) {
            log.error("Refresh Token 불일치 - 탈취 의심: userId={}", userId);
            // 보안: 탈취 의심 시 강제 로그아웃
            refreshTokenService.delete(userId);
            throw new BusinessException(ErrorCode.REFRESH_TOKEN_MISMATCH);
        }

        // 3. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 4. 새 토큰 발급 (Rotation)
        String newAccessToken = jwtProvider.createAccessToken(userId);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        refreshTokenService.save(userId, newRefreshToken);

        log.info("토큰 재발급 완료: userId={}", userId);

        return LoginResult.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .user(user)
                .isNewUser(false)
                .build();
    }

    /**
     * 로그아웃
     */
    public void logout(Long userId, String accessToken) {
        refreshTokenService.delete(userId);

        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                long remainingMs = jwtProvider.getRemainingExpiration(accessToken);
                if (remainingMs > 0) {
                    refreshTokenService.addToBlacklist(accessToken, remainingMs);
                }
            } catch (Exception e) {
                log.warn("Access Token 블랙리스트 등록 실패", e);
            }
        }

        log.info("로그아웃 완료: userId={}", userId);
    }
}