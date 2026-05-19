import { useContext } from 'react';
import { AuthContext } from '../contexts/AuthContextDefinition';

// Fast refresh only works when a file only exports components. Use a new file to share constants or functions between components.
// Fast Refresh는 파일이 컴포넌트만 export할 때만 작동합니다. 상수나 함수를 공유하려면 새 파일을 사용하세요.
// Vite의 Fast Refresh(HMR, 핫 리로드) 는 파일이 컴포넌트만 내보낼 때 제대로 작동해요.
// 컴포넌트 + 함수가 섞이면 코드 수정 시 전체 페이지 새로고침이 일어나서 개발 경험이 나빠집니다.
// 해결 방법 = 파일 분리

export function useAuth() {
  // 저장소에서 데이터 꺼내기
  // useContext(AuthContext)는 가장 가까운 부모의 <AuthContext.Provider>를 찾아서 값을 가져옴
  // Provider가 없으면? → null 반환 → 에러
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}