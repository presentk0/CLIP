/* global chrome */

import { useState, useEffect } from 'react';
import QuizPage from './components/QuizPage';
import DefaultPage from './components/DefaultPage';
import SettlementPage from './components/SettlementPage';
import './App.css';


function App() {
  // 현재 페이지 번호 (0: 기본, 1: 퀴즈, 2: 정산)
  const [move, setMove] = useState(0);
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
        console.log('패널 열림 신호1');
        if (tabs[0]) {
          chrome.tabs.sendMessage(tabs[0].id, { type: 'PANEL_OPENED' })
          .then(() => {
            isSent = true;
          })
          .catch((error) => {
            console.log('패널 열림 실패 신호', error.message);
            retryCount++;
            if (retryCount < maxRetry) {
              console.log('패널 열림 재시도');
              // 실패하면 1초 후 재시도
              setTimeout(sendPanelOpened, 1000);
            } else {
              console.log('연결 실패 - 페이지 새로고침 필요', error.message);
            }
          });
        }
      })
    };

    sendPanelOpened();

    // content.js 새로고침 감지
    const listener = (message) => {
      console.log('메시지 받음2:', message.type, Date.now());
      if (message.type === 'CONTENT_LOADED') {
        // isSent = false;
        // retryCount = 0;
        // sendPanelOpened();
        console.log('새로고침 받음', Date.now());

    // 성공한 적 있으면 바로 1번만 시도
    if (isSent) {
      console.log('새로고침 시도1', Date.now());
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        if (tabs[0]) {
          console.log('새로고침 시도2', Date.now());
          chrome.tabs.sendMessage(tabs[0].id, { type: 'PANEL_OPENED' })
            .catch((error) => console.log('재전송 실패', error.message));
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
    return () =>  chrome.runtime.onMessage.removeListener(listener);
  }, []);


  // 퀴즈페이지로 이동 및 영상 아이디와 제목, 전체 길이 저장
  useEffect(() => {
    console.log('App 시작:', Date.now());
    const listener = (message) => {
      console.log('메시지 받음:', message.type, Date.now());
      if (message.type === 'GO_TO_QUIZ') {
        console.log('퀴즈 페이지 이동1:', Date.now(), message.videoId);
        // if (isChange !== message.videoId) {setIsChange(true)};
        setVideoId(message.videoId);
        setVideoTitle(message.videoTitle);
        setChannelName(message.channelName);

        // 최종정산 페이지 전용
        setDuration(message.duration);
        setMove(1);
      }
      // if (message.type === 'VIDEO_DURATION') {
      //   console.log('퀴즈 페이지 이동2:', Date.now());
      //   setDuration(message.duration);
      //   setMove(1);
      // }
      if (message.type === 'GO_TO_DEFAULT') {
        console.log('언마운트 후 디폴트로', Date.now());
        goToDefault();
      }
    };
    chrome.runtime.onMessage.addListener(listener);
    return () => chrome.runtime.onMessage.removeListener(listener);
  }, []);


  // 영상 이동 시 퀴즈페이지 리셋 후 디폴트페이지로 이동
  const goToDefault = () => {
    setResetKey(prev => prev + 1);
    setMove(0);
  };



  // 정산 페이지로 이동
  const handleSettlement = (data) => {
    // 데이터 저장
    setSettlementData(data);
    // 정산 페이지로 이동
    setMove(2);
  };


  // 디폴트 페이지로 이동 (중간 이탈)
  const handleExit = () => {
    setMove(0);
  };


  // 최종 정산 완료 후 퀴즈페이지로 돌아가기
const handleGoBackToQuiz = () => {
  console.log('정산하고 버튼', Date.now());
  // 정산 데이터 초기화
  setSettlementData(null);
  // 퀴즈페이지로 이동
  setMove(1);
  
  // 영상 재생 신호 보내기
  chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
    if (tabs[0]) {
      chrome.tabs.sendMessage(tabs[0].id, { type: 'RESUME_VIDEO' });
    }
  });
};


  return (
    <div style={{
      width: '100%',
      height: '100%',
      // 최대 너비
      maxWidth: '402px',
    }}>

      {/* 페이지 0: 기본 화면 */}
      {move === 0 && <DefaultPage />}

      {/* 페이지 1: 퀴즈 화면 */}
      {move === 1 && (
        <QuizPage
          key={resetKey}
          videoId={videoId}
          videoTitle={videoTitle}
          duration={duration}
          onExitPage={handleExit}
          onSettlementPage={handleSettlement}
        />
      )}

      {/* 페이지 2: 정산 화면 */}
      {move === 2 && (
        // data 전달
        <SettlementPage 
          data={settlementData}
          videoId={videoId}
          videoTitle={videoTitle}
          channelName={channelName}
          duration={duration}
          onGoBackToQuiz={handleGoBackToQuiz}
        />
      )}
    </div>
  );
}

export default App;

