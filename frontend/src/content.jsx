/* global chrome */
import { apiFetch } from "./utils/api";

// 영상 중복 전송 방지용
let lastVideoId = '';
// 전체 자막 데이터 저장
let allSubtitles = [];
// 마지막에 전송한 자막 인덱스
let lastSentIndex = -1;
// 인터셉터 중복 방지
let isSubtitleInterceptorSet = false;
// 리스너 중복 방지
let isTimeUpdateSet = false;


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


// 구간 반복 시작 시간
let loopStart = null;
// 구간 반복 종료 시간(구간 반복 고치치)
let loopEnd = null;
// 영상 끝 감지 리스너 활성화 여부 체크
let isEndListenerSet = false;


// 구간 반복 이벤트 리스너 강제 종료용
let eventLoop = null;
// 영상 끝 감지 이벤트 리스너 강제 종료용
let eventEndListener = null;
// 영상 자막 감지 이벤트 리스너 강제 종료용
let eventSubtitleListener = null;


// 영상 페이지에서 영상 가져오기 실패 시 재시도 횟수 카운트용
let retryCount = 0;
// 영상 페이지에서 영상 가져오기 (실패 시 최대 15번까지 재시도)
const MAX_RETRY = 15;


// 홈 & 검색 화면에서 새로운 영상 썸네일이 나타나는지 감시용 (배지 부착용)
let thumbnailObserver = null;


let isQuiz = false;
// 현재 요청한 퀴즈 섹션 수
let quizSectionCount = 0;
// 현재 요청한 매칭 퀴즈 수
let matchingQuizCount = 0;

// 유튜브 SPA 대응 URL 변경 감지
let lastUrl = '';


// ========== 사이드 패널 신호 수신 ==========
chrome.runtime.onMessage.addListener((message) => {
  // 패널 열림
  if (message.type === 'PANEL_OPENED') {
    console.log('패널 열림1:', Date.now());
    isPanelOpen = true;
    // 패널 열리면 모든 시스템 리셋 및 시작
    init();
  }

  // 패널 닫힘
  if (message.type === 'PANEL_CLOSED') {
    isPanelOpen = false;

    // ========== 리스너/자막 리셋 ==========
    resetSubtitleSystem();

    // ========== 퀴즈/영상 상태 리셋 ==========
    isQuiz = false;
    quizSectionCount = 0;
    matchingQuizCount = 0;
    quizRequested = false;
    lastVideoId = '';

    // ========== 큐/기타 리셋 ==========
    resetQueue();
    retryCount = 0;

    // ========== 썸네일/UI ==========
    // 썸네일 옵저버 리셋
    if (thumbnailObserver) {
      thumbnailObserver.disconnect();
      thumbnailObserver = null;
    }
    // 붙인 뱃지 제거
    removeBadges();
    // 추천 영상 다시 보여주기
    showRecommendations();
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
      // 현재 영상 플레이어 객체 저장
      eventLoop = video;
      loopStart = message.startTime;
      loopEnd = message.endTime;
      console.log(loopStart);
      console.log(loopEnd);

      video.currentTime = loopStart;
      video.play();
      video.addEventListener('timeupdate', handleLoop);
    }
    if (message.type === 'STOP_LOOP') {
      loopStart = null;
      loopEnd = null;
      video.pause();
      video.removeEventListener('timeupdate', handleLoop);
      // 저장한 영상 영상 플레이어 객체 리셋
      eventLoop = null;
    }
  }
});


// 페이지 이동 시 실행(첫 화면과 새로고침은 미실행)
window.addEventListener('yt-navigate-finish', () => {
  console.log('실행여부1');
  // URL 바뀌면 실행
  // location.href = 현재 페이지의 전체 URL
  if (location.href !== lastUrl) {
    console.log('실행여부2');

    // ========== 썸네일 관련 ==========
    // 썸네일 배지 감시 중지
    if (thumbnailObserver) {
      thumbnailObserver.disconnect();
      thumbnailObserver = null;
    }

    // ========== 상태 리셋 ==========
    lastVideoId = '';
    quizRequested = false;
    isQuiz = false;
    // 현재 퀴즈 섹션 수 리셋
    quizSectionCount = 0;
    // 매칭 퀴즈 요청 횟수 리셋
    matchingQuizCount = 0;

    // ========== 큐/리스너 리셋 ==========
    // 영상 바뀔 때 큐 비우기
    resetQueue();
    // 이벤트 리스너 리셋
    resetSubtitleSystem();

    // ========== 패널 열려있을 때만 실행 ==========
    if (isPanelOpen) {
      // 영상 페이지면 추천 영상 숨기고 자막 감지
      if (location.href.includes('/watch')) {
        setTimeout(hideRecommendations, 500);
        console.log('페이지 이동 및 구분:', Date.now());
        setTimeout(observeSubtitles, 3000);
      } else {
        // 영상 페이지 아니면 디폴트 페이지로 이동 및 추천 영상 보이고 페이지 감지
        chrome.runtime.sendMessage({
          type: 'GO_TO_DEFAULT'
        });
        showRecommendations();
        setTimeout(observeThumbnails, 3000);
      }
    }

    // ========== 현재 URL 저장 ==========
    lastUrl = location.href;
  }
});


// 이벤트 리스너/자막 리셋
function resetSubtitleSystem() {
  // 전체 자막 데이터 리셋
  allSubtitles = [];
  // 마지막에 전송한 자막 인덱스 리셋
  lastSentIndex = -1;

  // 리스너 중복 방지 리셋
  isTimeUpdateSet = false;
  // 영상 끝 감지 횟수 리셋
  isEndListenerSet = false;

  // 영상 끝 감지 이벤트 리스너 강제 종료
  if (eventEndListener) {
    eventEndListener.removeEventListener('timeupdate', handleTimeUpdate);
    eventEndListener = null;
  }

  // 자막 감지 이벤트 리스너 강제 종료
  if (eventSubtitleListener) {
    eventSubtitleListener.removeEventListener('timeupdate', handleSubtitleSync);
    eventSubtitleListener = null;
  }

  // 구간 반복 이벤트 리스너 강제 종료
  if (eventLoop) {
    eventLoop.removeEventListener('timeupdate', handleLoop);
    // 영상 구간 반복 이벤트 리스너 강제 종료용
    eventLoop = null;
  }
  loopStart = null;
  loopEnd = null;
}


// 영상 시청 페이지면 자막 감지 함수 실행하고 아니면 자막 감지 중지 함수 실행
function observeSubtitles() {
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

  // 현재 영상 가져오기
  const video = document.querySelector('video');
  if (!video) {
    if (retryCount < MAX_RETRY) {
      retryCount++;
      // DOM이 렌더링 안 됐으면 1초 후 재시도
      setTimeout(observeSubtitles, 1000);
    }
    return;
  }
  // 성공하면 리셋
  retryCount = 0;


  // 영상 정보 가져오기
  const videoId = new URL(window.location.href).searchParams.get('v');
  const videoTitle = document.querySelector('#title h1')?.textContent?.trim() || '';
  const channelName = document.querySelector('#channel-name a')?.textContent;
  const duration = video.duration;

  // 퀴즈 페이지 이동(최초 1회)
  if (!isQuiz && videoId && videoTitle && channelName) {
    chrome.runtime.sendMessage({
      type: 'GO_TO_QUIZ',
      videoId: videoId,
      videoTitle: videoTitle,
      channelName: channelName,
      duration: duration
    }).catch((error) => console.log('에러:', error));
    isQuiz = true;
  }

  // 영상 자막 전체 가져오는 함수 실행
  setupSubtitleInterceptor();
}


// XMLHttpRequest 가로채기 설정
// 유튜브가 자막을 요청할 때 응답을 엿봄
// XMLHttpRequest = 브라우저 내장 객체 (네트워크 요청용)
// XMLHttpRequest.prototype = XMLHttpRequest에서 쓸 수 있는 모든 함수들 가져오기
function setupSubtitleInterceptor() {
  // 최초 1회만 발동
  if (isSubtitleInterceptorSet) return;
  isSubtitleInterceptorSet = true;
  console.log('최초 1회', Date.now());

  // ========== inject.js에서 자막 데이터 수신 ==========
  window.addEventListener('message', (event) => {
    // 유튜브에서 온 것만
    if (event.origin !== 'https://www.youtube.com') return;
    // inject.js에서 온 것만
    if (event.data.type !== 'CLIPZY_SUBTITLE_DATA') return;

    // 사이드 패널 닫혀있으면 무시
    if (!isPanelOpen) return;

    // 안에 tStartMs, dDurationMs, segs가 있는 객체들이 담겨있는 events 가져오기
    const response = event.data.response;

    try {
      // 문자열(자막 데이터가 ""로 담김)을 객체로 변환
      const data = JSON.parse(response);

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

      // 자막 데이터에 담긴 텍스트의 줄바꿈을 공백으로 변환
      allSubtitles = allSubtitles.map(sub => ({
        ...sub,
        text: sub.text.replace(/\n/g, ' ').trim()
      }));

      console.log('자막 나옴', allSubtitles.length, '개');

      // 인덱스 리셋
      lastSentIndex = -1;

      // timeupdate 리스너 설정
      setupTimeUpdateListener();
      console.log('타임', Date.now());
      setupEndListener();
      console.log('엔드', Date.now());
    } catch (error) {
      console.log('자막 파싱 실패:', error);
    }
  });
};


// timeupdate 리스너 설정
// 영상 재생 시간이 바뀔 때마다 호출됨
function setupTimeUpdateListener() {
  console.log('설정 전', Date.now());
  // 이미 설정됐으면 스킵
  if (isTimeUpdateSet) return;
  console.log('설정 후', Date.now());

  const video = document.querySelector('video');
  if (!video) return;

  eventSubtitleListener = video;
  video.addEventListener('timeupdate', handleSubtitleSync);
  isTimeUpdateSet = true;

  console.log('timeupdate 리스너 설정', Date.now());
}


// 영상 끝 감지 함수(최초 1회)
function setupEndListener() {
  console.log('설정 전1', Date.now());
  if (!isEndListenerSet) return;
  console.log('설정 후1', Date.now());
  const video = document.querySelector('video');
  if (!video) return;
  console.log('설정 후2', Date.now());
  eventEndListener = video;
  video.addEventListener('timeupdate', handleTimeUpdate);
  isEndListenerSet = true;
  console.log('영상 시간 감지 시작:', Date.now());
}


// 현재 시간에 맞는 자막 찾아서 전송
function handleSubtitleSync(e) {
  console.log('1. handleSubtitleSync 호출');
  const video = e.target;
  // // 영상이 없거나 자막 데이터가 없으면 스킵
  // if (!video || allSubtitles.length === 0) return;

  if (!video) {
    console.log('2. video 없음');
    return;
  }
  
  if (allSubtitles.length === 0) {
    console.log('3. allSubtitles 비어있음');
    return;
  }
  if (document.querySelector('.ad-showing')) {
    console.log('4. 광고 중');
    return;
  }

  // // 광고면 스킵
  // if (document.querySelector('.ad-showing')) return;

  // 현재 시간 실시간 업데이트
  const currentTime = video.currentTime;

  console.log('5. currentTime:', currentTime);
  console.log('6. 첫 자막 시간:', allSubtitles[0]?.startTime, '~', allSubtitles[0]?.endTime);

  // 현재 시간이 자막의 시작~끝 사이에 있으면 allSubtitles에 맞는 인덱스 반환
  const index = allSubtitles.findIndex(sub =>
    currentTime >= sub.startTime && currentTime <= sub.endTime
  );
    console.log('7. 찾은 index:', index);
  console.log('8. lastSentIndex:', lastSentIndex);

  // 자막 데이터가 있고 && 기존 영상이면 전송(새로고침하면 초기화)
  if (index !== -1 && index !== lastSentIndex) {
    // 인덱스 순서로 담기
    const sub = allSubtitles[index];
    console.log('9. 자막 전송:', sub.text);

    // 필요한 정보 가져오기
    const videoId = new URL(window.location.href).searchParams.get('v');
    const videoTitle = document.querySelector('#title h1')?.textContent?.trim() || '';
    const duration = video.duration;

    console.log('자막 전송:', sub.text);

    // 큐 대기열에 넣기
    addToQueue(sub.text, sub.startTime, sub.endTime, videoId, videoTitle, duration);
    
    lastSentIndex = index;
  } else {
    console.log('10. 전송 안 함 - index:', index, 'lastSentIndex:', lastSentIndex);
  }
}


// 영상이 켜지면 실시간 업데이트 및 일시정지 시 같이 멈춤
// 영상 이동 시 초기화 필요
// 리스너 함수는 이벤트가 발생했을 때 이벤트 객체를 첫 번째 인자로 받음
function handleTimeUpdate(e) {
  // 현재 영상 정보 담기
  const video = e.target;
  // 영상 종료까지 남은 시간 담기
  const matchingTiming = video.duration - video.currentTime;
  if ((matchingTiming < 3)) {
    video.removeEventListener('timeupdate', handleTimeUpdate);

    if (matchingQuizCount < 1) {
      video.pause();
      matchingQuiz();
    }
    console.log('끝남 확인1');
    return;
  }
}


// 영상 길이에 맞는 섹션 수 계산
function getSectionCount(duration) {
  // 1분 미만
  if (duration < 60) return 0;
  // 1분~5분 미만
  if (300 < duration <= 60) return 1;
  // 5분~10분 미만
  if (600 < duration <= 300) return 1;
  // 10분~20분 미만
  if (1200 < duration <= 600) return 2;
  // 20분~30분 미만
  if (1800 < duration <= 1200) return 3;
  // 30분 이상
  if (1800 <= duration) return 3;
}


// 큐에 자막 넣기
function addToQueue(text, startTime, endTime, videoId, videoTitle, duration) {
  console.log('addToQueue 실행함:', text, startTime, endTime, videoId, videoTitle, duration);
  // 번역이 너무 느려서 대기중인 자막이 10개 이상이면 오래된 자막부터 버리기
  if (textQueue.length - head >= MAX_QUEUE) {
    // 맨 앞 버리기
    head++;
  }
  textQueue.push({ text, startTime, endTime, videoId, videoTitle, duration });
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
  console.log('processQueue 실행함');
  while (head < textQueue.length) {
    // 맨 앞 꺼내기
    const item = textQueue[head];
    // 에러 발생해도 큐가 멈추지 않게 먼저 증가시키기
    head++;
    try {
      // DeepL 번역 요청 (백그라운드)
      const translation = await fetchTranslation(item.text);
      // 백엔드 DB에 자막 및 번역 데이터 저장
      const quizResponse = await apiFetch(`/videos/${item.videoId}/subtitles`, {
        method: 'POST',
        body: JSON.stringify({
          // videoid: item.videoId,
          title: item.videoTitle,
          text: item.text,
          translation: translation,
          startTime: item.startTime,
          endTime: item.endTime,
          duration: item.duration
        })
      });

      const sectionCount = getSectionCount(item.duration);

      // 섹션 수를 초과하지 않고 트루신호 받으면 (빈칸 / ox) 퀴즈 요청
      if (sectionCount > quizSectionCount && quizResponse.success) {
        startQuizSession()
        console.log('성공하면 퀴즈 요청하기');
      };

      console.log('자막 보내기:', item.videoId, item.text, translation, item.startTime, item.endTime);
      // 사이드 패널 화면에 실시간 자막 업데이트
      chrome.runtime.sendMessage({
        type: 'SUBTITLE_UPDATE',
        videoId: item.videoId,
        text: item.text,
        translation: translation,
        startTime: item.startTime,
        endTime: item.endTime
      });

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


// 영상 바뀔 때 대기열 리셋
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


// 세션 시작하고 (빈칸 / ox) 퀴즈 요청
async function startQuizSession() {
  // 사이드 패널 열려야 실행
  if (!isPanelOpen) return;

  // 퀴즈 요청 중이면 스킵 (중복 방지)
  if (quizRequested) return;

  const videoId = new URL(window.location.href).searchParams.get('v');

  // 같은 영상이면 스킵 (중복 방지)
  if (videoId === lastVideoId) return;

  quizRequested = true;
  lastVideoId = videoId;
  try {
    // // 복습 체크용 퀴즈 이력 조회하기
    // const quizHistory = await apiFetch('/api/quiz/sessions/history', {
    //   method: 'GET'
    // });
    // const sessionType = quizHistory.data.sessions && 

    // 서버에 퀴즈 받기
    const sessionResponse = await apiFetch('/quiz/sessions/generate/section', {
      method: 'POST',
      body: JSON.stringify({
        videoId: videoId,
        sessionType: 'NORMAL',  // 복습 체크하기
        sectionNumber: quizSectionCount,
        // 자막 배열 전송
        // subtitles: collectedSubtitles.map(sub => sub.text)
      })
    });
    quizSectionCount++;

    // 사이드 패널로 퀴즈 전송
    chrome.runtime.sendMessage({
      type: 'QUIZ_READY',
      videoId: videoId,
      sessionId: sessionResponse.data.sessionId,
      quizzes: sessionResponse.data.quizzes,
      totalCount: sessionResponse.data.totalQuizCount || 10
    }).catch((error) => { console.log('이거 에러1:', error) });

    // Chrome Storage에도 저장 (패널 닫혀있을 때 대비)
    chrome.storage.local.set({
      currentSessionId: sessionResponse.data.sessionId,
      currentQuizzes: sessionResponse.data.quizzes
    });

  } catch (error) {
    console.error('퀴즈 세션 실패', error);
  } finally {
    // 실행 후 다시 요청 가능
    quizRequested = false;
  }
}


async function matchingQuiz() {
  // 사이드 패널 열려야 실행
  if (!isPanelOpen) return;

  // 퀴즈 요청 중이면 스킵 (중복 방지)
  if (quizRequested) return;

  const videoId = new URL(window.location.href).searchParams.get('v');

  // 같은 영상이면 스킵 (중복 방지)
  if (videoId === lastVideoId) return;

  quizRequested = true;
  lastVideoId = videoId;

  try {
    // // 복습 체크용 퀴즈 이력 조회하기
    // const quizHistory = await apiFetch('/api/quiz/sessions/history', {
    //   method: 'GET'
    // });
    // const sessionType = quizHistory.data.sessions && 

    // 서버에 매칭 퀴즈 받기
    const matchingResponse = await apiFetch('/quiz/sessions/generate/matching', {
      method: 'POST',
      body: JSON.stringify({
        videoId: videoId,
        sessionType: 'NORMAL',  // 복습 체크하기
        // sectionNumber: quizSectionCount,
        // 자막 배열 전송
        // subtitles: collectedSubtitles.map(sub => sub.text)
      })
    });

    matchingQuizCount++;

    // 사이드 패널로 퀴즈 전송
    chrome.runtime.sendMessage({
      type: 'QUIZ_READY',
      videoId: videoId,
      sessionId: matchingResponse.data.sessionId,
      quizzes: matchingResponse.data.quizzes,
      totalCount: matchingResponse.data.totalQuizCount || 10
    }).catch((error) => { console.log('이거 에러2:', error) });

    // Chrome Storage에도 저장 (패널 닫혀있을 때 대비)
    chrome.storage.local.set({
      currentSessionId: matchingResponse.data.sessionId,
      currentQuizzes: matchingResponse.data.quizzes
    });
  } catch (error) {
    console.error('퀴즈 세션 실패', error);
  } finally {
    // 실행 후 다시 요청 가능
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
const handleLoop = (e) => {
  const video = e.target;
  // const video = document.querySelector('video');
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
      const response = await apiFetch(`/badges/video/${videoId}`);
      // 뱃지가 어떻게 오는지에 따라 수정예정(아마 결과에 맞는 뱃지 이미지를 붙일 듯)
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


// 뱃지 제거 함수
function removeBadges() {
  const badges = document.querySelectorAll('.clip-test-badge');
  badges.forEach(badge => badge.remove());

  // 만약 숨겨야 한다면
  // const badges = document.querySelectorAll('.clip-test-badge');
  // badges.forEach(badge => badge.style.display = 'none');

  // 숨겼을 때 다시 보여야 한다면
  // const badges = document.querySelectorAll('.clip-test-badge');
  // badges.forEach(badge => badge.style.display = 'block');
}


// 리셋
// 페이지 로드 대기 후 실행
// init()에서 페이지 종류에 따라 분기
function init() {
  console.log('콘텐츠 리셋1:', Date.now());
  // 재시도 횟수 리셋
  retryCount = 0;
  // 퀴즈 요청 상태 리셋
  quizRequested = false;
  // 저장된 영상ID 리셋
  lastVideoId = '';
  // 영상 제목과 아이디, 채널명 전송 횟수 리셋
  isQuiz = false;
  // 현재 퀴즈 섹션 수 리셋
  quizSectionCount = 0;
  // 매칭 퀴즈 요청 횟수 리셋
  matchingQuizCount = 0;

  // ========== 큐 리셋 ==========
  resetQueue();

  // ========== 리스너/자막 리셋 =========
  resetSubtitleSystem();

  // ========== 페이지별 처리 ==========
  // 영상 페이지일 경우
  if (window.location.href.includes('/watch')) {
    // 추천 영상 숨기기
    setTimeout(hideRecommendations, 500);
    // 현재 영상이 재생중이면 자막 감시하고 아니면 자막 감시 중지
    console.log('리셋 후 감시 여부 확인:', Date.now());
    setTimeout(observeSubtitles, 3000);
    // 영상 페이지가 아닐 경우
  } else {
    // 추천 영상 보이기
    showRecommendations();
    // 뱃지 붙이기
    setTimeout(observeThumbnails, 3000);
  }
}