/* global chrome */

import { useState, useEffect, useRef } from 'react';
import { log } from './utils/logger';
import { sendMessage } from './utils/messageHelper';

function Recorder() {
  // const [isRecording, setIsRecording] = useState(false);
  const [error, setError] = useState(null);


  const mediaRecorderRef = useRef(null);
  const chunksRef = useRef([]);
  const streamRef = useRef(null);
  const isCancelledRef = useRef(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
  let stream;
  let mediaRecorder;
  let cancelled = false;
  let autoCloseTimer;

  const init = async () => {
    try {
      // 마이크 권한 요청
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      
      if (cancelled) {
        stream.getTracks().forEach(t => t.stop());
        return;
      }

      // 권한 받은 후 창 위치를 화면 밖으로
      // 오류 이거 실패해서 나중에 지우기
      try {
          const currentWindow = await chrome.windows.getCurrent();


          await chrome.windows.update(currentWindow.id, {
            state: 'minimized',
            focused: false,
          });



        } catch (e) {
          log.debug('창 이동 실패:', e);
        }

      // 녹음 준비
      const supportedTypes = [
  'audio/webm',
  'audio/webm;codecs=opus',
  'audio/wav',
  'audio/ogg',
];

const mimeType = supportedTypes.find(t => MediaRecorder.isTypeSupported(t)) 
  || 'audio/webm';


      streamRef.current = stream;
      mediaRecorder = new MediaRecorder(stream, { mimeType });
      mediaRecorderRef.current = mediaRecorder;
      chunksRef.current = [];

      // 녹음 데이터 수집
      mediaRecorder.ondataavailable = (e) => chunksRef.current.push(e.data);

      // mediaRecorder.onstart = () => {
      //   if (!cancelled) setIsRecording(true);
      // };





      // 녹음 정지 시 처리
      mediaRecorder.onstop = async () => {
        stream.getTracks().forEach(t => t.stop());

        // 취소된 경우 전송 X
          if (isCancelledRef.current) {
            // log.debug('녹음 취소됨');
            window.close();
            return;
          }

          // 데이터 없음
          if (chunksRef.current.length === 0) {
            // log.debug('녹음 데이터 없음');
            window.close();
            return;
          }

        const audioBlob = new Blob(chunksRef.current, { type: 'audio/webm' });
        // base64 변환 + 전송 + 창 닫기
        const reader = new FileReader();


reader.onloadend = () => {
  sendMessage({
    type: 'RECORDING_COMPLETE',
    audioData: reader.result,
  });
  
  // stream.getTracks().forEach(t => t.stop());
  window.close();
};
reader.readAsDataURL(audioBlob);
      };

      // 녹음 시작
      mediaRecorder.start();

      // 3분 후 자동 종료
      autoCloseTimer = setTimeout(() => {
        // log.debug('3분 경과 - 자동 정지');
        if (mediaRecorderRef.current?.state === 'recording') {
          mediaRecorderRef.current.stop();  // 정지 후 전송
        } else {
          window.close();
        }
      }, 3 * 60 * 1000);

      // 사이드패널에 "녹음 시작됨" 알림
      sendMessage({ type: 'RECORDING_STARTED' });
    } catch (err) {
      if (!cancelled) {

        sendMessage({
          type: 'RECORDING_ERROR',
          error: err.message,
        });

        setError(err.message);
      }
    }
  };


// 사이드패널 메시지 수신
const handleMessage = (message) => {
      if (message.type === 'STOP_RECORDING') {
        // log.debug('정지 요청 수신');
        if (mediaRecorderRef.current?.state === 'recording') {
          mediaRecorderRef.current.stop();
        }
      }
      
      if (message.type === 'CANCEL_RECORDING') {
        // log.debug('취소 요청 수신');
        isCancelledRef.current = true;
        
        if (mediaRecorderRef.current?.state === 'recording') {
          mediaRecorderRef.current.stop();
        } else {
          window.close();
        }
      }
    };





  chrome.runtime.onMessage.addListener(handleMessage);
  init();
  
  // 클린업
  return () => {
    cancelled = true;
    if (autoCloseTimer) clearTimeout(autoCloseTimer);
    chrome.runtime.onMessage.removeListener(handleMessage);
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(t => t.stop());
    }
  };
}, []);

// // stopRecording은 그대로
// const stopRecording = () => {
//   if (mediaRecorderRef.current?.state === 'recording') {
//     mediaRecorderRef.current.stop();
//   }
// };
const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText('chrome://settings/content/microphone');
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      alert('복사 실패. 직접 입력해주세요.');
    }
  };


const styles = {
  overlay: {
    position: 'fixed',
    top: 0,
    left: 0,
    width: '100%',
    height: '100%',
    background: 'rgba(0, 0, 0, 0.3)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontFamily: 'sans-serif',
  },
  modal: {
    background: '#FEFDF9',
    borderRadius: '20px',
    padding: '24px',
    width: '340px',
    boxShadow: '0 4px 12px 0 rgba(158, 155, 137, 0.20)',
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },
  title: {
    margin: 0,
    fontSize: '18px',
    fontWeight: 700,
    textAlign: 'center',
    color: '#333',
  },
  messageBox: {
    background: '#F5F3EC',
    borderRadius: '12px',
    padding: '16px',
  },
  message: {
    margin: 0,
    fontSize: '14px',
    color: '#666',
    textAlign: 'center',
    lineHeight: 1.5,
  },
  guideBox: {
    background: '#F5F3EC',
    borderRadius: '12px',
    padding: '16px',
  },
  guideTitle: {
    margin: '0 0 12px 0',
    fontSize: '13px',
    fontWeight: 700,
    color: '#333',
  },
  guideStep: {
    margin: '6px 0',
    fontSize: '12px',
    color: '#666',
    lineHeight: 1.5,
  },
  urlBox: {
    background: 'white',
    border: '1px solid #E0DDD3',
    borderRadius: '8px',
    padding: '8px 12px',
    fontSize: '11px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: '8px',
    margin: '8px 0',
  },
  urlText: {
    fontFamily: 'monospace',
    color: '#01CF8A',
    flex: 1,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
  },
  copyButton: {
    background: '#01CF8A',
    color: 'white',
    border: 'none',
    borderRadius: '6px',
    padding: '4px 12px',
    fontSize: '11px',
    cursor: 'pointer',
    fontWeight: 600,
    flexShrink: 0,
  },
  button: {
    padding: '14px',
    background: '#01CF8A',
    color: '#FEFDF9',
    border: 'none',
    borderRadius: '12px',
    fontSize: '15px',
    fontWeight: 600,
    cursor: 'pointer',
  },
};

  if (error) {
    return (
      <div style={styles.overlay}>
        <div style={styles.modal}>
          <h2 style={styles.title}>
            마이크 권한이 필요해요
          </h2>
          
          <div style={styles.messageBox}>
            <p style={styles.message}>
              음성 모드를 사용하려면<br/>
              마이크 권한을 허용해주세요
            </p>
          </div>

          <div style={styles.guideBox}>
            <p style={styles.guideTitle}>설정 방법</p>
            
            <p style={styles.guideStep}>
              1. 아래 주소를 복사해서 새 탭에 붙여넣기
            </p>
            
            <div style={styles.urlBox}>
              <span style={styles.urlText}>
                chrome://settings/content/microphone
              </span>
              <button 
                style={styles.copyButton}
                onClick={handleCopy}
              >
                {copied ? '복사됨' : '복사'}
              </button>
            </div>
            
            <p style={styles.guideStep}>
              2. 마이크 사용이 허용되지 않음 목록에서 본 확장 프로그램 찾기
            </p>
            
            <p style={styles.guideStep}>
              3. 휴지통 아이콘으로 차단 해제
            </p>
          </div>

          <button 
            style={styles.button}
            onClick={() => window.close()}
          >
            닫기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div style={styles.overlay}>
      <div style={styles.modal}>
        <h2 style={styles.title}>
          마이크 권한이 필요해요
        </h2>
        
        <div style={styles.messageBox}>
          <p style={styles.message}>
            대화를 음성으로 진행하기 위해<br/>
            마이크 사용을 허용해주세요
          </p>
        </div>
      </div>
    </div>
  );
}

export default Recorder;