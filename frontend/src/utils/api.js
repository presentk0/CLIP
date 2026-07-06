/* global chrome */
import { log } from "./logger";
// 커스텀 에러 클래스
class ApiError extends Error {
  constructor(message, code, status) {
    // 부모(Error)한테 message 전달
    super(message);
    // code도 같이 저장
    this.code = code;
    // 상태코드 저장
    this.status = status;
    this.name = 'ApiError';
  }
}



// // 토큰 검증 헬퍼
// function checkAuth(headers) {
//   const authHeader = headers?.['Authorization'];
  
//   if (!authHeader?.startsWith('Bearer ')) {
//     return {
//       success: false,
//       error: {
//         code: "UNAUTHORIZED",
//         message: "인증 정보가 유효하지 않습니다. 다시 로그인해주세요."
//       }
//     };
//   }
  
//   return null;  // 검증 통과
// }



// // body를 객체로 변환하는 헬퍼
// // 진짜 서버는 fetch 요청 시 body를 문자열로 받아야하기에 AuthContext에서 보낼 때 JSON.stringify로 변환해야 함
// // 그래서 목업에서 받을 때 JSON.parse 변환이 필요
// function parseBody(body) {
//   if (!body) return {};

//   // instanceof로 body가 FormData 객체인지 체크
//   // null/undefined 먼저 막아야 다음에서 instanceof 호출 시 에러 안 남
//   // FormData = HTML <form> 태그가 데이터를 보내는 방식을 JavaScript 객체로 표현한 것
//   // JSON은 문자열 기반이라 이미지, 음성 같은 바이너리 데이터를 못 담음 (정확히는 base64로 인코딩하면 가능하지만 비효율적)
//   // FormData는 텍스트와 파일을 같이 묶을 수 있어서 파일 업로드의 표준
//   if (body instanceof FormData) {
//     // FormData를 일반 객체로 변환
//     // FormData는 일반 객체처럼 body.inputMode로 접근이 안 된다
//     // 대신 entries()로 키-값 쌍을 꺼낼 수 있다
//     // Object.fromEntries로 일반 객체로 변환
//     return Object.fromEntries(body.entries());
//   }

//   // 문자열 처리
//   if (typeof body === 'string') {
//     try { return JSON.parse(body); }
//     catch { return {}; }
//   }

//   // 일반 객체 그대로 반환
//   return body;
// }



// 서버 주소가 생기면 false로 바꾸고 URL을 실제 서버 주소로 변경하기
const IS_MOCK = false; 
const BASE_URL = import.meta.env.VITE_API_BASE_URL;

// 토큰 조회 헬퍼
export async function getAccessToken() {
  // 백그라운드(서비스워커) 컨텍스트인지 확인
  // chrome.runtime.sendMessage는 자기 자신(같은 컨텍스트)에겐 메시지 못 보냄
  // 백그라운드는 저장소에서 조회하게 변경
  const isServiceWorker = typeof window === 'undefined';
  
  try {
    if (isServiceWorker) {
      // 백그라운드면 storage 직접 조회
      const result = await chrome.storage.session.get(['accessToken']);
      return result.accessToken;
    } else {
      // 콘텐츠/사이드패널이면 메시지로 조회
      const auth = await chrome.runtime.sendMessage({ type: 'GET_AUTH' });
      return auth?.accessToken;
    }
  } catch (error) {
    log.debug('토큰 조회 실패', error);
    return null;
  }
}



// 토큰 저장 헬퍼
async function saveAccessToken(accessToken) {

  const isServiceWorker = typeof window === 'undefined';

  
  if (isServiceWorker) {
    await chrome.storage.session.set({ accessToken });

  } else {

    // user 정보 가져오기
    const auth = await chrome.runtime.sendMessage({ type: 'GET_AUTH' });


    await chrome.runtime.sendMessage({
      type: 'SET_AUTH',
      accessToken,
      user: auth?.user,
    });

  }

}


// 인증 정리 헬퍼
async function clearAuthLocal() {
  // 백그라운드(서비스워커) 컨텍스트인지 확인
  const isServiceWorker = typeof window === 'undefined';
  
  if (isServiceWorker) {
    
    await chrome.storage.session.remove(['accessToken', 'user']);
  } else {
    
    await chrome.runtime.sendMessage({ type: 'CLEAR_AUTH' });
  }
}



export const apiFetch = async (endpoint, options = {}) => {

  // 백그라운드에서 토큰 가져오기
  const accessToken = await getAccessToken();

  // headers에 토큰 자동 추가 (Mock/실제 공통)
  const headersWithAuth = {
    // Content-Type = 내가 지금 보내는 데이터가 어떤 형식인지 알려주는 헤더
    // Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW 는 form 데이터를 의미
    // multi(여러) + part(부분) = 여러 부분으로 나뉜 데이터 (text 부분 + 파일 부분이 한 요청 안에 여러 개 들어있다는 뜻)
    // form 데이터에는 여러 종류가 섞여 있을 수 있어서 각 데이터 사이에 구분선(boundary = ; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW)을 넣어야 함 (값 검증이 아닌 자르는 기준)
    // HTTPS 때문에 통신 자체가 암호화되어 있어서 중간에 헤더든 본문이든 변조 못함
    // boundary는 매 요청마다 다른 랜덤 문자열
    // FormData일 땐 Content-Type 자동 생략 (요청할 때 Content-Type 안 적으면 브라우저가 랜덤 boundary 생성해서 데이터를 그 boundary로 묶어줌 -> Content-Type: multipart/form-data; boundary=랜덤값 헤더 자동 추가)
    ...(options.body instanceof FormData 
    ? {} 
    : { 'Content-Type': 'application/json' }),
    ...(accessToken && { 'Authorization': `Bearer ${accessToken}` }),
    // 호출자가 명시적으로 헤더 주면 그걸로 덮어쓰기
    ...options.headers,
  };



  if (IS_MOCK) {







    // 기본 응답
    return { success: true };
  }

  // // 실제 서버 통신 로직 (나중에 사용)
  // const { accessToken } = await chrome.storage.session.get(['accessToken']);

  // 첫 호출
  // response = 서버 응답 객체 (아직 텍스트 상태)
  // fetch는 HTTP 응답이 오면 무조건 성공으로 처리해서 토큰 만료에도 멈추지 않음
  let response;
  try {
    response = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      // include의 경우 일반 웹사이트는 CSRF 공격 위험이 있다?
      // 확장프로그램과 서버 간 도메인이 다르기에 같은 도메인만 보내는 기본값인 'same-origin'는 쿠키를 보낼 수 없음
      // include를 사용해서 도메인이 달라도 항상 브라우저에서 쿠키를 보내게 함
      credentials: 'include',
      headers: headersWithAuth,
    });
  } catch (error) {
    log.error('네트워크 요청 실패', endpoint, error);
    // 이건 response 자체가 없어서 response.status 대신 0 넣기
    throw new ApiError('네트워크 오류', 'NETWORK_ERROR', 0);
  }



  // 토큰 만료 = 401 응답 또는 403 응답
  // (오류) 토큰 만료 시 HTTP 에러 /users/me 403 null, HTTP 에러 /videos/recommended 403 null 가 나타남.
  // 원인 찾는 중 일단 403도 대응하게 변경
  // endpoint !== '/auth/refresh' 있어서 리프레쉬 무한 루프 방지
  if ((response.status === 401 || response.status === 403) && endpoint !== '/auth/refresh') {
    try {
      // refresh 토큰으로 새 토큰 발급
      const refreshRes = await fetch(`${BASE_URL}/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
      });

      if (refreshRes.ok) {
        const refreshData = await refreshRes.json();
        const newAccessToken = refreshData.data.accessToken;

        // 새 토큰 저장
        await saveAccessToken(newAccessToken);

        // 원래 요청 재시도
        response = await fetch(`${BASE_URL}${endpoint}`, {
          ...options,
          credentials: 'include',
          headers: {
            ...headersWithAuth,
            Authorization: `Bearer ${newAccessToken}`,
          },
        });
      } else {
        log.warn('토큰 갱신 실패', refreshRes.status);
        throw new ApiError('인증 만료', 'UNAUTHORIZED', response.status);
      }
    } catch (error) {
      log.error('refresh 처리 중 오류', error);
      await clearAuthLocal();
      throw error;
    }
  }


  // 응답 처리
  const text = await response.text();

  // 빈 응답 처리
  let result = null;
  if (text) {
    try {
      result = JSON.parse(text);
    } catch {
      // JSON 파싱 실패
      throw new ApiError(
        `잘못된 응답 형식 (status: ${response.status})`,
        'INVALID_RESPONSE'
      );
    }
  }

  // 에러 예외
  const IGNORED_CODES = [
    "NO_RESUMABLE_CHAT_ROOM",
    "VIDEO_NOT_FOUND",
    "WORD_ALREADY_COLLECTED",
  ];

  // HTTP 에러 처리
  if (!response.ok) {

    // ai채팅방 이어하기 없는거랑 서버에 저장된 영상 없을 때 그리고 서버에 저장된 단어 있는데 전송한 경우는 경고창 없음
    if (!IGNORED_CODES.includes(result?.error?.code)) {
    // if (result?.error?.code !== "NO_RESUMABLE_CHAT_ROOM" && result?.error?.code !== "VIDEO_NOT_FOUND") {
      log.warn('HTTP 에러', endpoint, response.status, result);
    }

    throw new ApiError(
      result?.error?.message || `HTTP ${response.status} 에러`,
      result?.error?.code || `HTTP_${response.status}`,
      // 상태코드 전달
      response.status
    );
  }


  // success 필드 체크
  if (result && result.success === false) {
    throw new ApiError(
      result.error?.message || 'API Error',
      result.error?.code || 'UNKNOWN'
    );
  }

  return result;
};