/* global chrome */
import { log } from './logger'; // 기존 logger 임포트

export const sendMessage = (message, callback) => {
  try {
    chrome.runtime.sendMessage(message, (response) => {
      if (chrome.runtime.lastError) {
        log.debug('메시지 전송 실패:', chrome.runtime.lastError.message);
        return;
      }
      if (callback) callback(response);
    });
  } catch (e) {
    log.debug('sendMessage 에러:', e);
  }
};