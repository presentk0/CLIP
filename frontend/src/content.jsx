/* global chrome */
import { apiFetch } from "./utils/api";
import { log } from "./utils/logger";
import { saveUserData, loadUserData } from "./utils/userStorage";

// ========== 설정값 ==========
// 대기열이 많이 쌓이면 오래된 순서로 버리기는 기준 (10개 이상이면 버리기)
const MAX_QUEUE = 10;
// 영상 페이지에서 영상 가져오기 (실패 시 최대 15번까지 재시도)
const MAX_RETRY = 15;


// ========== 영상 정보 ==========
// 현재 영상Id 저장
let currentVideoId = null;
// 현재 영상 제목 저장
let currentVideoTitle = '';
// 현재 영상 전체 길이 저장
let currentDuration = 0;
// 영상 중복 전송 방지용
let lastVideoId = '';
// Video가 서버에 저장됐는지 여부
let isVideoSaved = false;
// 현재 영상 채널명 저장
let currentChannelName = null;
// 현재 영상 썸네일 url 저장
let currentThumbnailUrl = null;

// ========== 자막 관련 ==========
// 전체 자막 데이터 저장
let allSubtitles = [];
// 마지막에 전송한 자막 인덱스
let lastSentIndex = -1;
// 인터셉터 중복 방지
let isSubtitleInterceptorSet = false;
// 이미 자막 가져온 영상 ID
let translatedVideoId = null;


// ========== 전송 대기열 ==========
// 서버로 보낼 자막 데이터들 모아두는 박스
let textQueue = [];
// 대기열의 현재 처리 위치 (줄의 맨 앞)
let head = 0;
// 현재 대기열을 처리 중인지 확인용 (중복 실행 방지)
let isProcessing = false;


// ========== 퀴즈 관련 ==========
// 퀴즈 페이지 이동 여부
let isQuiz = false;
// 퀴즈 요청 가능한지 체크
let isQuizMode = false;
// 퀴즈 중복 요청 방지
let quizRequested = false;
// 현재 요청한 퀴즈 섹션 수
let quizSectionCount = 1;
// 현재 요청한 매칭 퀴즈 수
let matchingQuizCount = 0;
// 마지막 퀴즈가 나온 시점의 시청 시간
let lastQuizTime = 0;


// ========== 구간 반복 ==========
// 구간 반복 시작 시간
let loopStart = null;
// 구간 반복 종료 시간
let loopEnd = null;
// 구간 반복 이벤트 리스너 강제 종료용
let eventLoop = null;


// ========== 시청 시간 체크 ==========
// 총 시청 시간 (초)
let totalWatchTime = 0;
// 마지막 시청 시간 체크 시점
let lastWatchTime = 0;
// 시청 시간 감지 및 영상 끝남 감지 리스너 중복 방지
let isTimeUpdateListenerSet = false;
// 시청 시간 및 영상 끝남 감지 및 영상 자막 감지 이벤트 리스너 강제 종료용
let eventTimeUpdateListener = null;


// ========== UI 상태 ==========
// 패널 열고 닫기 상태
let isPanelOpen = false;


// ========== 옵저버 & 타이머 ==========
// // 홈 & 검색 화면에서 새로운 영상 썸네일이 나타나는지 감시용 (배지 부착용)
// let thumbnailObserver = null;
// 페이지 이동 감지 옵저버 중복 방지
let titleObserver = null;
// 광고 체크 옵저버
let adObserver = null;


// ========== 재시도 카운트 ==========
// 영상 요소 찾기 재시도 횟수
let videoRetryCount = 0;
// 영상 정보 찾기 재시도 횟수
let infoRetryCount = 0;


// ========== URL 감지 ==========
// 유튜브 SPA 대응 URL 변경 감지
let lastUrl = '';


// ========== 로그인 + 온보딩 완료여부 감지 ==========
let isActive = false;


// 사이드 패널 신호 수신
chrome.runtime.onMessage.addListener((message) => {
  // ========== 패널 열림 ==========
  if (message.type === 'PANEL_OPENED') {
    // 패널 열림 상태 저장
    isPanelOpen = true;
    // 패널 열리면 모든 시스템 리셋 및 시작
    init();
  }

   // ========== 패널 닫힘 ==========
  if (message.type === 'PANEL_CLOSED') {
    // 패널 닫힘 상태 저장
    isPanelOpen = false;

    // ========== 리스너/자막 리셋 ==========
    resetSubtitleSystem();

    // ========== 퀴즈 상태 리셋 ==========
    // 퀴즈 페이지 이동 여부
    isQuiz = false;
    // 섹션 퀴즈 출제 횟수
    quizSectionCount = 1;
    // 매칭 퀴즈 출제 횟수
    matchingQuizCount = 0;
    // 퀴즈 요청 중 여부
    quizRequested = false;
    // 마지막 퀴즈 요청한 영상 ID
    lastVideoId = '';
    // 퀴즈 요청 가능 여부
    isQuizMode = false;

    // ========== 영상 상태 리셋 ==========
    // 번역 완료된 영상 ID 리셋
    translatedVideoId = null;
    // 이미 자막 가로챘는지 여부 리셋
    isSubtitleInterceptorSet = false;
    // 전체 자막 배열 리셋
    allSubtitles = [];
    // 마지막 전송한 자막 인덱스 리셋
    lastSentIndex = -1;
    // 현재 영상Id 리셋
    currentVideoId = null;
    // 현재 영상 제목 리셋
    currentVideoTitle = '';
    // 현재 영상 전체 길이 리셋
    currentDuration = 0;
    // Video가 서버에 저장됐는지 여부 리셋
    isVideoSaved = false;
    // 현재 영상 채널명 리셋
    currentChannelName = null;
    // 현재 영상 썸네일 url 리셋
    currentThumbnailUrl = null;
    // ========== 큐/재시도 리셋 ==========
    // 자막 전송 대기열 초기화
    resetQueue();
    // 영상 요소 찾기 재시도 횟수 리셋
    videoRetryCount = 0;
    // 영상 정보 찾기 재시도 횟수 리셋
    infoRetryCount = 0;

    // ========== 로그인 + 온보딩 완료체크 리셋 ==========
    isActive = false;

    // ========== 페이지 감지 옵저버 리셋 ==========
    if (titleObserver) {
      titleObserver.disconnect();
      titleObserver = null;
    }

    // ========== UI 복원 ==========
    // // 썸네일 뱃지 제거
    // removeBadges();
    // 추천 영상 다시 보여주기
    showRecommendations();

    // ========== 광고 끝남 감지 옵저버 리셋 ==========
    cleanupAdObserver()
  }

  // ========== 퀴즈 시작 ==========
  if (message.type === 'SET_QUIZ_MODE') {
    const video = document.querySelector('video');
    // 영상 일시정지
    if (video) video.pause();
  }

  // ========== 퀴즈 완료 ==========
  if (message.type === 'RESUME_VIDEO') {
    const video = document.querySelector('video');
    // 영상 재생
    if (video) video.play();

    // ========== 다음 퀴즈 요청 가능하도록 리셋 ==========
    // 퀴즈 중복 방지용 ID 초기화
    lastVideoId = '';
    // 퀴즈 요청 가능
    quizRequested = false;
    // 퀴즈 모드 활성화
    isQuizMode = true;
  }

  // ========== 구간 반복 ==========
  if (message.type === 'START_LOOP' || message.type === 'STOP_LOOP') {
    const video = document.querySelector('video');
    if (!video) return;

    // ========== 구간 반복 시작 ==========
    if (message.type === 'START_LOOP') {
      // 현재 영상 플레이어 객체 저장
      eventLoop = video;
      // 반복 시작 시간 저장
      loopStart = message.startTime;
      // 반복 종료 시간 저장
      loopEnd = message.endTime;
      // 반복 시작 지점으로 이동
      video.currentTime = loopStart;
      // 영상 재생
      video.play();
      // 반복 리스너 제거 후 등록
      video.removeEventListener('timeupdate', handleLoop);
      video.addEventListener('timeupdate', handleLoop);
    }

    // ========== 구간 반복 종료 ==========
    if (message.type === 'STOP_LOOP') {
      // 시작 시간 초기화
      loopStart = null;
      // 종료 시간 초기화
      loopEnd = null;
      // 일시정지
      video.pause();
      // 반복 리스너 제거
      video.removeEventListener('timeupdate', handleLoop);
      // 저장한 영상 영상 플레이어 객체 리셋
      eventLoop = null;
    }
  }
});



// 상태 리셋 함수
function resetAllState() {
  // ========== 영상 상태 리셋 =========
  // 마지막 처리한 영상 ID 리셋
  lastVideoId = '';
  // 번역 완료된 영상 ID 리셋
  translatedVideoId = null;
  // Video가 서버에 저장됐는지 여부 리셋
  isVideoSaved = false;

  // ========== 자막 상태 리셋 ==========
  // 이미 자막 가로챘는지 여부 리셋
  isSubtitleInterceptorSet = false;
  // 전체 자막 배열
  allSubtitles = [];
  // 마지막에 전송한 자막 인덱스
  lastSentIndex = -1;

  // ========== 퀴즈 상태 리셋 ==========
  // 퀴즈 요청 중인지 여부
  quizRequested = false;
  // 퀴즈 페이지로 이동했는지 여부
  isQuiz = false;
  // 퀴즈 요청 가능한지 여부 리셋
  isQuizMode = false;
  // 현재 퀴즈 섹션 수 리셋
  quizSectionCount = 1;
  // 매칭 퀴즈 요청 횟수 리셋
  matchingQuizCount = 0;

  // ========== 시청 시간 리셋 ==========
  // 실제로 시청한 누적 시간 리셋
  totalWatchTime = 0;
  // 마지막으로 체크한 영상 시점 리셋
  lastWatchTime = 0;
  // 마지막 퀴즈가 나온 시점의 시청 시간 리셋
  lastQuizTime = 0;

  // ========== 로그인 + 온보딩 완료체크 리셋 ==========
  isActive = false;

  // ========== 큐/리스너 리셋 ==========
  // 자막 전송 대기열 초기화
  resetQueue();
  // 이벤트 리스너 제거
  resetSubtitleSystem();

  // ========== 광고 끝남 체크 옵저버 리셋 ==========
  cleanupAdObserver()
}



// 영상 바뀔 때 대기열 리셋
function resetQueue() {
  // 자막 전송 대기열 비우기
  textQueue = [];
  // 대기열 처리 위치 초기화
  head = 0;
  // 처리 중 상태 해제
  isProcessing = false;
}



// 이벤트 리스너/자막 리셋
function resetSubtitleSystem() {
  // ========== 자막 데이터 리셋 ==========
  // 전체 자막 배열 리셋
  allSubtitles = [];
  // 마지막에 전송한 자막 인덱스 리셋
  lastSentIndex = -1;

  // ========== 영상 정보 리셋 ==========
  // 현재 영상 Id 리셋
  currentVideoId = null;
  // 현재 영상 제목 리셋
  currentVideoTitle = '';
  // 현재 영상 전체 길이 리셋
  currentDuration = 0;
  // 현재 영상 채널명 리셋
  currentChannelName = null;
  // 현재 영상 썸네일 url 리셋
  currentThumbnailUrl = null;
  // ========== 리스너 상태 리셋 ==========
  // 시청시간, 자막동기화 리스너 중복 등록 감지 리셋
  isTimeUpdateListenerSet = false;

  // ========== 시청/자막 이벤트 리스너 제거 ==========
  if (eventTimeUpdateListener) {
    // 영상 시청 및 영상 끝 감지 이벤트 리스너 강제 종료
    eventTimeUpdateListener.removeEventListener('timeupdate', handleTimeUpdate);
    // 자막 감지 이벤트 리스너 강제 종료
    eventTimeUpdateListener.removeEventListener('timeupdate', handleSubtitleSync);
    eventTimeUpdateListener = null;
  }

  // ========== CustomEvent 리스너 제거 ==========
  // 자막 가져오는 이벤트 리스너 강제 종료
  document.removeEventListener('CLIPZY_SUBTITLE_DATA', subtitleDataHandler);

  // ========== 구간 반복 리스너 제거 ==========
  // 구간 반복 이벤트 리스너 강제 종료
  if (eventLoop) {
    eventLoop.removeEventListener('timeupdate', handleLoop);
    eventLoop = null;
  }
  loopStart = null;
  loopEnd = null;
}



// 토큰 + 유저 정보 체크 함수
async function checkAccess() {

  const cached = await chrome.runtime.sendMessage({ type: 'GET_AUTH' });


  if (!cached?.accessToken || !cached?.user) {

    return false;
  }

  // 온보딩 완료 시 기능 활성화
  return cached.user.needsOnboarding === false;
}



// 로그인 + 온보딩 설정 완료 시 활성화
async function activate() {

  if (isActive) return;
  isActive = true;

  // 현재 페이지 종류에 따라 다른 처리
  handlePageChange();

  // YouTube SPA 대응 - 페이지 이동 감지
  setupTitleObserver();


  // setupTitleObserver 실행 시 URL이 바뀌었을 때만 실행하게 설정
  lastUrl = location.href;

}



// 비활성화
function deactivate() {

  if (!isActive) return;

  isActive = false;

  // 패널 열림 처리
  chrome.runtime.sendMessage({ type: 'CONTENT_LOADED' });
  // 이전 영상의 자막, 퀴즈, 시청시간 등의 상태를 리셋
  resetAllState();
}



// 저장소 변경 감지
const handleAuthChanged = async (msg) => {

  if (msg.type !== 'AUTH_CHANGED') return;


  // 토큰/유저 정보가 바뀐 뒤 현재 실행 가능한지 다시 검사
  const canRun = await checkAccess();

  if (canRun && !isActive) {

    activate();
  } else if (!canRun && isActive) {

    deactivate();
  }
};


function listenForChanges() {
  chrome.runtime.onMessage.removeListener(handleAuthChanged);
  chrome.runtime.onMessage.addListener(handleAuthChanged);
}










// YouTube SPA 대응 - 페이지 이동 감지 옵저버 설치
function setupTitleObserver() {
  const title = document.querySelector('title');

  
  if (!title) {
    setTimeout(setupTitleObserver, 100);
    return;
  }

  // 기존 옵저버 제거
  if (titleObserver) {
    titleObserver.disconnect();
    titleObserver = null;
  }

  titleObserver = new MutationObserver(() => {
    //  URL 바뀌면 실행
    //  location.href = 현재 페이지의 전체 URL
    if (location.href !== lastUrl) {

      // 광고 중이면 무시
      if (document.querySelector('.ad-showing')) {
        return;
      }

      lastUrl = location.href;
      
      chrome.runtime.sendMessage({ type: 'CONTENT_LOADED' });
      // 상태 리셋 + 페이지 구분 및 이동
      resetAllState();
      handlePageChange();
    }
  });

  titleObserver.observe(title, { childList: true });
}













// 페이지 구분 및 이동
async function handlePageChange() {

  if (isPanelOpen) {

    // 영상 페이지면 추천 영상 숨기고 자막 감지
    if (location.href.includes('/watch')) {
      setTimeout(hideRecommendations, 500);
      setTimeout(observeSubtitles, 2000);

      // 퀴즈 진행 복원 (비동기라서 기다리기)
      const videoId = new URL(window.location.href).searchParams.get('v');


      if (videoId) {

        // const result = await chrome.storage.local.get(`quizProgress_${videoId}`);
        // const progress = result[`quizProgress_${videoId}`];
        const progress = await loadUserData(`quizProgress_${videoId}`);

        if (progress) {

          quizSectionCount = progress.sectionCount || 1;
          matchingQuizCount = progress.matchingCount || 0;
        }
      }
    } else {
      // 영상 페이지 아니면 디폴트 페이지로 이동 및 추천 영상 보이고 썸네일 감지
      chrome.runtime.sendMessage({
        type: 'GO_TO_DEFAULT'
      });
      showRecommendations();
      // setTimeout(observeThumbnails, 3000);
    }
  }
}



// 추천영상 숨기기
const hideRecommendations = () => {

  // 사이드 패널 열려야 실행
  if (!isPanelOpen) return;

  // 기존 스타일 태그 있는지 확인
  let styleTag = document.querySelector('#clip-hide-recommendations');

  // 기존 스타일 태그가 없으면 새로 만들기
  // ytd-watch-flexy = 영상 페이지 컨테이너
  // 그 안의 #secondary만 숨김
  // 메인 페이지의 #secondary는 영향 없음
  if (!styleTag) {

    // <style> 태그 생성
    styleTag = document.createElement('style');
    // 나중에 찾기/삭제 위해 ID 부여
    styleTag.id = 'clip-hide-recommendations';
    // CSS 규칙 작성 (각각 데스크톱 버전, 모바일 버전)
    styleTag.textContent = `
      ytd-watch-flexy #secondary {
        display: none !important;
      }

      ytd-watch-flexy #primary,
      ytd-watch-flexy #player,
      ytd-watch-flexy #player-container,
      ytd-watch-flexy .html5-video-player,
      ytd-watch-flexy video {
      max-width: 100% !important;
      width: 100% !important;
      }


      ytm-watch .related-items-container {
        display: none !important;
      }

      ytm-watch .player-container,
      ytm-watch #player,
      ytm-watch .html5-video-player,
      ytm-watch video {
        max-width: 100% !important;
        width: 100% !important;
      }
    `;

    // <head>에 추가
    document.head.appendChild(styleTag);
  }

};



// 추천영상 다시 보이기
const showRecommendations = () => {
  // 스타일 태그 찾기
  const styleTag = document.querySelector('#clip-hide-recommendations');

  // 스타일 태그 있으면 삭제
  if (styleTag) {
    // <style> 태그 제거 -> CSS 규칙 사라짐 -> 다시 보임
    styleTag.remove();
  }
};



// 영상 시청 페이지면 자막 감지 함수 실행하고 아니면 자막 감지 중지 함수 실행
function observeSubtitles() {

  // ========== 실행 조건 체크 ==========
  // 사이드 패널 열려야 실행
  if (!isPanelOpen) {

    showRecommendations();
    return;
  }
  // 쇼츠 차단
  if (window.location.href.includes('/shorts')) {

    showRecommendations();
    return;
  }
  // 영상 페이지가 아니면 추천 영상 표시하고 종료
  if (!window.location.href.includes('/watch')) {

    showRecommendations();
    return;
  }

  // ========== 현재 영상 요소 가져오기 ==========

  const video = document.querySelector('video');

  if (!video) {
    if (videoRetryCount < MAX_RETRY) {
      videoRetryCount++;
      // DOM이 렌더링 안 됐으면 1초 후 재시도
      setTimeout(observeSubtitles, 1000);
    }
    showRecommendations();
    return;
  }

  // video 찾으면 재시도 횟수 리셋
  videoRetryCount = 0;

  // ========== 영상 정보 가져오기 ==========
  // URL에서 영상 ID 추출
  const videoId = new URL(window.location.href).searchParams.get('v');
  // 영상 제목
  const videoTitle = getVideoTitle();
  // 채널명
  const channelName = getChannelName();
  // 영상 길이
  const duration = getDuration();

  // 썸네일 URL (480x360)
  const thumbnailUrl = `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;



  // 제목, 채널명, 영상 길이 아직 로딩 안 됐으면 재시도
  if (!videoTitle || !channelName || !duration) {

    if (infoRetryCount < MAX_RETRY) {
      infoRetryCount++;
      setTimeout(observeSubtitles, 1000);
    } else {
      log.debug('최대 재시도 초과');
    }
    showRecommendations();
    return;
  }
  // 성공하면 재시도 횟수 리셋
  infoRetryCount = 0;

  // ========== 퀴즈 페이지로 이동 (최초 1회) ==========

  if (!isQuiz && videoId && videoTitle && channelName && duration && thumbnailUrl) {

    chrome.runtime.sendMessage({
      type: 'GO_TO_QUIZ',
      videoId: videoId,
      videoTitle: videoTitle,
      channelName: channelName || '',
      duration: duration,
      thumbnailUrl: thumbnailUrl,
    }).catch((error) => log.debug('퀴즈 페이지 이동 에러', error));
    isQuiz = true;
  }

  // ========== 영상 자막 전체 가져오는 함수 실행 ==========
  setupSubtitleInterceptor(videoId, videoTitle, duration, channelName, thumbnailUrl);
}



// 비디오 제목 가져오기
const getVideoTitle = () => {
  // 데스크톱
  const desktop = document.querySelector('#title h1')?.textContent?.trim();
  if (desktop) return desktop;
  
  // 모바일
  const mobile = document.querySelector('.slim-video-information-title')?.textContent?.trim()
    || document.querySelector('h2.slim-video-information-title')?.textContent?.trim();
  if (mobile) return mobile;
  return '';
};



// 채널명 가져오기
const getChannelName = () => {
  // 데스크톱
  const desktop = document.querySelector('ytd-channel-name yt-formatted-string')?.getAttribute('title')
    || document.querySelector('ytd-channel-name a')?.textContent?.trim()
    || document.querySelector('#channel-name a')?.textContent?.trim()
    || document.querySelector('#channel-name yt-formatted-string')?.textContent?.trim();
  if (desktop) return desktop;
  
  // 모바일
  const mobile = document.querySelector('.slim-owner-channel-name span')?.textContent?.trim()
    || document.querySelector('h3.slim-owner-channel-name span')?.textContent?.trim();
  if (mobile) return mobile;
  
  return '';
};



// 비디오 전체 길이 가져오기
const getDuration = () => {
  // JSON-LD (데스크톱)
  const jsonLd = document.querySelector('script[type="application/ld+json"]');
  if (jsonLd) {
    try {
      const data = JSON.parse(jsonLd.textContent);
      const seconds = parseISODuration(data.duration);
      if (seconds) {

        return seconds;
      }
    } catch (error) { 
      log.debug('데스크톱 비디오 전체 가져오기 실패', error);
    }
  }
  
  // 페이지 로드 시점부터 본 영상 정보가 HTML에 있음
  // HTML에서 lengthSeconds 직접 추출 (모바일)
  const html = document.documentElement.outerHTML;
  const match = html.match(/"lengthSeconds":"(\d+)"/);
  if (match) {
    const sec = parseInt(match[1]);
    if (sec > 0) {

      return sec;
    }
  }


  return null;
}



// ISO 8601 형식의 duration 문자열을 초 단위 숫자로 변환
// 예: "PT1H23M45S" → 5025초 (1시간 23분 45초)
// 유튜브의 JSON-LD는 영상 길이를 "PT4M13S" 같은 ISO 8601 duration 형식으로 제공함
function parseISODuration(iso) {
  // 입력값이 없으면 null 반환
  if (!iso) return null;

  // 정규식으로 시(H), 분(M), 초(S) 부분을 각각 추출
  // PT 뒤에 H, M, S 단위가 선택적(?)으로 올 수 있음
  // 예: "PT1H23M45S" → match[1]="1", match[2]="23", match[3]="45"
  // 예: "PT4M13S"   → match[1]=undefined, match[2]="4", match[3]="13"
  const match = iso.match(/PT(?:(\d+)H)?(?:(\d+)M)?(?:(\d+)S)?/);

  // 형식이 맞지 않으면 null 반환
  if (!match) return null;
  // 각 단위를 초로 환산하여 합산 (없는 단위는 0으로 처리)
  // 시 × 3600 + 분 × 60 + 초
  return parseInt(match[1] || 0) * 3600 + parseInt(match[2] || 0) * 60 + parseInt(match[3] || 0);
}












// XMLHttpRequest 가로채기 설정
// 유튜브가 자막을 요청할 때 응답을 엿봄
// XMLHttpRequest = 브라우저 내장 객체 (네트워크 요청용)
// XMLHttpRequest.prototype = XMLHttpRequest에서 쓸 수 있는 모든 함수들 가져오기
async function setupSubtitleInterceptor(videoId, videoTitle, duration, channelName, thumbnailUrl ) {
  // // 테스트용 번역 막기 오류
  // if (isPanelOpen) {
  //   log.debug('테스트용 번역 막기 오류', Date.now());
  //   return;
  // }

  
  
  
  
  

  // 미리 광고 감지 옵저버 제거
  cleanupAdObserver();
  // ========== 실행 조건 체크 ==========
  // 사이드 패널 닫혀있으면 무시
  if (!isPanelOpen) {
    cleanupAdObserver();
    return;
  }

  // 광고 중이면 종료 대기
  if (document.querySelector('.ad-showing')) {

    await waitForAdEnd();

    // 대기 중 패널 닫혔으면 정리하고 종료
    if (!isPanelOpen) {
      cleanupAdObserver();
      return;
    }
  }

  // 광고 끝나면 옵저버 제거
  cleanupAdObserver();

  // 최초 1회만 발동
  if (isSubtitleInterceptorSet) return;
  // 중복 실행 방지
  isSubtitleInterceptorSet = true;

  // ========== 현재 영상Id, 제목, 전체 길이 저장 ==========
  currentVideoId = videoId;
  currentVideoTitle = videoTitle;
  currentDuration = duration;
  currentChannelName = channelName;
  currentThumbnailUrl = thumbnailUrl;

  // ========== 서버에서 자막 조회 ==========
  apiFetch(`/videos/${videoId}/subtitles`, {
    method: 'GET'
  }).then(response => {

    // 이미 저장된 자막 있는지 확인
    if (response?.data?.subtitles) {
      // 이미 저장된 자막 있으면 자막 배열 저장
      allSubtitles = response.data.subtitles;
      // 번역된 영상 ID 저장
      translatedVideoId = videoId;
      // 자막 인덱스 초기화
      lastSentIndex = -1;
      // 서버 저장 완료
      isVideoSaved = true;
      // 퀴즈 요청 가능
      isQuizMode = true;
      // 시청시간/자막동기화 리스너 등록
      setupTimeUpdateListener();

      // content.js에서 전체 자막 전달
      chrome.runtime.sendMessage({
        type: 'SUBTITLES_LOADED',
        subtitles: allSubtitles
      });
    } else {
      // 서버에 없으면 유튜브에서 가져오기
      registerSubtitleListener();
    }
  }).catch(error => {
    log.debug('자막 조회 실패', error);
    // 조회 실패해도 유튜브에서 가져오기
    registerSubtitleListener();
  });
};



// 광고 끝남 감지 옵저버
function waitForAdEnd() {

  // new Promise((resolve) => {...})로 resolve 라는 종료 함수 생성
  // resolve()를 호출하면 await waitForAdEnd()가 풀리고 다음 코드 실행
  return new Promise((resolve) => {
    // 이미 광고 없으면 즉시 종료
    if (!document.querySelector('.ad-showing')) {

      resolve();
      return;
    }
    
    // 이미 감시 중이면 종료 (중복 방지)
    if (adObserver) {

      resolve();
      return;
    }
    
    const player = document.querySelector('#movie_player') || document.body;

    adObserver = new MutationObserver(() => {

      if (!document.querySelector('.ad-showing')) {

        cleanupAdObserver();
        resolve();
      }
    });

    adObserver.observe(player, {
      // HTML 요소의 속성(attribute)이 바뀌는 걸 감지
      attributes: true,
      // 속성 중에서 class 변화만 감지
      attributeFilter: ['class'],
    });
  });
}







// 광고 끝남 감지 옵저버 제거
function cleanupAdObserver() {
  if (adObserver) {
    adObserver.disconnect();
    adObserver = null;
  }
}



// 시청 시간 감지 및 영상 끝남 감지 및 자막 감지 리스너 설정
function setupTimeUpdateListener() {
  if (isTimeUpdateListenerSet) return;

  const video = document.querySelector('video');
  if (!video) return;

  eventTimeUpdateListener = video;
  // 기존 리스너 제거 후 등록
  video.removeEventListener('timeupdate', handleTimeUpdate);
  video.removeEventListener('timeupdate', handleSubtitleSync);

  video.addEventListener('timeupdate', handleTimeUpdate);
  video.addEventListener('timeupdate', handleSubtitleSync);
  isTimeUpdateListenerSet = true;
}



// 시청 시간 감지
function handleTimeUpdate(e) {
  const video = e.target;
  
  // 패널 닫혀있으면 무시
  if (!isPanelOpen) return;
  
  // 광고 중이면 무시
  if (document.querySelector('.ad-showing')) return;

  if (allSubtitles.length === 0) {
    video.removeEventListener('timeupdate', handleTimeUpdate);
    eventTimeUpdateListener = null;
    return;
  }

  // ========== 시청 시간 체크 ==========
  const diff = video.currentTime - lastWatchTime;
  // 스킵/되감기/정지 제외
  if (diff > 0 && diff < 1) {
    totalWatchTime += diff;
  }
  lastWatchTime = video.currentTime;

  // ========== 빈칸/OX 퀴즈 체크 ==========
  // n초마다
  const quizIntervalCount = getQuizCount(video.duration);
  // 최대 x번 퀴즈 요청
  const sectionCount = getSectionCount(video.duration);
  // 0초가 아니고 && n초가 지났고 && 섹션 수를 초과하지 않고, 서버에서 퀴즈 요청 허용받으면 (빈칸 / ox) 퀴즈 요청
  if (quizIntervalCount > 0 && sectionCount >= quizSectionCount && totalWatchTime - lastQuizTime >= quizIntervalCount && isQuizMode) {
    startQuizSession();
    lastQuizTime = totalWatchTime;
  };

  // ========== 영상 끝 감지 (매칭 퀴즈) ==========
  // 영상 종료까지 남은 시간 담기
  const matchingTiming = video.duration - video.currentTime;

  // 끝나기 3초 전
  if ((matchingTiming < 3)) {
    // 리스너 제거
    video.removeEventListener('timeupdate', handleTimeUpdate);
    isTimeUpdateListenerSet = false;

    // 매칭 퀴즈 1회만
    if (matchingQuizCount < 1) {
      video.pause();
      matchingQuiz();
    }
    return;
  }
};



// 현재 시간에 맞는 자막 찾아서 전송
function handleSubtitleSync(e) {
  const video = e.target;

  if (!video) {
    return;
  }

  if (document.querySelector('.ad-showing')) {
    return;
  }

  if (allSubtitles.length === 0) {
    video.removeEventListener('timeupdate', handleSubtitleSync);
    eventTimeUpdateListener = null;
    return;
  }

  // 현재 시간 실시간 업데이트
  const currentTime = video.currentTime;

  // 현재 시간이 자막의 시작~끝 사이에 있으면 allSubtitles에 맞는 인덱스 반환
  const index = allSubtitles.findIndex(sub =>
    currentTime >= sub.startTime && currentTime <= sub.endTime
  );

  // 자막 데이터가 있고 && 기존 영상이면 전송(새로고침하면 초기화)
  if (index !== -1 && index !== lastSentIndex) {
    // 인덱스 순서로 담기
    const sub = allSubtitles[index];

    // 필요한 정보 가져오기
    const videoId = new URL(window.location.href).searchParams.get('v');
    const duration = video.duration;
    const videoTitle = currentVideoTitle || getVideoTitle();

    if (!videoTitle) {
      return;
    }


    // 큐 대기열에 넣기
    addToQueue(sub.text, sub.translation, sub.startTime, sub.endTime, videoId, videoTitle, duration);  // , sub.translation 추가하기
    
    lastSentIndex = index;
  }
}



// 큐에 자막 넣기
function addToQueue(text, translation, startTime, endTime, videoId, videoTitle, duration) {
  // 대기중인 자막이 10개 이상이면 오래된 자막부터 버리기
  if (textQueue.length - head >= MAX_QUEUE) {
    // 맨 앞 버리기
    head++;
  }
  textQueue.push({ text, translation, startTime, endTime, videoId, videoTitle, duration });
  // 처리 시도
  processQueue();
}



// 자막 데이터를 서버와 사이드패널로 보내기 전 순서대로 처리
async function processQueue() {
  // 이미 처리중이면 스킵
  if (isProcessing) return;
  // 빈 큐면 스킵
  if (head >= textQueue.length) return;
  isProcessing = true;
  while (head < textQueue.length) {
    // 맨 앞 꺼내기
    const item = textQueue[head];
    // 에러 발생해도 큐가 멈추지 않게 먼저 증가시키기
    head++;
    try {
      // 백엔드 DB에 자막 1개 및 번역 데이터 저장
      const quizResponse = await apiFetch(`/videos/${item.videoId}/subtitles`, {
        method: 'POST',
        body: JSON.stringify({
          title: item.videoTitle,
          text: item.text,
          translation: item.translation || null,
          startTime: item.startTime,
          endTime: item.endTime,
          duration: item.duration
        })
      });
      if (quizResponse.success) {
        isVideoSaved = true;
        isQuizMode = true;
      }

      // 사이드 패널 화면에 실시간 자막 업데이트
      chrome.runtime.sendMessage({
        type: 'SUBTITLE_UPDATE',
        videoId: item.videoId,
        text: item.text,
        translation: item.translation || null,
        startTime: item.startTime,
        endTime: item.endTime
      });
    } catch (error) {
      log.debug('처리 실패', error);
    }
  }

  // 메모리 정리 (20개마다)
  if (head > 20) {
    textQueue = textQueue.slice(head);
    head = 0;
  }
  isProcessing = false;
}



function registerSubtitleListener() {
  // 이전 리스너 제거
  document.removeEventListener('CLIPZY_SUBTITLE_DATA', subtitleDataHandler);
  
  // 새 리스너 등록
  document.addEventListener('CLIPZY_SUBTITLE_DATA', subtitleDataHandler);

  // 자막 버튼 껐다 켜기
  const btn = document.querySelector('.ytp-subtitles-button');
  if (btn) {
    if (btn.getAttribute('aria-pressed') === 'true') {
      // 켜져 있으면 껐다 켜기
      // 끄기
      btn.click();
      setTimeout(() => {
        // 켜기
        btn.click();
      }, 1000);
    } else {
      // 꺼져 있으면 그냥 켜기
      btn.click();
    }
  }
}



// 자막 가져오는 리스너 설정
async function subtitleDataHandler (event) {

  if (translatedVideoId === currentVideoId && allSubtitles.length > 0) {
    return;
  }

  if (translatedVideoId === currentVideoId) {
    return;
  }

  // 영상 페이지에서만 처리
  if (!location.href.includes('/watch')) {
    return;
  }

  // detail.response가 문자열인지 확인 (타입 검증)
  if (typeof event.detail?.response !== 'string') return;

  // ========== JSON 파싱 ==========
  let data;
  try {

    // 문자열(자막 데이터가 ""로 담김)을 객체로 변환 (JSON 형식 검증)
    data = JSON.parse(event.detail.response)
  } catch (error) {
    log.debug('잘못된 데이터', error);
    return;
  }

  // ========== 데이터 구조 검증 ==========
  // data.events가 (없거나 falsy 또는 배열이 아닌 경우면) 리턴 (데이터 구조 검증)
  if (!data.events || !Array.isArray(data.events)) {
    return;
  }

  translatedVideoId = currentVideoId;
  try {

    // ========== 자막 데이터 처리 ==========
    // 자막 데이터의 이벤트 요소(tStartMs,dDurationMs, segs) 저장
    allSubtitles = data.events
    // segs(텍스트 조각)가 있는 이벤트 요소만 가져오기
    .filter(event => event.segs)
    .map(event => ({
      // 각각의 segs에 담긴 텍스트 조각들(utf8) 합치기
      text: event.segs.map(seg => seg.utf8 || '').join('').trim(),
      // 시작 시간 초 단위로 저장
      startTime: event.tStartMs / 1000,
      // 끝 시간 초 단위로 저장(시작 시간 + 지속 시간)
      endTime: (event.tStartMs + (event.dDurationMs || 0)) / 1000
    }))
    // sub.text가 빈 텍스트이면 해당 데이터 제거(segs가 공백 또는 빈 값인 경우 방지)
    .filter(sub => sub.text)
    // 전체가 [...] 형태일 때만 해당 데이터 제거
    .filter(sub => !/^\[.*\]$/.test(sub.text.trim()));

    // ========== 텍스트 정리 ==========
    // 자막 데이터에 담긴 텍스트의 줄바꿈을 공백으로 변환
    allSubtitles = allSubtitles.map(sub => ({
      ...sub,
      text: sub.text.replace(/\n/g, ' ').trim()
    }));

     // ========== 서버에 전체 자막 및 번역 저장 ==========

    chrome.runtime.sendMessage({
      type: 'TRANSLATE',
      endpoint: '/translate/subtitles',
      options: {
        method: 'POST',
        body: JSON.stringify({
          videoId: currentVideoId,
          title: currentVideoTitle,
          duration: currentDuration,
          channelName: currentChannelName,
          thumbnailUrl: currentThumbnailUrl,
          subtitleRequests: allSubtitles.map(sub => ({
            text: sub.text,
            startTime: sub.startTime,
            endTime: sub.endTime
          }))
        })
      }
    }).then(response => {
      // 나중에 번역 도착하면 업데이트
      if (response?.data?.translatedTexts) {
        allSubtitles = allSubtitles.map((sub, index) => ({
          ...sub,
          translation: response.data.translatedTexts[index] || ''
        }));

        // 번역 포함해서 다시 전송
        chrome.runtime.sendMessage({
          type: 'SUBTITLES_LOADED',
          subtitles: allSubtitles
        });
      } else {
        log.debug('번역 응답 형식 이상', response);
      }
    }).catch(error => {
      log.debug('번역 실패:', error);
    });

    // ========== 리스너 설정 ==========
    // 인덱스 리셋
    lastSentIndex = -1;

    // 영상 시청 시간 감지 및 끝 시간 감지 리스너 설정
    setupTimeUpdateListener();
  } catch (error) {
    log.debug('자막 파싱 실패:', error);
    // 다시 요청 가능
    translatedVideoId = null;
  }
};



// 영상 길이에 맞는 퀴즈 간격 계산
function getQuizCount(duration) {
  const sectionCount = getSectionCount(duration);
  if (sectionCount === 0) return 0;
  
  // 총 퀴즈 수 = 섹션 수 + 매칭 1개
  const totalQuizCount = sectionCount + 1;
  
  return duration / totalQuizCount;
}



// 영상 길이에 맞는 섹션 수 계산
function getSectionCount(duration) {
  // 1분 미만
  if (duration < 60) return 0;
  // 1분~5분 미만
  if (duration >= 60 && duration < 300) return 1;
  // 5분~10분 미만
  if (duration >= 300 && duration < 600) return 1;
  // 10분~20분 미만
  if (duration >= 600 && duration < 1200) return 2;
  // 20분~30분 미만
  if (duration >= 1200 && duration < 1800) return 3;
  // 30분 이상
  if (1800 <= duration) return 3;
}



// 세션 시작하고 (빈칸 / ox) 퀴즈 요청
async function startQuizSession() {
  // 사이드 패널 열려야 실행
  if (!isPanelOpen) return;

  // 퀴즈 요청 중이면 스킵 (중복 방지)
  if (quizRequested) return;

  // Video가 저장되지 않았으면 스킵
  if (!isVideoSaved) {
    return;
  }

  const videoId = new URL(window.location.href).searchParams.get('v');

  // 같은 영상이면 스킵 (중복 방지)
  if (videoId === lastVideoId) return;

  quizRequested = true;
  lastVideoId = videoId;
  try {
    // 복습 체크용 퀴즈 이력 조회하기
    const quizHistory = await apiFetch(`/badges/video/${videoId}`, {
      method: 'GET'
    });
    const sessionType = !quizHistory?.data?.currentBadge ? 'NORMAL' : 'REVIEW';

    // 서버에 퀴즈 받기
    const sessionResponse = await apiFetch('/quiz/sessions/generate/section', {
      method: 'POST',
      body: JSON.stringify({
        videoId: videoId,
        sessionType: sessionType,  // 복습 체크하기
        sectionNumber: quizSectionCount,
      })
    });

    quizSectionCount++;

    // // 섹션 저장
    // chrome.storage.local.set({
    //   [`quizProgress_${videoId}`]: {
    //     sectionCount: quizSectionCount,
    //     matchingCount: matchingQuizCount
    //   }
    // });

    // 영상과 유저에 맞게 섹션 저장
    await saveUserData(`quizProgress_${videoId}`, {
      sectionCount: quizSectionCount,
      matchingCount: matchingQuizCount
    });


    // 사이드 패널로 퀴즈 전송
    chrome.runtime.sendMessage({
      type: 'QUIZ_READY',
      videoId: videoId,
      sessionId: sessionResponse.data.sessionId,
      quizzes: sessionResponse.data.quizzes,
      totalCount: sessionResponse.data.totalQuizCount || 10
    }).catch((error) => { log.debug('퀴즈 전송 에러', error) });

    // // Chrome Storage에도 저장 (패널 닫혀있을 때 대비)
    // chrome.storage.local.set({
    //   currentSessionId: sessionResponse.data.sessionId,
    //   currentQuizzes: sessionResponse.data.quizzes
    // });
  } catch (error) {
    log.debug('퀴즈 세션 실패', error);
  } finally {
    // 실행 후 다시 요청 가능
    quizRequested = false;
    isQuizMode = false;
  }
}



async function matchingQuiz() {
  // 사이드 패널 열려야 실행
  if (!isPanelOpen) return;

  // 퀴즈 요청 중이면 스킵 (중복 방지)
  if (quizRequested) return;

    if (!isVideoSaved) {
    return;
  }

  const videoId = new URL(window.location.href).searchParams.get('v');

  // 같은 영상이면 스킵 (중복 방지)
  if (videoId === lastVideoId) return;

  quizRequested = true;
  lastVideoId = videoId;
  try {
    // 복습 체크용 퀴즈 이력 조회하기
    const matchingQuizHistory = await apiFetch(`/badges/video/${videoId}`, {
      method: 'GET'
    });
    const matchingSessionType = !matchingQuizHistory?.data?.currentBadge ? 'NORMAL' : 'REVIEW';


    // 서버에 매칭 퀴즈 받기
    const matchingResponse = await apiFetch('/quiz/sessions/generate/matching', {
      method: 'POST',
      body: JSON.stringify({
        videoId: videoId,
        sessionType: matchingSessionType,
      })
    });
    matchingQuizCount++;

    // 영상과 유저에 맞게 진행도 저장
    await saveUserData(`quizProgress_${videoId}`, {
      sectionCount: quizSectionCount,
      matchingCount: matchingQuizCount
    });
  
    // chrome.storage.local.set({
    //   [`quizProgress_${videoId}`]: {
    //     sectionCount: quizSectionCount,
    //     matchingCount: matchingQuizCount
    //   }
    // });

    // 사이드 패널로 퀴즈 전송
    chrome.runtime.sendMessage({
      type: 'QUIZ_READY',
      videoId: videoId,
      sessionId: matchingResponse.data.sessionId,
      quizzes: matchingResponse.data.quizzes,
      totalCount: matchingResponse.data.totalQuizCount || 10
    }).catch((error) => { log.debug('매칭 퀴즈 전송 실패', error) });

    // // Chrome Storage에도 저장 (패널 닫혀있을 때 대비)
    // chrome.storage.local.set({
    //   currentSessionId: matchingResponse.data.sessionId,
    //   currentQuizzes: matchingResponse.data.quizzes
    // });
  } catch (error) {
    log.debug('매칭 퀴즈 세션 실패', error);
  } finally {
    // 실행 후 다시 요청 가능
    quizRequested = false;
  }
}



// 구간 반복
const handleLoop = (e) => {
  const video = e.target;
  if (loopEnd && video.currentTime >= loopEnd) {
    video.currentTime = loopStart;
  }
};



// 리셋
// 페이지 로드 대기 후 실행
// init()에서 페이지 종류에 따라 분기
function init() {
  videoRetryCount = 0;
  infoRetryCount = 0;

  // 퀴즈 요청 상태 리셋
  quizRequested = false;
  // 저장된 영상ID 리셋
  lastVideoId = '';
  // 영상 제목과 아이디, 채널명 전송 횟수 리셋
  isQuiz = false;
  // 현재 퀴즈 섹션 수 리셋
  quizSectionCount = 1;
  // 매칭 퀴즈 요청 횟수 리셋
  matchingQuizCount = 0;

  // 변수 리셋
  totalWatchTime = 0;
  lastWatchTime = 0;

  // 마지막 퀴즈가 나온 시점의 시청 시간 리셋
  lastQuizTime = 0;

  // 퀴즈 요청 가능한지 여부 리셋
  isQuizMode = false;

  // 같은 영상인지 체크 리셋
  translatedVideoId = null;
  isSubtitleInterceptorSet = false;
  allSubtitles = [];
  lastSentIndex = -1;


  // 현재 영상Id 저장 리셋
  currentVideoId = null;

  // 현재 영상 제목 리셋
  currentVideoTitle = '';

  // 현재 영상 전체 길이 리셋
  currentDuration = 0;
  // Video가 서버에 저장됐는지 여부 리셋
  isVideoSaved = false;
  // 현재 영상 채널명 리셋
  currentChannelName = null;
  // 현재 영상 썸네일 url 리셋
  currentThumbnailUrl = null;

  // ========== 로그인 + 온보딩 완료체크 리셋 ==========
  isActive = false;

  // ========== 광고 끝남 감지 옵저버 리셋 ==========
  cleanupAdObserver();

  // ========== 큐 리셋 ==========
  resetQueue();

  // ========== 리스너/자막 리셋 =========
  resetSubtitleSystem();

  // // ========== 패널 열려있으면 상태 리셋 + 페이지 구분 및 이동 ==========
  // handlePageChange();
}



// ========== 즉시 실행 (첫 로드/새로고침/F5) ==========
(async function start() {


  // 패널 열림 처리
  chrome.runtime.sendMessage({ type: 'CONTENT_LOADED' });
  // 이전 영상의 자막, 퀴즈, 시청시간 등의 상태를 리셋
  resetAllState();

  // 토큰과 유저 정보가 있고 온보딩 설정을 했으면 activate 실행
  const canRun = await checkAccess();

  if (canRun) {

    activate();
  }

  // 세션 저장소 변경 감지 리스너 등록
  listenForChanges();

})();