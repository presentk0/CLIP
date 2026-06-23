/* global chrome */

import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { handleApiError } from '../../utils/errorHandler';
import styles from '../LoginPage/LoginPage.module.css'
import { Spinner } from '../../components/Spinner/Spinner';


function Button({
  variant = 'primary',
  size = 'medium',
  // fullWidth = true 넓게 펼침
  // fullWidth = false 아무것도 안 붙임
  fullWidth = false,
  isLoading = false,
  children,
  className = '',
  disabled,
  type= "button",
  ...props
}) {
  // true가 되는 값인 클래스 이름을 합치고 빈 문자열 ''같은 값 제거
  const classNames = [
    styles.button,
    styles[variant],
    styles[size],
    fullWidth ? styles.fullWidth : '',
    className,
  ].filter(Boolean).join(' ');

  return (
    <button
      type={type}
      className={classNames}
      // disabled가 true이거나 로딩 중이면 클릭 불가
      disabled={disabled || isLoading}
      // 화면 읽기 도구에게 이 버튼 지금 작업 중 이라고 알려줌
      aria-busy={isLoading}
      {...props}
    >
      {/* children = 버튼 안에 들어갈 기본 내용 */}
      {isLoading ? <Spinner /> : children}
    </button>
  );
}

Button.displayName = 'Button';




export default function LoginPage() {
  // 코드로 페이지 이동
  const navigate = useNavigate();
  // 현재 경로 정보 가져오기
  const location = useLocation();
  const { loginWithGoogle } = useAuth();


  // const { needsOnboarding } = useAuth();




  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const SAFE_LOGIN_ERRORS = {
    'INVALID_TOKEN': '유효하지 않은 Google 토큰입니다',
    'UNAUTHORIZED': '인증에 실패했습니다',
    'REFRESH_TOKEN_EXPIRED': '세션이 만료되었습니다. 다시 로그인해주세요',
    'INVALID_REFRESH_TOKEN': '인증 정보가 유효하지 않습니다',
    'TOKEN_EXPIRED': '토큰이 만료되었습니다',
    'USER_NOT_FOUND': '사용자 정보를 찾을 수 없습니다',
  };

  // 사용자가 원래 가려던 페이지(from)로 돌아갈 경로를 가져오기. 없으면 홈(/)으로
  const from = location.state?.from?.pathname || '/';



  // Google 로그인
  const handleGoogleLogin = async () => {
    setError('');
    setIsLoading(true);

    // CLIENT_ID는 내 앱 식별표.
    const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

  try {
    // 리다이렉트 URI (Google Console에 등록한 것과 같아야 함) 가져오기
    // 크롬이 자동으로 만들어주는 확장프로그램 전용 주소
    // 구글 로그인 끝나면 -> 구글이 이 주소로 사용자를 보냄 -> 크롬이 그 신호를 잡아서 우리 코드에 전달
    const redirectUri = chrome.identity.getRedirectURL();
    
    // 보안 토큰 nonce 생성 (재사용 공격 방지)
    // 매번 다른 랜덤 문자열 생성
    const nonce = crypto.randomUUID();

    // Google OAuth URL 만들기
    // encodeURIComponent는 URL에 못 들어가는 특수문자를 안전하게 변환해주는 함수 (예: : -> %3A)
    const authUrl = 
      // 구글 로그인 페이지
      `https://accounts.google.com/o/oauth2/v2/auth` +
      // 내 앱이 누군지
      `?client_id=${GOOGLE_CLIENT_ID}` +
      // ID Token 달라고 요청
      `&response_type=id_token` +
      // 결과 받을 주소
      `&redirect_uri=${encodeURIComponent(redirectUri)}` +
      // 받고 싶은 정보
      `&scope=${encodeURIComponent('openid email profile')}` +
      // 보안용 일회용 코드
      `&nonce=${nonce}` +
      // prompt = 어떤 화면 보여줄지
      // 매번 계정 선택 화면 표시
      `&prompt=select_account`;

    // 구글 로그인 팝업 열기
    const responseUrl = await new Promise((resolve, reject) => {
      // launchWebAuthFlow = 새 창을 띄워서 사용자가 구글 로그인하게 함
      // interactive: true = 사용자에게 화면 보여줌 (false면 백그라운드에서 시도)
      // 에러 (chrome.runtime.lastError), 취소 (!redirectUrl), 성공 (redirectUrl 있음) 시 콜백
      // 크롬 API가 콜백 방식이라서, await로 쓰려고 Promise로 변환
      chrome.identity.launchWebAuthFlow(
        // 어떤 URL을 열지 + 사용자 보여줄지
        { url: authUrl, interactive: true },
        // 끝났을 때 콜백
        (redirectUrl) => {
          if (chrome.runtime.lastError) {
            reject(new Error(chrome.runtime.lastError.message));
          } else if (!redirectUrl) {
            reject(new Error('로그인이 취소되었습니다'));
          } else {
            resolve(redirectUrl);
          }
        }
      );
    });

    // URL의 # 뒤에서 id_token 추출
    // 구글이 보내주는 결과 URL 모양
    // https://abcdefghijklmnop.chromiumapp.org/#id_token=eyJhbGc...&token_type=Bearer&...
    // new URL(responseUrl) -> URL 객체로 변환
    // .hash = "#id_token=eyJ...&token_type=Bearer&..." (# 포함)
    // .substring(1) = "id_token=eyJ...&token_type=Bearer&..." (# 제거)
    const hash = new URL(responseUrl).hash.substring(1);
    // new URLSearchParams(hash) = 쿼리 파싱 객체로 변환
    const params = new URLSearchParams(hash);


    const error = params.get('error');
    if (error) {
      throw new Error(`구글 인증 실패: ${error}`);
    }

    // .get('id_token') = "eyJhbGc..." (실제 ID Token 값)
    const idToken = params.get('id_token');

    if (!idToken) {
      throw new Error('ID Token을 받지 못했습니다');
    }

    // nonce 검증
    const payload = decodeJwtPayload(idToken);
    if (payload.nonce !== nonce) {
      throw new Error('nonce 불일치');
    }
    

    const user = await loginWithGoogle(idToken);


    if (user.needsOnboarding) {
      // 신규 유저는 약관 동의부터
      // replace: true 는 뒤로가기 해도 이 페이지에 못 오게 막기
      navigate('/onboarding/terms', { replace: true });
    } else {
      // 기존 유저는 원래 가려던 곳 또는 디폴트
      navigate(from, { replace: true });
    }
  } catch (error) {
    // setError(SAFE_LOGIN_ERRORS[error.code] || '로그인에 실패했습니다');
    setError(handleApiError(error, '로그인', SAFE_LOGIN_ERRORS) || '로그인에 실패했습니다');
  } finally {
    setIsLoading(false);
  }
};



function decodeJwtPayload(token) {
  // payload 부분
  const base64Url = token.split('.')[1];
  
  // Base64URL → Base64 변환
  const base64 = base64Url
    // - -> +
    .replace(/-/g, '+')
    // _ -> /
    .replace(/_/g, '/');

  // 패딩 추가 (4의 배수로 맞추기)
  // 부족한 만큼 = 추가
  const padded = base64 + '='.repeat((4 - base64.length % 4) % 4);

  
  // 디코딩
  return JSON.parse(atob(padded));
}



  return (
    <div className={styles.login}>
      <div className={styles.login2}>
        <div className={styles.login3}>
          <div className={styles.login4}>
            <svg 
            className={styles.login5}
            xmlns="http://www.w3.org/2000/svg" width="41" height="24" viewBox="0 0 41 24" fill="none">
              <path d="M28.194 0C30.9105 0 33.2725 1.88063 33.9643 4.42123C37.963 5.13258 40.9999 8.65481 41 12.8551V15.4305C40.9997 20.1556 37.1703 23.9998 32.4632 24H16.6534C13.5995 24 11.1023 21.4932 11.1023 18.4276C11.1024 15.362 13.5996 12.8551 16.6534 12.8551H23.4024C24.7522 12.8551 26.0177 12.1771 26.777 11.0423C27.3001 10.2469 28.3622 10.0447 29.1381 10.5693C29.9142 11.0943 30.1339 12.1604 29.611 12.9395C28.2275 15.0227 25.8995 16.2773 23.4024 16.2773H16.6534C15.4724 16.2773 14.5099 17.2421 14.5097 18.4276C14.5097 19.6132 15.4723 20.5795 16.6534 20.5795H32.4467C35.2639 20.579 37.5741 18.2754 37.5744 15.4305V12.8551C37.5743 10.027 35.2809 7.70657 32.4467 7.70613C31.5019 7.70613 30.7414 6.94433 30.7414 5.99587C30.7414 4.57324 29.5947 3.42072 28.1775 3.42054C26.7602 3.42054 25.612 4.57313 25.612 5.99587C25.612 6.94433 24.8531 7.70613 23.9083 7.70613H17.0752C16.1304 7.70613 15.3715 6.94433 15.3715 5.99587C15.3715 4.57313 14.2233 3.42054 12.806 3.42054C11.3888 3.42068 10.2422 4.57322 10.2422 5.99587C10.2421 6.94433 9.48164 7.70613 8.53679 7.70613C5.7195 7.70653 3.40762 10.01 3.40747 12.8551V15.4305C3.40779 18.2585 5.70274 20.5791 8.53679 20.5795C9.48166 20.5795 10.2422 21.3412 10.2422 22.2897C10.242 23.2381 9.48157 24 8.53679 24C3.82988 23.9996 0.000321159 20.1554 0 15.4305V12.8551C0.000137234 8.63796 3.054 5.13267 7.03573 4.42123C7.72748 1.88072 10.0559 0.000120259 12.806 0C15.5562 0 17.801 1.81275 18.5433 4.2856H22.4567C23.199 1.81277 25.4775 2.86267e-05 28.194 0Z" fill="#FEFDF9"/>
            </svg>
          </div>

          <div className={styles.login6}>
            <p className={styles.login7}>Welcome to CLIPZY</p>
          </div>
        </div>

        {error && (
          <div className={styles.error}>
            {error}
          </div>
        )}

        <button 
        type="button"
        className={styles.login8}
        onClick={handleGoogleLogin}
        disabled={isLoading}
        aria-busy={isLoading}
        >
          {isLoading ? (
            <Spinner />
          ) : (
            <>
              <div className={styles.login9}>
                <svg 
                className={styles.login10}
                xmlns="http://www.w3.org/2000/svg" width="19" height="20" viewBox="0 0 19 20" fill="none">
                  <g clip-path="url(#clip0_935_1141)">
                    <path d="M18.9899 10.1871C18.9899 9.36767 18.9253 8.76973 18.7854 8.14966H9.68921V11.848H15.0285C14.9209 12.7671 14.3396 14.1512 13.0478 15.0813L13.0297 15.2051L15.9057 17.4969L16.105 17.5174C17.935 15.7789 18.9899 13.221 18.9899 10.1871Z" fill="#4285F4"/>
                    <path d="M9.68923 19.9313C12.305 19.9313 14.501 19.0454 16.105 17.5174L13.0478 15.0813C12.2297 15.6682 11.1317 16.0779 9.68923 16.0779C7.12724 16.0779 4.95278 14.3395 4.17765 11.9366L4.06403 11.9466L1.07347 14.3273L1.03436 14.4391C2.62753 17.6945 5.90001 19.9313 9.68923 19.9313Z" fill="#34A853"/>
                    <path d="M4.17765 11.9366C3.97312 11.3165 3.85476 10.6521 3.85476 9.96559C3.85476 9.27902 3.97313 8.61467 4.16689 7.9946L4.16147 7.86253L1.13344 5.4436L1.03437 5.49208C0.377746 6.84299 0.000976562 8.36002 0.000976562 9.96559C0.000976562 11.5712 0.377746 13.0881 1.03437 14.439L4.17765 11.9366Z" fill="#FBBC05"/>
                    <path d="M9.68923 3.85336C11.5084 3.85336 12.7356 4.66168 13.4353 5.33718L16.1696 2.59107C14.4903 0.985496 12.305 0 9.68923 0C5.90001 0 2.62753 2.23672 1.03436 5.49214L4.16689 7.99466C4.95278 5.59183 7.12724 3.85336 9.68923 3.85336Z" fill="#EB4335"/>
                  </g>
                  <defs>
                    <clipPath id="clip0_935_1141">
                      <rect width="19" height="20" fill="white"/>
                    </clipPath>
                  </defs>
                </svg>
                <div className={styles.login11}>
                  <p className={styles.login12}>Google</p>
                  <span className={styles.login13}>로 계속하기</span>
                </div>
              </div>
            </>
          )}
        </button>
      </div>
    </div>
  );
}