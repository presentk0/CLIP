/* global chrome */
import { apiFetch } from "./utils/api";

// 영상 중복 전송 방지용
let lastVideoId = '';
// 자막 배열 중복 방지용
let lastSubtitle = [];
// 자막 문자열로 변환 후 중복 체크용
let lastKey = '';
// 시작 시간 저장용
let startTime = null;
// 퀴즈 생성을 위해 임시로 모아두는 자막 저장 박스 (전송 로직 고칠 예정)
let collectedSubtitles = [];


// 서버로 보낼 자막 데이터들 모아두는 박스
let textQueue = [];
// 대기열의 현재 처리 위치 (줄의 맨 앞)
let head = 0;
// 현재 대기열을 처리 중인지 확인용 (중복 실행 방지)
let isProcessing = false;
// 대기열이 많이 쌓이면 오래된 순서로 버리기는 기준 (10개 이상이면 버리기)
const MAX_QUEUE = 10;


// 패널 열고 닫기 상태
let isPanelOpen = false;
// 퀴즈 중복 요청 방지
let quizRequested = false;
// 초기값 감지 미실행 상태
let observer = null;
// 자막 감시중인지 제크
let isObserving = false;
// 구간 반복 시작 시간
let loopStart = null;
// 구간 반복 종료 시간(구간 반복 고치치)
let loopEnd = null;


// 재시도 횟수 카운트용
let retryCount = 0;
// 최대 5번까지 재시도(무한 루프 방지)
const MAX_RETRY = 5;
// play, pause 같은 영상 이벤트 리스너 중복 방지용
let listenersAdded = false;
// 홈 & 검색 화면에서 새로운 영상 썸네일이 나타나는지 감시용 (배지 부착용)
let thumbnailObserver = null;
// 서버에서 받은 현재 퀴즈 세션의 고유 ID (제출 및 결과 조회용)
let sessionId = null;








// 사이드 패널 신호 수신
chrome.runtime.onMessage.addListener((message) => {
  // 패널 열림
  if (message.type === 'PANEL_OPENED') {
    isPanelOpen = true;
    // 패널 열리면 모든 시스템 초기화 및 시작
    init();
  }

  // 패널 닫힘
  if (message.type === 'PANEL_CLOSED') {
    isPanelOpen = false;
    // 저장한 자막 삭제
    localStorage.removeItem('subtitles');
    stopObserver();
    // 추천 영상 다시 보여주기
    showRecommendations();
    const video = document.querySelector('video');
    if (video) {
      // 루프 제거
      video.removeEventListener('timeupdate', handleTimeUpdate);
    }
    loopStart = null;
    loopEnd = null;
  }

  // 퀴즈 시 영상 멈추기
  if (message.type === 'SET_QUIZ_MODE') {
    const video = document.querySelector('video');
    if (video) video.pause();
  }

  // 사이드 패널에서 보낸 시점 이동 메시지 수신
  // 루프 관련만 video 체크
  if (message.type === 'START_LOOP' || message.type === 'STOP_LOOP') {
    const video = document.querySelector('video');
    if (!video) return;
    if (message.type === 'START_LOOP') {
      loopStart = message.startTime;
      loopEnd = message.endTime;
      video.currentTime = loopStart;
      video.play();
      video.addEventListener('timeupdate', handleTimeUpdate);
    }
    if (message.type === 'STOP_LOOP') {
      loopStart = null;
      loopEnd = null;
      video.pause();
      video.removeEventListener('timeupdate', handleTimeUpdate);
    }
  }
});


// 유튜브 SPA 대응 URL 변경 감지
let lastUrl = location.href;

// 유튜브 전용 이벤트
window.addEventListener('yt-navigate-finish', () => {
  // URL 바뀌면 실행
  if (location.href !== lastUrl) {
    lastUrl = location.href;
    // 기존 감시 종료 (메모리 누수 방지)
    stopObserver();
    // 썸네일 옵저버 리셋
    if (thumbnailObserver) {
      thumbnailObserver.disconnect();
      thumbnailObserver = null;
    }
    // 새 영상이면 이전 자막 캐시 초기화
    lastSubtitle = '';
    lastVideoId = '';
    collectedSubtitles = [];
    quizRequested = false;

    // 영상 바뀔 때 큐 비우기
    resetQueue();

    // 패널 열려있을 때만 실행
    if (isPanelOpen) {
      // 영상 페이지면 추천 영상 숨기고 자막 감지
      if (location.href.includes('/watch')) {
        setTimeout(hideRecommendations, 500);
        setTimeout(observeSubtitles, 3000);
      } else {
        // 영상 페이지 아니면 추천 영상 보이고 페이지 감지
        showRecommendations();
        setTimeout(observeThumbnails, 3000);
      }
    }
  }
});


// 영상 시청 페이지 전용 자막 감지 (패널 열렸을 때만)
function observeSubtitles() {
  // 사이드 패널 열려야 실행
  if (!isPanelOpen) return;

  // watch 페이지에서만 실행
  if (!window.location.href.includes('/watch')) {
    showRecommendations();
    return;
  }

  // 현재 영상 가져오기
  const video = document.querySelector('video');
  if (!video) {
    if (retryCount < MAX_RETRY) {
      retryCount++;
      // DOM이 렌더링 안 됐으면 3초 후 재시도
      setTimeout(observeSubtitles, 3000);
    }
    return;
  }
  // 성공하면 리셋
  retryCount = 0;

  // 이벤트 리스너 중복 방지
  if (!listenersAdded) {
  // 영상이 재생 중일 때만 MutationObserver를 켜서 메모리 낭비 방지
  video.addEventListener('play', () => {
    startObserver();
  });
  // 영상이 멈추면 자막 감시 끄기
  video.addEventListener('pause', () => {
    stopObserver();
  });
    listenersAdded = true;
  }

  // 이미 재생 중이면 바로 시작
  if (!video.paused) {
    startObserver();
  }
}



function startObserver() {
  // 사이드 패널 열려야 실행
  if (!isPanelOpen) return;

  // 쇼츠 차단
  if (window.location.href.includes('/shorts')) {
    return;
  }

  // watch 페이지에서만 실행
  if (!window.location.href.includes('/watch')) {
    showRecommendations();
    return;
  }

  if (isObserving) {return;}
  
  const container = document.querySelector('.ytp-caption-window-container') || document.body;
  if (!container) {return;}

  // URL에서 videoId 추출
  // 유튜브 URL 구조 = https://www.youtube.com/watch?v=dQw4w9WgXcQ 이런 형태에서 v= 부분이 videoId
  const videoId = new URL(window.location.href).searchParams.get('v');
  if (!videoId) {return;}
  const videoTitle = document.querySelector('#title h1')?.textContent?.trim() || '';
  if (!videoTitle) {return;}

  // 영상 페이지 진입 시 퀴즈페이지로 이동 및 영상 아이디 보내기
  chrome.runtime.sendMessage({ 
    type: 'GO_TO_QUIZ',
    videoId: videoId
  }).catch(() => {});

  setTimeout(() => {
    chrome.runtime.sendMessage({
      type: 'SUBTITLES_DATA',
      videoId: videoId,
      videoTitle: videoTitle
    }).catch(() => {});
    // 나중에 퀴즈 페이지 이동이 느려서 오류나면 코드 고치기
  }, 100);

  // HTML 요소(DOM)에 변화가 생기는지 감시하는 브라우저 내장 기능, 자막이 바뀔 때만 브라우저가 알아서 알림을 줌
  // 단어가 나올때마다 감지하지만 중복체크로 완성된 문장만 가져옴
  observer = new MutationObserver(async () => { 
    // 광고면 자막 수집 안 함
    if (document.querySelector('.ad-showing')) {
    return;
  }
    const video = document.querySelector('video');
    // 자막 전체 가져오기
    const subtitleElement = document.querySelectorAll('.ytp-caption-segment');
    if (!video) {return;}

    // 단일 요소는 배열 안 됨
    const texts = Array.from(subtitleElement)
    .map(el => el.textContent.trim())
    .filter(t => t);

    // 배열 구조를 통채로 문자열처리
    // 배열 === 배열 -> 항상 false (참조 비교)
    // 문자열 === 문자열 -> 내용 비교 가능
    const currentKey =  JSON.stringify(texts);

    // 텍스트가 바뀐 순간의 비디오 재생 시점(초)을 가져옴
    const currentTime = video.currentTime;

    // 자막 끝나는 시점 (여기서 서버에 데이터 전송)
    if (texts.length === 0 && lastSubtitle.length > 0) {
      // 각 문장 따로 큐에 넣기
      for (const text of lastSubtitle) {
        addToQueue(text, startTime, currentTime, videoId);
      }
      // 번역 기다리기 전 미리 초기화
      lastSubtitle = [];
      startTime = null;
      lastKey = '';
      return;
    }

    // 중복 체크
    if (currentKey === lastKey) {return;}

    // 새 자막 시작
    if (lastKey === '' && texts.length > 0) {
      startTime = currentTime;
    }

    // 저장
    lastSubtitle = texts;
    lastKey = currentKey;
  });

  // 자막이 사라질때만 감지 코드
  // observe는 observer에 있는 자막 감시를 실행하는 함수
  observer.observe(container, { 
    // 새로운 HTML 태그(<span>, <div> 등)가 생기거나, 기존 태그가 삭제될 때 알림
    childList: true,
    // 전체 구역(subtree)을 훑어봄으로써 자식의 자식들까지 전부 감지
    subtree: true,
    // 글자(텍스트)가 바뀌는지 감시, 태그의 변화가 없고 글자만 변해도 작동하는 코드
    characterData: true
  });
  isObserving = true;
}



function stopObserver() {
  // 해당 클래스에서 자막 감시
  // if 없어도 작동하지만 로딩 고려해서 값이 있을 때만 실행하는 안전장치
  if (observer) {
    // MutationObserver 감시 중지
    observer.disconnect();  
    isObserving = false;
  }
}



// 큐에 자막 넣기
function addToQueue(text, startTime, endTime, videoId) {
  // 번역이 너무 느려서 대기중인 자막이 10개 이상이면 오래된 자막부터 버리기
  if (textQueue.length - head >= MAX_QUEUE) {
    // 맨 앞 버리기
    head++;
  }
  textQueue.push({ text, startTime, endTime, videoId });
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
    head++;
    try {
      // DeepL 번역 요청 (백그라운드)
      const translation = await fetchTranslation(item.text);
      // 백엔드 DB에 자막 및 번역 데이터 저장
      await apiFetch(`/api/videos/${item.videoId}/subtitles`, {
        method: 'POST',
        body: JSON.stringify({
          text: item.text,
          translation: translation,
          startTime: item.startTime,
          endTime: item.endTime
        })
      });

      // 사이드 패널 화면에 실시간 자막 업데이트
      chrome.runtime.sendMessage({
        type: 'SUBTITLE_UPDATE',
        videoId: item.videoId,
        text: item.text,
        translation: translation,
        startTime: item.startTime,
        endTime: item.endTime
      });

      collectedSubtitles.push({
        text: item.text,
      });

      // 자막이 10개 쌓였고 퀴즈를 요청한 적이 없다면 퀴즈 생성 요청 (수정 예정)
      if (collectedSubtitles.length >= 10 && !quizRequested) {
        startQuizSession();
      }
    } catch (error) {
      console.log('처리 실패:', error);
    }
  }

  // 메모리 정리 (20개마다)
  if (head > 20) {
    textQueue = textQueue.slice(head);
    head = 0;
  }
  isProcessing = false;
}


// 영상 바뀔 때 리셋
function resetQueue() {
  textQueue = [];
  head = 0;
  isProcessing = false;
}


// 번역 API 호출
async function fetchTranslation(text) {
  return new Promise((resolve) => {
    // 응답 리스너 먼저 등록
    const listener = (message) => {
      if (message?.type === 'TRANSLATE_RESULT' && message?.text === text) {
        chrome.runtime.onMessage.removeListener(listener);
        resolve(message?.translation || '');
      }
    };
    chrome.runtime.onMessage.addListener(listener);

    // 요청 보내기
    chrome.runtime.sendMessage({
      type: 'TRANSLATE',
      text: text,
      source: 'content'
    });
  });
}


// 세션 시작하고 퀴즈 요청
async function startQuizSession() {
  // 사이드 패널 열려야 실행
  if (!isPanelOpen) return;

  const videoId = new URL(window.location.href).searchParams.get('v');

  // 중복 방지
  if (quizRequested) return;
  // 같은 영상이면 스킵
  if (videoId === lastVideoId && quizRequested) return;
  quizRequested = true;
  lastVideoId = videoId;
  try {
    // 세션 시작하면 자막 전송
    const sessionResponse = await apiFetch('/api/quiz/sessions/start', {
      method: 'POST',
      body: JSON.stringify({
        videoId: videoId,
        sessionType: 'NORMAL',
        // 자막 배열 전송
        subtitles: collectedSubtitles.map(sub => sub.text)
      })
    });
    sessionId = sessionResponse.data.sessionId;

    // 빈칸 퀴즈 요청
    const quizResponse = await apiFetch('/api/quiz/generate/blank', {
      method: 'POST',
      body: JSON.stringify({
        sessionId: sessionId,
        wordCount: 5
      })
    });


    // OX 퀴즈 요청
    // const oxQuiz = await apiFetch('/api/quiz/generate/ox', {
    //   method: 'POST',
    //   body: JSON.stringify({ sessionId })
    // });


    // 사이드 패널로 퀴즈 전송
    chrome.runtime.sendMessage({
      type: 'QUIZ_READY',
      videoId: videoId,
      sessionId: sessionId,
      quizzes: quizResponse.data.quizzes,
      totalCount: quizResponse.data.totalQuizCount || 10
    }).catch(() => {});

   // Chrome Storage에도 저장 (패널 닫혀있을 때 대비)
    chrome.storage.local.set({
      currentSessionId: sessionId,
      currentQuizzes: quizResponse.data.quizzes
    });
  } catch (error) {
    console.error('퀴즈 세션 실패', error);
    // 실패 시 재시도 가능
    quizRequested = false;
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
    // CSS 규칙 작성
    styleTag.textContent = `ytd-watch-flexy #secondary {display: none !important;}`;
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


// 구간 반복
const handleTimeUpdate = () => {
  const video = document.querySelector('video');
  if (loopEnd && video.currentTime >= loopEnd) {
    video.currentTime = loopStart;
  }
};



// 썸네일 뱃지 테스트 (메인 페이지 인식 불가 오류 확인)
async function addTestBadge() {
  if (window.location.href.includes('/watch')) {
    return;
  }
  // 홈페이지: a#thumbnail
  // 영상 썸네일 컨테이너 찾기
  // const allThumbnails = document.querySelectorAll('ytd-thumbnail');
  // 위의 코드가 오작동한 이유: 모든 ytd-thumbnail을 찾았는데 여기 포함된게 밑의 것들
  // player-container-background-image	플레이어 배경
  // ytd-video-preview	미리보기 플레이어
  // ytd-watch-flexy	영상 시청 페이지 요소

  // 영상 링크가 있는 썸네일만 찾기
  const allThumbnails = document.querySelectorAll('ytd-thumbnail a#thumbnail[href*="/watch?v="]');

  if (allThumbnails.length === 0) {
    return;
  }


  for (const thumb of allThumbnails) {
    try {
      //  ytd-thumbnail 찾기
      // .closest()는 부모 태그중에서 해당하는거 찾기
      // 이거 있으면 thumb.style.position = 'relative'; 이 코드 필요없음
      // 이 코드 변경 금지
      const ytdThumbnail = thumb.closest('ytd-thumbnail');
      if (!ytdThumbnail) continue;

      // 광고 필터
      if (ytdThumbnail.closest('ytd-ad-slot-renderer')) continue;

      // 이미 뱃지 있으면 스킵
      if (ytdThumbnail.querySelector('.clip-test-badge')) continue;

      // URL에서 videoId 추출
      const href = thumb.getAttribute('href');
      const videoId = new URLSearchParams(href.split('?')[1]).get('v');
    
      // API 호출(수정 예정)
      const response = await apiFetch(`/api/badges/video/${videoId}`);
      const badge = response.data.currentBadge;

      // 뱃지 표시(수정 예정)
      if (badge) {
        // position 체크 후 추가
        const currentPosition = window.getComputedStyle(ytdThumbnail).position;
        if (currentPosition === 'static' || !currentPosition) {
          ytdThumbnail.style.position = 'relative';
        }

      // thumbnail 스타일 안 건드림
      // 부모 태그 안에 자식 추가
      ytdThumbnail.appendChild(badge);
    }
  } catch (error) {
      // 에러나도 이 항목만 스킵, 나머지는 계속
      console.log('이 썸네일 스킵 (에러):', error.message);
      continue;
    }
  }
}



// MutationObserver로 DOM 변화 감지
// 메인/검색 페이지 전용 (뱃지 부착용)
function observeThumbnails() {
  // 사이드 패널 열려야 실행
  if (!isPanelOpen) return;

  // 영상 페이지면 시작 안 함
  if (window.location.href.includes('/watch')) {
    return;
  }

  // 중복 방지용 기존 옵저버 정리
  if (thumbnailObserver) {
    thumbnailObserver.disconnect();
    thumbnailObserver = null;
  }

  // 썸네일 영역만 감시
  // 유튜브 페이지마다 구조가 달라서 둘 다 체크
  // 이거 없이 전체감시하면 창모드하고 다른 페이지보면 무한 호출해버림
  // #contents = 유튜브 메인 콘텐츠 영역 -> 영상 목록이 들어있는 큰 박스가 있으면 그거 사용.
  // 없으면 유튜브 홈페이지의 그리드 레이아웃 -> 썸네일들이 격자로 배열된 영역이 있으면 ytd-rich-grid-renderer 사용.
  // 없으면 전체 감시
  const contents = document.querySelector('#contents') || document.querySelector('ytd-rich-grid-renderer') || document.body;

  // 디바운스(debounce) 패턴
  // 값 없이 선언 -> undefined 상태
  let timeout;
  thumbnailObserver = new MutationObserver(() => {
    // 이전 타이머 취소, undefined 넣어도 에러없이 무시됨
    clearTimeout(timeout);  
    timeout = setTimeout(addTestBadge, 1000);
  });

  // 페이지 전체 감시가 아닌 영상 목록만 감시해서 검색창의 추천 검색어들 뜨는거 감지 무시
  thumbnailObserver.observe(contents, {
    childList: true,
    subtree: true
  });

  addTestBadge();
}



// 초기화
// 페이지 로드 대기 후 실행
// init()에서 페이지 종류에 따라 분기
function init() {
  retryCount = 0;
  listenersAdded = false;
  // 자막 배열 리셋
  collectedSubtitles = [];
  // 퀴즈 요청 상태 리셋
  quizRequested = false;

  // 페이지 종류에 따라 분기
  if (window.location.href.includes('/watch')) {
    setTimeout(hideRecommendations, 500);
    setTimeout(observeSubtitles, 3000);
  } else {
    showRecommendations();
    setTimeout(observeThumbnails, 3000);
  }
}