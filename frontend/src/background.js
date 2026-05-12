/* global chrome */
import { apiFetch } from "./utils/api";

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
          chrome.tabs.sendMessage(tabs[0].id, { type: 'PANEL_CLOSED' }).catch(() => {});
        }
      });
    });
  }
});



// 번역 요청 처리
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'TRANSLATE') {
    const fetchTranslate = async (retryCount = 0) => {
      // 최대 재시도 횟수
      const MAX_RETRY = 3;
      // 재시도 간격 (2초)
      const RETRY_DELAY = 2000;
      try {
        const translateData = await apiFetch(message.endpoint, message.options);
        sendResponse(translateData);
      } catch (error) {
        console.log('백그라운드 번역 에러:', Date.now(), error);
        
        // 재시도 로직
        if (retryCount < MAX_RETRY) {
          setTimeout(() => {
            fetchTranslate(retryCount + 1);
          }, RETRY_DELAY);
        } else {
          console.log('번역 최대 재시도 초과');
          sendResponse({ success: false, error: error.message });
        }
      }
    };

    fetchTranslate();
    // 비동기 응답
    return true;
  }
});