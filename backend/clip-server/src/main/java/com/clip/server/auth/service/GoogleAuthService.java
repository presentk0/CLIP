package com.clip.server.auth.service;

import com.clip.server.auth.dto.response.GoogleUserInfo;
import com.clip.server.common.exception.BusinessException;
import com.clip.server.common.exception.ErrorCode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Collections;

/**
 * [GoogleAuthService]
 * 구글 OAuth 2.0 ID Token을 검증하고 사용자 정보를 추출합니다.
 * - Google 공개키로 서명 검증
 * - audience(Client ID) 검증
 * - 만료 시간 검증
 * - 자체 검증 방식으로 Stateless 인증 보장
 */
@Slf4j
@Service
public class GoogleAuthService {

    @Value("${google.client-id}")
    private String googleClientId;

    private GoogleIdTokenVerifier verifier;

    /**
     * 검증기 초기화
     */
    private GoogleIdTokenVerifier getVerifier() {
        if (verifier == null) {
            verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    new GsonFactory()
            )
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
        }
        return verifier;
    }

    /**
     * 구글 ID Token 검증 및 사용자 정보 추출
     *
     * @param idTokenString 클라이언트가 보낸 구글 ID Token
     * @return 검증된 사용자 정보
     * @throws IllegalArgumentException 토큰이 유효하지 않은 경우
     */
    public GoogleUserInfo verifyIdToken(String idTokenString) {
        try {
            log.info("ID Token 검증 시작 (길이: {})", idTokenString.length());
            log.info("ID Token 앞 50자: {}", idTokenString.substring(0, Math.min(50, idTokenString.length())));
            // 1. ID Token 검증
            GoogleIdToken idToken = getVerifier().verify(idTokenString);

            if (idToken == null) {
                log.error("유효하지 않은 구글 ID Token");
                throw new IllegalArgumentException("유효하지 않은 구글 ID Token입니다.");
            }

            // 2. Payload(사용자 정보) 추출
            GoogleIdToken.Payload payload = idToken.getPayload();

            // 3. 이메일 검증 여부 확인 (보안)
            Boolean emailVerified = payload.getEmailVerified();
            if (emailVerified == null || !emailVerified) {
                log.error("이메일이 검증되지 않은 구글 계정");
                throw new IllegalArgumentException("이메일이 검증되지 않은 구글 계정입니다.");
            }

            // 4. 사용자 정보 추출
            String oauthId = payload.getSubject();        // 구글 고유 ID (sub)
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String picture = (String) payload.get("picture");

            log.info("구글 로그인 성공: email={}, name={}", email, name);

            return GoogleUserInfo.builder()
                    .oauthId(oauthId)
                    .email(email)
                    .name(name)
                    .profileImageUrl(picture)
                    .build();

        } catch (GeneralSecurityException e) {
            log.error("구글 ID Token 검증 중 보안 오류 발생", e);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        } catch (Exception e) {
            log.error("진짜 에러 메시지: {}", e.getMessage(), e);
            log.error("구글 ID Token 처리 중 오류 발생", e);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
    }
}