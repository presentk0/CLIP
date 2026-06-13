import { useState, useCallback } from 'react';
import { OnboardingContext } from './OnboardingContextDefinition';

const INITIAL_STATE = {
  learningGoal: null,
  difficultyLevel: null,
  absoluteLevel: null,
};

export function OnboardingProvider({ children }) {
  const [onboardingData, setOnboardingData] = useState(INITIAL_STATE);

  const setLearningGoal = useCallback((value) => {
    setOnboardingData((prev) => ({ ...prev, learningGoal: value }));
  }, []);

  const setDifficultyLevel = useCallback((value) => {
    setOnboardingData((prev) => ({ ...prev, difficultyLevel: value }));
  }, []);

  const setAbsoluteLevel = useCallback((value) => {
    setOnboardingData((prev) => ({ ...prev, absoluteLevel: value }));
  }, []);

  const resetOnboarding = useCallback(() => {
    setOnboardingData(INITIAL_STATE);
  }, []);

  return (
    <OnboardingContext.Provider
      value={{
        onboardingData,
        setLearningGoal,
        setDifficultyLevel,
        setAbsoluteLevel,
        resetOnboarding,
      }}
    >
      {children}
    </OnboardingContext.Provider>
  );
}