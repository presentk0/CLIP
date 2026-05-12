// 서버 주소가 생기면 false로 바꾸고 URL을 실제 서버 주소로 변경하기
const IS_MOCK = false; 
const BASE_URL = 'https://clip-server.com/api';

export const apiFetch = async (endpoint, options = {}) => {
  if (IS_MOCK) {
    // 네트워크 지연 시뮬레이션
    await new Promise(resolve => setTimeout(resolve, 500)); 



    // 기본 응답
    return { success: true };
  }

  // 실제 서버 통신 로직 (나중에 사용)
  // const { accessToken } = await chrome.storage.local.get(['accessToken']);
  // response = 서버 응답 객체 (아직 텍스트 상태)
  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      // 'Authorization': `Bearer ${accessToken}`,
      ...options.headers,
    }
  });

  // result = JSON으로 파싱된 데이터
  const result = await response.json();
  if (!result.success) throw new Error(result.error?.message || 'API Error');
  return result;
};