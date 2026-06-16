package com.clip.server.admin.login.service;

import com.clip.server.admin.login.dto.request.AdminLoginRequest;
import com.clip.server.admin.login.dto.response.AdminLoginResponse;
import com.clip.server.admin.entity.AdminUser;
import com.clip.server.admin.repository.AdminUserRepository;
import com.clip.server.auth.jwt.JwtProvider;
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
}