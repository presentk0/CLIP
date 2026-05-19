import { log, IS_DEV } from "./logger";

export function handleApiError(error, context, errorMap = {}) {
  log.error(`[${context}]`, error);
  
  const safeMessage = errorMap[error.code] || '오류가 발생했습니다';
  
  if (IS_DEV && error.code) {
    return `${safeMessage} [${error.code}]`;
  }
  return safeMessage;
}