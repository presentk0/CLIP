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
// 프로덕션 모드에선 정보 x
// debug: (...args) => console.log(...args) 는 logger.js에서 console.log 를 호출하기에 위치가 logger.js로 잡힘
// debug: console.log.bind(console) 는 .bind 로 console.log 함수 자체를 log.debug 이름으로 저장
// bind는 함수를 새로 만들지 않고 그대로 이름표만 붙이기
export const log = IS_DEV
  ? {
      debug: console.log.bind(console),
      warn: console.warn.bind(console),
      error: console.error.bind(console),
    }
  : {
      debug: () => {},
      warn: () => {},
      error: () => {},
    };