/* global chrome */

import { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
// import { useAuth } from '../contexts/AuthContext';
import { useAuth } from '../hooks/useAuth';
import { handleApiError } from '../utils/errorHandler';
// import { apiFetch } from '../utils/api';


// import { forwardRef } from 'react';

// import inputStyles from './InputBox.module.css';

import buttonStyles from './ButtonBox.module.css';
















// const authContext = createContext(null);









// //  forwardRef - ref 전달용
// // 부모가 자식 컴포넌트의 실제 DOM에 접근하고 싶을 때 사용
// // 일반 컴포넌트는 ref를 prop으로 못 받아서 forwardRef로 감싸야함
// // 예: 로그인 실패 시 이메일 input에 자동 focus
// const InputBox = forwardRef(
//   // props 구조분해 할당
//   // label:	라벨 텍스트 ("이메일")
//   // error:	에러 메시지
//   // fullWidth:	가로 100% 여부
//   // className:	추가 CSS 클래스
//   // id:	input의 id 속성
//   // ...props: type, value, onChange, placeholder, name, required 등 input 기본 속성
//   ({ label, error, fullWidth = false, className = '', id, ...props }, ref) => {
//     // inputId 자동 생성
//     // <label htmlFor>와 <input id>를 연결하려면 id가 필요
//     // id를 안 줬으면 name을 대신 사용. (label 클릭 시 input 포커스되는 효과)
//     const inputId = id || props.name;

//     return (
//       // 항상 wrapper 클래스 적용, fullWidth={true}면 fullWidth 클래스 추가
//       <div className={`${inputStyles.wrapper} ${fullWidth ? inputStyles.fullWidth : ''}`}>
//         {/* label prop이 있을 때만 <label> 렌더링 */}
//         {label && (
//           <label htmlFor={inputId} className={inputStyles.label}>
//             {label}
//             {/* required={true}면 빨간 별표 * 추가 */}
//             {props.required && <span className={inputStyles.required}>*</span>}
//           </label>
//         )}
//         <input
//           // ref 연결
//           ref={ref}
//           // label과 연결
//           id={inputId}
//           // error가 있으면 빨간 테두리 클래스 추가
//           className={`${inputStyles.input} ${error ? inputStyles.error : ''} ${className}`}
//           // 나머지 속성 자동 적용
//           {...props}
//         />
//         {/* 에러가 있을 때만 빨간 에러 메시지 표시 */}
//         {error && <span className={inputStyles.errorMessage}>{error}</span>}
//       </div>
//     );
//   }
// );

// // React DevTools에서 컴포넌트 이름이 ForwardRef가 아닌 InputBox로 표시
// InputBox.displayName = 'InputBox';



function ButtonBox({
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
    buttonStyles.button,
    buttonStyles[variant],
    buttonStyles[size],
    fullWidth ? buttonStyles.fullWidth : '',
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
      {isLoading ? (
        <span className={buttonStyles.loading}>
          <span 
          className={buttonStyles.spinner} 
          // 스피너는 장식이라 읽지 말라고 하기
          aria-hidden="true" />
          로딩 중...
        </span>
      ) : (
        // 버튼 안에 들어갈 기본 내용
        children
      )}
    </button>
  );
}

ButtonBox.displayName = 'ButtonBox';




export default function LoginPage() {
  // 코드로 페이지 이동
  const navigate = useNavigate();
  // 현재 경로 정보 가져오기
  const location = useLocation();
  const { loginWithGoogle } = useAuth();

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

  // navigate로 전달한 데이터의 
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

    // 백엔드로 전송
    await loginWithGoogle(idToken);
    navigate(from, { replace: true });
  } catch (error) {
    // setError(SAFE_LOGIN_ERRORS[error.code] || '로그인에 실패했습니다');
    setError(handleApiError(error, SAFE_LOGIN_ERRORS) || '로그인에 실패했습니다');
  } finally {
    setIsLoading(false);
  }


    // try {
    //   // Chrome Identity API로 idToken 받기
    //   const idToken = await new Promise((resolve, reject) => {
    //     chrome.identity.getAuthToken({ interactive: true }, (token) => {
    //       if (chrome.runtime.lastError) {
    //         reject(chrome.runtime.lastError);
    //       } else {
    //         resolve(token);
    //       }
    //     });
    //   });

    //   await loginWithGoogle(idToken);
    //   navigate(from, { replace: true });
    // } catch (error) {
    //   setError(error.message || '로그인에 실패했습니다');
    // } finally {
    //   setIsLoading(false);
    // }
  };


  // const handleSubmit = async (FormEvent) => {
  //   FormEvent.preventDefault();
  //   setApiError('');

  //   if (!validate()) return;

  //   try {
  //     setIsLoading(true);
  //     await login({ email, password });
  //     navigate(from, { replace: true });
  //   } catch (err) {
  //     setApiError(err instanceof Error ? err.message : '로그인에 실패했습니다');
  //   } finally {
  //     setIsLoading(false);
  //   }
  // };


function decodeJwtPayload(token) {
  // payload 부분
  const base64Url = token.split('.')[1];
  
  // Base64URL → Base64 변환
  const base64 = base64Url
    // - → +
    .replace(/-/g, '+')
    // _ → /
    .replace(/_/g, '/');

  // 패딩 추가 (4의 배수로 맞추기)
  // 부족한 만큼 = 추가
  const padded = base64 + '='.repeat((4 - base64.length % 4) % 4);

  
  // 디코딩
  return JSON.parse(atob(padded));
}








  // const [email, setEmail] = useState('');
  // const [password, setPassword] = useState('');


  // const [apiError, setApiError] = useState('');





  // const validate = () => {
  //   const newErrors = {};

  //   if (!email.trim()) {
  //     newErrors.email = '이메일을 입력해주세요';
  //   } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
  //     newErrors.email = '올바른 이메일 형식이 아닙니다';
  //   }

  //   if (!password) {
  //     newErrors.password = '비밀번호를 입력해주세요';
  //   }

  //   setErrors(newErrors);
  //   // 에러 없으면 true, 에러 있으면 false
  //   return Object.keys(newErrors).length === 0;
  // };





  return (
    <div style={{
      display: 'flex',
      justifyContent: 'center',
      alignItems: 'center',
      minHeight: 'calc(100vh - 300px)',
      padding: '2rem 1rem',
    }}>

      <div style={{
        width: '100%',
        maxWidth: '400px',
        backgroundColor: 'white',
        borderRadius: '12px',
        padding: '2rem',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
      }}>

        <h1 style={{
          fontSize: '1.5rem',
          textAlign: 'center',
          marginBottom: '2rem',
        }}>
          로그인
        </h1>

        {/* {apiError && 
        <div style={{
          padding: '0.75rem 1rem',
          backgroundColor: '#fef2f2',
          border: '1px solid #fecaca',
          borderRadius: '8px',
          color: '#dc2626',
          fontSize: '0.875rem',
          marginBottom: '1rem',
        }}>
          {apiError}
        </div>} */}
{/* 
        <form 
        onSubmit={handleSubmit}
        style={{
          display: 'flex',
          flexDirection: 'column',
          gap: '1.25rem',
        }}>
          <InputBox
            type="email"
            label="이메일"
            name="email"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            placeholder="example@email.com"
            error={errors?.email}
            fullWidth
          />

          <InputBox
            type="password"
            label="비밀번호"
            name="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="비밀번호를 입력하세요"
            error={errors.password}
            fullWidth
          />

          <ButtonBox type="submit" fullWidth isLoading={isLoading}>
            로그인
          </ButtonBox>
        </form> */}

        {error && (
          <div style={{
            padding: '0.75rem',
            backgroundColor: '#fef2f2',
            border: '1px solid #fecaca',
            borderRadius: '8px',
            color: '#dc2626',
            fontSize: '0.875rem',
            marginBottom: '1rem',
          }}>
            {error}
          </div>
        )}

        <ButtonBox 
          onClick={handleGoogleLogin}
          fullWidth 
          isLoading={isLoading}
        >
          Google로 로그인
        </ButtonBox>

        {/* <p style={{
          color: '#3b82f6',
          fontWeight: '500',
        }}>
          계정이 없으신가요? <Link to="/signup">회원가입</Link>
        </p>

        <div style={{
          marginTop: '1.5rem',
          padding: '1rem',
          backgroundColor: '#f3f4f6',
          borderRadius: '8px',
          textAlign: 'center',
        }}>

          <p style={{
            fontSize: '0.75rem',
            color: '#6b7280',
            marginBottom: '0.5rem',
          }}>
            테스트 계정
          </p>

          <code style={{
            fontSize: '0.8125rem',
            color: '#374151',
            backgroundColor: 'white',
            padding: '0.25rem 0.5rem',
            borderRadius: '4px',
          }}>
            test@example.com / password123
          </code> */}

        </div>
      </div>
    // </div>
  );
}