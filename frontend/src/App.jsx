/* global chrome */

import { useState, useEffect, useCallback } from 'react';
import { MemoryRouter, Routes, Route, useNavigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';

import QuizPage from './pages/QuizPage/QuizPage';
import DefaultPage from './pages/DefaultPage/DefaultPage';
import SettlementPage from './pages/SettlementPage/SettlementPage';
import LoginPage from './pages/LoginPage/LoginPage';
import { MyPage } from './pages/MyPage/MyPage';
import { AiChatPage } from './pages/AiChatPage/AiChatPage';

import ProtectedRoute from './components/ProtectedRoute';
import { log } from './utils/logger';
import './App.css';
import { useAuth } from './hooks/useAuth';
import { Navigate } from 'react-router-dom';

// 온보딩 페이지들 추가
import TermsAgreementPage from './pages/TermsAgreementPage/TermsAgreementPage';
import LevelSelectPage from './pages/LevelSelectPage/LevelSelectPage';
import GoalSelectPage from './pages/GoalSelectPage/GoalSelectPage';
import DifficultySelectPage from './pages/DifficultySelectPage/DifficultySelectPage';

import { OnboardingProvider } from './contexts/OnboardingProvider';
import FeedbackPage from './pages/FeedbackPage/FeedbackPage';
import { loadUserData, removeUserData } from './utils/userStorage';





// 온보딩 체크용
function RequireOnboarding({ children }) {
  const { needsOnboarding } = useAuth();
  
  if (needsOnboarding) {
    // 약관 페이지로 보내기
    return <Navigate to="/onboarding/terms" replace />;
  }
  return children;
}



// 실제 라우팅 + 메시지 처리
function AppContent() {
  const navigate = useNavigate();

  // // 퀴즈 결과 데이터 (정산 페이지에서 사용)
  // const [settlementData, setSettlementData] = useState(null);
  // 현재 영상에 퀴즈 중간 저장 데이터가 있는지 확인용
  const [videoId, setVideoId] = useState(null);
  // 단어 수집으로 보낼 영상제목
  const [videoTitle, setVideoTitle] = useState('');
  // 영상 채널명
  const [channelName, setChannelName] = useState('');
  // 영상 전체 길이
  const [duration, setDuration] = useState(0);
  // 영상 썸네일 url
  const [thumbnailUrl, setThumbnailUrl] = useState(0);
  // 영상 바뀌면 퀴즈페이지 언마운트
  const [resetKey, setResetKey] = useState(0);


  // useCallback(의존성이 안 바뀌면 같은 함수 재사용)으로 감싸기
  // 영상 이동 시 퀴즈페이지 리셋 후 디폴트페이지로 이동
  const goToDefault = useCallback(() => {
    setResetKey(prev => prev + 1);
    navigate('/');
  }, [navigate]);



  // 사이드패널 열림 감지 및 연결
  useEffect(() => {
    // 연결 실패 시 재시도 횟수
    let retryCount = 0;
    // 최대 10번까지 재시도
    const maxRetry = 10;  
    // 패널 열림 중복 요청 방지
    let isSent = false;
    // 백그라운드와 연결 (사이드패널 닫힘 감지용)
    chrome.runtime.connect({ name: 'sidepanel' });

    // content.jsx에 사이드패널 열림 신호 보내기
    const sendPanelOpened = () => {
      if (isSent) return;

      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        if (tabs[0]) {
          chrome.tabs.sendMessage(tabs[0].id, { type: 'PANEL_OPENED' })
          .then(() => {
            isSent = true;
          })
          .catch((error) => {
            log.debug('패널 열림 실패 신호', error.message);
            retryCount++;
            if (retryCount < maxRetry) {
              // 실패하면 1초 후 재시도
              setTimeout(sendPanelOpened, 1000);
            } else {
              log.error('연결 실패 - 페이지 새로고침 필요', error.message);
            }
          });
        }
      })
    };
    sendPanelOpened();

    // content.js 새로고침 감지
    const handleContentMessage = (message) => {

  

      if (message.type === 'CONTENT_LOADED') {
    // 성공한 적 있으면 바로 1번만 시도
    if (isSent) {
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        if (tabs[0]) {
          chrome.tabs.sendMessage(tabs[0].id, { type: 'PANEL_OPENED' })
            .catch((error) => log.error('재전송 실패', error.message));
        }
      });
    } else {
      // 아직 성공 못 했으면 재시도 로직
      retryCount = 0;
      sendPanelOpened();
    }
      }
    };

    chrome.runtime.onMessage.addListener(handleContentMessage);
    return () =>  {
      chrome.runtime.onMessage.removeListener(handleContentMessage);
    }
  }, []);



  // 퀴즈페이지로 이동 및 영상 아이디와 제목, 전체 길이 저장
  // 이게 문제 같음 오류
  useEffect(() => {
    // const handleMessage = async (message, sender, sendResponse) => {
      

        const handleGoToQuiz = async (message) => {
    const saved = await loadUserData('quizState');
    if (saved?.videoId !== message.videoId) {
      await removeUserData('quizState');
    }
    
    setVideoId(message.videoId);
    setVideoTitle(message.videoTitle);
    setChannelName(message.channelName);
    setThumbnailUrl(message.thumbnailUrl);
    setDuration(message.duration);
    
    setResetKey(prev => prev + 1);
    navigate('/quiz');
  };

    // 동기 리스너
  const handleMessage = (message) => {

    switch (message.type) {
      case 'GO_TO_QUIZ':
        handleGoToQuiz(message);  // 비동기 호출, 결과 안 기다림
        break;
      case 'GO_TO_DEFAULT':
        goToDefault();
        break;
      case 'GO_TO_LOGIN':
        navigate('/login');
        break;
      default:
        return;  // 다른 메시지는 무시
    }
  };


      // if (message.type === 'GO_TO_QUIZ') {
      //   // 새 퀴즈면 이전 진행 상태 삭제
      //   const saved = await loadUserData('quizState');
      //   if (saved?.videoId !== message.videoId) {
      //     // 다른 영상이면 삭제 (오류 나중에 퀴즈여부 묻기)
      //     await removeUserData('quizState');
      //   }


      //   setVideoId(message.videoId);
      //   setVideoTitle(message.videoTitle);
      //   setChannelName(message.channelName);
      //   setThumbnailUrl(message.thumbnailUrl)
      //   setDuration(message.duration);
      //   // 강제 마운트
      //   setResetKey(prev => prev + 1);
      //   navigate('/quiz');
      // }
      // if (message.type === 'GO_TO_DEFAULT') {
      //   goToDefault();
      // }
      // // 로그인 페이지로 이동 신호
      // if (message.type === 'GO_TO_LOGIN') {
      //   navigate('/login');
      // }
    // };
    chrome.runtime.onMessage.addListener(handleMessage);
    return () => chrome.runtime.onMessage.removeListener(handleMessage);
  }, [navigate, goToDefault]);



  // 영상 이동 시 퀴즈페이지 리셋 후 디폴트페이지로 이동
  // const goToDefault = () => {
  //   setResetKey(prev => prev + 1);
  //   navigate('/');
  // };



  // // 정산 페이지로 이동
  // const handleSettlement = (data) => {
  //   // 데이터 저장
  //   setSettlementData(data);
  //   // 정산 페이지로 이동
  //   navigate('/settlement');
  // };



  // 디폴트 페이지로 이동 (중간 이탈)
  const handleExit = () => {
    navigate('/');
  };


  // 마이 페이지로 이동
  const handleMyPage = () => {
    navigate('/my');
  };

  // // ai 채팅방으로 이동
  // const handleAiChat = () => {
  //   navigate('/ai');
  // };


  return (
    <div style={{
      width: '100%',
      // height: '100%',
      // 최대 너비
      maxWidth: '402px',
      // 가로 가운데
      margin: '0 auto',
    }}>
      <Routes>
        <Route 
          path="/" 
          element={
            <ProtectedRoute>
              <RequireOnboarding>
                <DefaultPage 
                  onMyPage={handleMyPage}
                  // onAiChatPage={handleAiChat}
                />
              </RequireOnboarding>
            </ProtectedRoute>
          } 
        />
        <Route path="/login" element={<LoginPage />} />
          {/* 온보딩 라우트 추가 (ProtectedRoute로 감싸야 로그인된 유저만 접근) */}
  <Route 
    path="/onboarding/terms" 
    element={
      <ProtectedRoute>
        <TermsAgreementPage />
      </ProtectedRoute>
    } 
  />
  <Route 
    path="/onboarding/level" 
    element={
      <ProtectedRoute>
        <LevelSelectPage />
      </ProtectedRoute>
    } 
  />
  <Route 
    path="/onboarding/goal" 
    element={
      <ProtectedRoute>
        <GoalSelectPage />
      </ProtectedRoute>
    } 
  />
  <Route 
    path="/onboarding/difficulty" 
    element={
      <ProtectedRoute>
        <DifficultySelectPage />
      </ProtectedRoute>
    } 
  />
        <Route 
          path="/quiz" 
          element={
            <ProtectedRoute>
              <QuizPage
                key={resetKey}
                videoId={videoId}
                videoTitle={videoTitle}
                channelName={channelName}
                thumbnailUrl={thumbnailUrl}
                duration={duration}
              />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/ai" 
          element={
            <ProtectedRoute>
              <AiChatPage
              />
            </ProtectedRoute>
          }
        />
        <Route 
        path="/feedback" 
        element={
          <ProtectedRoute>
            <FeedbackPage />
          </ProtectedRoute>
        } />
        <Route 
          path="/my" 
          element={
            <ProtectedRoute>
              <MyPage 
                onExitPage={handleExit}
              />
            </ProtectedRoute>
          } 
        />
      </Routes>
    </div>
  );
}


// Router + AuthProvider로 감싸기
function App() {
  return (
    <MemoryRouter>
      <AuthProvider>
        <OnboardingProvider>
          <AppContent />
        </OnboardingProvider>
        
      </AuthProvider>
    </MemoryRouter>
  );
}

export default App;