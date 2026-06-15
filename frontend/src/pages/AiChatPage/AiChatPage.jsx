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
function Header ({ currentStep, onBack, onMyPage }) {
  return (
    currentStep !== 'CHAT' ? (
      <div className={styles.head}>
        <div className={styles.head2}>
          <div className={styles.head3}>
            <button 
            onClick={onBack}
            className={styles.head4}>뒤로가기</button>
            <button 
            onClick={onMyPage}
            className={styles.head4}>
              my
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
            className={styles['chat-head4']}>
              뒤로가기
            </button>
            <button 
            onClick={onMyPage}
            className={styles['chat-head4']}>
              my
            </button>
          </div>

          <div className={styles['chat-head5']}>
            <p className={styles['chat-head6']}>AI Talk</p>
          </div>
        </div>

        <div className={styles['chat-head7']}>더보기</div>
      </div>
    )
  )
}


// 더보기 박스 (비활성화)
function SeeMore () {
  return (
    <div className={styles.more}>
      <div className={styles.more2}>
        <div className={styles.more3}>
          <p className={styles.more4}>AI 대화 신고하기</p>
        </div>

        <div className={styles.more3}>
          <p className={styles.more4}>시나리오 신고하기</p>
        </div>
      </div>

      <div className={styles.more5}></div>

      <div className={styles.more6}>
        <div className={styles.more7}>
          <img src={union} alt="" className={styles.more8} />
        </div>


        <div className={styles.more9}>
          <p className={styles.more10}>대화 다시하기</p>
        </div>
      </div>
    </div>
  );
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

  // return (
  //   <div className={styles.step}>
  //     {/* 요소 totalSteps개짜리 배열을 만들고 map */}
  //     {Array.from({ length: totalSteps }).map((_, i) => {
  //       const stepNum = i + 1;
  //       const isLast = stepNum === totalSteps;
        // const isActive = stepNum === stepNumber;
        // const isDone = stepNum < stepNumber;


        // return (
        //   <div key={stepNum} className={`
        //     ${isActive ? styles['step-active'] : isDone ? styles['step-done'] : styles.step2}
        //   `}>

        //     <StepCircle stepNum={stepNum} currentStep={stepNumber} />
            
        //     {/* 점선 (마지막 단계 제외) */}
        //     {stepNum < totalSteps && 
        //     <div className={styles.step4}>
        //       <svg 
        //       className={styles.step5}
        //       xmlns="http://www.w3.org/2000/svg" 
        //       width="4" 
        //       height="4" 
        //       viewBox="0 0 4 4" 
        //       fill="none">
        //         <circle cx="2" cy="2" r="2" fill="#A5E7D2"/>
        //       </svg>
        //       <svg 
        //       className={styles.step5}
        //       xmlns="http://www.w3.org/2000/svg" 
        //       width="4" 
        //       height="4" 
        //       viewBox="0 0 4 4" 
        //       fill="none">
        //         <circle cx="2" cy="2" r="2" fill="#A5E7D2"/>
        //       </svg>
        //       <svg 
        //       className={styles.step5}
        //       xmlns="http://www.w3.org/2000/svg" 
        //       width="4" 
        //       height="4" 
        //       viewBox="0 0 4 4" 
        //       fill="none">
        //         <circle cx="2" cy="2" r="2" fill="#A5E7D2"/>
        //       </svg>
        //     </div>}
        //   </div>
        // );
    //   })}
    // </div>
  // );
}


// 활성화 구분 후 스탭 표시
function StepCircle({ stepNum, currentStep }) {
  const isActive = stepNum === currentStep;
  const isDone = stepNum < currentStep;
  
    // 현재 스텝: 진한 초록 + 번호
  if (isActive) {
    return <div className={styles.step2}>{stepNum}</div>;
  }

  // if (isActive) {
  //   return (
  //     // <svg 
  //     // className={styles['step-active2']}
  //     // xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
  //     //   <path d="M12.137 3.77574C12.566 3.30103 13.2975 3.26012 13.7767 3.6859C14.2579 4.1139 14.302 4.85088 13.8743 5.33238L13.8675 5.3402L7.08809 12.6732L7.08711 12.6722C6.86614 12.9168 6.55276 13.0578 6.22284 13.058C5.88978 13.058 5.57214 12.9152 5.35076 12.6664L1.79505 8.66637C1.36701 8.18483 1.41026 7.44699 1.89173 7.01891C2.37332 6.59106 3.11121 6.63506 3.53921 7.11656L6.22968 10.1439L12.137 3.77477V3.77574Z" fill="white"/>
        
  //       <svg 
  //       className={styles['step-active3']}
  //       xmlns="http://www.w3.org/2000/svg" width="13" height="10" viewBox="0 0 13 10" fill="none">
  //         <path d="M10.6367 0.384385C11.0656 -0.090327 11.7972 -0.13124 12.2763 0.294542C12.7576 0.72254 12.8017 1.45953 12.374 1.94103L12.3672 1.94884L5.58778 9.28185L5.58681 9.28087C5.36583 9.52548 5.05245 9.66645 4.72254 9.66661C4.38947 9.66661 4.07184 9.52383 3.85046 9.27501L0.294745 5.27501C-0.133297 4.79347 -0.0900501 4.05564 0.391426 3.62755C0.873018 3.1997 1.6109 3.24371 2.03891 3.72521L4.72937 6.75255L10.6367 0.383409V0.384385Z" fill="white"/>
  //       </svg>
  //     // </svg>
  //   );
  // }


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


  // if (isDone) {
  //   return (
  //     <div className={styles['step-done2']}>
  //       {stepNum}
  //     </div>
  //   );
  // }


  // 대기 스텝: 테두리 + 연한 번호
    return (
    <div className={styles['step-done']}>
      <span className={styles['step-done2']}>{stepNum}</span>
    </div>
  );

  // return (
  //   <div className={styles.step3}>
  //     {stepNum}
  //   </div>
  // );
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
    { id: 'FEMALE', label: '여성', description: '?????' },
    { id: 'MALE', label: '남성', description: '?????' },
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
        <div className={styles.voice3}></div>

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





function ReportBubble({ message }) {
  if (message.reportType === 'SUGGESTION') {
    const item = message.improvements[0];
    
    return (
      <div className={styles.reportCard}>
        <div className={styles.reportCard2}>
          <p className={styles.reportCard3}>
            다음엔 이렇게 문장을 사용해보세요
          </p>
        </div>

        <div className={styles.reportCard4}>
          <div className={styles.reportCard5}>
            <p className={styles.reportCard6}>{item.suggested}</p>
          </div>
          <div className={styles.reportCard9}>
            <p className={styles.reportCard10}>{item.explanation}</p>
          </div>
        </div>

        {/* {item.modelAudioUrl && (
          <button 
            onClick={() => new Audio(item.modelAudioUrl).play()}
            className={styles.playBtn}
          >
            들어보기
          </button>
        )} */}
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
            
            {/* {item.modelAudioUrl && (
              <button 
                onClick={() => new Audio(item.modelAudioUrl).play()}
                className={styles.playBtn}
              >
                
              </button>
            )} */}
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













// 채팅방
function ChatRoom ({ chatData, progress, setProgress, leaveChat }) {
  const [messages, setMessages] = useState([]);

  const [voiceModeOn, setVoiceModeOn] = useState(false);
  const voiceModeOnRef = useRef(voiceModeOn);

  const [inputText, setInputText] = useState('');
  // const wsRef = useRef(null);

  const [isRecording, setIsRecording] = useState(false);
  // const mediaRecorderRef = useRef(null);
  // const chunksRef = useRef([]);

  const clientRef = useRef(null);


  const [suggestion, setSuggestion] = useState(null);      // 추천
  const [isCompleted, setIsCompleted] = useState(false);   // 종료 여부
  const [toast, setToast] = useState(null);

  const chatRoomId = chatData?.chatRoomId;

  const [isWaiting, setIsWaiting] = useState(false);

  const [chatStartDate] = useState(new Date().toISOString());





  // voiceModeOn이 바뀔 때마다 ref 동기화
  useEffect(() => {
    voiceModeOnRef.current = voiceModeOn;
  }, [voiceModeOn]);

  useEffect(() => {
    if (!chatRoomId) return;

    // 언마운트 감지용
    let cancelled = false;
    let client;


const showToast = (message, type = 'info') => {
  setToast({ message, type });
  setTimeout(() => setToast(null), 3000);  // 3초 후 사라짐
};

const subscribeToChat = () => {
  try {
    client.subscribe('/user/queue/chat', (message) => {
      try {
        const data = JSON.parse(message.body);
        handleServerMessage(data);
      } catch (e) {
        console.error('메시지 파싱 실패:', e);
      }
    });
  } catch (e) {
    console.error('구독 실패:', e);
  }
};

const requestIntro = () => {
  if (chatData.hasPreviousMessages) return;
  
  try {
    client.publish({
      destination: '/app/chat/start',
      body: JSON.stringify({ chatRoomId }),
    });
  } catch (e) {
    console.error('인트로 요청 실패:', e);
  }
};


// 에러 코드들
const handleError = ({ code, message }) => {
  log.debug(`[${code}]`, message);
  
  switch (code) {
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
          }
    
          // 케이스 2: streaming 메시지 없으면 → 새 메시지 추가 (CHUNK 없이 DONE만 왔을 때)
          return [...prev, {
            messageId: payload.messageId,
            senderType: 'AI',
            content: payload.content,
            highlightWord: payload.highlightWord,
            streaming: false,
          }];
        });
        setIsWaiting(false);
        break;
    
    
        // ========== AI 음성 도착 (TEXT_DONE보다 늦을 수 있음) ==========
        case 'AI_AUDIO':
          setMessages(prev => {
            const found = prev.find(msg => msg.messageId === payload.messageId);
    
            if (found) {
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
          const response = await apiFetch(`/chats/rooms/${chatRoomId}/messages`);

          // 이미 언마운트됐으면 중단
          if (cancelled) return;

          if (response.success) {
            setMessages(response.data.messages);

            // progress 복원
            if (response.data.progress) {
              setProgress(response.data.progress);
            }
          }
        } catch (e) {
          log.debug('메시지 로딩 실패:', e);
        }
      }




      // 이미 언마운트됐으면 중단
      if (cancelled) return;

      // 토큰 가져오기
      const token = await getAccessToken();

      if (!token) {
        log.debug('토큰 없음');
        return;
      }












      // STOMP 연결 (실시간)
      client = new Client({
        // brokerURL: `${import.meta.env.VITE_WS_URL}`,

        // 서버가 SockJS 사용함
        // SockJS는 HTTP(S) 사용함
        webSocketFactory: () => new SockJS(`${import.meta.env.VITE_WS_URL}/ws-chat`),
        connectHeaders: { Authorization: `Bearer ${token}` },

        // 실패하면 5초마다 재시도
        reconnectDelay: 5000,

        onConnect: () => {
  subscribeToChat();
  requestIntro();
},



        onStompError: (frame) => {

          log.debug('STOMP 에러:', frame.headers['message']);
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
    // 원시값만 의존성
  }, [chatRoomId, chatData?.hasPreviousMessages, setProgress]);

  // AI 메시지 자동 재생 (음성 모드)
  useEffect(() => {
    if (!voiceModeOn) return;
    
    const lastMsg = messages[messages.length - 1];
    if (lastMsg?.senderType !== 'AI') return;
    
    if (lastMsg.audioUrl) {
      const audio = new Audio(lastMsg.audioUrl);
      audio.play().catch(e => log.debug('재생 실패:', e));
    } else {
      const utterance = new SpeechSynthesisUtterance(lastMsg.content);
      utterance.lang = 'en-US';
      speechSynthesis.speak(utterance);
    }
    
  }, [messages, voiceModeOn]);









  // 텍스트 전송
  const sendTextMessage = () => {

    if (!inputText.trim()) return;
    if (isWaiting) return;
    if (!clientRef.current?.connected) return;
    
    // 클라이언트에서 사용자 메시지 즉시 추가 (낙관적 업데이트)
    setMessages(prev => [...prev, {
      messageId: `temp_user_${Date.now()}`,
      senderType: 'USER',
      content: inputText,
    }]);

    // 서버로 전송
    clientRef.current.publish({
      destination: '/app/chat/send',
      body: JSON.stringify({
        chatRoomId: chatRoomId,
        inputMode: 'TEXT',
        content: inputText,
        audioUrl: null,
      })
    });

    setInputText('');
    setIsWaiting(true);
  };




// 녹음 시작
const startRecording = () => {
  chrome.windows.create({
    url: chrome.runtime.getURL('recorder.html'),
    type: 'popup',
    width: 360,
    height: 320,
  });
  setIsRecording(true);
};

// 녹음 정지 (팝업창에서 처리하니 상태만)
const stopRecording = () => {
  setIsRecording(false);
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
      // const fileName = `audio_${Date.now()}.wav`;
      const fileName = `audio_${Date.now()}.webm`;
      const presignRes = await apiFetch('/files/audio/presigned-url', {
        method: 'POST',
        body: JSON.stringify({
          fileName,
          // contentType: 'audio/wav'
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
        // headers: { 'Content-Type': 'audio/wav' },
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
    if (message.type === 'RECORDING_COMPLETE') {
      fetch(message.audioData)
        .then(res => res.blob())
        .then(blob => uploadAndSend(blob));
      setIsRecording(false);
    }
  };
  
  chrome.runtime.onMessage.addListener(handler);
  return () => chrome.runtime.onMessage.removeListener(handler);
}, [uploadAndSend]);







const hasReport = messages.some(msg => msg.senderType === 'REPORT');

const fetchReport = async () => {
  // 리포트 있으면 무시
  if (hasReport) return;
  try {
    const response = await apiFetch(`/chats/rooms/${chatRoomId}/report`);
    
    if (response.success) {
      const report = response.data;
      
      // 리포트를 메시지로 변환해서 추가
      setMessages(prev => [
        ...prev,
        {
          messageId: `report_suggestion_${Date.now()}`,
          senderType: 'REPORT',  // 새 타입
          reportType: 'SUGGESTION',
          improvements: report.expressionNaturalness.improvements,
          completedAt: report.summary.completedAt,
        },
        {
          messageId: `report_native_${Date.now()}`,
          senderType: 'REPORT',
          reportType: 'NATIVE',
          improvements: report.expressionNaturalness.improvements,
        },
      ]);
    }
  } catch (e) {
    log.debug('리포트 로딩 실패:', e);
  }
};



  // 받은 메시지 화면에 그리기
  return (
    <div className={styles['chat-page']}>
    <div className={styles['chat-messages']}>
      <div className={styles['chat-scenario']}>
        <div className={styles['chat-scenario2']}>
          <p className={styles['chat-scenario3']}>사용할 단어 : {chatData.word}</p>
        </div>
      </div>

      <div className={styles['chat-time']}>
        <p className={styles['chat-time2']}>{formatDate(chatStartDate)}</p>
      </div>

    {/* 시나리오 정보: 아이콘 + 말풍선 3개를 하나의 그룹으로 */}
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
          <div className={styles['chat-ai5']}>{chatData.scenarioGoal}</div>
          <div className={styles['chat-ai5']}>{chatData.scenarioSituation}</div>
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
          msg.senderType === 'REPORT'
        ? <ReportBubble key={msg.messageId} message={msg} />
        : msg.senderType === 'AI'
          ? <AI key={msg.messageId} msg={msg.content} isFirst={isFirstOfGroup} />
          : <User key={msg.messageId} msg={msg.content} progress={progress} />
          );
        })}
      </>

      

    {/* 진행 상황 */}
    {/* <div className={styles.progress}>
      남은 갯수 : {progress.remainingTurn}
    </div> */}

    {/* 조기 종료 Suggestion 토스트 */}
    {suggestion && (
      <div className={styles.suggestion}>
        {suggestion.message}
      </div>
    )}

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
      onClick={fetchReport}
      disabled={hasReport}>
        <p className={styles.completionBox7}>정산하러 가기</p>
      </button>
      <button 
      className={styles.completionBox8}
      onClick={leaveChat}>
        <p className={styles.completionBox9}>대화창 나가기</p>
      </button>
        </div>
      </div>
    )}
</div>

      {/* 메시지 입력 창 */}
      <div className={styles['chat-input']}>
        <div className={styles['chat-input2']}>
          <textarea 
          className={styles['chat-input3']}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === 'Enter' && !e.shiftKey) {
              e.preventDefault();
              sendTextMessage();
            }
          }}
          placeholder={
            isRecording 
            ? '🔴 녹음 중...' 
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
            <button 
            onClick={() => setVoiceModeOn(prev => !prev)}
            className={styles['chat-input6']}>
              {voiceModeOn ? '음성모드' : '텍스트모드'}
            </button>
            <div className={styles['chat-input7']}>
              <p className={styles['chat-input8']}>
                {inputText.length}
                <span className={styles['chat-input9']}>/250자</span>
              </p>
            </div>
          </div>

          <div className={styles['chat-input10']}>
            <button onClick={isRecording ? stopRecording : startRecording}
            disabled={isWaiting}
            >
              {isRecording ? '⏹ 음성 녹음 정지' : '음성 녹음'}
            </button>
            <button className={styles['chat-input12']} onClick={sendTextMessage}
            disabled={isWaiting || !inputText.trim()}
            >텍스트전송</button>
          </div>
        </div>
      </div>
    </div>
  );
}


// AI 말풍선
function AI({ msg, isFirst }) {
  return (
    <div className={styles['chat-ai-group']}>
      {/* 첫 메시지일 때만 아이콘 */}
      {isFirst && <div className={styles['chat-icon']}></div>}

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

          <div className={styles['chat-ai5']}>{msg}</div>

        </div>
      </div>
    </div>
  );
}


// User 말풍선
function User({ msg, progress }) {
  return (
    <div className={styles['chat-box']}>
    <div className={styles['chat-user']}>

      <div className={styles['chat-user3']}>
        <div className={styles['chat-user4']}>
          <div className={styles['chat-user5']}>{msg}</div>
        </div>
      </div>
      <svg 
      className={styles['chat-user2']}
      xmlns="http://www.w3.org/2000/svg" width="8" height="18" viewBox="0 0 8 18" fill="none">
        <path d="M-1.43091e-07 18C0.411308 15.9545 0.948612 14.4511 1.83507 12.7996C3.1266 10.6577 4.87491 9.14286 7.4253 6.38095C8.89233 3.6865 7.38115 0.000207803 4.80919 -2.59289e-07L1.43051e-06 -7.19048e-07L-1.43091e-07 18Z" fill="#0AC290"/>
      </svg>
    </div>

      <div className={styles['chat-count']}>
        <div className={styles['chat-count2']}>
          <p className={styles['chat-count3']}>남은 갯수 : {progress.remainingTurn}</p>
        </div>
      </div>
    </div>

  );
}




// 에러 처음 진입 시 스테이트 리셋 하는거 넣기

export function AiChatPage () {
  const [isLoading, setIsLoading] = useState(false);
  const [words, setWords] = useState([]);
  const [currentIndex, setCurrentIndex] = useState(0);



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
  const [step, setStep] = useState('WORD_SELECT');

  // 채팅방 나가기 확인 팝업 표시 여부
  const [exitModal, setExitModal] = useState(null);
  const navigate = useNavigate();
  // 진행 중인지
  const isInProgress = step === 'CHAT' 
    && progress.currentTurn > 0 
    && !progress.isCompleted;

  // 뒤로가기
  const handleBack = () => {
    if (isInProgress) {
      setExitModal('back');
    } else {
      navigate(-1);
    }
  };

  // 마이페이지
  const handleMyPage = () => {
    if (isInProgress) {
      setExitModal('mypage');
    } else {
      navigate('/my');
    }
  };

  const handleExit = () => {
    if (exitModal === 'back') navigate(-1);
    else if (exitModal === 'mypage') navigate('/my');
    setExitModal(null);
  };

const leaveChat = () => {
  navigate('/feedback'); // 피드백 페이지로 이동
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
          setChatData(prev => ({ ...prev, wordId: word.wordId, word: word.word }));
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

      // 시나리오와 목소리 선택 후 방 생성 (POST)
      try {
        const response = await apiFetch('/chats/rooms', {
          method: 'POST',
          body: JSON.stringify({
            // 새로 시작
            isNewStart: true,
            wordId: chatData.wordId,
            scenarioTitle: chatData.scenarioTitle,
            // selectedScenario: chatData.selectedScenario,

            scenarioGoal: chatData.scenarioGoal,
            scenarioSituation: chatData.scenarioSituation,

            // selectedScenario: chatData.scenarioDescription,
            aiGender: selectedVoice,
          })
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


  useEffect(() => {
    const init = async () => {
      let data = passedData;

      // passedData 없으면 직접 호출 (URL 직접 진입 등)
      if (!data) {
        try {
          const response = await apiFetch('/chats/words', { method: 'GET' });

          data = response.data;

          if (!data.hasWords) {
            // 단어 없으면 디폴트로 보내기
            alert(data.guideMessage || '수집된 단어가 없어요!');
            navigate('/');
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
  }, [navigate, passedData]);





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
    <div className={`${styles.main} ${step === 'CHAT' ? styles['main-chat'] : ''}`}>
      {<Header currentStep={step} onBack={handleBack} onMyPage={handleMyPage} />}

      {step !== 'CHAT' && <Stepper currentStep={step} />}

      {step !== 'CHAT' && TextCard()}

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

      {step === 'CHAT' && <ChatRoom 
      chatData={chatData}
      progress={progress}
      setProgress={setProgress}
      leaveChat={leaveChat}
      />}

      {step !== 'CHAT' && Next()}

      {exitModal && (
        <ExitModal 
          onConfirm={handleExit}
          onCancel={() => setExitModal(null)}
        />
      )}
    </div>
  );
}