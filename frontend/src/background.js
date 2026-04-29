/* global chrome */

// 아이콘 클릭 시 사이드 패널 열리게 설정
chrome.sidePanel.setPanelBehavior({ openPanelOnActionClick: true });


const YOUTUBE_ORIGIN = 'https://www.youtube.com';

// 탭이 업데이트될 때마다 실행 (주소 이동, 새로고침 등)
chrome.tabs.onUpdated.addListener(async (tabId, info, tab) => {
  if (!tab.url) return;

  const url = new URL(tab.url);

  // 현재 주소가 유튜브라면 사이드 패널 활성화
  if (url.origin === YOUTUBE_ORIGIN) {
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
  console.log('패널 연결1');
  if (port.name === 'sidepanel') {
    console.log('패널 연결2');
    // 연결 끊김 감지
    port.onDisconnect.addListener(() => {
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        console.log('패널 연결 끊김1');
        if (tabs[0]) {
          console.log('패널 연결 끊김2');
          chrome.tabs.sendMessage(tabs[0].id, { type: 'PANEL_CLOSED' }).catch(() => {});
        }
      });
    });
  }
});



// 번역 요청 처리
chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
  if (message.type === 'TRANSLATE') {
    translateText(message.text).then((result) => {
      // 사이드 패널 전용
      if (message.source === 'sidepanel'){
        sendResponse(result);
      }
      // content 전용
      if (message.source === 'content' && sender.tab?.id) {
        chrome.tabs.sendMessage(sender.tab.id, {
          type: 'TRANSLATE_RESULT',
          text: message.text,
          translation: result.translatedText
        });
      }
    });
    // 비동기 응답
    return true;
  }
});



async function translateText(text) {
  try {
    const response = await fetch('https://api-free.deepl.com/v2/translate', {
      method: 'POST',
      headers: {
        // API 키 형식 'Authorization': 'DeepL-Auth-Key xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx:fx'
        'Authorization': 'DeepL-Auth-Key 여기에 입력하기',
        
        // JSON으로 변경
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        // 배열
        text: [text],
        target_lang: 'KO'
      })
    });

    const data = await response.json();
    return { translatedText: data.translations[0].text };
  } catch (error) {
    console.log('BG: API 에러 ->', error);
    return { translatedText: null };
  }
}