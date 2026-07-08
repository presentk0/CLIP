/* global chrome */

import styles from './AiChatPage.module.css';
import { apiFetch } from '../../utils/api';
import { useState, useEffect, useRef } from 'react';
import { useLocation } from 'react-router-dom';
import { useNavigate } from 'react-router-dom';
import { log } from '../../utils/logger';

import { Client } from '@stomp/stompjs';
import { getAccessToken } from '../../utils/api';
import SockJS from 'sockjs-client';
import union from '../../imgs/Union.svg';
import { useCallback } from 'react';
import { Fragment } from 'react';
import React from 'react';
import { Spinner } from '../../components/Spinner/Spinner';
import frog from '../../imgs/ChatGPT_Image_2026_4_29_11_38_54.png';
import { loadUserData, saveUserData } from '../../utils/userStorage';
// import frog2 from '../../imgs/image_809.png';
// import { useSearchParams } from 'react-router-dom';
import { openYoutubeVideo } from '../../utils/openInTab';
import { TestSpinner } from '../../components/Spinner/Spinner';

import { Toast } from '../../contexts/Toast';

// 무음 재생용
const SILENT_MP3 = 'data:audio/mpeg;base64,SUQzBAAAAAAAI1RTU0UAAAAPAAADTGF2ZjU4Ljc2LjEwMAAAAAAAAAAAAAAA//tQxAADB8AhSmxhIIEVCSiJrDCQBTcu3UrAIwUdkRgQbFAZC1CQEwTJ9mjRvBA4UOLD8nKVOWfh+UlK3z/177OXrfOdKl7pyn3Xf//FJAhCQIQiIRVX///v////lQZ0P9pmviQBZW9NIQEUYcQlIhqRoYUEBEQMkYgQ0BAg';

// JWT 토큰 만료 체크
const isTokenExpired = (token) => {
  if (!token) return true;
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    // 만료 30초 전부터 만료로 간주 (네트워크 지연 대비)
    return payload.exp * 1000 < Date.now() + 30 * 1000;
  } catch {
    return true;
  }
};

// WebSocket은 apiFetch를 안 써서 전용 토큰 갱신이 필요
// apiFetch는 HTTP 요청 (REST API) 전용
// WebSocket 연결은 다른 메커니즘 (HTTP 핸드셰이크 후 영속 연결)
// WebSocket이 만료된 토큰으로 연결 시도하면 apiFetch의 자동 갱신 로직이 안 돌아가서 WebSocket 전용으로 따로 처리
// 토큰 갱신
const refreshAccessToken = async () => {
  try {
    const res = await fetch(`${import.meta.env.VITE_API_BASE_URL}/auth/refresh`, {
      method: 'POST',
      credentials: 'include',
    });
    
    if (!res.ok) {
      log.debug('refresh 응답 실패');
      return null;
    }

    const data = await res.json();

    if (!data.success) {
      log.debug('refresh 데이터 실패');
      return null;
    }

    const newToken = data.data.accessToken;

    // 백그라운드에 저장
    const auth = await chrome.runtime.sendMessage({ type: 'GET_AUTH' });
    await chrome.runtime.sendMessage({
      type: 'SET_AUTH',
      accessToken: newToken,
      user: auth?.user,
    });
    
    return newToken;
  } catch (err) {
    log.debug('토큰 갱신 실패', err);
    return null;
  }
};






function ContinuePopup({ info, onContinue, onNewStart }) {
  return (
    <div className={styles.popupOverlay}>
      <div className={styles.popupBox}>
        <h2 className={styles.popupTitle}>진행 중인 대화가 있어요</h2>
        
        <div className={styles.popupInfo}>
          <p>단어: <strong>{info.word}</strong></p>
          <p>시나리오: {info.scenarioTitle}</p>
        </div>
        
        <p className={styles.popupQuestion}>이어서 하시겠어요?</p>
        
        <div className={styles.popupButtons}>
          <button 
            className={styles.popupBtnSecondary}
            onClick={onNewStart}
          >
            새로 시작
          </button>
          <button 
            className={styles.popupBtnPrimary}
            onClick={onContinue}
          >
            이어하기
          </button>
        </div>
      </div>
    </div>
  );
}




function ConsentPopup({ handleAudioConsent, handleAudioReject }) {
  return (
      <>
      {/* 어두워지기 */}
      <div className={styles['exit-overlay']}></div>
      
      <div className={styles.exit}>
        <div className={styles.exit2}>
          <p className={styles.exit3}>AI 음성 자동 재생을 허용하시겠어요?</p>
          <div className={styles.exit4}>
            <p className={styles.exit5}>AI가 대답할 때마다 음성으로 들려드려요. 한 번 동의 하면 앞으로 자동으로 적용돼요!</p>
          </div>
        </div>


        <div className={styles.exit6}>
          <button 
          className={styles.exit7}
          onClick={handleAudioConsent}>
            <p className={styles.exit8}>허용할게요</p>
          </button>
          <button 
          className={styles.exit9}
          onClick={handleAudioReject}
          >
            <p className={styles.exit10}>내가 원할 때만 들을게요</p>
          </button>
        </div>
      </div>
      </>
  );
}












// 채팅방 나가기 테스트용 오류
  function ExitModal ({onConfirm, onCancel}) {
    return (
      <>
      {/* 어두워지기 */}
      <div className={styles['exit-overlay']} onClick={onCancel}></div>
      
      <div className={styles.exit}>
        <div className={styles.exit2}>
          <p className={styles.exit3}>지금 끝내기에는 아쉬워요! <br />조금만 더 가봐요!</p>
          <div className={styles.exit4}>
            <p className={styles.exit5}>지금까지의 진행 상황을 저장해두고 <br />나중에 이어서 다시 할 수 있어요</p>
          </div>
        </div>


        <div className={styles.exit6}>
          <button 
          className={styles.exit7}
          onClick={onCancel}>
            <p className={styles.exit8}>대화 계속하기</p>
          </button>
          <button 
          className={styles.exit9}
          onClick={onConfirm}
          >
            <p className={styles.exit10}>저장하고 나가기</p>
          </button>
        </div>
      </div>
      </>
    )
  }





// 상단 부분
function Header ({ handleAiReset, currentStep, onBack, onMyPage, onStartMessageReport, onStartScenarioReport, isMessage, handleCloseReport, showReportModal }) {

  // 더보기 창이 열려있는지 여부 상태
  const [isMoreOpen, setIsMoreOpen] = useState(false);

  // 더보기 버튼 클릭 시 호출되는 함수 (열림/닫힘 토글만 수행)
  const handleSeeMore = () => {
    setIsMoreOpen((prev) => !prev);
  };



  return (
    currentStep !== 'CHAT' ? (
      <div className={styles.head}>
        <div className={styles.head2} style={currentStep === 'REPORT' ? { background: '#F5F3E7' } : {}}>
          <div className={styles.head3}>
            <button 
            onClick={onBack}
            className={styles.head4}
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M16.5014 4.35565C16.9756 4.82987 16.9756 5.59871 16.5014 6.07292L10.0743 12.5L16.5014 18.9271C16.9756 19.4013 16.9756 20.1702 16.5014 20.6444C16.0272 21.1185 15.2584 21.1185 14.7842 20.6444L7.49848 13.3586C7.02427 12.8845 7.02427 12.1156 7.49848 11.6414L14.7842 4.35565C15.2584 3.88145 16.0272 3.88145 16.5014 4.35565Z" fill="#454440"/>
              </svg>
            </button>

            <button 
            onClick={onMyPage}
            className={styles.head4}
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M10.125 16.8572V20.0001C10.125 20.5524 9.67728 21.0001 9.125 21.0001H5.5C4.94772 21.0001 4.5 20.5524 4.5 20.0001V10.3485C4.5 9.76471 4.75512 9.21001 5.19842 8.83005L10.6984 4.11576C11.4474 3.47378 12.5526 3.47378 13.3016 4.11576L18.8016 8.83005C19.2449 9.21001 19.5 9.76471 19.5 10.3485V20.0001C19.5 20.5524 19.0523 21.0001 18.5 21.0001H14.875C14.3227 21.0001 13.875 20.5524 13.875 20.0001V16.8572C13.875 16.305 13.4273 15.8572 12.875 15.8572H11.125C10.5727 15.8572 10.125 16.305 10.125 16.8572Z" fill="#454440"/>
              </svg>
            </button>
          </div>

          <div className={styles.head5}>
            <p className={styles.head6}>AI Talk</p>
          </div>
        </div>
      </div>
    ) : (
      // ai 채팅방
      <div className={styles['chat-head']}>
        <div className={styles['chat-head2']}>
          <div className={styles['chat-head3']}>
            <button 
            onClick={onBack}
            className={styles['chat-head4']}
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M16.5014 4.35565C16.9756 4.82987 16.9756 5.59871 16.5014 6.07292L10.0743 12.5L16.5014 18.9271C16.9756 19.4013 16.9756 20.1702 16.5014 20.6444C16.0272 21.1185 15.2584 21.1185 14.7842 20.6444L7.49848 13.3586C7.02427 12.8845 7.02427 12.1156 7.49848 11.6414L14.7842 4.35565C15.2584 3.88145 16.0272 3.88145 16.5014 4.35565Z" fill="#454440"/>
              </svg>
            </button>

            <button 
            onClick={onMyPage}
            className={styles['chat-head4']}
            >
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M10.125 16.8572V20.0001C10.125 20.5524 9.67728 21.0001 9.125 21.0001H5.5C4.94772 21.0001 4.5 20.5524 4.5 20.0001V10.3485C4.5 9.76471 4.75512 9.21001 5.19842 8.83005L10.6984 4.11576C11.4474 3.47378 12.5526 3.47378 13.3016 4.11576L18.8016 8.83005C19.2449 9.21001 19.5 9.76471 19.5 10.3485V20.0001C19.5 20.5524 19.0523 21.0001 18.5 21.0001H14.875C14.3227 21.0001 13.875 20.5524 13.875 20.0001V16.8572C13.875 16.305 13.4273 15.8572 12.875 15.8572H11.125C10.5727 15.8572 10.125 16.305 10.125 16.8572Z" fill="#454440"/>
              </svg>
            </button>
          </div>

          <div className={styles['chat-head5']}>
            <p className={styles['chat-head6']}>{showReportModal ? '신고하기' : 'AI Talk'}</p>
          </div>
        </div>

        {/* 더보기 밑에 나오도록 감싸기 */}
        <div className={styles['more-container']}>
          {/* 더보기 클릭하고 AI 대화 신고하기 누르면 선택 해제가 나타나게 설정 시나리오 신고하기는 비활성화 */}
          {isMessage ? (
            <button onClick={handleCloseReport} className={styles['chat-more2']}>
              <p className={styles['chat-more3']}>선택 해제</p>
            </button>
          ) : (
            !showReportModal && (
              <button onClick={handleSeeMore} className={styles['chat-more']}>
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
  <path d="M12.5 11.25C12.5 11.9404 11.9404 12.5 11.25 12.5C10.5596 12.5 10 11.9404 10 11.25C10 10.5596 10.5596 10 11.25 10C11.9404 10 12.5 10.5596 12.5 11.25Z" fill="#454440"/>
  <path opacity="0.8" d="M12.5 6.25C12.5 6.94036 11.9404 7.5 11.25 7.5C10.5596 7.5 10 6.94036 10 6.25C10 5.55964 10.5596 5 11.25 5C11.9404 5 12.5 5.55964 12.5 6.25Z" fill="#454440"/>
  <path opacity="0.8" d="M12.5 16.25C12.5 16.9404 11.9404 17.5 11.25 17.5C10.5596 17.5 10 16.9404 10 16.25C10 15.5596 10.5596 15 11.25 15C11.9404 15 12.5 15.5596 12.5 16.25Z" fill="#454440"/>
</svg>
              </button>
            )
          )}

          {/* 더보기 창이 열려있을 때만 SeeMore 렌더링 */}
          {isMoreOpen && (
            <SeeMore 
              onStartMessageReport={() => {
                onStartMessageReport(); // AiChatPage의 선택 모드 활성화
                setIsMoreOpen(false);   // 더보기 창 닫기
              }}
              onStartScenarioReport={() => {
                onStartScenarioReport(); // AiChatPage의  시나리오 팝업 활성화
                setIsMoreOpen(false);    // 더보기 창 닫기
              }}
              handleAiReset={handleAiReset}
            />
          )}
        </div>
      </div>
    )
  )
}


// 더보기 박스 (비활성화)
function SeeMore ({ onStartMessageReport, onStartScenarioReport, handleAiReset }) {

  return (
    <>
    <div className={styles.more}>
      <div className={styles.more2}>
        <button
        onClick={() => {
          // AiChatPage에 선택 모드 켜달라고 요청
          onStartMessageReport();
        }}
        className={styles.more3}>
          <p className={styles.more4}>AI 대화 신고하기</p>
        </button>

        <button
        onClick={() => {
          // AiChatPage에 시나리오 신고 팝업 요청
          onStartScenarioReport();
        }}
        className={styles.more3}>
          <p className={styles.more4}>시나리오 신고하기</p>
        </button>
      </div>

      <div className={styles.more5}></div>

      <div className={styles.more6}>
        <div className={styles.more7}>
          <img src={union} alt="" className={styles.more8} />
        </div>


        <button
        className={styles.more9}
        onClick={handleAiReset}
        >
          <p className={styles.more10}>대화 다시하기</p>
        </button>
      </div>
    </div>
    </>
  );
}


function Reports ({mode, chatData, targetId, onClose, showPopup}) {
  const [isReports, setIsReports] = useState(false);
  const [inputReports, setInputReports] = useState('');
  const chatRoomId = chatData?.chatRoomId;
  const titles = {
    SCENARIO: '시나리오 신고',
    MESSAGE: 'AI 답변 신고'
  };

  // 산고 전송
  const handleSubmit = async() => {
    if (!inputReports.trim()) {
      showPopup('신고 내용을 입력해주세요.');

      return;
    }

    if (isReports) return;
    try {
      setIsReports(true);

      const result = await apiFetch('/chats/reports', {
        method: 'POST',
        body: JSON.stringify({
          reportType: mode,
          targetId: mode === 'MESSAGE' ? targetId : chatRoomId,
          content: inputReports
        })
      });

      if (result.success) {
        showPopup('신고가 접수되었습니다.');

        setInputReports('');
        onClose();
      }
    } catch (error) {
      log.error('ai채팅방 신고 에러', error);
    } finally {
      setIsReports(false);
    }
  };


  return (
    <div className={styles.reports}>
      <div className={styles.reports2}>
        <div className={styles.reports3}>
          <p className={styles.reports4}>{titles[mode]}</p>
        </div>

        <div className={styles.reports5}>
        <textarea 
          className={styles.reports6}
          value={inputReports}
          onChange={(e) => setInputReports(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              handleSubmit();
            }
          }}
          placeholder={
            '신고 내용을 작성해주세요'
          }
          maxLength={250}
          disabled={isReports}
          >
          </textarea>
            <p className={styles['chat-input8']}>
              {inputReports.length}
              <span className={styles['chat-input9']}>/250자</span>
            </p>
        </div>
      </div>

      <div className={styles.reports7}>
        <button 
        onClick={onClose}
        className={styles.reports8}
        >
          <p className={styles.reports9}>취소</p>
        </button>

        <button 
        onClick={handleSubmit}
        className={styles.reports10}>
          <p className={styles.reports11}>{isReports ? '전송 중...' : '신고하기'}</p>
        </button>
      </div>
    </div>
  )
}





// 단어 선택? 테스트용 오류
function TestDictionary ({ wordData, currentIndex }) {
  return (
    <div className={styles.viewport}>
      <div 
        className={styles.track}
        style={{
          '--index': currentIndex
      }}>
        {wordData?.map((word) => (
          <div key={word.wordId} className={styles.dictionary}>
            <div className={styles.dictionary2}>
              <div className={styles.dictionary3}>
                <div className={styles.dictionary4}>
                  <div className={styles.dictionary5}>
                    <div className={styles.dictionary8}>
                      <div className={styles.dictionary9}>
                        <p className={styles['dictionary9-t']}>{word.word}</p>
                      </div>
                      <div className={styles.dictionary10}>
                        <div className={styles.dictionary11}>
                          <div className={styles['dictionary11-a']}>
                            <div className={styles['dictionary11-b']}>
                              <div className={styles['dictionary11-c']}>
                                <p className={styles['dictionary11-d']}>US </p>
                              </div>
                              <div className={styles['dictionary11-e']}>
                                <div className={styles['dictionary11-f']}>
                                  <svg 
                                  className={styles['dictionary11-f2']}
                                  xmlns="http://www.w3.org/2000/svg" 
                                  width="16" 
                                  height="16" 
                                  viewBox="0 0 16 16" 
                                  fill="none">
                                    <path d="M7.33337 4.18509C7.33337 3.54139 6.61183 3.16128 6.08095 3.52531L4.10228 4.88211C4.03568 4.92778 3.95682 4.95222 3.87607 4.95222H2.13337C1.69155 4.95222 1.33337 5.31039 1.33337 5.75222V10.2475C1.33337 10.6893 1.69155 11.0475 2.13337 11.0475H3.87607C3.95682 11.0475 4.03568 11.0719 4.10228 11.1176L6.08095 12.4744C6.61183 12.8384 7.33337 12.4583 7.33337 11.8146V4.18509Z" fill="#A0A08A"/>
                                    <path d="M9.69336 5.64014C10.3183 6.26523 10.6693 7.11292 10.6693 7.9968C10.6693 8.88068 10.3183 9.72838 9.69336 10.3535" stroke="#A0A08A" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                                    <path d="M11.4879 3.99658C12.7685 5.05756 13.4879 6.49636 13.4879 7.99658C13.4879 9.4968 12.7685 10.9356 11.4879 11.9966" stroke="#A0A08A" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                                  </svg>
                                </div>
                              </div>
                            </div>
                            {word.ukPhonetic && (
                            <div className={styles['dictionary11-g']}>
                              <p className={styles['dictionary11-h']}>[{word.usPhonetic}]</p>
                            </div>
                            )}
                          </div>
                        <div className={styles['dictionary11-a']}>
                          <div className={styles['dictionary11-b']}>
                            <div className={styles['dictionary11-c']}>
                              <p className={styles['dictionary11-d']}>UK </p>
                            </div>
                            <div className={styles['dictionary11-e']}>
                              <div className={styles['dictionary11-f']}>
                                <svg 
                                className={styles['dictionary11-f2']}
                                xmlns="http://www.w3.org/2000/svg" 
                                width="16" 
                                height="16" 
                                viewBox="0 0 16 16" 
                                fill="none">
                                  <path d="M7.33337 4.18509C7.33337 3.54139 6.61183 3.16128 6.08095 3.52531L4.10228 4.88211C4.03568 4.92778 3.95682 4.95222 3.87607 4.95222H2.13337C1.69155 4.95222 1.33337 5.31039 1.33337 5.75222V10.2475C1.33337 10.6893 1.69155 11.0475 2.13337 11.0475H3.87607C3.95682 11.0475 4.03568 11.0719 4.10228 11.1176L6.08095 12.4744C6.61183 12.8384 7.33337 12.4583 7.33337 11.8146V4.18509Z" fill="#A0A08A"/>
                                  <path d="M9.69336 5.64014C10.3183 6.26523 10.6693 7.11292 10.6693 7.9968C10.6693 8.88068 10.3183 9.72838 9.69336 10.3535" stroke="#A0A08A" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                                  <path d="M11.4879 3.99658C12.7685 5.05756 13.4879 6.49636 13.4879 7.99658C13.4879 9.4968 12.7685 10.9356 11.4879 11.9966" stroke="#A0A08A" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                                </svg>
                              </div>
                            </div>
                          </div>
  
                          {word.ukPhonetic && (
                          <div className={styles['dictionary11-g']}>
                            <p className={styles['dictionary11-h']}>[{word.ukPhonetic}]</p>
                          </div>
                          )}
                        </div>
                      </div>
                    </div>
                  </div>
  
  
                  <div className={styles.dictionary12}>
                    <div className={styles.dictionary13}>
                      <div className={styles['dictionary14-s']}>
                        <p className={styles['dictionary15-s']}>뜻</p>
                      </div>
                      <div className={styles['dictionary16-n']}>
                        <p className={styles['dictionary17-n']}>표현</p>
                      </div>
                      <div className={styles.dictionary18}>
                        <p className={styles.dictionary19}>관계어</p>
                      </div>
                      <div className={styles['dictionary16-n']}>
                        <p className={styles['dictionary17-n']}>파생형</p>
                      </div>
                    </div>
  
                    {/* 뜻 탭 내용 */}
    {word.meaningsByPos?.map((m, i) => (
      <div key={i}>
        <div className={styles.dictionary20}>
          <p className={styles.dictionary21}>
            {m.partOfSpeech}
          </p>
          {m.meanings?.map((meaning, j) => (
          <div key={j} className={styles.dictionary22}>
            <div className={styles.dictionary23}>
              <div className={styles.dictionary24}>
                {j+1}.{meaning}
              </div>
            </div>
          </div>
          ))}
        </div>
      </div>
    ))}
                  </div>
                </div>
  
  
                <div className={styles['dictionary-bar']}></div>
  
                <div className={styles.dictionary27}>
                  {/* <div className={styles.dictionary28}>
                    <p className={styles.dictionary29}>관용구</p>
                  </div>
                  <div className={styles.dictionary30}>
                    <div className={styles.dictionary31}>
                      <p className={styles.dictionary32}>a stroke of serendipity</p>
                    </div>
                    <div className={styles.dictionary33}>
                      <p className={styles.dictionary34}>뜻밖의 행운 같은 순간</p>
                    </div>
                  </div> */}
  
                  {word.synonyms.length > 0 && (
                  <div className={styles.dictionary35}>
                    <div className={styles.dictionary36}>
                      <p className={styles.dictionary37}>유의어</p>
                    </div>
                    <div className={styles.dictionary38}>
                      {word.synonyms.map((syn, i) => (
                        <div className={styles.dictionary39}>
                          <p key={i} className={styles.dictionary40}>{syn}</p>
                        </div>
                      ))}
                    </div>
                  </div>
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
        ))}
      </div>
    </div>
  )
};
















// 상단 진행 표시 (1 - 2 - 3)
function Stepper({ currentStep, totalSteps = 3 }) {

  let stepNumber;
  if (currentStep === 'WORD_SELECT') {
    stepNumber = 1;
  } else if (currentStep === 'SCENARIO_SELECT') {
    stepNumber = 2;
  } else if (currentStep === 'VOICE_SELECT') {
    stepNumber = 3;
  }

  return (
    <div className={styles.step}>
      {Array.from({ length: totalSteps }).map((_, i) => {
        const stepNum = i + 1;
        const isLast = stepNum === totalSteps;

        return (
          <Fragment key={stepNum}>
            <StepCircle stepNum={stepNum} currentStep={stepNumber} />
            {!isLast && <StepDots />}
          </Fragment>
        );
      })}
    </div>
  );
}


// 활성화 구분 후 스탭 표시
function StepCircle({ stepNum, currentStep }) {
  const isActive = stepNum === currentStep;
  const isDone = stepNum < currentStep;
  
    // 현재 스텝: 진한 초록 + 번호
  if (isActive) {
    return <div className={styles.step2}>{stepNum}</div>;
  }

  // 완료 스텝: 연한 초록 + 체크
    if (isDone) {
    return (
      <div className={styles['step-active']}>
        <svg 
        className={styles['step-active3']}
        xmlns="http://www.w3.org/2000/svg" width="13" height="10" viewBox="0 0 13 10" fill="none">
          <path d="M10.6367 0.384385C11.0656 -0.090327 11.7972 -0.13124 12.2763 0.294542C12.7576 0.72254 12.8017 1.45953 12.374 1.94103L12.3672 1.94884L5.58778 9.28185L5.58681 9.28087C5.36583 9.52548 5.05245 9.66645 4.72254 9.66661C4.38947 9.66661 4.07184 9.52383 3.85046 9.27501L0.294745 5.27501C-0.133297 4.79347 -0.0900501 4.05564 0.391426 3.62755C0.873018 3.1997 1.6109 3.24371 2.03891 3.72521L4.72937 6.75255L10.6367 0.383409V0.384385Z" fill="white"/>
        </svg>
      </div>
    );
  }

  // 대기 스텝: 테두리 + 연한 번호
    return (
    <div className={styles['step-done']}>
      <span className={styles['step-done2']}>{stepNum}</span>
    </div>
  );
}

function StepDots() {
  return (
    <div className={styles.step4}>
      {[0, 1, 2].map((i) => (
        <svg
          key={i}
          className={styles.step5}
          xmlns="http://www.w3.org/2000/svg"
          width="4"
          height="4"
          viewBox="0 0 4 4"
          fill="none"
        >
          <circle cx="2" cy="2" r="2" fill="#A5E7D2" />
        </svg>
      ))}
    </div>
  );
}




// 시나리오 선택
function Scenario ({ scenarioData, selectedScenario, onSelect }) {



    return (
      <div className={styles.scenario}>
        {scenarioData.map(({ scenarioId, title }) => {
          const isSelected = selectedScenario === scenarioId;
          return (
            <button key={scenarioId}
            onClick={() => onSelect(scenarioId)}
            className={`${styles.scenario2} ${isSelected ? styles['scenario2-selected'] : ''}`}>
              <div className={styles.scenario3}>
                <div className={styles.scenario4}>
                  <p className={styles.scenario5}>{title}</p>
                </div>

                {isSelected === true && (
                <div className={styles.scenario6}>
                  {/* <svg 
                  className={styles.scenario7}
                  xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
                    <path d="M12.137 3.77574C12.566 3.30103 13.2975 3.26012 13.7767 3.6859C14.2579 4.1139 14.302 4.85088 13.8743 5.33238L13.8675 5.3402L7.08809 12.6732L7.08711 12.6722C6.86614 12.9168 6.55276 13.0578 6.22284 13.058C5.88978 13.058 5.57214 12.9152 5.35076 12.6664L1.79505 8.66637C1.36701 8.18483 1.41026 7.44699 1.89173 7.01891C2.37332 6.59106 3.11121 6.63506 3.53921 7.11656L6.22968 10.1439L12.137 3.77477V3.77574Z" fill="white"/> */}

                    <svg 
                    className={styles.scenario8}
                    xmlns="http://www.w3.org/2000/svg" width="13" height="10" viewBox="0 0 13 10" fill="none">
                      <path d="M10.6367 0.384385C11.0656 -0.090327 11.7972 -0.13124 12.2763 0.294542C12.7576 0.72254 12.8017 1.45953 12.374 1.94103L12.3672 1.94884L5.58778 9.28185L5.58681 9.28087C5.36583 9.52548 5.05245 9.66645 4.72254 9.66661C4.38947 9.66661 4.07184 9.52383 3.85046 9.27501L0.294745 5.27501C-0.133297 4.79347 -0.0900501 4.05564 0.391426 3.62755C0.873018 3.1997 1.6109 3.24371 2.03891 3.72521L4.72937 6.75255L10.6367 0.383409V0.384385Z" fill="white"/>
                    </svg>
                  {/* </svg> */}
                </div>
                )}
              </div>
            </button>
          )
        })}
      </div>
    );
  }


// 보이스 선택
function Voice ({ selectedVoice, onSelect }) {
  const voices = [
    { id: 'FEMALE', label: '여성', description: 'Jenny', icon: (<div className={styles.female}>
  <div className={styles.female2}>
    <svg 
    className={styles.female3}
    xmlns="http://www.w3.org/2000/svg" width="40" height="36" viewBox="0 0 40 36" fill="none">
      <path d="M20 0C31.0457 0 40 7.55943 40 16.8845C40 26.2095 31.0457 33.7689 20 33.7689C18.0376 33.7689 16.1412 33.5299 14.3493 33.0852C11.3853 35.4658 8.61324 36.0685 6.61858 35.994C5.35998 35.947 5.15315 34.4579 5.98521 33.4946C6.85849 32.4835 7.61779 31.3546 8.06975 30.437C3.17237 27.3594 0 22.4348 0 16.8845C0 7.55943 8.95431 0 20 0Z" fill="#FEE3A3"/>
      <path d="M14 19C14.7324 20.6403 17.0069 23 20.5915 23C24.0704 23 26.3286 20.0264 27 19" stroke="#454440" stroke-width="2" stroke-linecap="round"/>
      <path d="M15 16C16.1046 16 17 15.1046 17 14C17 12.8954 16.1046 12 15 12C13.8954 12 13 12.8954 13 14C13 15.1046 13.8954 16 15 16Z" fill="#454440"/>
      <path d="M26 16C27.1046 16 28 15.1046 28 14C28 12.8954 27.1046 12 26 12C24.8954 12 24 12.8954 24 14C24 15.1046 24.8954 16 26 16Z" fill="#454440"/>
    </svg>
  </div>
</div>) },
    { id: 'MALE', label: '남성', description: 'Guy', icon: (<div className={styles.male}>
  <div className={styles.male2}>
    <svg 
    className={styles.male3}
    xmlns="http://www.w3.org/2000/svg" width="40" height="36" viewBox="0 0 40 36" fill="none">
      <path d="M20 0C31.0457 0 40 7.55943 40 16.8845C40 26.2095 31.0457 33.7689 20 33.7689C18.0376 33.7689 16.1412 33.5299 14.3493 33.0852C11.3853 35.4658 8.61324 36.0685 6.61858 35.994C5.35998 35.947 5.15315 34.4579 5.98521 33.4946C6.85849 32.4835 7.61779 31.3546 8.06975 30.437C3.17237 27.3594 0 22.4348 0 16.8845C0 7.55943 8.95431 0 20 0Z" fill="#FF9D98"/>
      <path d="M14 19C14.7324 20.6403 17.0069 23 20.5915 23C24.0704 23 26.3286 20.0264 27 19" stroke="#454440" stroke-width="2" stroke-linecap="round"/>
      <path d="M15 16C16.1046 16 17 15.1046 17 14C17 12.8954 16.1046 12 15 12C13.8954 12 13 12.8954 13 14C13 15.1046 13.8954 16 15 16Z" fill="#454440"/>
      <path d="M26 16C27.1046 16 28 15.1046 28 14C28 12.8954 27.1046 12 26 12C24.8954 12 24 12.8954 24 14C24 15.1046 24.8954 16 26 16Z" fill="#454440"/>
    </svg>
  </div>
</div>) },
  ];













  return (
    <div className={styles.voiceList}>
    {voices.map(voice => {
      const isSelected = selectedVoice === voice.id;
      return (
    <button
    className={`${styles.voice} ${isSelected ? styles['voice-selected'] : ''}`}
    onClick={() => onSelect(voice.id)}
    key={voice.id}
    >
      <div className={styles.voice2}>
        <div className={styles.voice3}>
          {voice.icon}
        </div>

        <div className={styles.voice4}>
          <p className={styles.voice5}>{voice.label}</p>
          <p className={styles.voice6}>{voice.description}</p>
        </div>

        {isSelected && (
        <div
        className={styles.voice7}>
            <svg 
            className={styles.voice9}
            xmlns="http://www.w3.org/2000/svg" width="13" height="10" viewBox="0 0 13 10" fill="none">
              <path d="M10.6367 0.384385C11.0656 -0.090327 11.7972 -0.13124 12.2763 0.294542C12.7576 0.72254 12.8017 1.45953 12.374 1.94103L12.3672 1.94884L5.58778 9.28185L5.58681 9.28087C5.36583 9.52548 5.05245 9.66645 4.72254 9.66661C4.38947 9.66661 4.07184 9.52383 3.85046 9.27501L0.294745 5.27501C-0.133297 4.79347 -0.0900501 4.05564 0.391426 3.62755C0.873018 3.1997 1.6109 3.24371 2.03891 3.72521L4.72937 6.75255L10.6367 0.383409V0.384385Z" fill="white"/>
            </svg>

        </div>
        )}

      </div>
    </button>
    );
    })}
    </div>
  )
}


// 단어 강조하기
function MessageContent({ content, highlightWord }) {
  if (!highlightWord) return <p>{content}</p>;
  
  // highlightWord를 분리해서 강조
  const parts = content.split(new RegExp(`(${highlightWord})`, 'gi'));
  
  return (
    <p>
      {parts.map((part, i) => 
        part.toLowerCase() === highlightWord.toLowerCase() 
          ? <span key={i} className={styles.highlight}>{part}</span>
          : part
      )}
    </p>
  );
}











// 발음 점수 표시
// {msg.senderType === 'USER' && msg.pronunciation && (
//   <div className={styles.pronunciation}>
//     발음: {msg.pronunciation.score}점
//     {msg.pronunciation.recommendedAlternative && (
//       <p>추천 표현: {msg.pronunciation.recommendedAlternative}</p>
//     )}
//   </div>
// )}



// 힌트 보기
function HintCard({ message }) {
  // log.debug('힌트 값', message);

  return (
    <div className={styles['hint-card']}>
      <div className={styles['hint-card2']}>
        <div className={styles['hint-card3']}>
          <div className={styles['hint-card4']}>
            <p className={styles['hint-card5']}>{message.contextMessage}</p>
          </div>

          <div className={styles['hint-card6']}>
            <p className={styles['hint-card7']}>
              <MessageContent content={message.guideMessage} highlightWord={message.highlightWord} />
            </p>
          </div>
        </div>

        <div className={styles['hint-card8']}>
          <div className={styles['hint-card9']}>
            <div className={styles['hint-card10']}>
              <p className={styles['hint-card11']}>{message.word}</p>
            </div>

            <div className={styles['hint-card12']}>
              {message.meanings?.slice(0, 2).map((s, i) => (
                <div key={i} className={styles['hint-card13']}>
                  <div className={styles['hint-card14']}>
                    <p className={styles['hint-card15']}>{i + 1}.</p>
                  </div>
                  <div className={styles['hint-card16']}>
                    <p className={styles['hint-card17']}>{s}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className={styles['hint-card18']}>
            <div className={styles['hint-card19']}></div>
          </div>

          <div className={styles['hint-card20']}>
            <div className={styles['hint-card21']}>
              <p className={styles['hint-card22']}>예시 문장</p>
            </div>

            <div className={styles['hint-card23']}>
              <div className={styles['hint-card24']}>
                <p className={styles['hint-card25']}>{message.exampleSentence}</p>
              </div>

              <div className={styles['hint-card26']}>
                <p className={styles['hint-card27']}>{message.exampleTranslation}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}







// 힌트 볼지 여부
function HintOffer({ message, handleHintAccept, handleHintReject }) {



  return (
    <div className={styles.hint}>
      <div className={styles.hint2}>
        <div className={styles.hint3}>
          <div className={styles.hint4}>
            <p className={styles.hint5}>{message.message}</p>
          </div>

          <div className={styles.hint6}>
            <p className={styles.hint7}>{message.subMessage}</p>
          </div>
        </div>

        <div className={styles.hint8}>
          <button className={styles.hint9}
            onClick={() => handleHintAccept(message.messageId)}
          >
            <div className={styles.hint10}

            >
              <p className={styles.hint11}>네</p>
            </div>
          </button>

          <button className={styles.hint12}
            onClick={() => handleHintReject(message.messageId)}
          >
            <div className={styles.hint13}

            >
              <p className={styles.hint14}>아니요</p>
            </div>
          </button>
        </div>
      </div>
    </div>
  );
}



// 오류 이거 미사용임
function ReportBubble({ message }) {
  // log.debug('추천 단어', message);
    // 인트로 (캐릭터 + 한마디)
  if (message.reportType === 'INTRO') {
    return (
      <div className={styles.reportIntro}>
        <div className={styles.reportFrog}></div>
        <p className={styles.reportFeedback}>{message.feedback}</p>
      </div>
    );
  }
  
  // AI 채팅 진행 결과 (점수)
  if (message.reportType === 'SCORES') {
    return (
      <div className={styles.reportScores}>
        <p className={styles.reportTitle}>AI 채팅 진행 결과</p>
        <div className={styles.scoreGrid}>
          <div className={styles.scoreItem}>
            <p>종합 점수</p>
            <p className={styles.scoreNumber}>{message.overallScore}</p>
          </div>
          {message.pronunciationAvailable && (
            <div className={styles.scoreItem}>
              <p>발음 정확도</p>
              <p className={styles.scoreNumber}>{message.pronunciationScore}</p>
            </div>
          )}
          <div className={styles.scoreItem}>
            <p>표현 자연도</p>
            <p className={styles.scoreNumber}>{message.expressionScore}</p>
          </div>
        </div>
        <div className={styles.wordBadge}>
          목표 단어 {message.targetWord}를 {message.usageCount}회 사용했어요!
        </div>
      </div>
    );
  }
  
  // 발음이 아쉬웠던 문장
  if (message.reportType === 'PRONUNCIATION') {
    return (
      <div className={styles.reportCard}>
        <p className={styles.reportTitle}>🎤 발음이 아쉬웠던 문장</p>
        {message.weakSentences.map((s, i) => (
          <div key={i} className={styles.reportItem}>
            <p className={styles.engText}>{s.sentence}</p>
            <p className={styles.korText}>{s.feedback}</p>
            <button className={styles.playBtn}>🔊 모범 발음 듣기</button>
          </div>
        ))}
      </div>
    );
  }
  
  // 기억하면 좋은 표현
  if (message.reportType === 'NATIVE') {
    return (
      <div className={styles.reportCard}>
        <p className={styles.reportTitle}>기억하면 좋은 표현</p>
        {message.improvements.map((item, i) => (
          <div key={i} className={styles.reportItem}>
            {item.original && <p className={styles.strikethrough}>{item.original}</p>}
            <p className={styles.engText}>{item.suggested}</p>
            <p className={styles.korText}>{item.explanation}</p>
            <button className={styles.playBtn}>🔊 모범 발음 듣기</button>
          </div>
        ))}
      </div>
    );
  }
  
  // 다음엔 이렇게
  if (message.reportType === 'NEXT_SCENARIO') {
    return (
      <div className={styles.reportCard}>
        <p className={styles.reportTitle}>✨ 다음엔 이렇게</p>
        <p>{message.improvePoints}</p>
      </div>
    );
  }


    // 종합 점수
  if (message.reportType === 'OVERALL') {
    return (
      <div className={styles.reportCard}>
        <div className={styles.reportCard2}>
          <p className={styles.reportCard3}>종합 점수: {message.score}점</p>
        </div>
        
        <div className={styles.reportCard4}>
          <p className={styles.reportCard6}>잘한 점</p>
          <p className={styles.reportCard10}>{message.goodPoints}</p>
        </div>
        
        <div className={styles.reportCard4}>
          <p className={styles.reportCard6}>개선할 점</p>
          <p className={styles.reportCard10}>{message.improvePoints}</p>
        </div>
      </div>
    );
  }
  
  // 단어 사용
  if (message.reportType === 'WORD_USAGE') {
    return (
      <div className={styles.reportCard}>
        <div className={styles.reportCard2}>
          <p className={styles.reportCard3}>
            "{message.targetWord}" 단어 활용
          </p>
        </div>
        
        <div className={styles.reportCard4}>
          <p className={styles.reportCard6}>{message.feedback}</p>
        </div>
        
        {message.usageContext?.map((ctx) => (
          <div key={ctx.messageId} className={styles.reportCard4}>
            <p className={styles.reportCard6}>"{ctx.userSentence}"</p>
            <p className={styles.reportCard10}>
              {ctx.natural ? '자연스러운 사용' : '어색한 사용'}
            </p>
          </div>
        ))}
      </div>
    );
  }
  

  if (message.reportType === 'SUGGESTION_INLINE') {
    return (
      <div className={styles.reportCard}>
        <div className={styles.reportCard2}>
          <p className={styles.reportCard3}>
            다음엔 이렇게 문장을 사용해보세요
          </p>
        </div>
        <div className={styles.reportCard4}>
          <p className={styles.reportCard6}>
            {message.recommendedAlternative}
          </p>
        </div>
      </div>
    );
  }

// 칭찬 카드 (자연스러울 때)
  if (message.reportType === 'PRAISE') {
    return (
      <div className={styles.praiseCard}>
        <div className={styles.praiseCard2}>
          <p className={styles.praiseCard3}>아주 좋아요!</p>
        </div>
        <div className={styles.praiseCard4}>
          <p className={styles.praiseCard5}>상황과 질문을 함께 잘 전달했어요</p>
        </div>
      </div>
    );
  }



  // 표현 자연스러움 (개선점 없을 때)
  if (message.reportType === 'EXPRESSION_FEEDBACK') {
    return (
      <div className={styles.reportCard}>
        <div className={styles.reportCard2}>
          <p className={styles.reportCard3}>
            표현 점수: {message.score}점
          </p>
        </div>
        
        <div className={styles.reportCard4}>
          <p className={styles.reportCard10}>{message.feedback}</p>
        </div>
      </div>
    );
  }
  
  if (message.reportType === 'NATIVE') {
    return (
      
      <div className={styles.reportCard}>
        <div className={styles.reportCard2}>
          <p className={styles.reportCard3}>
            ⭐ 원어민은 이렇게 말해요
          </p>
        </div>

        {message.improvements.map((item) => (
          <div key={item.messageId} className={styles.reportCard4}>
            <div className={styles.reportCard5}>
              <p className={styles.reportCard6}>"{item.suggested}"</p>
            </div>
            <div className={styles.reportCard9}>
              <p className={styles.reportCard10}>{item.explanation}</p>
            </div>
          </div>
        ))}
      </div>
    );
  }
  
  return null;
}








const formatDate = (isoString) => {
  const date = new Date(isoString);
  
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  
  const days = ['일요일', '월요일', '화요일', '수요일', '목요일', '금요일', '토요일'];
  const dayName = days[date.getDay()];
  
  return `${year}.${month}.${day} ${dayName}`;
};



// 추천 영상
function Recommendation() {
  // 추천 영상 관리, 나중에 다시 넣기
  const [recommendedData, setRecommendedData] = useState([]);

  // 썸네일 클릭 시 메인 탭에서 유튜브 영상 열기
  const handleClick = (e) => {
    // e.preventDefault() = HTML 요소의 기본 동작(default behavior)을 막는 함수
    // <a> 태그는 클릭하면 href로 이동하는게 기본 동작
    e.preventDefault();
    openYoutubeVideo(recommendedData?.videoId);
  };

  // 컴포넌트 mount 시 추천영상 API 호출, 나중에 다시 넣기 오류
  useEffect(() => {
    const fetchData = async () => {
      try {
        // // ? 써서 recommended + limit이라는 옵션값 5 로 인식
        // const result = await apiFetch('/videos/recommended?limit=5', { method: 'GET' });
        const result = await apiFetch('/videos/recommended', { method: 'GET' });
        setRecommendedData(result?.data?.recommendations[0]);
      } catch (error) {
        log.debug('데이터 로딩 실패', error);
      }
    };

    fetchData();
  }, []);

  return (
  <div className={styles['recommendation-card']}>
    <div className={styles['recommendation-card2']}>
      <div className={styles['recommendation-card3']}>
        <div className={styles['recommendation-card4']}>
          <svg xmlns="http://www.w3.org/2000/svg" width="23" height="23" viewBox="0 0 23 23" fill="none">
  <path d="M11.6471 0.41721C17.744 0.417582 22.6862 5.36163 22.6862 11.4592C22.6858 17.5565 17.7438 22.4994 11.6471 22.4997C5.55012 22.4997 0.606941 17.5567 0.606581 11.4592C0.606581 11.1755 0.618045 10.8936 0.640273 10.6155C0.665945 10.2954 0.940283 10.0573 1.26137 10.0573C1.64929 10.0577 1.94593 10.4012 1.91908 10.7883C1.90366 11.0097 1.89564 11.2339 1.89564 11.4592C1.896 16.8453 6.26146 21.2107 11.6471 21.2107C17.0324 21.2103 21.3967 16.8451 21.3971 11.4592C21.3971 6.07304 17.0327 1.70665 11.6471 1.70627C11.422 1.70627 11.1975 1.71454 10.9762 1.72971C10.589 1.75601 10.2453 1.45871 10.2453 1.07053C10.2454 0.749618 10.4835 0.476337 10.8034 0.450901C11.0815 0.428973 11.3635 0.41721 11.6471 0.41721ZM13.736 15.1404H12.3239L11.7086 13.321H8.93861L8.32777 15.1404H6.91566L9.50697 7.77951H11.1344L13.736 15.1404ZM15.9318 15.1404H14.6105V7.77951H15.9318V15.1404ZM9.29457 12.2531H11.3527L10.3507 9.28537H10.2907L9.29457 12.2531ZM4.4767 0.311741C4.54544 -0.0972571 5.13007 -0.106529 5.21205 0.300022L5.27943 0.635472C5.68492 2.64634 7.33556 4.16964 9.37221 4.41477C9.7578 4.46151 9.80116 5.00459 9.42787 5.11203L8.96058 5.2468C7.10696 5.77891 5.68432 7.26854 5.23842 9.14475L5.21937 9.21946C5.12358 9.62014 4.54946 9.60923 4.46937 9.20481C4.09333 7.30606 2.6786 5.78225 0.813124 5.26584L0.260878 5.11203C-0.115769 5.00773 -0.0731594 4.46129 0.315077 4.41623C2.41283 4.17267 4.09713 2.57156 4.4474 0.488987L4.4767 0.311741Z" fill="#7F7569"/>
</svg>
        </div>
        <div className={styles['recommendation-card5']}>
          <p className={styles['recommendation-card6']}>이런 영상은 어때요?</p>
        </div>
      </div>


      <div className={styles['recommendation-card7']}>
        <p className={styles['recommendation-card8']}>문맥 연결에 약한 부분을 보완해줄 영상이에요! 같이 한 번 봐볼까요?</p>
      </div>
    </div>


    <a 
    className={styles['recommendation-card9']}
    href={`https://youtube.com/watch?v=${recommendedData?.videoId}`}
    onClick={handleClick}
    rel="noopener noreferrer"
    >
      <div className={styles['recommendation-card10']}>
        <div className={styles['recommendation-card11']}>
          <img 
          className={styles['quiz-result-thumbnail']}
          src={`https://img.youtube.com/vi/${recommendedData?.videoId}/maxresdefault.jpg`}
          alt={`${recommendedData?.title} 영상 썸네일`}
          onError={(e) => {
            e.target.src = `https://img.youtube.com/vi/${recommendedData?.videoId}/hqdefault.jpg`;
          }}
          />
          <div className={styles['recommendation-card12']}>
            <div className={styles['recommendation-card13']}>
              <p className={styles['recommendation-card14']}>
                {recommendedData?.duration}
              </p>
            </div>
          </div>
        </div>




        <div className={styles['recommendation-card15']}>
          <div className={styles['recommendation-card16']}></div>
            <div className={styles['recommendation-card17']}>
              <p className={styles['recommendation-card18']}>{recommendedData?.title}</p>
              <p className={styles['recommendation-card19']}>{recommendedData?.channelName}</p>
            </div>
          
        </div>
      </div>
    </a>
  </div>
  )
}


// 최종정산 페이지
function ReportPage({ reportData, chatData }) {
  // log.debug('최종 정산 값', reportData, chatData);
// function ReportBubble({ message }) {
  if (!reportData || !chatData ) return null;
  const { 
  overall, 
  expressionNaturalness, 
  pronunciationScore,
  // weaknessAnalysis,
  // summary 
} = reportData;

  const getGrade = (score) => {
    if (score >= 95) return 'S';
    if (score >= 85 && score < 95) return 'A';
    if (score >= 75 && score < 85) return 'B';
    if (score >= 60 && score < 75) return 'C';
    if (score >= 50 && score < 60) return 'D';
    return 'E';
  };

  // const {
  //   overall,
  //   pronunciationScore,
  //   expressionNaturalness,
  //   wordUsage,
  // } = reportData;

  return (
    <div className={styles.reportPage}>
      <div className={styles.reportPage2}>
        <p className={styles.reportPage3}>AI TALK 정산</p>
      </div>

      <div className={styles.reportPage4}>
        <div className={styles.reportPage5}>
          <div className={styles.reportPage7}>
            <div className={styles.reportPage8}>
              <div className={styles.reportPage9}>
                <div className={styles.reportPage10}>
                  <div className={styles.reportPage11}>
                    <p className={styles.reportPage12}>최종정산</p>
                  </div>
                </div>

                <div className={styles.gradeRow}>
                <div className={styles.reportPage13}>
                  <div className={styles.reportPage14}>
                    <p className={styles.reportPage15}>종합</p>
                  </div>

                  <div className={styles.reportPage16}>
                    <p className={styles.reportPage17}>{getGrade(overall?.score)}</p>
                  </div>
                </div>

                  <div className={styles.reportPage18}></div>

                  <div className={styles.reportPage19}>
                    <div className={styles.reportPage20}>
                      <p className={styles.reportPage21}>발음</p>
                    </div>

                    <div className={styles.reportPage22}>
                      <p className={styles.reportPage23}>
                        {pronunciationScore?.available 
                        ? getGrade(pronunciationScore.overallScore) 
                        : '-'}</p>
                    </div>
                  </div>

                    <div className={styles.reportPage24}></div>


                    <div className={styles.reportPage25}>
                      <div className={styles.reportPage26}>
                        <p className={styles.reportPage27}>표현</p>
                      </div>

                      <div className={styles.reportPage28}>
                        <p className={styles.reportPage29}>{getGrade(expressionNaturalness?.overallScore)}</p>
                      </div>
                    </div>
                  </div>
              </div>

              <div className={styles.reportPage30}>
                <div className={styles.reportPage31}>
                  <div className={styles.reportPage32}>
                    <p className={styles.reportPage33}>{overall?.goodPoints}</p>
                    <p className={styles.reportPage33}>{overall?.improvePoints}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {expressionNaturalness?.improvements?.[0] && (
        <div className={styles.reportPage34}>
          <div className={styles.reportPage35}>
            <div className={styles.reportPage36}>
              <div className={styles.reportPage37}>
                <div className={styles.reportPage38}>
                  <p className={styles.reportPage39}>
                    기억하면 좋을 표현
                  </p>
                </div>

                <div className={styles.reportPage40}>

                  <div className={styles.reportPage41}>
                    <div className={styles.reportPage42}>
                      <svg 
                      className={styles.reportPage43}
                      xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
  <path d="M11 6.27788C11 5.31233 9.91769 4.74216 9.12137 5.2882L6.15336 7.32341C6.05346 7.39191 5.93517 7.42857 5.81405 7.42857H3.2C2.53726 7.42857 2 7.96583 2 8.62857V15.3714C2 16.0342 2.53726 16.5714 3.2 16.5714H5.81405C5.93517 16.5714 6.05346 16.6081 6.15336 16.6766L9.12137 18.7118C9.91769 19.2578 11 18.6877 11 17.7221V6.27788Z" fill="#5E5A4E"/>
  <path d="M15.1035 11.9946C15.1035 10.9076 14.6709 9.86449 13.9024 9.0957C13.5516 8.74412 13.5525 8.17396 13.9038 7.82275C14.2554 7.47197 14.8256 7.4729 15.1768 7.82421C16.2824 8.93049 16.9037 10.4306 16.9038 11.9946C16.9038 13.559 16.2827 15.0601 15.1768 16.1665C14.8255 16.5179 14.2554 16.5176 13.9038 16.1665C13.5524 15.8152 13.5513 15.2451 13.9024 14.8936C14.671 14.1247 15.1035 13.0818 15.1035 11.9946Z" fill="#5E5A4E"/>
  <path d="M19.3325 11.9951C19.3325 10.044 18.3976 8.12947 16.6577 6.68799C16.275 6.37086 16.222 5.80363 16.5391 5.4209C16.8562 5.03824 17.4234 4.98517 17.8061 5.30225C19.908 7.0437 21.1313 9.44558 21.1313 11.9951C21.1313 14.5447 19.908 16.9465 17.8061 18.688C17.4234 19.0051 16.8562 18.952 16.5391 18.5693C16.222 18.1866 16.275 17.6194 16.6577 17.3023C18.3976 15.8608 19.3325 13.9462 19.3325 11.9951Z" fill="#5E5A4E"/>
</svg>
                    </div>

                    <div className={styles.reportPage44}>
                      <p className={styles.reportPage45}>{expressionNaturalness.improvements[0].original}</p>
                    </div>
                  </div>




                  <div className={styles.reportPage46}>
                    <div className={styles.reportPage47}>
                      <svg 
                      className={styles.reportPage48}
                      xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
  <path d="M11 6.27788C11 5.31233 9.91769 4.74216 9.12137 5.2882L6.15336 7.32341C6.05346 7.39191 5.93517 7.42857 5.81405 7.42857H3.2C2.53726 7.42857 2 7.96583 2 8.62857V15.3714C2 16.0342 2.53726 16.5714 3.2 16.5714H5.81405C5.93517 16.5714 6.05346 16.6081 6.15336 16.6766L9.12137 18.7118C9.91769 19.2578 11 18.6877 11 17.7221V6.27788Z" fill="#01CF8A"/>
  <path d="M15.1035 11.9946C15.1035 10.9076 14.6709 9.86449 13.9024 9.0957C13.5516 8.74412 13.5525 8.17396 13.9038 7.82275C14.2554 7.47197 14.8256 7.4729 15.1768 7.82421C16.2824 8.93049 16.9037 10.4306 16.9038 11.9946C16.9038 13.559 16.2827 15.0601 15.1768 16.1665C14.8255 16.5179 14.2554 16.5176 13.9038 16.1665C13.5524 15.8152 13.5513 15.2451 13.9024 14.8936C14.671 14.1247 15.1035 13.0818 15.1035 11.9946Z" fill="#01CF8A"/>
  <path d="M19.3325 11.9951C19.3325 10.044 18.3976 8.12947 16.6577 6.68799C16.275 6.37086 16.222 5.80363 16.5391 5.4209C16.8562 5.03824 17.4234 4.98517 17.8061 5.30225C19.908 7.0437 21.1313 9.44558 21.1313 11.9951C21.1313 14.5447 19.908 16.9465 17.8061 18.688C17.4234 19.0051 16.8562 18.952 16.5391 18.5693C16.222 18.1866 16.275 17.6194 16.6577 17.3023C18.3976 15.8608 19.3325 13.9462 19.3325 11.9951Z" fill="#01CF8A"/>
</svg>
                    </div>

                    <p className={styles.reportPage49}>{expressionNaturalness.improvements[0].suggested}</p>
                  </div>
                </div>
              </div>

              <div className={styles.reportPage50}>
                <div className={styles.reportPage51}>
                  <div className={styles.reportPage52}>
                    <p className={styles.reportPage53}>{expressionNaturalness.improvements[0].explanation}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
        )}

        {pronunciationScore?.available && pronunciationScore?.weakSentences?.[0] && (
        <div className={styles.reportPage54}>
          <div className={styles.reportPage55}>
            <div className={styles.reportPage56}>
              <div className={styles.reportPage57}>
                <div className={styles.reportPage58}>
                  <p className={styles.reportPage59}>발음이 아쉬웠던 문장</p>
                </div>

                <div className={styles.reportPage60}>
                  <div className={styles.reportPage61}>
                    <div className={styles.reportPage62}>
                      <svg 
                      className={styles.reportPage63}
                      xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
  <path d="M11 6.2779C11 5.31234 9.91769 4.74217 9.12137 5.28822L6.15336 7.32342C6.05346 7.39192 5.93517 7.42859 5.81405 7.42859H3.2C2.53726 7.42859 2 7.96584 2 8.62859V15.3714C2 16.0342 2.53726 16.5714 3.2 16.5714H5.81405C5.93517 16.5714 6.05346 16.6081 6.15336 16.6766L9.12137 18.7118C9.91769 19.2579 11 18.6877 11 17.7221V6.2779Z" fill="#5E5A4E"/>
  <path d="M15.1035 11.9946C15.1035 10.9076 14.6709 9.8645 13.9024 9.09571C13.5516 8.74414 13.5525 8.17397 13.9038 7.82277C14.2554 7.47198 14.8256 7.47292 15.1768 7.82423C16.2824 8.9305 16.9037 10.4306 16.9038 11.9946C16.9038 13.559 16.2827 15.0601 15.1768 16.1665C14.8255 16.5179 14.2554 16.5176 13.9038 16.1665C13.5524 15.8152 13.5513 15.2451 13.9024 14.8936C14.671 14.1247 15.1035 13.0818 15.1035 11.9946Z" fill="#5E5A4E"/>
  <path d="M19.3325 11.9951C19.3325 10.044 18.3976 8.12945 16.6577 6.68798C16.275 6.37085 16.222 5.80361 16.5391 5.42089C16.8562 5.03823 17.4234 4.98515 17.8061 5.30224C19.908 7.04369 21.1313 9.44557 21.1313 11.9951C21.1313 14.5446 19.908 16.9465 17.8061 18.688C17.4234 19.0051 16.8562 18.952 16.5391 18.5693C16.222 18.1866 16.275 17.6194 16.6577 17.3022C18.3976 15.8608 19.3325 13.9462 19.3325 11.9951Z" fill="#5E5A4E"/>
</svg>
                    </div>

                    <div className={styles.reportPage64}>
                      <p className={styles.reportPage65}>1111</p>
                    </div>
                  </div>
                </div>
              </div>

              <div className={styles.reportPage66}>
                <div className={styles.reportPage67}>
                  <div className={styles.reportPage68}>
                    <p className={styles.reportPage69}>2222</p>
                  </div>
                </div>
              </div>
            </div>

            <div className={styles.reportPage70}>
              <div className={styles.reportPage71}>
                <svg 
                className={styles.reportPage72}
                xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
  <path d="M11 6.27787C11 5.31231 9.91769 4.74214 9.12137 5.28819L6.15336 7.32339C6.05346 7.39189 5.93517 7.42855 5.81405 7.42855H3.2C2.53726 7.42855 2 7.96581 2 8.62855V15.3714C2 16.0342 2.53726 16.5714 3.2 16.5714H5.81405C5.93517 16.5714 6.05346 16.6081 6.15336 16.6766L9.12137 18.7118C9.91769 19.2578 11 18.6877 11 17.7221V6.27787Z" fill="#01CF8A"/>
  <path d="M15.1035 11.9946C15.1035 10.9076 14.6709 9.86447 13.9024 9.09568C13.5516 8.74411 13.5525 8.17394 13.9038 7.82273C14.2554 7.47195 14.8256 7.47289 15.1768 7.8242C16.2824 8.93047 16.9037 10.4306 16.9038 11.9946C16.9038 13.559 16.2827 15.0601 15.1768 16.1665C14.8255 16.5179 14.2554 16.5176 13.9038 16.1665C13.5524 15.8152 13.5513 15.2451 13.9024 14.8935C14.671 14.1247 15.1035 13.0818 15.1035 11.9946Z" fill="#01CF8A"/>
  <path d="M19.3325 11.9951C19.3325 10.044 18.3976 8.12945 16.6577 6.68798C16.275 6.37085 16.222 5.80361 16.5391 5.42089C16.8562 5.03823 17.4234 4.98515 17.8061 5.30224C19.908 7.04369 21.1313 9.44557 21.1313 11.9951C21.1313 14.5446 19.908 16.9465 17.8061 18.688C17.4234 19.0051 16.8562 18.952 16.5391 18.5693C16.222 18.1866 16.275 17.6194 16.6577 17.3022C18.3976 15.8608 19.3325 13.9462 19.3325 11.9951Z" fill="#01CF8A"/>
</svg>
              </div>

              <p className={styles.reportPage73}>모범 발음 듣기</p>
            </div>
          </div>
        </div>
          
        )}

        <Recommendation />
      </div>



























{/* 
      캐릭터
      <div className={styles.bar2}>
        <div 
        className={styles.bar3}
        style={{ backgroundImage: `url(${frog2})` }}
        >
        </div>
      </div>

      한마디
      <div className={styles.balloon}>
        <div className={styles.balloon2}>
          <p className={styles['balloon2-t']}>
            {expressionNaturalness?.feedback}
          </p>
        </div>
      </div>

      <div className={styles.reportPage3}>

        <div className={styles.reportPage4}>
          <p>AI 채팅 진행 결과</p>

          <div className={styles.reportPage5}>
            <div className={styles.reportPage6}>
              <p className={styles.reportPage7}>종합 점수</p>
              <p className={styles.reportPage8}>{overall?.score ?? '-'}</p>
            </div>

            {pronunciationScore?.available && (
              <>
                <div className={styles.reportPage9}></div>
                <div className={styles.reportPage6}>
                  <p className={styles.reportPage7}>발음 정확도</p>
                  <p className={styles.reportPage8}>
                    {pronunciationScore?.overallScore ?? '-'}
                  </p>
                </div>
              </>
            )}

            <div className={styles.reportPage9}></div>

            <div className={styles.reportPage6}>
              <p className={styles.reportPage7}>표현 자연도</p>
              <p className={styles.reportPage8}>
                {expressionNaturalness?.overallScore ?? '-'}
              </p>
            </div>
          </div>

          <div className={styles.reportPage10}>
            목표 단어 {wordUsage?.targetWord}를 {wordUsage?.usageCount}회 사용했어요!
          </div>
        </div>

        발음이 아쉬웠던 문장 (있을 때만)
        {pronunciationScore?.available && 
          pronunciationScore?.weakSentences?.length > 0 && (
          <div className={styles.reportPage12}>
            <p className={styles.reportPage13}>🎤 발음이 아쉬웠던 문장</p>

            {pronunciationScore.weakSentences.map((s, i) => (
              <div key={i} className={styles.reportPage14}>
                <p className={styles.reportPage15}>{s.sentence}</p>
                <p className={styles.reportPage16}>{s.feedback}</p>

                <button className={styles.sound}>
                  <div className={styles.sound2}>🔊</div>
                  <p className={styles.sound3}>모범 발음 듣기</p>
                </button>
              </div>
            ))}
          </div>
        )}

        기억하면 좋은 표현
        {expressionNaturalness?.improvements?.length > 0 && (
          <div className={styles.reportPage12}>
            <p className={styles.reportPage13}>기억하면 좋은 표현</p>

            {expressionNaturalness.improvements.map((item, i) => (
              <div key={i} className={styles.reportPage14}>
                <p className={styles.reportPage15}>{item.suggested}</p>
                <p className={styles.reportPage16}>{item.explanation}</p>

                <button className={styles.sound}>
                  <div className={styles.sound2}></div>
                  <p className={styles.sound3}>모범 발음 듣기</p>
                </button>
              </div>
            ))}
          </div>
        )}

        다음엔 이렇게
        {overall?.improvePoints && (
          <div className={styles.reportPage17}>
            <p className={styles.reportPage13}>다음엔 이렇게</p>
            <div className={styles.reportPage18}>
              <p className={styles.reportPage19}>{overall.improvePoints}</p>
            </div>
          </div>
        )}
      </div> */}
    </div>
  );
}




// 채팅방
function ChatRoom ({ setChatData, chatData, progress, setProgress, setMessages, messages, onFetchReport, isSelectMode, onSelectMessage, isReportOpen, selectedTargetId, isMessage, reportData, step, audioAllowed, setAudioAllowed }) {

  const audioQueueRef = useRef([]);
  const isPlayingRef = useRef(false);
  const addToQueueRef = useRef(null);

  const [showVoiceConsent, setShowVoiceConsent] = useState(false);

  // 동의 중복클릭 방지
  const [isConsenting, setIsConsenting] = useState(false);

  const [isRecording, setIsRecording] = useState(false);
  const recorderWindowIdRef = useRef(null);

  const clientRef = useRef(null);

  // 현재 재생 중인 오디오 추적 (중복 재생 방지)
  const currentAudioRef = useRef(null);

  const [suggestion, setSuggestion] = useState(null);      // 추천
  const [isCompleted, setIsCompleted] = useState(false);   // 종료 여부
  const [toast, setToast] = useState(null);

  const chatRoomId = chatData?.chatRoomId;

  // 첫 진입 시 AI 첫 메시지 대기
  const [isWaiting, setIsWaiting] = useState(!chatData.hasPreviousMessages);

  const [chatStartDate] = useState(new Date().toISOString());

  const progressRef = useRef(progress);

  const messagesContainerRef = useRef(null);

  const navigate = useNavigate();

  // 매 렌더링마다 최신 함수 저장
  addToQueueRef.current = (audioUrl) => {
    audioQueueRef.current.push(audioUrl);
    playNext();
  };




  const playNext = async () => {
    if (isPlayingRef.current) return;
    if (audioQueueRef.current.length === 0) return;
    if (!audioAllowed) return;
    
    isPlayingRef.current = true;
    const url = audioQueueRef.current.shift();
    
    try {
      const audio = new Audio(url);
      await new Promise((resolve, reject) => {
        audio.onended = resolve;
        audio.onerror = reject;
        audio.play().catch(reject);
      });
    } catch (error) {
      log.debug('재생 실패', error);
    } finally {
      isPlayingRef.current = false;
      playNext();
    }
  };



const showToast = (message, type = 'info') => {
  setToast({ message, type });
  setTimeout(() => setToast(null), 3000);  // 3초 후 사라짐
};


  // 시간 포맷
  const formatTime = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };



  // 녹음 시작
  const startRecording = () => {
    chrome.windows.create({
      url: chrome.runtime.getURL('recorder.html'),
      type: 'popup',
      width: 360,
      height: 320,
      focused: true,  // 권한 팝업 위해
    }, (window) => {
      recorderWindowIdRef.current = window.id;
    });
  };

  // 녹음 정지
  const stopRecording = () => {
    chrome.runtime.sendMessage({
      type: 'STOP_RECORDING',
    });

  };






  // 녹음 취소 (음성 -> 텍스트 모드)
  const cancelRecording = () => {
    chrome.runtime.sendMessage({
      type: 'CANCEL_RECORDING',
    });
    
    // 백업 창 직접 닫기
    if (recorderWindowIdRef.current) {
      chrome.windows.remove(recorderWindowIdRef.current).catch(() => {});
      recorderWindowIdRef.current = null;
    }

    setIsRecording(false);
  };


// 팝업 X로 닫은 경우 대응
useEffect(() => {
  const handleWindowClosed = (windowId) => {
    if (windowId === recorderWindowIdRef.current) {
      recorderWindowIdRef.current = null;
      setIsRecording(false);
    }
  };

  chrome.windows.onRemoved.addListener(handleWindowClosed);
  
  return () => {
    chrome.windows.onRemoved.removeListener(handleWindowClosed);
  };
}, []);



  // 언마운트 시 녹음 창 정리
  useEffect(() => {
    return () => {
      if (recorderWindowIdRef.current) {
        chrome.windows.remove(recorderWindowIdRef.current).catch(() => {});
      }
    };
  }, []);









const scrollToBottom = () => {
  const container = messagesContainerRef.current;
  if (!container) return;
  // 즉시 이동은 smooth 대신 instant 쓰기
  container.scrollTo({
    // 컨테이너의 맨 끝까지 강제 스크롤
    top: container.scrollHeight,
    behavior: 'smooth',
  });
};


  // progress 바뀔 때마다 ref 업데이트
  useEffect(() => {
    progressRef.current = progress;
  }, [progress]);


// 메시지 추가될 때마다 자동 스크롤 
useEffect(() => {
  // 약간 딜레이 후 (DOM 업데이트 후)
  const timer = setTimeout(scrollToBottom, 100);
  return () => clearTimeout(timer);
}, [messages, isCompleted, suggestion, isSelectMode, isReportOpen]);




  useEffect(() => {
    if (!chatRoomId) return;

    // 언마운트 감지용
    let cancelled = false;
    let client;




const subscribeToChat = () => {
  try {
    client.subscribe('/user/queue/chat', (message) => {
      try {
        const data = JSON.parse(message.body);
        handleServerMessage(data);
      } catch (e) {
        log.debug('메시지 파싱 실패:', e);
      }
    });
  } catch (e) {
    log.debug('구독 실패:', e);
  }
};

const requestIntro = () => {
  // 이어하기면 스킵
  if (chatData.hasPreviousMessages) return;
  
  try {
    client.publish({
      destination: '/app/chat/start',
      body: JSON.stringify({ chatRoomId }),
    });
  } catch (e) {
    log.debug('인트로 요청 실패:', e);
  }
};


// 에러 코드들
const handleError = ({ code, message }) => {

  // 에러 발생 시 항상 대기 해제
  setIsWaiting(false);


  switch (code) {
    case 'HINT_FAILED':
      showToast( message || '힌트 생성 중 오류가 발생했습니다.', 'error');

      break;


    case 'HINT_NOT_AVAILABLE':
      showToast( message || '타겟 단어가 없어 힌트를 제공할 수 없습니다.', 'warning');

      break;





    case 'STT_FAILED':
      showToast('음성을 인식할 수 없습니다. 다시 시도해주세요.', 'error');
      // 마지막 streaming 메시지가 있으면 제거 (실패한 응답)
      setMessages(prev => prev.filter(msg => !msg.streaming));
      break;
    
    case 'AI_TIMEOUT':
      showToast('AI 응답이 지연되고 있습니다.', 'warning');
      setMessages(prev => prev.filter(msg => !msg.streaming));
      break;
    
    case 'AI_FAILED':
      showToast('AI 응답 생성에 실패했습니다.', 'error');
      setMessages(prev => prev.filter(msg => !msg.streaming));
      break;
    
    case 'TTS_FAILED':
      // 음성 합성만 실패, 텍스트는 정상 표시되었을 것
      showToast('음성 변환에 실패했습니다. 텍스트로 확인해주세요.', 'warning');
      break;
    
    case 'INVALID_INPUT':
      showToast(message || '입력이 올바르지 않습니다.', 'error');
      break;
    
    case 'UNAUTHORIZED':
      showToast('로그인이 만료되었습니다.', 'error');
      // 로그인 페이지로 이동 처리
      // 또는 토큰 재발급 시도
      break;
    
    case 'CHAT_ROOM_NOT_FOUND':
      showToast('채팅방을 찾을 수 없습니다.', 'error');
      // 이전 화면으로 이동
      break;
    
    case 'CHAT_ROOM_COMPLETED':
    case 'MAX_TURN_REACHED':
      // 이미 종료됐거나 턴이 다 됨
      setIsCompleted(true);
      showToast('대화가 종료되었습니다.', 'info');
      break;
    
    case 'INTERNAL_ERROR':
    default:
      showToast(message || '서버 오류가 발생했습니다.', 'error');
      break;
  }
};

    const handleServerMessage = (data) => {

      // data 라는 키를 꺼내서 payload라는 이름으로 사용
      const { type, data: payload } = data;
    
      switch (type) {
        // ========== 사용자 음성 -> 텍스트 변환 완료 ==========
        case 'USER_TEXT':
          setMessages(prev => [...prev, {
            messageId: payload.messageId,
            senderType: 'USER',
            content: payload.content,
            audioUrl: payload.audioUrl,
            highlightWord: payload.highlightWord,
            remainingTurnAtTime: progressRef.current.remainingTurn,
          }]);
          break;
    
    
        // ========== 발음 평가 결과 (USER 메시지에 추가) ==========
        case 'PRONUNCIATION':
          setMessages(prev => prev.map(msg => 
            msg.messageId === payload.messageId
              ? {
                  ...msg,
                  pronunciation: {
                    score: payload.pronunciationScore,
                    isNatural: payload.isNatural,
                    recommendedAlternative: payload.recommendedAlternative,
                    wordUsedNaturally: payload.wordUsedNaturally,
                  }
                }
              : msg
          ));
          break;

        // ========== AI 응답 스트리밍 (타이핑 효과) ==========
        case 'AI_TEXT_CHUNK':
          setMessages(prev => {
            const last = prev[prev.length - 1];
        
            // 마지막이 스트리밍 중인 AI 메시지면 chunk 누적
            if (last?.senderType === 'AI' && last?.streaming) {
              return [
                ...prev.slice(0, -1),
                { ...last, content: last.content + payload.chunk }
              ];
            }
        
            // 새 AI 메시지 시작 (messageId는 아직 없음, 임시값)
            return [...prev, {
              messageId: `streaming_${Date.now()}`,  // 임시 ID
              senderType: 'AI',
              content: payload.chunk,
              streaming: true,
            }];
          });
          
          break;
    
    
        // ========== AI 응답 완료 ==========
        // AI_TEXT_CHUNK × N → AI_TEXT_DONE 로 진행되야 하는데 CHUNK 없이 AI_TEXT_DONE만 보내고 있음
        // streaming 있으면 업데이트, 없으면 새로 추가하게 설정
        case 'AI_TEXT_DONE':
          setMessages(prev => {
            // 이미 같은 messageId 있으면 무시 (중복 방지)
            const exists = prev.some(msg => msg.messageId === payload.messageId);
            if (exists) return prev;

            const last = prev[prev.length - 1];
            let updated;

            // 케이스 1: streaming 메시지가 있으면 → 완료 처리 (CHUNK 받았을 때)
            if (last?.streaming) {
              return prev.map(msg => 
                msg.streaming
                  ? {
                    ...msg,
                    messageId: payload.messageId,
                    content: payload.content,
                    highlightWord: payload.highlightWord,
                    streaming: false,
                    audioUrl: msg.pendingAudioUrl || msg.audioUrl,
                    pendingAudioUrl: undefined,
                  }
                : msg
              );
            } else {
              // 케이스 2: streaming 메시지 없으면 새 메시지 추가 (CHUNK 없이 DONE만 왔을 때)
              updated = [...prev, {
                messageId: payload.messageId,
                senderType: 'AI',
                content: payload.content,
                highlightWord: payload.highlightWord,
                streaming: false,
              }];
            }

    // recommendedAlternative 있으면 자동으로 SUGGESTION 메시지 추가
    if (payload.recommendedAlternative) {
      updated = [...updated, {
        messageId: `suggestion_${payload.messageId}`,
        senderType: 'REPORT',
        reportType: 'SUGGESTION_INLINE',  // ← 정산용 SUGGESTION과 구분
        recommendedAlternative: payload.recommendedAlternative,
      }];
    }

  // 자연스럽고 단어도 잘 사용 "아주 좋아요!"
    else if (payload.isNatural === true && payload.wordUsedNaturally === true) {
      updated = [...updated, {
        messageId: `praise_${payload.messageId}`,
        senderType: 'REPORT',
        reportType: 'PRAISE',
      }];
    }
            return updated;
        });
        setIsWaiting(false);
        break;
    
    
        // ========== AI 음성 도착 (TEXT_DONE보다 늦을 수 있음) ==========
        case 'AI_AUDIO':
          setMessages(prev => {
            const found = prev.find(msg => msg.messageId === payload.messageId);
    
            if (found) {
              // senderType이 AI일 때만 재생
              if (found.senderType === 'AI' && payload.audioUrl) {
                addToQueueRef.current?.(payload.audioUrl);
              }

              // 메시지 있음 - audioUrl 추가
              return prev.map(msg => 
                msg.messageId === payload.messageId
                  ? { ...msg, audioUrl: payload.audioUrl }
                  : msg
              );
            }
            
            // messageId 매칭 안 되고 streaming도 없으면 임시 저장 필요
            const hasStreaming = prev.some(msg => msg.streaming);
            if (hasStreaming) {
              if (payload.audioUrl) {
                addToQueueRef.current?.(payload.audioUrl);
              }
              return prev.map(msg => 
                msg.streaming ? { ...msg, pendingAudioUrl: payload.audioUrl } : msg
              );
            }
            
            return prev;
          });
          setIsWaiting(false);
          break;
    
    
        // ========== 턴 진행 상황 ==========
        case 'PROGRESS':
          setProgress({
            currentTurn: payload.currentTurn,
            remainingTurn: payload.remainingTurn,
            maxTurn: payload.maxTurn,
            isCompleted: payload.isCompleted,
          });
          break;

        // ========== 학습 조건 충족 알림 ==========
        case 'SUGGESTION':
          // 조기 종료 처리
          if (payload.suggestionType === 'COMPLETION') {
            setProgress(prev => ({ ...prev, isCompleted: true }));
          }

          // 힌트 안내
          setSuggestion({
            type: payload.suggestionType,
            message: payload.payload.message,
          });
          break;

        // ========== 채팅 종료 (10턴 도달) ==========
        case 'COMPLETION':
          setIsCompleted(true);
          break;

        // ========== 에러 처리 ==========
        case 'ERROR':
          handleError(payload);
          setIsWaiting(false);
          break;


        // ========== 힌트 제안 서버가 자동 푸시 ==========
        case 'HINT_OFFER':

          setMessages(prev => {
            // 중복 방지를 위해 이전에 나온 메시지들 확인해서 HINT_OFFER 메시지가 있는지 확인
            // .some(): 조건 만족하는 요소가 1개라도 있으면 true
            const hasHintOffer = prev.some(msg => 
              msg.senderType === 'HINT_OFFER'
            );
    
            // 이미 있으면 추가하지 않고 기존 배열 그대로 반환
            // React가 리렌더링 안 함
            // 같은 힌트 팝업 두 번 안 뜸
            if (hasHintOffer) return prev;
    
            // 없으면 새 HINT_OFFER 메시지 추가
            return [...prev, {
              // 임시 Id
              messageId: `hint_offer_${Date.now()}`,
              senderType: 'HINT_OFFER',
              message: payload.message,
              subMessage: payload.subMessage,
              hintAvailable: payload.hintAvailable,
            }];
          });

          break;


        // ========== 힌트 카드 받기 ==========
        case 'HINT_CARD':

          setMessages(prev => {
            // senderType으로 체크 (한 채팅방에 1개만)
            const hasHintCard = prev.some(msg => 
              msg.senderType === 'HINT_CARD'
            );
    
            // 이미 있으면 추가하지 않고 기존 배열 그대로 반환
            // React가 리렌더링 안 함
            // 같은 힌트 팝업 두 번 안 뜸
            if (hasHintCard) return prev;
    
            // 없으면 새 HINT_OFFER 메시지 추가
            return [...prev, {
              messageId: `hint_card_${Date.now()}`,
              senderType: 'HINT_CARD',
                  word: payload.word,
    meanings: payload.meanings,
    contextMessage: payload.contextMessage,
    guideMessage: payload.guideMessage,
    exampleSentence: payload.exampleSentence,
    exampleTranslation: payload.exampleTranslation,
    highlightWord: payload.highlightWord,

            }];
          });

          break;


        default:
          log.debug('알 수 없는 type:', type, payload);
      }
    };





    // CHAT 화면 진입 시 이어가기면 과거 메시지 조회
    // 과거 메시지 fetch 도중 새 메시지가 웹소켓으로 오면 꼬일 수 있으니 같은 유즈이펙트 공유
    const init = async () => {

      // 과거 메시지 먼저 조회
      if (chatData.hasPreviousMessages) {
        try {
          // 메시지와 단어 정보 병렬 호출
          const [response, wordRes] = await Promise.all([
            apiFetch(`/chats/rooms/${chatRoomId}/messages`),
            apiFetch(`/chats/rooms/${chatRoomId}/word`),
          ]);

          // 이미 언마운트됐으면 중단
          if (cancelled) return;

          if (response.success) {
            setMessages(response.data.messages);

            // 이어하기는 서버 값으로 자동 재생 판단
            if (response.data.voiceConsentRead === true) {
              // 이미 동의함 자동재생 ON
              setAudioAllowed(true);
            } else {
              // 미동의 팝업
              setShowVoiceConsent(true);
            }

            // progress 복원
            if (response.data.progress) {
              setProgress(response.data.progress);
            }
          }

          if (wordRes.success) {
            setChatData(prev => ({
              ...prev,
              wordId: wordRes.data.wordId,
              word: wordRes.data.word,
              meanings: wordRes.data.meanings,
            }));
          }
        } catch (e) {
          log.debug('메시지 로딩 실패:', e);
        }
      } else {
        // 새로하기는 무조건 팝업
        setShowVoiceConsent(true);
      }




      // 이미 언마운트됐으면 중단
      if (cancelled) return;

      // 토큰 가져오기
      let token = await getAccessToken();

      // WebSocket은 처음 연결 시 토큰만 검증하고, 이후 토큰이 만료돼도 연결 자체는 끊기지 않을 수 있음
      // 가져온게 만료된 토큰일 수 있으니 만료 체크 + 갱신
      if (isTokenExpired(token)) {
        log.debug('WebSocket 연결 전 토큰 만료, 갱신 시도');
        token = await refreshAccessToken();
      }

      // 자동 토큰 갱신이 실패한 경우
      if (!token) {
        log.debug('토큰 없음 - WebSocket 연결 중단');
        navigate('/login', {
          state: { from: '/ai' },
          replace: true
        });
        return;
      }



      // STOMP 연결 (실시간)
      client = new Client({

        // 서버가 SockJS 사용함
        // SockJS는 HTTP(S) 사용함
        webSocketFactory: () => new SockJS(`${import.meta.env.VITE_WS_URL}/ws-chat`),
        connectHeaders: { Authorization: `Bearer ${token}` },

        // 실패하면 5초마다 재시도(자동 재연결)
        reconnectDelay: 5000,

          // 재연결 시마다 토큰 갱신
        beforeConnect: async () => {
          let currentToken = await getAccessToken();
          if (isTokenExpired(currentToken)) {
            log.debug('STOMP 재연결 전 토큰 갱신');
            currentToken = await refreshAccessToken();
            if (!currentToken) {
              log.debug('갱신 실패 - 연결 중단');
              client.deactivate();
              navigate('/login', {
                state: { from: '/ai' },
                replace: true
              });
              return;
            }
          }
          client.connectHeaders = { Authorization: `Bearer ${currentToken}` };
        },
        // 연결 성공
        onConnect: () => {
          subscribeToChat();
          requestIntro();
        },


        // STOMP 에러
        onStompError: (frame) => {
          log.debug('STOMP 에러:', frame.headers['message']);
        },


        // WebSocket 끊김 감지
        onWebSocketClose: (event) => {
          log.debug('WebSocket 닫힘:', event.code);
        },
      });
      
      client.activate();
      clientRef.current = client;
    };

    init();

    return () => {
      cancelled = true;
      // ref로 항상 최신 ws 값 참조
      clientRef.current?.deactivate();
      clientRef.current = null;
    };
    // useRef는 의존성 배열에 안 넣어도 됨
    // navigate는 useNavigate()가 반환하는 함수인데, React Router 내부에서 stable reference로 만들어짐
    // 즉, 매 렌더링마다 새로 만들어지지 않아서 의존성에 추가해도 useEffect가 재실행되지 않음
  }, [setChatData, chatRoomId, chatData?.hasPreviousMessages, setProgress, navigate, setMessages, setAudioAllowed, setShowVoiceConsent ]);


  // 팝업에서 동의 클릭 (unlockAudio과 다른 기능임)
  const handleAudioConsent = async () => {
    if (isConsenting) return;
    setIsConsenting(true);

    try {
      // 오디오 락 해제
      const audio = new Audio(SILENT_MP3);
      audio.muted = true;
      audio.volume = 0;
      const p = audio.play();
      if (p !== undefined) {
        await p.catch((e) => log.debug('play 에러:', e));
      }
      setAudioAllowed(true);
      setShowVoiceConsent(false);

      // 서버에 저장 (다음엔 안 뜨게)
      await apiFetch('/users/me/voice-consent', {
        method: 'PATCH',
      });
      // await apiFetch('/users/me/ui-state', {
      //   method: 'PATCH',
      //   body: JSON.stringify({ voiceConsentRead: true }),
      // });
    } catch (error) {
      log.debug('서버에 저장 실패', error);
    } finally {
      setIsConsenting(false);
    }
  };


  // 자동 재생 거절
  // 서버에는 저장 안 함 (다음 접속 시 다시 팝업 뜸)
  const handleAudioReject = async () => {
    setAudioAllowed(false);       // 자동재생 OFF
    setShowVoiceConsent(false);   // 팝업 닫기
  };

  // 클릭한 메시지의 음성 재생
  const handleAiSound = (msg) => {

    if (!msg || (msg.senderType !== 'AI' && msg.senderType !== 'USER')) return;

     // 이전 재생 중인 오디오 중지
    if (currentAudioRef.current) {
      currentAudioRef.current.pause();
      currentAudioRef.current = null;
    }

    // SpeechSynthesis 중지
    if (speechSynthesis.speaking) {
      speechSynthesis.cancel();
    }

    // 오디오 URL 있으면 그걸로 재생
    if (msg.audioUrl) {
      const audio = new Audio(msg.audioUrl);
      currentAudioRef.current = audio;
      
      audio.play().catch(e => log.debug('재생 실패:', e));
      
      // 재생 끝나면 정리
      audio.onended = () => {
        currentAudioRef.current = null;
      };
    } 
    // 없으면 브라우저 TTS
    else if (msg.content) {
      const utterance = new SpeechSynthesisUtterance(msg.content);
      utterance.lang = 'en-US';

      // 목소리 살짝 다를 수 있음
      const voices = speechSynthesis.getVoices();


      utterance.voice = chatData.aiGender === 'MALE'
      ? voices.find(v => v.name === 'Google UK English Male')
      : voices.find(v => v.name === 'Google US English');



      speechSynthesis.speak(utterance);
    }
  };

  // 컴포넌트 언마운트 시 재생 중인 오디오 정리
  useEffect(() => {
    return () => {
      if (currentAudioRef.current) {
        currentAudioRef.current.pause();
      }
      if (speechSynthesis.speaking) {
        speechSynthesis.cancel();
      }
    };
  }, []);



  // 텍스트 전송
  const sendTextMessage = (text) => {

    // if (!inputText.trim()) return;
    if (isWaiting) return;
    if (!clientRef.current?.connected) return;


    // 클라이언트에서 사용자 메시지 즉시 추가 (낙관적 업데이트)
    setMessages(prev => [...prev, {
      messageId: `temp_user_${Date.now()}`,
      senderType: 'USER',
      content: text,
      remainingTurnAtTime: progress.remainingTurn, 
    }]);

    // 서버로 전송
    clientRef.current.publish({
      destination: '/app/chat/send',
      body: JSON.stringify({
        chatRoomId: chatRoomId,
        inputMode: 'TEXT',
        content: text,
        audioUrl: null,
      })
    });

    // setInputText('');
    setIsWaiting(true);
  };



  // ========== 사용자 음성 녹음 전송 ==========
  // TDZ(Temporal Dead Zone) 때문에 선언 전에 useEffect로 접근하면 에러
  const uploadAndSend = useCallback(async (audioBlob) => {
    if (isWaiting) return;

    // STOMP 연결 확인
    if (!clientRef.current?.connected) {
      log.warn('STOMP 연결 안 됨');
      return;
    }
    if (!chatRoomId) return;
    try {
      // presigned URL 받기
      const fileName = `audio_${Date.now()}.webm`;
      const presignRes = await apiFetch('/files/audio/presigned-url', {
        method: 'POST',
        body: JSON.stringify({
          fileName,
          contentType: 'audio/webm',
        })
      });

      if (!presignRes.success || !presignRes.data) {
        log.debug('presigned URL 실패:');
        return;
      }

      const { presignedUrl, audioUrl } = presignRes.data;



      // S3에 직접 업로드 (5분 안에)
      const uploadRes = await fetch(presignedUrl, {
        method: 'PUT',
        headers: { 'Content-Type': 'audio/webm' },
        body: audioBlob
      });


      
      if (!uploadRes.ok) {
        log.debug('S3 업로드 실패');
        return;
      }

      // STOMP 전송 (USER_TEXT는 서버가 알아서 푸시)
      clientRef.current.publish({
        destination: '/app/chat/send',
        body: JSON.stringify({
          chatRoomId: chatRoomId,
          inputMode: 'VOICE',
          audioUrl: audioUrl,
          content: null,
        })
      });
      setIsWaiting(true);
    } catch (e) {
      log.debug('음성 업로드 실패:', e);
    }
  }, [chatRoomId, isWaiting]);


// 녹음 데이터 수신
useEffect(() => {

  const handler = (message) => {

    if (message.type === 'RECORDING_STARTED') {
      setIsRecording(true);  // Recorder가 실제 시작하면 true
    }

    if (message.type === 'RECORDING_COMPLETE') {
      fetch(message.audioData)
        .then(res => res.blob())
        .then(blob => uploadAndSend(blob));
      setIsRecording(false);
    }

    if (message.type === 'RECORDING_ERROR') {
      showToast(`녹음 실패 ${message.error}`, 'error');
      setIsRecording(false);
      recorderWindowIdRef.current = null;
    }
  };
  
  chrome.runtime.onMessage.addListener(handler);
  return () => chrome.runtime.onMessage.removeListener(handler);
}, [uploadAndSend]);






// 홈 버튼
const handleHome = async () => {
  try {
    // 로컬 스토리지에서 제출 여부 확인
    const feedback = await loadUserData('submittedFeedback');
    if (!feedback?.hasSubmittedFeedback) {
      // 로컬에 완료 기록이 없다면 서버에 한 번 더 확인
      const check = await apiFetch('/feedback/check', { method: 'GET' });

      if (!check.data.hasSubmittedFeedback) {
        // 설문조사 미제출 상태인 경우 설문조사 페이지로 이동 (완료 후 홈으로 가도록 returnTo 설정)
        navigate('/feedback', { 
          state: { returnTo: '/' }
          , replace: true
        });
        return;
      } else {
        // 서버에는 이미 제출된 상태라면 로컬 갱신 후 그냥 홈으로 이동
        await saveUserData('submittedFeedback', { hasSubmittedFeedback: true });
      }
    }

    // 이미 설문을 완료한 유저라면 바로 홈으로 이동
    navigate('/', {replace: true});

  } catch (error) {
    log.debug('홈 이동 중 설문 여부 조회 실패:', error);
    // 에러 발생 시 사용자 경험을 위해 일단 홈으로 보내주는 안전장치(Fallback)
    navigate('/', {replace: true});
  }
};


// 힌트 요청
const handleHintAccept = (messageId) => {
  if (!clientRef.current?.connected) {
    showToast('연결이 끊어졌습니다. 잠시 후 다시 시도해주세요.', 'error');
    return;
  }

  // 서버에 힌트 요청
  clientRef.current.publish({
    destination: '/app/chat/hint',
    body: JSON.stringify({ chatRoomId }),
  });
  
  // 이전 메시지에서 HINT_OFFER 메시지 제거
  setMessages(prev => 
    prev.filter(msg => msg.messageId !== messageId)
  );

};



  // 힌트 거절
  const handleHintReject = (messageId) => {
    // 이전 메시지에서 HINT_OFFER 메시지 제거
    setMessages(prev => 
      prev.filter(msg => msg.messageId !== messageId)
    );
  };




  // 받은 메시지 화면에 그리기
  return (
    <>
      {/* 재생 동의 팝업 */}
      {showVoiceConsent && (
        <ConsentPopup 
        handleAudioConsent={handleAudioConsent}
        handleAudioReject={handleAudioReject}
        />
      )}

    <div className={styles['chat-page']}>
    <div className={styles['chat-messages']}
    ref={messagesContainerRef}
    >
      <div className={styles['chat-scenario']}>
        <div className={styles['chat-scenario2']}>
          <p className={styles['chat-scenario3']}>사용할 단어 : {chatData.word}</p>
        </div>
      </div>

      <div className={styles['chat-time']}>
        <p className={styles['chat-time2']}>{formatDate(chatStartDate)}</p>
      </div>

    {step === 'REPORT' ? (
        <ReportPage 
          reportData={reportData}
          chatData={chatData}
        />
      ) : (


    // 시나리오 정보: 아이콘 + 말풍선 3개를 하나의 그룹으로
    <>
    <div className={styles['chat-ai-group']}>
      <div className={styles['chat-ai-bubbles']}>
      <div className={styles['chat-ai']}>
        <svg 
        className={styles['chat-ai2']}
        xmlns="http://www.w3.org/2000/svg" width="10" height="22" viewBox="0 0 10 22" fill="none">
          <path d="M9.7251 22C9.2251 19.5 8.57193 17.6624 7.49432 15.644C5.92429 13.0261 3.79898 11.1746 0.698629 7.79894C-1.08475 4.50573 0.752297 0.000253982 3.87887 -3.15072e-07L9.7251 -8.74231e-07L9.7251 22Z" fill="#FEFDF9"/>
        </svg>

        <div className={styles['chat-ai3']}>
          <div className={styles['chat-ai5']}>[시나리오 : {chatData.scenarioTitle}]</div>
        </div>


      </div>

      <div className={styles['chat-ai']}>
        <div className={styles['chat-ai3']}>
          <div className={styles['chat-ai5']}>{chatData.scenarioGoal}</div>
        </div>
      </div>

      <div className={styles['chat-ai']}>
        <div className={styles['chat-ai3']}>
          <div className={styles['chat-ai5']}>{chatData.scenarioSituation}</div>
        </div>
      </div>
      </div>
    </div>


      <>
        {messages.map((msg, i) => {
          const prevMsg = messages[i - 1];
          const isFirstOfGroup = !prevMsg || prevMsg.senderType !== msg.senderType;

          return (
            // 값이 겹치는거 같아 일단 미사용
          // msg.senderType === 'REPORT'?
          msg.senderType === 'HINT_OFFER'? <HintOffer key={msg.messageId} message={msg} handleHintAccept={handleHintAccept} handleHintReject={handleHintReject} />
            : msg.senderType === 'HINT_CARD'? <HintCard key={msg.messageId} message={msg} />
        
        : msg.senderType === 'AI'
          ? <AI key={msg.messageId} messageId={msg.messageId} onSelect={() => onSelectMessage(msg.messageId)} msg={msg.content} highlightWord={msg.highlightWord} isFirst={isFirstOfGroup} selectedTargetId={selectedTargetId} isMessage={isMessage} message={msg} onPlaySound={handleAiSound} />
          : msg.senderType === 'USER' ? <User key={msg.messageId} msg={msg.content} highlightWord={msg.highlightWord} remainingTurn={msg.remainingTurnAtTime} message={msg} onPlaySound={handleAiSound} turnNumber={msg.remainingTurn} />
          : null
          );
        })}

    {/* 종료 화면 */}
    {isCompleted && (
      <div className={styles.completionBox}>
        <div className={styles.completionBox2}>
        <p className={styles.completionBox4}>대화가 끝났어요</p>
        <p className={styles.completionBox4}>지금까지의 대화를 정산하시겠어요?</p>
        </div>

    <div className={styles.completionBox5}>
      <button 
      className={styles.completionBox6}
      onClick={onFetchReport}>
        <p className={styles.completionBox7}>정산하러 가기</p>
      </button>
      <button 
      className={styles.completionBox8}
      onClick={handleHome}>
        <p className={styles.completionBox9}>대화창 나가기</p>
      </button>
        </div>
      </div>
    )}

    {/* 조기 종료 Suggestion 토스트 */}
    {suggestion && (
      <div className={styles.suggestion}>
        {suggestion.message}
      </div>
    )}
      </>

</>
          )}
</div>

      {/* 메시지 인풋 입력 창 모달 떠 있을 때만 사라짐 */}
      {(!isReportOpen && step !== 'REPORT') && (
      <div className={styles['chat-input']}>
        {<ChatInput sendTextMessage={sendTextMessage} showToast={showToast} startRecording={startRecording} stopRecording={stopRecording} cancelRecording={cancelRecording} formatTime={formatTime} isWaiting={isWaiting} isRecording={isRecording} toast={toast} />}
      </div>
      )}
    </div>
    </>
  );
}





// 인풋창 분리
function ChatInput({ sendTextMessage, showToast, startRecording, stopRecording, cancelRecording, formatTime, isWaiting, isRecording, toast }) {
  // 여기서만 리렌더링
  const [inputText, setInputText] = useState('');
  const [recordingTime, setRecordingTime] = useState(0);

  // 녹음 시간 추적
  useEffect(() => {
    if (!isRecording) return;  // 녹음 중일 때만 실행

    const startTime = Date.now();
    let lastSecond = 0;
    let rafId;
    
    const tick = () => {
      const elapsed = Math.floor((Date.now() - startTime) / 1000);
      if (elapsed !== lastSecond) {
        lastSecond = elapsed;
        setRecordingTime(elapsed);
      }
      rafId = requestAnimationFrame(tick);
    };
    
    rafId = requestAnimationFrame(tick);
    
    return () => {
      if (rafId) cancelAnimationFrame(rafId);
      // 녹음 끝나면 초기화
      setRecordingTime(0);
    };
  }, [isRecording]);

  const handleSend = () => {
    const text = inputText.trim();
    if (!text) return;
    
    if (/[가-힣ㄱ-ㅎㅏ-ㅣ]/.test(text)) {
      showToast('영어로 입력해주세요', 'warning');
      setInputText('');
      return;
    }
    
    if (!/[a-zA-Z0-9]/.test(text)) {
      showToast('의미 있는 내용을 입력해주세요', 'warning');
      setInputText('');
      return;
    }
    
    sendTextMessage(text);
    setInputText('');
  };

  return (
    <>
    <div className={styles['chat-input2']}>
      <textarea 
      className={styles['chat-input3']}
      value={inputText}
      onChange={(e) => setInputText(e.target.value)}
      onKeyDown={(e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
          e.preventDefault();
          handleSend();
        }
      }}
      placeholder={
        isRecording 
        ? '편하게 말해보세요' 
        : toast 
          ? toast.message 
          : '메세지 입력'
      }
      maxLength={250}
      disabled={isWaiting || isRecording}
      >
      </textarea>
    </div>

      <div className={styles['chat-input4']}>
          <div className={styles['chat-input5']}>
            {isRecording ? (
              // 음성모드
              <>
                {/* 텍스트 모드로 전환 */}
                <button className={styles['chat-change3']}
                onClick={() => cancelRecording()}
                disabled={isWaiting}
                >
                  <svg 
                  className={styles['chat-change4']}
                  xmlns="http://www.w3.org/2000/svg" width="13" height="16" viewBox="0 0 13 16" fill="none">
                    <path d="M11.5536 6.31641C11.9925 6.36462 12.3095 6.75923 12.2616 7.19824C12.1653 8.08089 11.7446 9.35571 10.7899 10.4229C9.93662 11.3767 8.67914 12.1357 6.93253 12.3145V14.4004H8.88858C9.33022 14.4006 9.68839 14.7585 9.68839 15.2002C9.68828 15.6418 9.33015 15.9998 8.88858 16H3.37686C2.93535 15.9997 2.57716 15.6417 2.57706 15.2002C2.57706 14.7586 2.93529 14.4007 3.37686 14.4004H5.33292V12.3145C3.58665 12.1356 2.32976 11.3765 1.47647 10.4229C0.521883 9.35573 0.101168 8.08089 0.00479101 7.19824C-0.0431171 6.75908 0.273677 6.36442 0.712799 6.31641C1.15193 6.26853 1.54663 6.58531 1.59463 7.02441C1.66001 7.62323 1.96715 8.57101 2.66885 9.35547C3.34697 10.1134 4.4237 10.7558 6.13272 10.7559C7.84194 10.7558 8.91942 10.1135 9.59756 9.35547C10.2992 8.57104 10.6054 7.62319 10.6708 7.02441C10.7188 6.58526 11.1144 6.2685 11.5536 6.31641ZM6.13272 0C7.60548 0 8.79971 1.19423 8.79971 2.66699V6.22266C8.79948 7.69522 7.60533 8.88867 6.13272 8.88867C4.66022 8.88854 3.46694 7.69513 3.46671 6.22266V2.66699C3.46671 1.19432 4.66007 0.000133481 6.13272 0Z" fill="#454440"/>
                  </svg>
                </button>

                <div className={styles['chat-recording']}>
                  <p className={styles['chat-recording2']}>
                    {formatTime(recordingTime)}
                  </p>
                </div>
              </>
            ) : (
              // 텍스트 모드
              <>
                {/* 음성 모드로 전환 */}
                <button className={styles['chat-change']}
                onClick={() => startRecording()}
                disabled={isWaiting}
                >
                  
                  <svg 
                  className={styles['chat-change2']}
                  xmlns="http://www.w3.org/2000/svg" width="20" height="13" viewBox="0 0 20 13" fill="none">
                    <path d="M17.2727 0H2.72727C1.22104 0 0 1.22104 0 2.72727V10C0 11.5063 1.22104 12.7273 2.72727 12.7273H17.2727C18.779 12.7273 20 11.5063 20 10V2.72727C20 1.22104 18.779 0 17.2727 0Z" fill="#454440"/>
                    <path d="M2.72754 8.18159C3.22947 8.18169 3.6366 8.58889 3.63672 9.09079C3.63672 9.59289 3.22955 9.99989 2.72754 9.99999C2.22546 9.99999 1.81836 9.59289 1.81836 9.09079C1.81848 8.58889 2.22553 8.18159 2.72754 8.18159ZM14.5459 8.18159C15.0478 8.18179 15.455 8.58899 15.4551 9.09079C15.4551 9.59279 15.0478 9.99979 14.5459 9.99999H5.45508C4.953 9.99999 4.5459 9.59289 4.5459 9.09079C4.54602 8.58889 4.95307 8.18159 5.45508 8.18159H14.5459ZM17.2725 8.18159C17.7745 8.18159 18.1815 8.58889 18.1817 9.09079C18.1817 9.59289 17.7746 9.99999 17.2725 9.99999C16.7706 9.99979 16.3633 9.59279 16.3633 9.09079C16.3634 8.58899 16.7707 8.18189 17.2725 8.18159ZM4.5459 5.45509C5.04766 5.45529 5.45481 5.86149 5.45508 6.36329C5.45508 6.86529 5.04782 7.27229 4.5459 7.27249C4.04382 7.27249 3.63672 6.86539 3.63672 6.36329C3.63698 5.86139 4.04398 5.45509 4.5459 5.45509ZM8.18168 5.45509C8.68358 5.45509 9.09058 5.86139 9.09078 6.36329C9.09078 6.86539 8.68368 7.27249 8.18168 7.27249C7.6797 7.27229 7.27246 6.86529 7.27246 6.36329C7.27272 5.86149 7.67986 5.45519 8.18168 5.45509ZM11.8184 5.45509C12.3203 5.45509 12.7273 5.86149 12.7276 6.36329C12.7276 6.86529 12.3204 7.27239 11.8184 7.27249C11.3163 7.27249 10.9092 6.86539 10.9092 6.36329C10.9095 5.86139 11.3165 5.45509 11.8184 5.45509ZM15.4551 5.45509C15.9568 5.45529 16.363 5.86159 16.3633 6.36329C16.3633 6.86519 15.957 7.27219 15.4551 7.27249C14.953 7.27249 14.5459 6.86539 14.5459 6.36329C14.5462 5.86139 14.9532 5.45509 15.4551 5.45509ZM2.72754 2.72754C3.22957 2.72759 3.63672 3.13467 3.63672 3.63672C3.63667 4.13872 3.22954 4.54589 2.72754 4.54589C2.22549 4.54589 1.8184 4.13875 1.81836 3.63672C1.81836 3.13464 2.22546 2.72754 2.72754 2.72754ZM6.36328 2.72754C6.86535 2.72754 7.27246 3.13464 7.27246 3.63672C7.27234 4.13869 6.86528 4.54589 6.36328 4.54589C5.86149 4.54559 5.45519 4.13853 5.45508 3.63672C5.45508 3.1348 5.86142 2.7278 6.36328 2.72754ZM9.99998 2.72754C10.5021 2.72754 10.9092 3.13464 10.9092 3.63672C10.9091 4.13869 10.502 4.54589 9.99998 4.54589C9.49808 4.54579 9.09098 4.13865 9.09078 3.63672C9.09078 3.13468 9.49798 2.7276 9.99998 2.72754ZM13.6367 2.72754C14.1387 2.72766 14.5459 3.13472 14.5459 3.63672C14.5458 4.13861 14.1386 4.54579 13.6367 4.54589C13.1347 4.54589 12.7277 4.13869 12.7276 3.63672C12.7276 3.13464 13.1347 2.72754 13.6367 2.72754ZM17.2725 2.72754C17.7746 2.72754 18.1817 3.13464 18.1817 3.63672C18.1815 4.13869 17.7745 4.54589 17.2725 4.54589C16.7707 4.54569 16.3634 4.13856 16.3633 3.63672C16.3633 3.13477 16.7706 2.72775 17.2725 2.72754Z" fill="white"/>
                  </svg>
                </button>

                <div className={styles['chat-input7']}>
                  <p className={styles['chat-input8']}>
                    {inputText.length}
                    <span className={styles['chat-input9']}>/250자</span>
                  </p>
                </div>
              </>
            )}
          </div>

          <div className={styles['chat-input10']}>
            {/* <button onClick={isRecording ? stopRecording : startRecording}
            disabled={isWaiting}
            >
              {isRecording ? '⏹ 음성 녹음 정지' : '음성 녹음'}
            </button> */}
            {/* <button className={styles['chat-input12']} onClick={sendTextMessage}
            disabled={isWaiting || !inputText.trim()}
            >텍스트전송</button> */}

            {isRecording ? (
              // 녹음 정지 + 음성 전송 버튼
              <button 
          className={styles.send}
          onClick={() => stopRecording()}
          disabled={isWaiting}
        >
          <div className={styles.send2}>
            <svg 
            className={styles.send3}
            xmlns="http://www.w3.org/2000/svg" width="22" height="16" viewBox="0 0 22 16" fill="none">
  <path d="M2 7C2 6.44772 1.55228 6 1 6C0.447715 6 0 6.44772 0 7V9C0 9.55228 0.447715 10 1 10C1.55228 10 2 9.55228 2 9V7Z" fill="#454440"/>
  <path d="M6 5C6 4.44772 5.55228 4 5 4C4.44772 4 4 4.44772 4 5V11C4 11.5523 4.44772 12 5 12C5.55228 12 6 11.5523 6 11V5Z" fill="#454440"/>
  <path d="M10 2.85714C10 2.38376 9.55228 2 9 2C8.44772 2 8 2.38376 8 2.85714V13.1429C8 13.6162 8.44772 14 9 14C9.55228 14 10 13.6162 10 13.1429V2.85714Z" fill="#454440"/>
  <path d="M14 4.8C14 4.35817 13.5523 4 13 4C12.4477 4 12 4.35817 12 4.8V11.2C12 11.6418 12.4477 12 13 12C13.5523 12 14 11.6418 14 11.2V4.8Z" fill="#454440"/>
  <path d="M18 0.888889C18 0.397969 17.5523 0 17 0C16.4477 0 16 0.397969 16 0.888889V15.1111C16 15.602 16.4477 16 17 16C17.5523 16 18 15.602 18 15.1111V0.888889Z" fill="#454440"/>
  <path d="M22 5C22 4.44772 21.5523 4 21 4C20.4477 4 20 4.44772 20 5V11C20 11.5523 20.4477 12 21 12C21.5523 12 22 11.5523 22 11V5Z" fill="#454440"/>
</svg>
          </div>

          <div className={styles.send4}>
            <div className={styles.send5}>
              <svg 
              className={styles.send6}
              xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 12 12" fill="none">
  <path d="M4.15818 0.579287C4.93062 -0.193096 6.18356 -0.193096 6.95589 0.579287L10.8246 4.44795C11.2106 4.83421 11.2107 5.46117 10.8246 5.84734C10.4385 6.23291 9.81234 6.23291 9.42627 5.84734L6.77344 2.73865V11.0111C6.77324 11.5571 6.33061 12 5.78453 12C5.23846 11.9998 4.79573 11.5571 4.79563 11.0111V2.73865L1.68787 5.84734C1.30178 6.23282 0.675554 6.23291 0.289506 5.84734C-0.0965911 5.46117 -0.0964131 4.83421 0.289506 4.44795L4.15818 0.579287Z" fill="#FEFDF9"/>
</svg>
            </div>
          </div>
        </button>
            ) : (
              <button 
          onClick={() => handleSend()}
          disabled={isWaiting || !inputText.trim()}
        >
          <div className={styles.send4}>
            <div className={styles.send5}>
              <svg 
              className={styles.send6}
              xmlns="http://www.w3.org/2000/svg" width="12" height="12" viewBox="0 0 12 12" fill="none">
                <path d="M4.15818 0.579287C4.93062 -0.193096 6.18356 -0.193096 6.95589 0.579287L10.8246 4.44795C11.2106 4.83421 11.2107 5.46117 10.8246 5.84734C10.4385 6.23291 9.81234 6.23291 9.42627 5.84734L6.77344 2.73865V11.0111C6.77324 11.5571 6.33061 12 5.78453 12C5.23846 11.9998 4.79573 11.5571 4.79563 11.0111V2.73865L1.68787 5.84734C1.30178 6.23282 0.675554 6.23291 0.289506 5.84734C-0.0965911 5.46117 -0.0964131 4.83421 0.289506 4.44795L4.15818 0.579287Z" fill="#FEFDF9"/>
              </svg>
            </div>
          </div>
        </button>
            )}
          </div>
        </div>
      </>
  );
}














// AI 말풍선
const AI = React.memo(function AI({ msg, highlightWord, isFirst, isMessage, onSelect, selectedTargetId, messageId, message, onPlaySound }) {
  // 선택 여부
  const isSelected = selectedTargetId === messageId;

  // log.debug('메시지 덮어씌워졌는지 확인용', message );
  
  return (
    <div className={styles['chat-ai-group']}>
      {/* 메시지 모드인 경우 아이콘 옆에 체크박스/신고 선택 버튼 노출 */}
      {isMessage && (
        <button 
          onClick={onSelect} 
          className={styles['report-select-btn']}
        >
          {isSelected
          ? <div className={styles.agreement16}>
            {/* 선택됨 */}
            <svg 
            className={styles.agreement17}
            xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
              <path d="M12.137 3.77574C12.566 3.30103 13.2975 3.26012 13.7767 3.6859C14.2579 4.1139 14.302 4.85088 13.8743 5.33238L13.8675 5.3402L7.08809 12.6732L7.08711 12.6722C6.86614 12.9168 6.55276 13.0578 6.22284 13.058C5.88978 13.058 5.57214 12.9152 5.35076 12.6664L1.79505 8.66637C1.36701 8.18483 1.41026 7.44699 1.89173 7.01891C2.37332 6.59106 3.11121 6.63506 3.53921 7.11656L6.22968 10.1439L12.137 3.77477V3.77574Z" fill="white"/>
            </svg>
          </div>

          : <svg 
          className={styles.agreement22}
          xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
            <g clip-path="url(#clip0_935_1154)">
              <path d="M23.3 12C23.3 5.75913 18.2408 0.699951 12 0.699951C5.75918 0.699951 0.699997 5.75913 0.699997 12C0.699997 18.2408 5.75918 23.2999 12 23.2999C18.2408 23.2999 23.3 18.2408 23.3 12Z" fill="#FEFDF9" stroke="#A5E7D2" stroke-width="1.4"/>
            </g>
            <defs>
              <clipPath id="clip0_935_1154">
                <rect width="24" height="24" fill="white"/>
              </clipPath>
            </defs>
          </svg>}
        </button>
      )}



      {/* 첫 메시지일 때만 아이콘 */}
      {isFirst && <div 
      className={styles['chat-icon']}
      style={{ backgroundImage: `url(${frog})` }}
      ></div>}





      <div className={styles['chat-ai']}>
        {/* 첫 메시지에만 svg 꼬리 */}
        {isFirst && (
          <svg 
          className={styles['chat-ai2']}
          xmlns="http://www.w3.org/2000/svg" width="10" height="22" viewBox="0 0 10 22" fill="none">
            <path d="M9.7251 22C9.2251 19.5 8.57193 17.6624 7.49432 15.644C5.92429 13.0261 3.79898 11.1746 0.698629 7.79894C-1.08475 4.50573 0.752297 0.000253982 3.87887 -3.15072e-07L9.7251 -8.74231e-07L9.7251 22Z" fill="#FEFDF9"/>
          </svg>
        )}

        <div className={styles['chat-ai3']}>
          <div className={styles['chat-ai5']}>
            <MessageContent content={msg} highlightWord={highlightWord} />
          </div>
        </div>
      </div>

      <button className={styles['ai-audio']}
      onClick={() => onPlaySound(message)}
      >
        <div className={styles['ai-audio2']}>
          <div className={styles['ai-audio3']}>
            <svg 
            className={styles['ai-audio4']}
            xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M8.25 4.51859C8.25 3.87489 7.52846 3.49477 6.99758 3.8588L4.60224 5.50132C4.53564 5.54699 4.45678 5.57143 4.37603 5.57143H2.3C1.85817 5.57143 1.5 5.9296 1.5 6.37143V11.6286C1.5 12.0704 1.85817 12.4286 2.3 12.4286H4.37603C4.45678 12.4286 4.53564 12.453 4.60224 12.4987L6.99758 14.1412C7.52846 14.5052 8.25 14.1251 8.25 13.4814V4.51859Z" fill="#5CC49D"/>
              <path d="M10.9053 6.34497C11.6083 7.0482 12.0032 8.00185 12.0032 8.99622C12.0032 9.99059 11.6083 10.9442 10.9053 11.6475" stroke="#5CC49D" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M12.9238 4.49634C14.3645 5.68994 15.1738 7.30859 15.1738 8.99634C15.1738 10.6841 14.3645 12.3027 12.9238 13.4963" stroke="#5CC49D" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
      </button>
    </div>
  );
});


// User 말풍선
const User = React.memo(function User({ msg, highlightWord, remainingTurn, message, onPlaySound, turnNumber }) {

  // const turn = turnNumber ?? remainingTurn;
  const turn = turnNumber !== undefined 
  ? turnNumber + 1 
  : remainingTurn;

  return (
    <div className={styles['chat-box']}>
    <div className={styles['chat-user']}>

      <div className={styles['chat-user3']}>
        <div className={styles['chat-user4']}>
          <div className={styles['chat-user5']}>
            <MessageContent content={msg} highlightWord={highlightWord} />
          </div>
        </div>
      </div>
      <svg 
      className={styles['chat-user2']}
      xmlns="http://www.w3.org/2000/svg" width="8" height="18" viewBox="0 0 8 18" fill="none">
        <path d="M-1.43091e-07 18C0.411308 15.9545 0.948612 14.4511 1.83507 12.7996C3.1266 10.6577 4.87491 9.14286 7.4253 6.38095C8.89233 3.6865 7.38115 0.000207803 4.80919 -2.59289e-07L1.43051e-06 -7.19048e-07L-1.43091e-07 18Z" fill="#0AC290"/>
      </svg>
    </div>

    {turn !== undefined && (
      <div className={styles['user-audio']}>
        <div className={styles['chat-count']}>
          <div className={styles['chat-count2']}>
            <p className={styles['chat-count3']}>남은 개수 : {turn !== undefined ? turn - 1 : '-'}</p>
          </div>
        </div>

        <button className={styles['user-audio2']}
        onClick={() => onPlaySound(message)}
        >
          <div className={styles['user-audio3']}>
            <svg 
            className={styles['user-audio4']}
            xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 18 18" fill="none">
              <path d="M8.25 4.51859C8.25 3.87489 7.52846 3.49477 6.99758 3.8588L4.60224 5.50132C4.53564 5.54699 4.45678 5.57143 4.37603 5.57143H2.3C1.85817 5.57143 1.5 5.9296 1.5 6.37143V11.6286C1.5 12.0704 1.85817 12.4286 2.3 12.4286H4.37603C4.45678 12.4286 4.53564 12.453 4.60224 12.4987L6.99758 14.1412C7.52846 14.5052 8.25 14.1251 8.25 13.4814V4.51859Z" fill="#5CC49D"/>
              <path d="M10.9053 6.34497C11.6083 7.0482 12.0032 8.00185 12.0032 8.99622C12.0032 9.99059 11.6083 10.9442 10.9053 11.6475" stroke="#5CC49D" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M12.9238 4.49634C14.3645 5.68994 15.1738 7.30859 15.1738 8.99634C15.1738 10.6841 14.3645 12.3027 12.9238 13.4963" stroke="#5CC49D" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>

          </div>
        </button>
      </div>
    )}
    </div>
  );
});




// 에러 처음 진입 시 스테이트 리셋 하는거 넣기

export function AiChatPage ({ handleAiReset }) {
  const [audioAllowed, setAudioAllowed] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [words, setWords] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);

    const [messages, setMessages] = useState([]);

  const [scenarios, setScenarios] = useState([]);
  const [selectedVoice, setSelectedVoice] = useState(null);
  const [chatData, setChatData] = useState({
    // 단어 정보
    wordId: null,
    word: null,

    // 시나리오 정보
    selectedScenario: null,
    selectedScenarioId: null,
    scenarioTitle: null, 
    scenarioGoal: null,
    scenarioSituation: null,
    scenarioDescription: null,

    // 음성
    aiGender: null,

    // 채팅방
    chatRoomId: null,
    hasPreviousMessages: false,
  });

  const [selectedScenario, setSelectedScenario] = useState(null);

  // 남은 턴 수
  const [progress, setProgress] = useState({ 
    currentTurn: 0, 
    remainingTurn: 10, 
    maxTurn: 10,
    isCompleted: false 
  });

  // 현재 단계 (1, 2, 3)
  const [step, setStep] = useState('CHECK');
  const [previousChatInfo, setPreviousChatInfo] = useState(null);
  const [reportData, setReportData] = useState(null);

  // 신고 선택 모드 활성화 여부
  const [isSelectMode, setIsSelectMode] = useState(false);

  // 메시지 선택 모드 구분용
  const [isMessage, setIsMessage] = useState(false);
  // 선택된 대상 ID (MESSAGE일 때는 messageId, SCENARIO일 때는 chatRoomId)
  const [selectedTargetId, setSelectedTargetId] = useState(null);
  // 신고 팝업 오픈 여부
  const [showReportModal, setShowReportModal] = useState(false);
  // 신고 모드 (MESSAGE 또는 SCENARIO)
  const [reportMode, setReportMode] = useState(null);

  // 팝업 메시지
  const [popupMessage, setPopupMessage] = useState('');
  const [popupCount, setPopupCount] = useState(0);

  const showPopup = (msg) => {
    setPopupMessage(msg);
    setPopupCount(prev => prev + 1);
  };


  // 대화 신고 선택 완료 처리용
  const handleSelectMessage = (messageId) => {
    if (!isMessage) return;  // 메시지 신고 모드 아니면 무시

    setSelectedTargetId(messageId);
    setReportMode('MESSAGE');
    setShowReportModal(true); // 메시지가 선택되면 바로 신고 팝업을 띄움
  };

// 취소 버튼 누르거나, 신고가 완료되었을 때 호출하는 공통 닫기 함수
const handleCloseReport = () => {
  setShowReportModal(false);
  setIsMessage(false);        // 메시지 선택모드 종료
  setIsSelectMode(false);
  setSelectedTargetId(null);  // 선택된 메시지 ID 초기화
  setReportMode(null);
};



const handleFetchReport = async () => {
  if (!chatData?.chatRoomId) return;
  if (isLoading) return;
  setIsLoading(true);
  try {
    const response = await apiFetch(`/chats/rooms/${chatData.chatRoomId}/report`);

    if (response.success) {
      setReportData(response.data);
      setStep('REPORT');  // 화면 전환
    }
  } catch (e) {
    log.debug('리포트 실패:', e);
  } finally {
    setIsLoading(false);
  }
};


  // 채팅방 나가기 확인 팝업 표시 여부
  const [exitModal, setExitModal] = useState(null);
  const navigate = useNavigate();
  // 진행 중인지
  const isInProgress = step === 'CHAT' 
    && progress?.currentTurn > 0 
    && !progress?.isCompleted;

  const handleExit = () => {
    if (exitModal === 'back') navigate(-1);
    else if (exitModal === 'mypage') navigate('/my', {replace: true});
    setExitModal(null);
  };

const handleBack = async () => {
  // REPORT 단계 처리
  if (step === 'REPORT') {
    try {
      const feedback = await loadUserData('submittedFeedback');

      if (!feedback?.hasSubmittedFeedback) {
        const check = await apiFetch('/feedback/check', { method: 'GET' });

        if (!check.data.hasSubmittedFeedback) {
          // 미제출은 피드백 페이지로
          navigate('/feedback', { 
            state: { returnTo: -1 } 
            , replace: true
          });
          return;
        } else {
          // 서버는 제출됨 로컬 갱신
          await saveUserData('submittedFeedback', { hasSubmittedFeedback: true });
        }
      }

      // 이미 제출한 유저 그냥 뒤로
      navigate(-1);
    } catch (error) {
      log.debug('설문 여부 조회 실패:', error);
      navigate(-1);
    }
    return;
  }

  // 진행 중이면 모달
  if (isInProgress) {
    setExitModal('back');
  } else {
    // 그 외 그냥 뒤로
    navigate(-1);
  }
};


const handleMyPage = async () => {
  // REPORT 단계 처리
  if (step === 'REPORT') {
    try {
      const feedback = await loadUserData('submittedFeedback');

      if (!feedback?.hasSubmittedFeedback) {
        const check = await apiFetch('/feedback/check', { method: 'GET' });

        if (!check.data.hasSubmittedFeedback) {
          navigate('/feedback', { 
            state: { returnTo: '/my' } 
            , replace: true
          });
          return;
        } else {
          await saveUserData('submittedFeedback', { hasSubmittedFeedback: true });
        }
      }

      navigate('/my', {replace: true});
    } catch (error) {
      log.debug('설문 여부 조회 실패:', error);
      navigate('/my', {replace: true});
    }
    return;
  }

  // 진행 중이면 모달
  if (isInProgress) {
    setExitModal('mypage');
  } else {
    navigate('/my', {replace: true});
  }
};


  // 최대 3단계 까지
  const handleNext = async () => {
    if (isLoading) return;
    setIsLoading(true);

    try {
    if (step === 'WORD_SELECT') {
      const word = words[currentIndex];


      try {

        const response = await apiFetch(`/chats/scenarios?word=${encodeURIComponent(word.word)}`, {
          method: 'GET'
        });

        if (response.success) {

          setScenarios(response.data.scenarios);
          setChatData(prev => ({ ...prev, 
            wordId: word.wordId, 
            word: word.word, 
            // AI 추천 단어 정보 저장
            aiRecommendedMeanings: word.meanings || [],
          }));
          setStep('SCENARIO_SELECT');
        }
      } catch (e) {

        log.debug('시나리오 로딩 실패:', e);
      }

    } else if (step === 'SCENARIO_SELECT') {
      const selectedObj = scenarios.find(s => s.scenarioId === selectedScenario);


      if (!selectedObj) {
        log.debug('시나리오를 찾을 수 없음');
        return;
      }

      setChatData(prev => ({ 
        ...prev, 
        selectedScenario: selectedScenario,
        selectedScenarioId: selectedObj.scenarioId,
        scenarioTitle: selectedObj.title,
        scenarioGoal: selectedObj.goal,
        scenarioSituation: selectedObj.situation,

        // scenarioDescription: selectedObj.description,
      }));
      setStep('VOICE_SELECT');
    } else if (step === 'VOICE_SELECT') {

          const body = chatData.wordId 
          ? {
              // 수집된 단어
              isNewStart: true,
              wordId: chatData.wordId,
              aiRecommendedWord: "",
              aiRecommendedMeanings: [],
              scenarioTitle: chatData.scenarioTitle,
              scenarioGoal: chatData.scenarioGoal,
              scenarioSituation: chatData.scenarioSituation,
              aiGender: selectedVoice,
            }
          : {
              // AI 추천 단어
              isNewStart: true,
              wordId: null,
              aiRecommendedWord: chatData.word,
              aiRecommendedMeanings: chatData.aiRecommendedMeanings || [],
              scenarioTitle: chatData.scenarioTitle,
              scenarioGoal: chatData.scenarioGoal,
              scenarioSituation: chatData.scenarioSituation,
              aiGender: selectedVoice,
            };

      // 시나리오와 목소리 선택 후 방 생성 (POST)
      try {
        const response = await apiFetch('/chats/rooms', {
          method: 'POST',
          body: JSON.stringify(body),
        });
        if (response.success) {
          setChatData(prev => ({ 
            ...prev, 
            aiGender: selectedVoice,
            chatRoomId: response.data.chatRoomId,
            hasPreviousMessages: response.data.hasPreviousMessages,

            // 응답에서 시나리오 정보 받아서 업데이트 (서버가 확정한 값)
            scenarioTitle: response.data.scenarioTitle,
            scenarioGoal: response.data.scenarioGoal,
            scenarioSituation: response.data.scenarioSituation,

            word: response.data.word,
            meanings: response.data.meanings,


          }));
          setStep('CHAT');
        } else {
          log.debug(response.message);
        }
      } catch (e) {
        log.debug('채팅방 생성 실패', e);
      }
    }
    } finally {
    setIsLoading(false);
  }
  }





const location = useLocation();
const passedData = location.state?.wordsData;






  // 진입 시 복원 할건지 확인
  useEffect(() => {
    // 마운트 시점에 URL 직접 읽기
    const params = new URLSearchParams(window.location.search);
    const isFresh = params.get('fresh') === 'true';

    // 다시하기 버튼 눌렀으면 복원 팝업 스킵
    if (isFresh) {
      // 복원 체크 스킵
      setStep('WORD_SELECT');
      // 뒤로가기 시 ?fresh=true로 돌아오지 않게
      navigate('/ai', { replace: true });
      return;
    }

    const checkPrevious = async () => {
      try {
        const res = await apiFetch('/chats/rooms', {
          method: 'POST',
          body: JSON.stringify({
            isNewStart: false,
            wordId: null,
            aiRecommendedWord: "",
            aiRecommendedMeanings: [],
            scenarioTitle: null,
            scenarioGoal: null,
            scenarioSituation: null,
            aiGender: null,
          }),
        });


        if (res.success && res.data.hasPreviousMessages) {
          setPreviousChatInfo(res.data);
          setStep('CONTINUE_POPUP');
        } else {
          setStep('WORD_SELECT');
        }
      } catch {
        setStep('WORD_SELECT');
      }
    };
    checkPrevious();
  }, [navigate]);
  
  // 이어하기
  const handleContinue = async () => {
    if (isLoading) return;
    setIsLoading(true);
    try {
      const chatRoomId = previousChatInfo.chatRoomId;
    const res = await apiFetch(`/chats/rooms/${chatRoomId}/messages`);

    
    if (res.success) {
      setMessages(res.data.messages);
    // progress 응답에 없으면 메시지 개수로 계산
    const messageCount = res.data.messages?.filter(m => m.senderType === 'USER').length || 0;
    setProgress({
      currentTurn: messageCount,
      remainingTurn: 10 - messageCount,
      maxTurn: 10,
      isCompleted: false,
    });
    
    // chatData에 이전 정보 모두 포함
    setChatData({
      chatRoomId,
      word: previousChatInfo.word,
      meanings: previousChatInfo.meanings,
      wordId: previousChatInfo.wordId,
      scenarioTitle: previousChatInfo.scenarioTitle,
      scenarioGoal: previousChatInfo.scenarioGoal,
      scenarioSituation: previousChatInfo.scenarioSituation,
      hasPreviousMessages: true,
    });
      setStep('CHAT');
    }
    } catch (error) {
      log.debug('이어하기 실패', error);
    } finally {
      setIsLoading(false);
    }
  };
  
  // 새로 시작 (팝업에서)
  const handleNewStart = async () => {
    if (isLoading) return;
    setIsLoading(true);

    try {
      setPreviousChatInfo(null);
      setStep('WORD_SELECT');
    } catch (error) {
      log.debug('새로 시작 실패', error);
    } finally {
      setIsLoading(false);
    }
  };











  useEffect(() => {
    if (step !== 'WORD_SELECT') return;

    const init = async () => {
      let data = passedData;

      // passedData 없으면 직접 호출 (URL 직접 진입 등)
      if (!data) {
        try {
          const response = await apiFetch('/chats/words', { method: 'GET' });

          data = response.data;

          if (!data.hasWords) {
            // 단어 없으면 디폴트로 보내기
            showPopup(data.guideMessage || '수집된 단어가 없어요!');
            navigate('/', {replace: true});
            return;
          }
        } catch (error) {
          log.debug('단어 조회 실패', error);
          return;
        }
      }

      // 3개 배열 합치기
      const wordsData = [
        ...(data.todayWords ?? []),
        ...(data.collectedWords ?? []),
        ...(data.recommendedWords ?? []),
      ];

      // 각 단어마다 사전 API로 발음/음성 추가
      const enriched = await Promise.all(
        wordsData.map(async (word) => {
          const defaultExtra = {
            usPhonetic: '',
            usAudio: '',
            ukPhonetic: '',
            ukAudio: '',
          };

          try {
            const res = await fetch(
              `https://api.dictionaryapi.dev/api/v2/entries/en/${word.word}`
            );
            if (!res.ok) return { ...word, ...defaultExtra };

            const dict = await res.json();
            const usPhonetic = dict[0]?.phonetics?.find(p => p.audio?.includes('-us'));
            const ukPhonetic = dict[0]?.phonetics?.find(p => p.audio?.includes('-uk'));

            // 모든 품사의 유의어를 합쳐서 중복 제거 (상위 3개만)
            const synonyms = [
              ...new Set(
                dict[0]?.meanings?.flatMap(m => m.synonyms || []) || []
              )
            ].slice(0, 3);

            return {
              ...word,
              usPhonetic: usPhonetic?.text || dict[0]?.phonetic || '',
              usAudio: usPhonetic?.audio || '',
              ukPhonetic: ukPhonetic?.text || dict[0]?.phonetic || '',
              ukAudio: ukPhonetic?.audio || '',
              // 유의어 배열
              synonyms,
            };
          } catch {
            return { ...word, ...defaultExtra };
          }
        })
      );

      setWords(enriched);
    };

    init();
  }, [navigate, passedData, step]);





  // 시나리오 선택 변경
  const handleScenarioChange = (scenario) => {

    if (scenario === selectedScenario) return;

    setSelectedScenario(scenario);
  };




  // 보이스 선택 변경
  const handleVoiceChange = (voice) => {
    if (voice === selectedVoice) return;
    setSelectedVoice(voice);
  };















    // 중단 안내 문구
    const TextCard = () => {
      return (
        <>
        {step === 'CHECK' && <Spinner />}

    {step === 'CONTINUE_POPUP' && (
      <ContinuePopup 
        info={previousChatInfo}
        onContinue={handleContinue}
        onNewStart={handleNewStart}
      />
    )}

        {step === 'WORD_SELECT' && (
          <>
            <div className={styles.textcard}>
              <p className={styles.textcard2}>어떤 단어카드로 <br />대화 연습을 해볼까요?</p>
            </div>

            <div className={styles.textcard3}>
              <p className={styles.textcard4}>단어카드를 선택하여 AI와 대화 해보세요</p>
            </div>
          </>
        )}

        {step === 'SCENARIO_SELECT' && (
          <>
            <div className={styles.textcard}>
              <p className={styles.textcard2}>시나리오를 선택해주세요</p>
            </div>

            <div className={styles.textcard3}>
              <p className={styles.textcard4}>원하는 상황을 선택하여 시나리오를 완성해보세요</p>
            </div>
          </>
        )}

        {step === 'VOICE_SELECT' && (
          <>
            <div className={styles.textcard}>
              <p className={styles.textcard2}>대화를 나누고자 하는 ai를 선택해주세요</p>
            </div>

            <div className={styles.textcard3}>
              <p className={styles.textcard4}>원하는 ai의  보이스를 정해주세요</p>
            </div>
          </>
        )}
        </>
      )
    }






















  const Pagination = () => {
    return (
    // 페이지네이션
    <div className={styles.pagination}>
      <button 
      className={styles.pagination2}
      onClick={() => setCurrentIndex(i => Math.max(0, i - 1))}>
        <svg 
        className={styles.pagination3}
        xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
          <path d="M14.7071 5.29289C15.0976 5.68342 15.0976 6.31658 14.7071 6.70711L9.41421 12L14.7071 17.2929C15.0976 17.6834 15.0976 18.3166 14.7071 18.7071C14.3166 19.0976 13.6834 19.0976 13.2929 18.7071L7.29289 12.7071C6.90237 12.3166 6.90237 11.6834 7.29289 11.2929L13.2929 5.29289C13.6834 4.90237 14.3166 4.90237 14.7071 5.29289Z" fill="#BDB4AB"/>
        
          <svg 
          className={styles.pagination4}
          xmlns="http://www.w3.org/2000/svg" width="8" height="14" viewBox="0 0 8 14" fill="none">
            <path d="M7.70711 0.292893C8.09763 0.683418 8.09763 1.31658 7.70711 1.70711L2.41421 7L7.70711 12.2929C8.09763 12.6834 8.09763 13.3166 7.70711 13.7071C7.31658 14.0976 6.68342 14.0976 6.29289 13.7071L0.292893 7.70711C-0.0976318 7.31658 -0.0976317 6.68342 0.292893 6.29289L6.29289 0.292893C6.68342 -0.0976312 7.31658 -0.0976311 7.70711 0.292893Z" fill="#BDB4AB"/>
          </svg>
        </svg>
      </button>
        {words.map((_, i) => (
          <button 
            key={i}
            className={i === currentIndex ? styles['pagination7-t'] : styles['pagination5-n']}
            onClick={() => setCurrentIndex(i)}
          >
            <p className={i === currentIndex ? styles['pagination8-t'] : styles['pagination6-n']}>
              {i + 1}
            </p>
          </button>
        ))}
      <button 
      className={styles.pagination2}
      onClick={() => setCurrentIndex(i => Math.min(words.length - 1, i + 1))}>
        <svg 
        className={styles.pagination3}
        xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
          <path d="M9.29289 18.7071C8.90237 18.3166 8.90237 17.6834 9.29289 17.2929L14.5858 12L9.29289 6.70711C8.90237 6.31658 8.90237 5.68342 9.29289 5.29289C9.68342 4.90237 10.3166 4.90237 10.7071 5.29289L16.7071 11.2929C17.0976 11.6834 17.0976 12.3166 16.7071 12.7071L10.7071 18.7071C10.3166 19.0976 9.68342 19.0976 9.29289 18.7071Z" fill="#7F7569"/>

          <svg 
          className={styles.pagination9}
          xmlns="http://www.w3.org/2000/svg" width="8" height="14" viewBox="0 0 8 14" fill="none">
            <path d="M0.292893 13.7071C-0.0976311 13.3166 -0.0976312 12.6834 0.292893 12.2929L5.58579 7L0.292893 1.70711C-0.0976317 1.31658 -0.0976317 0.683417 0.292893 0.292893C0.683417 -0.0976315 1.31658 -0.0976315 1.70711 0.292893L7.70711 6.29289C8.09763 6.68342 8.09763 7.31658 7.70711 7.70711L1.70711 13.7071C1.31658 14.0976 0.683418 14.0976 0.292893 13.7071Z" fill="#7F7569"/>
          </svg>
        </svg>
      </button>
    </div>
    )
  }



  const Next = () => {
    return (
      <button 
      className={styles.next}
      onClick={() => handleNext()}
      disabled={isLoading}
      >
        <div className={styles.next2}>
          <p className={styles.next3}>
            {step === 'VOICE_SELECT' ? '대화 시작하기' : '계속하기' }
          </p>
        </div>
        
      </button>
    )
  }




  return (
    <div className={`${styles.main} ${step === 'CHAT' || step === 'REPORT' ? styles['main-chat'] : ''}`}>
      
      {/* 로딩 */}
      {(isLoading || (step === 'WORD_SELECT' && !words?.length)) && <TestSpinner />}

      {popupMessage && 
        <Toast 
          message={popupMessage}
          count={popupCount}
          onClose={() => setPopupMessage('')}
        />}

      {<Header 
      handleAiReset={handleAiReset}
      currentStep={step} 
      onBack={handleBack}
      onMyPage={handleMyPage} 
      chatData={chatData} 
      isMessage={isMessage}
      showReportModal={showReportModal}
      handleCloseReport={handleCloseReport}
      onStartMessageReport={() => {
      setIsSelectMode(true);
      setIsMessage(true);
      }} 
      onStartScenarioReport={() => {
        setReportMode('SCENARIO');
        // 시나리오는 바로 룸 ID 할당
        setSelectedTargetId(chatData.chatRoomId);
        setIsSelectMode(true);
        setShowReportModal(true);
      }}/>}

      {step !== 'CHAT' && step !== 'REPORT' && <Stepper currentStep={step} />}

      {step !== 'CHAT' && step !== 'REPORT' && TextCard()}

      {step === 'WORD_SELECT' && <TestDictionary
      wordData={words} 
      currentIndex={currentIndex}
      />}

      {step === 'SCENARIO_SELECT' && <Scenario 
      scenarioData={scenarios} 
      // 읽기용
      selectedScenario={selectedScenario}
      // 클릭 콜백
      onSelect={handleScenarioChange}
      />}

      {step === 'VOICE_SELECT' && <Voice 
      // 읽기용
      selectedVoice={selectedVoice}
      // 클릭 콜백
      onSelect={handleVoiceChange}
      />}

      {step === 'WORD_SELECT' && Pagination()}

      {(step === 'CHAT' || step === 'REPORT') && <ChatRoom 
      setChatData={setChatData}
      chatData={chatData}
      progress={progress}
      setProgress={setProgress}
      messages={messages}
      setMessages={setMessages}
      onFetchReport={handleFetchReport}
      isSelectMode={isSelectMode}
      onSelectMessage={handleSelectMessage}
      isReportOpen={showReportModal}
      selectedTargetId={selectedTargetId}
      isMessage={isMessage}
      reportData={reportData}
      step={step}
      audioAllowed={audioAllowed}
      setAudioAllowed={setAudioAllowed}
      />}

      {step !== 'CHAT' && step !== 'REPORT' && Next()}

      {/* 신고 모달 */}
      {showReportModal && (
        <Reports 
          mode={reportMode}
          // 신고할 메시지 Id 1개
          targetId={selectedTargetId}
          chatData={chatData}
          onClose={handleCloseReport}
          showPopup={showPopup}
        />
      )}

      {exitModal && (
        <ExitModal 
          onConfirm={handleExit}
          onCancel={() => setExitModal(null)}
        />
      )}
    </div>
  );
}