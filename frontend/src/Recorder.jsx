// /* global chrome */

import { useState, useEffect, useRef } from 'react';
import { log } from './utils/logger';
import { sendMessage } from './utils/messageHelper';

function Recorder() {
  const [isRecording, setIsRecording] = useState(false);
  const [error, setError] = useState(null);
  const mediaRecorderRef = useRef(null);
  const chunksRef = useRef([]);
  const streamRef = useRef(null);

  useEffect(() => {
  let stream;
  let mediaRecorder;
  let cancelled = false;

  const init = async () => {
    try {
      stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      
      if (cancelled) {
        stream.getTracks().forEach(t => t.stop());
        return;
      }
      const supportedTypes = [
  'audio/webm',
  'audio/webm;codecs=opus',
  'audio/wav',
  'audio/ogg',
];

const mimeType = supportedTypes.find(t => MediaRecorder.isTypeSupported(t)) 
  || 'audio/webm';
log.debug('사용할 형식:', mimeType);

      streamRef.current = stream;
      mediaRecorder = new MediaRecorder(stream, { mimeType });
      mediaRecorderRef.current = mediaRecorder;
      chunksRef.current = [];

      mediaRecorder.onstart = () => {
        if (!cancelled) setIsRecording(true);
      };

      mediaRecorder.ondataavailable = (e) => chunksRef.current.push(e.data);

      mediaRecorder.onstop = async () => {
        const audioBlob = new Blob(chunksRef.current, { type: 'audio/webm' });
          
        

        
  // base64 변환 + 전송 + 창 닫기
  const reader = new FileReader();
reader.onloadend = () => {
  sendMessage({
    type: 'RECORDING_COMPLETE',
    audioData: reader.result,
  });
  
  stream.getTracks().forEach(t => t.stop());
  window.close();
};
reader.readAsDataURL(audioBlob);
      };

      mediaRecorder.start();
    } catch (err) {
      if (!cancelled) {
        setError(err.message);
      }
    }
  };

  init();
  
  return () => {
    cancelled = true;
    if (streamRef.current) {
      streamRef.current.getTracks().forEach(t => t.stop());
    }
  };
}, []);

// stopRecording은 그대로
const stopRecording = () => {
  if (mediaRecorderRef.current?.state === 'recording') {
    mediaRecorderRef.current.stop();
  }
};

  if (error) {
    return (
      <div style={{ padding: 24, textAlign: 'center', fontFamily: 'sans-serif' }}>
        <h3>❌ 녹음 실패</h3>
        <p>{error}</p>
        <button onClick={() => window.close()}>닫기</button>
      </div>
    );
  }

  return (
    <div style={{ 
      padding: 24, 
      textAlign: 'center', 
      fontFamily: 'sans-serif',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      gap: 16,
    }}>
      <div style={{ fontSize: 60 }}>🎤</div>
      
      {isRecording && (
        <div style={{ 
          color: '#FF4444',
          fontWeight: 600,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}>
          <span style={{ 
            width: 12, 
            height: 12, 
            background: '#FF4444', 
            borderRadius: '50%',
            animation: 'blink 1s infinite',
          }} />
          녹음 중...
        </div>
      )}
      
      <p style={{ color: '#666', fontSize: 14 }}>
        말씀이 끝나면 정지 버튼을 눌러주세요
      </p>
      
      <button 
        onClick={stopRecording}
        style={{
          padding: '12px 32px',
          background: '#FF4444',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontSize: 16,
          fontWeight: 600,
        }}
      >
        ⏹ 정지 및 전송
      </button>
      
      <style>{`
        @keyframes blink {
          0%, 100% { opacity: 1; }
          50% { opacity: 0.3; }
        }
      `}</style>
    </div>
  );
}

export default Recorder;