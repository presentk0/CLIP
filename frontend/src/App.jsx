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

  // 사이드패널 열림 감지 및 연결
  useEffect(() => {
    // 연결 실패 시 재시도 횟수
    let retryCount = 0;
    // 최대 10번까지 재시도
    const MAX_RETRY = 10;  

    // 백그라운드와 연결 (사이드패널 닫힘 감지용)
    chrome.runtime.connect({ name: 'sidepanel' });

    // content.jsx에 사이드패널 열림 신호 보내기
    const sendPanelOpened = () => {
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        console.log('패널 열림 신호1');
        if (tabs[0]) {
          chrome.tabs.sendMessage(tabs[0].id, { type: 'PANEL_OPENED' }).catch(() => {
            console.log('패널 열림 신호2');
            retryCount++;
            if (retryCount < MAX_RETRY) {
              console.log('패널 열림 재시도');
              // 실패하면 1초 후 재시도
              setTimeout(sendPanelOpened, 1000);
            } else {
              console.log('연결 실패 - 페이지 새로고침 필요');
            }
          });
        }
      })};
    sendPanelOpened();
  }, []);


  // 퀴즈페이지로 이동 및 영상 아이디와 제목, 전체 길이 저장
  useEffect(() => {
    console.log('App 시작:', Date.now());
    const listener = (message) => {
      console.log('메시지 받음:', message.type, Date.now());
      if (message.type === 'GO_TO_QUIZ') {
        console.log('퀴즈 페이지 이동1:', Date.now());
        setVideoId(message.videoId);
        setVideoTitle(message.videoTitle);
        setChannelName(message.channelName);
        setMove(1);
      }
      if (message.type === 'VIDEO_DURATION') {
        console.log('퀴즈 페이지 이동2:', Date.now());
        setDuration(message.duration);
        setMove(1);
      }
    };
    chrome.runtime.onMessage.addListener(listener);
    return () => chrome.runtime.onMessage.removeListener(listener);
  }, []);


  // 정산 페이지로 이동
  const handleSettlement = (data) => {
    // 데이터 저장
    setSettlementData(data);
    // 정산 페이지로 이동
    setMove(2);
  };


  // 디폴트 페이지로 이동 (이탈페이지)
  const handleExit = () => {
    setMove(0);
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
          videoId={videoId}
          videoTitle={videoTitle}
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
        />
      )}
    </div>
  );
}

export default App;

