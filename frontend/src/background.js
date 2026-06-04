/* global chrome */
import { apiFetch } from "./utils/api";
import { log } from "./utils/logger";

// 아이콘 클릭 시 사이드 패널 열리게 설정
chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });

const YOUTUBE_ORIGINS = [
  'https://www.youtube.com',
  'https://m.youtube.com'
];



// 탭이 업데이트될 때마다 실행 (주소 이동, 새로고침 등)
chrome.tabs.onUpdated.addListener(async (tabId, info, tab) => {
  if (!tab.url) return;
  const url = new URL(tab.url);

  // 현재 주소가 유튜브라면 사이드 패널 활성화
  if (YOUTUBE_ORIGINS.includes(url.origin)) {
    await chrome.sidePanel.setOptions({
      tabId: tab.id,
      // 사이드 패널 화면 연결
      path: 'sidepanel.html',
      // 활성화
      enabled: true
    });
  } else {
    // 유튜브가 아니면 사이드 패널 비활성화
    await chrome.sidePanel.setOptions({
      tabId,
      // 비활성화
      enabled: false
    });
  }
});



// 사이드패널 연결 감지
chrome.runtime.onConnect.addListener((port) => {
  if (port.name === 'sidepanel') {
    // 연결 끊김 감지
    port.onDisconnect.addListener(() => {
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        if (tabs[0]) {
          console.log('백그라운드 새로고침', Date.now());
          chrome.tabs.sendMessage(tabs[0].id, { type: 'PANEL_CLOSED' }).catch(() => {});
        }
      });
    });
  }
});






// ============ 에러 처리 ============
// 에러 메시지 변환
const SAFE_ERROR_MESSAGES = {
  AUTH_FAILED: '인증에 실패했습니다',
  NETWORK_ERROR: '네트워크 오류가 발생했습니다',
  INVALID_DATA: '잘못된 요청입니다',
  UNAUTHORIZED: '권한이 없습니다',
  UNKNOWN: '알 수 없는 오류가 발생했습니다',
};

function sanitizeError(error) {
  // 알려진 에러 코드만 노출
  if (error.code && SAFE_ERROR_MESSAGES[error.code]) {
    return SAFE_ERROR_MESSAGES[error.code];
  }
  return SAFE_ERROR_MESSAGES.UNKNOWN;
}




// 발신자 검증
function isValidSender(sender) {
  // 같은 확장프로그램에서 온 메시지인지
  if (sender.id !== chrome.runtime.id) {
    return false;
  }

  // 특정 origin만 허용
  const ALLOWED_ORIGINS = [
    `chrome-extension://${chrome.runtime.id}`,
    'https://www.youtube.com',
    'https://youtube.com',
  ];

  if (sender.origin && !ALLOWED_ORIGINS.includes(sender.origin)) {
    return false;
  }

  return true;
}



// 토큰 검증 (JWT 형식)
function validateAccessToken(token) {
  // 기본 타입 검증
  if (!token || typeof token !== 'string') return false;

  // 화이트리스트: base64url + 점(.)만 허용
  // → 제어 문자, 공백, HTML 태그 등 자동 차단
  // [] 밖에서는 점(.)이 특별한 의미, [] 안에서는 \.이나 .이나 똑같음, [] 안의 \. 는 불필요한 이스케이프 -> ESLint 경고
  // 하이픈을 맨 끝에 두면 이스케이프 필요없음
  if (!/^[A-Za-z0-9_.-]+$/.test(token)) return false;

  // JWT 형식
  if (token.split('.').length !== 3) return false;

  // JWT 시작 패턴 (JWT 헤더 부분은 항상 {로 시작하고 이걸 base64로 인코딩하면 eyJ 이 나옴 )
  if (!token.startsWith('eyJ')) return false;

  // DoS 방지
  // 길이로 유추 못 하게 정확한 범위 설정 금지
  if (token.length > 32000) return false;
  return true;
}



// 사용자 정보 검증 (느슨하게 - O'Brien 같은 이름 허용)
// user 검증 (SET_AUTH)
function validateUser(user) {
  if (!user || typeof user !== 'object') return false;

  // 필수 필드
  if (typeof user.id !== 'number') return false;
  if (typeof user.email !== 'string') return false;
  if (typeof user.name !== 'string') return false;

  // 이메일 형식
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(user.email)) return false;

  // 이름 (HTML 태그만 차단, 따옴표 허용)
  if (user.name.length === 0 || user.name.length > 100) return false;
  if (/[<>]/.test(user.name)) return false;

  // 전체 크기 제한
  if (JSON.stringify(user).length > 5000) return false;

  return true;
}



// 번역 endpoint 화이트리스트
const ALLOWED_ENDPOINTS = [
  '/translate',
  '/translate/subtitles',
];



// endpoint 검증 (TRANSLATE)
function validateEndpoint(endpoint) {
  if (typeof endpoint !== 'string') return false;

  // 절대 URL 차단
  if (endpoint.startsWith('http://') || endpoint.startsWith('https://')) {
    return false;
  }

  // 화이트리스트 검사
  return ALLOWED_ENDPOINTS.some(allowed => 
    endpoint === allowed || endpoint.startsWith(allowed + '?')
  );
}



// ============ 인증 헬퍼 ============
async function getAuth() {

  // return await chrome.storage.session.get(['accessToken', 'user']);
  const result = await chrome.storage.session.get(['accessToken', 'user']);
  // log.debug('백그라운드 GET_AUTH:', JSON.stringify(result, null, 2), Date.now());
  return result;
}

async function setAuth({ accessToken, user }) {
  await chrome.storage.session.set({ accessToken, user });
}

async function clearAuth() {
  await chrome.storage.session.remove(['accessToken', 'user']);
}



// ============ 메시지 핸들러 ============
// 번역 요청 처리
const handleTranslate = (message, sendResponse) => {
  // log.debug('=== TRANSLATE 받음 ===', Date.now());
  // log.debug('endpoint:', message.endpoint, Date.now());
  // log.debug('body 타입:', typeof message.options?.body, Date.now());
  // log.debug('body 길이:', message.options?.body?.length, Date.now());

  // endpoint 검증
  if (!validateEndpoint(message.endpoint)) {
    log.debug('endpoint 검증 실패', Date.now());
    sendResponse({ success: false, error: SAFE_ERROR_MESSAGES.INVALID_DATA });
    return;
  }

  const MAX_RETRY = 3;
  const RETRY_DELAY = 2000;
  const fetchTranslate = async (retryCount = 0) => {
    try {
      log.debug(`apiFetch 시도 ${retryCount + 1}`, Date.now());
      const translateData = await apiFetch(message.endpoint, message.options);
      // log.debug('apiFetch 성공:', translateData, Date.now());
      sendResponse(translateData);
    } catch (error) {
      console.error('번역 apiFetch 실패:', error.message, error.code,Date.now());



      if (retryCount < MAX_RETRY) {
        log.debug('재시도 예정', Date.now());
        setTimeout(() => {
          fetchTranslate(retryCount + 1);
        }, RETRY_DELAY);
      } else {
        log.debug('최대 재시도 초과', Date.now());
        sendResponse({ success: false, error: sanitizeError(error) });
      }
    }
  };

  fetchTranslate();
};



// 인증 정보 조회
const handleGetAuth = async (sendResponse) => {
  try {
    const auth = await getAuth();
    sendResponse(auth);
  } catch (error) {
    log.error('인증 조회 실패', error);
    sendResponse({ success: false, error: sanitizeError(error) });
  }
};



// 인증 정보 저장 (로그인 시)
const handleSetAuth = async (message, sendResponse) => {
  // 토큰 검증
  if (!validateAccessToken(message.accessToken)) {
    log.error('잘못된 토큰 형식');
    sendResponse({ 
      success: false, 
      error: SAFE_ERROR_MESSAGES.INVALID_DATA 
    });
    return;
  }

  // user 검증
  if (!validateUser(message.user)) {
    log.error('잘못된 사용자 정보');
    sendResponse({ 
      success: false, 
      error: SAFE_ERROR_MESSAGES.INVALID_DATA 
    });
    return;
  }

  try {
    await setAuth(message);
    sendResponse({ success: true });
  } catch (error) {
    log.error('인증 저장 실패:', error);
    sendResponse({ success: false, error: sanitizeError(error) });
  }
};



// 인증 정보 삭제 (로그아웃 시)
const handleClearAuth = async (sendResponse) => {
  try {
    await clearAuth();
    sendResponse({ success: true });
  } catch (error) {
    log.error('인증 삭제 실패:', error);
    sendResponse({ success: false, error: sanitizeError(error) });
  }
};




// ============ 메시지 라우터 ============
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  // 메시지 검증
  if (!message?.type) return;

  // 내가 처리하는 타입 목록
  const HANDLED_TYPES = [
    'TRANSLATE',
    'SET_AUTH',
    'GET_AUTH',
    'CLEAR_AUTH',
    // 'PAGE_RELOADED',
  ];

  // 백그라운드용 메시지 아니면 리턴
  if (!HANDLED_TYPES.includes(message.type)) {
    return;
  }

  // 발신자 검증
  if (!isValidSender(sender)) {
    log.error('차단된 발신자:', sender);
    sendResponse({ success: false, error: SAFE_ERROR_MESSAGES.UNAUTHORIZED });
    return false;
  }

  switch (message.type) {
    case 'TRANSLATE':
      handleTranslate(message, sendResponse);
      return true;

    case 'SET_AUTH':
      handleSetAuth(message, sendResponse);
      return true;

    case 'GET_AUTH':
      handleGetAuth(sendResponse);
      return true;

    case 'CLEAR_AUTH':
      handleClearAuth(sendResponse);
      return true;

    // case 'PAGE_RELOADED':
    //   handlePageReloaded();
    //   return false;

    default:
      return false;
  }
});











// 저장소 변경 감지 리스너 등록
chrome.storage.onChanged.addListener((changes, area) => {
  // 변경된 저장소가 session이 아니면 리턴
  if (area !== 'session') return;

  // 이번 변경에 토큰 또는 유저 정보가 포함되어야만 반응
  if (!changes.accessToken && !changes.user) return;
  
  // 모든 유튜브 탭에 알림
  chrome.tabs.query({ url: '*://*.youtube.com/*' }, (tabs) => {
    tabs.forEach(tab => {
      chrome.tabs.sendMessage(tab.id, { type: 'AUTH_CHANGED' })
        .catch(() => {});
    });
  });
});