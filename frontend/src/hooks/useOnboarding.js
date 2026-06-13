import { useContext } from 'react';
import { OnboardingContext } from '../contexts/OnboardingContextDefinition';

export function useOnboarding() {
  const ctx = useContext(OnboardingContext);
  if (!ctx) {
    throw new Error('useOnboarding은 OnboardingProvider 내부에서만 사용 가능합니다');
  }
  return ctx;
}