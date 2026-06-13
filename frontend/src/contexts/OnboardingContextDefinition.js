// Vite의 Fast Refresh는 한 파일에서 컴포넌트만 export하거나, 컴포넌트가 아닌 것만 export해야 함
// 같은 파일에 OnboardingProvider(컴포넌트) + useOnboarding(훅) 섞여 있으면 에러

import { createContext } from 'react';

export const OnboardingContext = createContext(null);