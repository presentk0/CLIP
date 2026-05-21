import { log, IS_DEV } from "./logger";

export function handleApiError(error, context, errorMap = {}) {
  log.error(`[${context}]`, error);
  
  // 네트워크 에러 (서버 도달 못 함)
  if (error instanceof TypeError && error.message.includes('fetch')) {
    return '서버에 연결할 수 없습니다';
  }
  
  // HTTP 에러 (백엔드 응답)
  const errorCode = 
    // axios + 백엔드
    error?.response?.data?.code ||
    // 직접 던진 응답
    error?.data?.code ||
    // 일반 에러
    error?.code ||
    null;
  
  const safeMessage = errorMap[errorCode] || '오류가 발생했습니다';
  
  if (IS_DEV && errorCode) {
    return `${safeMessage} [${errorCode}]`;
  }
  return safeMessage;
}