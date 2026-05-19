/* global chrome */

import { useState, useEffect, useCallback } from 'react';
import { MemoryRouter, Routes, Route, useNavigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';

import QuizPage from './components/QuizPage';
import DefaultPage from './components/DefaultPage';
import SettlementPage from './components/SettlementPage';
import LoginPage from './components/LoginPage';
import ProtectedRoute from './components/ProtectedRoute';
import { log, IS_DEV } from './utils/logger';
import './App.css';


// 실제 라우팅 + 메시지 처리
function AppContent() {
  const navigate = useNavigate();

  // 퀴즈 결과 데이터 (정산 페이지에서 사용)
  const [settlementData, setSettlementData] = useState(null);
  // 현재 영상에 퀴즈 중간 저장 데이터가 있는지 확인용
  const [videoId, setVideoId] = useState(null);
  // 단어 수집으로 보낼 영상제목
  const [videoTitle, setVideoTitle] = useState('');
  // 영상 채널명
  const [channelName, setChannelName] = useState('');
  // 영상 전체 길이
  const [duration, setDuration] = useState(0);
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
              log.debug('연결 실패 - 페이지 새로고침 필요', error.message);
            }
          });
        }
      })
    };
    sendPanelOpened();

    // content.js 새로고침 감지
    const listener = (message) => {
      if (message.type === 'CONTENT_LOADED') {
    // 성공한 적 있으면 바로 1번만 시도
    if (isSent) {
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        if (tabs[0]) {
          chrome.tabs.sendMessage(tabs[0].id, { type: 'PANEL_OPENED' })
            .catch((error) => log.debug('재전송 실패', error.message));
        }
      });
    } else {
      // 아직 성공 못 했으면 재시도 로직
      retryCount = 0;
      sendPanelOpened();
    }
      }
    };

    chrome.runtime.onMessage.addListener(listener);
    return () =>  {
      chrome.runtime.onMessage.removeListener(listener);
    }
  }, []);



  // 퀴즈페이지로 이동 및 영상 아이디와 제목, 전체 길이 저장
  useEffect(() => {
    const listener = (message) => {
      if (message.type === 'GO_TO_QUIZ') {
        setVideoId(message.videoId);
        setVideoTitle(message.videoTitle);
        setChannelName(message.channelName);

        // 최종정산 페이지 전용
        setDuration(message.duration);
        navigate('/quiz');
      }
      if (message.type === 'GO_TO_DEFAULT') {
        goToDefault();
      }
      // 로그인 페이지로 이동 신호
      if (message.type === 'GO_TO_LOGIN') {
        navigate('/login');
      }
    };
    chrome.runtime.onMessage.addListener(listener);
    return () => chrome.runtime.onMessage.removeListener(listener);
  }, [navigate, goToDefault]);



  // 영상 이동 시 퀴즈페이지 리셋 후 디폴트페이지로 이동
  // const goToDefault = () => {
  //   setResetKey(prev => prev + 1);
  //   navigate('/');
  // };



  // 정산 페이지로 이동
  const handleSettlement = (data) => {
    // 데이터 저장
    setSettlementData(data);
    // 정산 페이지로 이동
    navigate('/settlement');
  };



  // 디폴트 페이지로 이동 (중간 이탈)
  const handleExit = () => {
    navigate('/');
  };




  // // 로그아웃
  // const handleLogout = async () => {
  //   await logout();
  //   // navigate 안 해도 됨! ProtectedRoute가 자동으로 /login 보냄
  //   // 만약 명시적으로 하고 싶으면:
  //   // navigate('/login');
  // };


  // // 로그아웃 시 로그인 페이지로 이동
  // const handleLogout = async () => {
  //   try {
  //     await apiFetch('/auth/logout', { method: 'POST' });
  //   } catch (error) {
  //     console.warn('로그아웃 실패', error);
  //   }
  //   setUser(null);
  //   await chrome.runtime.sendMessage({ type: 'CLEAR_AUTH' });
  //   // navigate는 여기서 못 함 (Context는 라우터 밖에 있을 수 있어서)
  // };




  return (
    <div style={{
      width: '100%',
      height: '100%',
      // 최대 너비
      maxWidth: '402px',
    }}>
      <Routes>
        <Route 
          path="/" 
          element={
            <ProtectedRoute>
              <DefaultPage />
            </ProtectedRoute>
          } 
        />
        <Route path="/login" element={<LoginPage />} />
        <Route 
          path="/quiz" 
          element={
            <ProtectedRoute>
              <QuizPage
                key={resetKey}
                videoId={videoId}
                videoTitle={videoTitle}
                duration={duration}
                onExitPage={handleExit}
                onSettlementPage={handleSettlement}
              />
            </ProtectedRoute>
          } 
        />
        <Route 
          path="/settlement" 
          element={
            <ProtectedRoute>
              <SettlementPage
                data={settlementData}
                videoId={videoId}
                videoTitle={videoTitle}
                channelName={channelName}
                duration={duration}
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
        <AppContent />
      </AuthProvider>
    </MemoryRouter>
  );
}

export default App;