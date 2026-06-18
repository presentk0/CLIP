package com.clip.server.admin.login.service;

import com.clip.server.admin.login.dto.request.AdminLoginRequest;
import com.clip.server.admin.login.dto.response.AdminLoginResponse;
import com.clip.server.admin.entity.AdminUser;
import com.clip.server.admin.repository.AdminUserRepository;
import com.clip.server.auth.jwt.JwtProvider;
import com.clip.server.auth.service.RefreshTokenService;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 관리자 로그인
     */
    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        log.info("관리자 로그인 시도. username={}", request.getUsername());

        // 1. 관리자 조회
        AdminUser admin = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("관리자 로그인 실패: 존재하지 않는 아이디");
                    return new BusinessException(ErrorCode.ADMIN_INVALID_CREDENTIALS);
                });

        // 2. 활성 상태 확인
        if (!admin.isActive()) {
            log.warn("관리자 로그인 실패: 비활성 계정. adminId={}", admin.getId());
            throw new BusinessException(ErrorCode.ADMIN_ACCOUNT_DISABLED);
        }

        // 3. 비밀번호 검증 (BCrypt)
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            log.warn("관리자 로그인 실패: 잘못된 비밀번호. username={}", request.getUsername());
            throw new BusinessException(ErrorCode.ADMIN_INVALID_CREDENTIALS);
        }

        // 4. 마지막 로그인 시각 갱신
        admin.updateLastLoginAt();

        // 5. 관리자용 JWT 발급
        String accessToken = jwtProvider.createAdminToken(admin.getId(), admin.getUsername());

        log.info("관리자 로그인 성공. adminId={}, username={}", admin.getId(), admin.getUsername());

        return AdminLoginResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresIn(3600 * 24)  // 24시간
                .build();
    }

    /**
     * 관리자 로그아웃
     * @param adminId 컨트롤러(또는 시큐리티)에서 토큰 파싱 후 넘겨받은 관리자 고유 ID
     * @param accessToken Authorization 헤더 등에서 파싱해 온 순수 토큰 문자열
     */
    public void logout(Long adminId, String accessToken) {
        log.info("관리자 로그아웃 시도. adminId={}", adminId);

        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                // 토큰의 남은 TTL(밀리초) 계산
                long remainingMs = jwtProvider.getRemainingExpiration(accessToken);

                if (remainingMs > 0) {
                    // Redis 블랙리스트 세팅 완료!
                    refreshTokenService.addToBlacklist(accessToken, remainingMs);
                    log.info("관리자 Access Token 블랙리스트 등록 완료. 남은 시간: {}ms", remainingMs);
                }
            } catch (Exception e) {
                log.warn("관리자 Access Token 블랙리스트 등록 실패", e);
            }
        }

        log.info("관리자 로그아웃 완료. adminId={}", adminId);
    }
}