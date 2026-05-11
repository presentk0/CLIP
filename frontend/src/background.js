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
  console.log('패널 연결1', Date.now());
  if (port.name === 'sidepanel') {
    console.log('패널 연결2', Date.now());
    // 연결 끊김 감지
    port.onDisconnect.addListener(() => {
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        console.log('패널 연결 끊김1', Date.now());
        if (tabs[0]) {
          console.log('패널 연결 끊김2', Date.now());
          chrome.tabs.sendMessage(tabs[0].id, { type: 'PANEL_CLOSED' }).catch(() => {});
        }
      });
    });
  }
});



// 번역 요청 처리
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  console.log('백그라운드 메시지 도착:', Date.now(), message.type);
  if (message.type === 'TRANSLATE') {
    const body = JSON.parse(message.options.body);
    console.log('백그라운드 요청 endpoint:', Date.now(), message.endpoint);
    console.log('백그라운드 번역 요청 영상:', Date.now(), body.videoId);
    console.log('백그라운드 자막 개수:', Date.now(), body.subtitleRequests.length);

    const fetchTranslate = async (retryCount = 0) => {
      // 최대 재시도 횟수
      const MAX_RETRY = 3;
      // 재시도 간격 (2초)
      const RETRY_DELAY = 2000;
      try {
        const translateData = await apiFetch(message.endpoint, message.options);
        console.log('백그라운드 번역 응답:', Date.now(), translateData);
        sendResponse(translateData);
      } catch (error) {
        console.log('백그라운드 번역 에러:', Date.now(), error);
        
        // 재시도 로직
        if (retryCount < MAX_RETRY) {
          console.log(`번역 재시도 (${retryCount + 1}/${MAX_RETRY})...`);
          setTimeout(() => {
            fetchTranslate(retryCount + 1);
          }, RETRY_DELAY);
        } else {
          console.log('번역 최대 재시도 초과');
          sendResponse({ success: false, error: error.message });
        }
      }
    };


    // translateText(message.text).then((result) => {
    //   // 사이드 패널 전용
    //   if (message.source === 'sidepanel'){
    //     sendResponse(result);
    //   }
    //   // content 전용
    //   if (message.source === 'content' && sender.tab?.id) {
    //     chrome.tabs.sendMessage(sender.tab.id, {
    //       type: 'TRANSLATE_RESULT',
    //       text: message.text,
    //       translation: result.translatedText
    //     });
    //   }
    // });
    fetchTranslate();
    console.log('백그라운드 번역 완료:', Date.now());
    // 비동기 응답
    return true;
  }
});



// async function translateText(text) {
//   try {
//     const response = await fetch('https://api-free.deepl.com/v2/translate', {
//       method: 'POST',
//       headers: {
//         // API 키 형식 'Authorization': 'DeepL-Auth-Key xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx:fx'
//         'Authorization': 'DeepL-Auth-Key 여기에 입력하기',
        
//         // JSON으로 변경
//         'Content-Type': 'application/json',
//       },
//       body: JSON.stringify({
//         // 배열
//         text: [text],
//         target_lang: 'KO'
//       })
//     });

//     const data = await response.json();
//     return { translatedText: data.translations[0].text };
//   } catch (error) {
//     console.log('BG: API 에러 ->', error);
//     return { translatedText: null };
//   }
// }