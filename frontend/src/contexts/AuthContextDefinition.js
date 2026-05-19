// 파일 분리 권장 경고
// Fast refresh only works when a file only exports components. Move your React context(s) to a separate file.
// Fast Refresh(빠른 새로고침)는 한 파일에서 컴포넌트만 export할 때 작동해요. Context는 다른 파일로 옮기세요.
import { createContext } from 'react';

export const AuthContext = createContext(null);