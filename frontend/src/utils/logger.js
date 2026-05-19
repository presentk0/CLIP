/* global chrome */

export const IS_DEV = (() => {
  // Vite 모드 체크
  if (import.meta.env?.MODE === 'development') return true;
  
  // 확장프로그램 컨텍스트인지 확인
  if (typeof chrome === 'undefined' || !chrome.runtime) return false;
  
  try {
    // update_url 체크 (개발자 모드 감지)
    return !('update_url' in chrome.runtime.getManifest());
  } catch {
    return false;
  }
})();

// 로거 추상화 (런타임 방어)
export const log = {
  debug: (...args) => {
    if (IS_DEV) console.log(...args);
  },
  warn: (...args) => {
    if (IS_DEV) console.warn(...args);
  },
  error: (message, error) => {
    if (IS_DEV) {
      console.error(message, error);
    } else {
      // 프로덕션: 메시지만, 상세 정보 X
      console.error(message);
    }
  },
};