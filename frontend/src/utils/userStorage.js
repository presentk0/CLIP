/* global chrome */
import { log } from "./logger";

// 현재 로그인 유저 ID 가져오기
// 보안때문에 토큰을 content에서 읽는 대신 백그라운드로 메시지 보내기
async function getCurrentUserId() {
  try {
    const response = await chrome.runtime.sendMessage({ type: 'GET_USER_ID' });
    return response?.success ? response.userId : null;
  } catch (error) {
    log.debug('userId 조회 실패', error);
    return null;
  }
}

// 유저별 키로 저장
export async function saveUserData(key, value) {
  const userId = await getCurrentUserId();
  if (!userId) return;
  await chrome.storage.local.set({ [`${key}_${userId}`]: value });
}

// 유저별 키로 읽기
export async function loadUserData(key) {
  const userId = await getCurrentUserId();
  if (!userId) return undefined;
  const result = await chrome.storage.local.get(`${key}_${userId}`);
  return result[`${key}_${userId}`];
}

// 유저별 키 삭제
export async function removeUserData(key) {
  const userId = await getCurrentUserId();
  if (!userId) return;
  await chrome.storage.local.remove(`${key}_${userId}`);
}