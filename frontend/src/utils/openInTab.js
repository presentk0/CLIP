/* global chrome */
import { log } from "./logger";

// 사이드패널/팝업은 유지하고 메인 탭의 URL을 변경 (확장프로그램용)
export const openInTab = (url) => {
  // typeof chrome !== 'undefined' = chrome 객체 체크 (크롬 확장프로그램 환경인지 체크용)
  // chrome.tabs?.update = chrome.tabs.update 함수 체크 (tabs 권한 있는지 체크용)
  if (typeof chrome !== 'undefined' && chrome.tabs?.update) {
    // 현재 활성 탭의 URL을 새 URL로 변경
    chrome.tabs.update({ url });
  } else {
    log.debug('chrome.tabs API를 사용할 수 없습니다');
  }
};

// 유튜브 영상 페이지로 이동
export const openYoutubeVideo = (videoId) => {
  openInTab(`https://youtube.com/watch?v=${videoId}`);
};