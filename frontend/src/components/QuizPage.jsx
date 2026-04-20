/* global chrome */

import { useState, useEffect } from 'react';
import frog from '../imgs/image_710.png';
import frog1 from '../imgs/image_712.png';
import { apiFetch } from '../utils/api';
import bulb from '../imgs/image_62.png';

function QuizPage({ videoId, onSettlementPage, onExitPage }) {
  // 현재 문제 번호 (0부터 시작)
  const [currentIndex, setCurrentIndex] = useState(0);
  // 유저가 선택한 보기 (아직 제출 안 함)
  const [tempChoice, setTempChoice] = useState(null);
  // 정답 확인 버튼을 눌렀는지 여부
  const [isConfirmed, setIsConfirmed] = useState(false);
  // 퀴즈 나가기 확인 팝업 표시 여부
  const [exitModal, setExitModal] = useState(false);


  // 퀴즈 세션 고유 번호 (서버에서 받기)
  const [sessionId, setSessionId] = useState(null);
  // 퀴즈 문제 목록 (서버에서 받기)
  const [quizzes, setQuizzes] = useState([]);
  // 정답 제출 후 서버에서 받은 결과 (정답/오답, 피드백 등)
  const [feedback, setFeedback] = useState(null);


  // 맞은 문제 개수
  const [correctCount, setCorrectCount] = useState(0);
  // 틀린 문제 개수
  const [wrongCount, setWrongCount] = useState(0);
  // 각 문제별 정답/오답 기록
  const [answers, setAnswers] = useState([]);


  // 이전 진행 상황 복원 여부 체크 (중복 저장 방지용)
  const [isRestoring, setIsRestoring] = useState(true);


  // 전체 자막 목록
  const [subtitles, setSubtitles] = useState([]);
  // 현재 보고 있는 자막 위치 (인덱스)
  const [subtitleIndex, setSubtitleIndex] = useState(-1);
  // 현재 화면에 표시되는 자막 정보
  const [currentSubtitle, setCurrentSubtitle] = useState({
    text: '자막 대기 중..',
    startTime: null,
    endTime: null,
    translation: '',
    id: null
  });
  // 영상 제목
  const [videoTitle, setVideoTitle] = useState('');


  // 현재 구간 반복 중인지 여부
  const [isLooping, setIsLooping] = useState(false);


  // 이미 조회한 단어 저장
  const [cache, setCache] = useState({});
  // 팝업이 고정되어 있는지 여부
  const [isPinned, setIsPinned] = useState(false);
  // 현재 팝업에 표시할 단어 정보
  const [dictionaryData, setDictionaryData] = useState(null);
  // 팝업 위치 (클릭한 단어 위에 표시)
  const [popupPosition, setPopupPosition] = useState({ top: 0, left: 0 });
  // 단어 클릭 시점의 자막 정보 (단어 수집용)
  const [clickedSubtitle, setClickedSubtitle] = useState(null);


  // content.jsx에서 자막 수신
  useEffect(() => {
    const listener = async (message) => {

      // 영상 아이디와 제목 가져오기
      if (message.type === 'SUBTITLES_DATA') {
        setVideoTitle(message.videoTitle);

        // 서버에서 자막 조회
        const response = await apiFetch(`/api/videos/${message.videoId}/subtitles`, {
          method: 'GET'
        });

        // 자막이 있으면 시간순 정렬 후 저장
        if (response.success && response.data.subtitles.length > 0) {
          const sorted = response.data.subtitles.sort((a, b) => a.startTime - b.startTime);
          setSubtitles(sorted);
          setSubtitleIndex(sorted.length - 1);
          setCurrentSubtitle(sorted[sorted.length - 1]);
        }
      }

      // 새 자막 도착
      if (message.type === 'SUBTITLE_UPDATE') {
        const newSubtitle = {
          text: message.text,
          startTime: message.startTime,
          endTime: message.endTime || null,
          translation: message.translation,
          id: Date.now()
        };

        // 중복 자막 방지 후 목록에 추가
        setSubtitles(prev => {
        if (prev.some(s => s.text === message.text)) return prev;
          const newList = [...prev, newSubtitle].sort((a, b) => a.startTime - b.startTime);

          // 새 자막의 정렬 후 인덱스 찾기
          const insertedIndex = newList.findIndex(s => s.id === newSubtitle.id);

          // 갱신된 자막 받아서 최근 자막을 현재 위치로 설정
          setSubtitleIndex(insertedIndex);  
          return newList;
        });

        setCurrentSubtitle(newSubtitle);
      }
    };

    // 메시지 리스너 등록
    chrome.runtime.onMessage.addListener(listener);

    // 페이지 벗어날 때 리스너 제거
    return () => chrome.runtime.onMessage.removeListener(listener);
  }, []);


  // 이전 자막 스크립트 이동
  const goPrev = () => {
    if (subtitleIndex > 0) {
      const newIndex = subtitleIndex - 1;
      setSubtitleIndex(newIndex);
      setCurrentSubtitle(subtitles[newIndex]);
    }
  };


  // 다음 자막 스크립트 이동
  const goNext = () => {
    if (subtitleIndex < subtitles.length - 1) {
      const newIndex = subtitleIndex + 1;
      setSubtitleIndex(newIndex);
      setCurrentSubtitle(subtitles[newIndex]);
    }
  };


  // 구간 반복 재생 (startTime ~ endTime)
  const toggleLoop = () => {
    chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
      if (!tabs[0]) return;

      if (isLooping) {
        // 반복 중이면 중지
        chrome.tabs.sendMessage(tabs[0].id, { type: 'STOP_LOOP' });
        setIsLooping(false);
      } else {
        // 반복 안 하고 있으면 시작
        chrome.tabs.sendMessage(tabs[0].id, {
          type: 'START_LOOP',
          startTime: currentSubtitle.startTime,
          endTime: currentSubtitle.endTime
        });
        setIsLooping(true);
      }
    });
  };


  // 불용어 제외 (축약형 포함)
  const stopWords = [
    'i', 'am', 'a', 'the', 'is', 'are', 'it', 'to', 'for', 'of', 'and', 'in', 'on', 'at', "'s", "'re", "'m", "'ll", "'ve", "'d", "n't"
  ];


  // 번역 API 호출
  async function fetchTranslation(word) {
    try {
      const response = await chrome.runtime.sendMessage({
        type: 'TRANSLATE',
        text: word,
        source: 'sidepanel'
      });
      return response?.translatedText || '';
    } catch (error) {
      console.log('번역 실패', error);
      return '';
    }
  }


  // 단어 클릭 시 사전 팝업 열기
  const togglePin = async (e, word) => {
    // 다른 클릭 이벤트 막기
    e.stopPropagation();

    // 이미 열려있으면 닫기
    if (isPinned) {
      setIsPinned(false);
      setDictionaryData(null);
      setClickedSubtitle(null);
      return;
    }

    // 클릭 시점의 자막 정보 저장 (단어 수집용)
    setClickedSubtitle({ ...currentSubtitle });

    // 클릭한 단어 위치 계산
    const rect = e.target.getBoundingClientRect();
    // 팝업 너비
    const popupWidth = 270;
    let left = rect.left + (rect.width / 2);

    // 화면 왼쪽 밖으로 나가면 조정
    if (left - popupWidth / 2 < 10) {
      left = popupWidth / 2 + 10;
    }
  
    // 화면 오른쪽 밖으로 나가면 조정
    if (left + popupWidth / 2 > window.innerWidth - 10) {
      left = window.innerWidth - popupWidth / 2 - 10;
    }

    setPopupPosition({
      // 단어 위쪽에 표시
      top: rect.top - 210,
      // 중앙 정렬
      left: left - popupWidth / 2
    });

    // 이미 조회한 단어면 캐시에서 가져오기
    if (cache[word]) {
      setDictionaryData(cache[word]);
      setIsPinned(true);
      return;
    }

    // 사전 API 호출
    try {
      const response = await fetch(`https://api.dictionaryapi.dev/api/v2/entries/en/${word}`);
      
      if (!response.ok) {
        setDictionaryData({
          word: word,
          phonetic: '',
          translation: ''
        });
        return;
      }

      const data = await response.json();

      if (data && data[0]) {
        const meaning = data[0].meanings[0];
        const def = meaning.definitions[0];
        const result = {
          word: word,
          // 발음 기호
          phonetic: data[0].phonetic || '',
          // 발음 음성
          audio: data[0].phonetics?.find(p => p.audio)?.audio || '',
          // 품사
          partOfSpeech: meaning.partOfSpeech,
          // 영어 뜻
          definition: def.definition,
          // 예문
          example: def.example || '',
          // 유의어
          synonyms: meaning.synonyms?.slice(0, 5) || [],
          // 반의어
          antonyms: meaning.antonyms?.slice(0, 5) || [],
          translation: ''
        };

        // 한글 번역 추가
        const translationWord = await fetchTranslation(word);
        result.translation = translationWord;

        // 캐시에 저장
        setCache(prev => ({ ...prev, [word]: result }));
        setDictionaryData(result);
        setIsPinned(true);
      } else {
        setDictionaryData({
          word: word,
          phonetic: '',
          translation: ''
        });
      }
    } catch (error) {
      console.log('사전 조회 실패:', error);
      setDictionaryData({
        word: word,
        phonetic: '',
        translation: ''
      });
    }
  };


  // 사전 팝업 닫기
  const closePopup = () => {
    setIsPinned(false);
    setDictionaryData(null);
  };


  // 시간 형식 변환 (초 → 00:00:00)
  const formatTime = (seconds) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };


  // 단어 수집
  const collectWord = async (word) => {
    try {
      await apiFetch('/api/words/collect', {
        method: 'POST',
        body: JSON.stringify({
          videoId: videoId,
          word: word,
          // 단어가 포함된 문장
          sentence: clickedSubtitle.text,
          // 영상 시간
          timestamp: formatTime(currentSubtitle.startTime),
          // 한글 뜻
          translation: dictionaryData.translation,
          // 영상 제목
          title: videoTitle
        })
      });
    } catch (error) {
      console.log('단어 수집 실패:', error);
    }
  };


  // 보기 버튼 위치
  const buttonPositions = [
    // 1번: 왼쪽 위
    { padding: '11px 63px 12px 63px' },
    // 2번: 오른쪽 위
    { padding: '11px 65px 12px 65px' },
    // 3번: 왼쪽 아래
    { padding: '11px 65px 12px 65px' },
    // 4번: 오른쪽 아래
    { padding: '11px 74px 12px 74px' },
  ];


  // 현재 문제 정보
  const currentQuiz = quizzes[currentIndex];
  const isCorrect = tempChoice === currentQuiz?.correctAnswer;


  // 정답 오답 선택 미선택에 따른 border 색상 변화
  const borderColor = (choice) => {
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '1px solid #9B87E8' : 'none';
    }

    // 확정 후: 정답이면
    if (choice === currentQuiz?.correctAnswer) {
      return '1px solid #C1C0FC';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !== currentQuiz?.correctAnswer) {
      return '1px solid #FFB484';
    }

    // 확정 후: 미선택이면
    return 'none';
  };


  // 정답 오답 선택 미선택에 따른 background 색상 변화
  const backgroundColor = (choice) => {
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '#F0F3FF' : '#F7F7F7';
    }

    // 확정 후: 정답이면
    if (choice === currentQuiz?.correctAnswer) {
      return '#F0F3FF';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !== currentQuiz?.correctAnswer) {
      return '#FFF4EE';
    }

    // 확정 후: 미선택이면
    return '#F7F7F7';
  };


  // 정답 오답 선택 미선택에 따른 color 색상 변화
  const textColor = (choice) => {
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '#5559D9' : '#4D525C';
    }

    // 확정 후: 정답이면
    if (choice === currentQuiz?.correctAnswer) {
      return '#5559D9';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !== currentQuiz?.correctAnswer) {
      return '#F7731E';
    }

    // 확정 후: 미선택이면
    return '#4D525C';
  };


  // 퀴즈 초기화 및 이전 진행 상황 복원
  useEffect(() => {
    // 크롬 저장소에서 이전 진행 상황 확인
    chrome.storage.local.get('quizState', async (result) => {

      // 같은 영상의 저장된 퀴즈가 있으면 이어하기
      if (result.quizState?.videoId === videoId) {
        setQuizzes(result.quizState.quizzes);
        setSessionId(result.quizState.sessionId);
        setCurrentIndex(result.quizState.currentIndex);
        setCorrectCount(result.quizState.correctCount);
        setWrongCount(result.quizState.wrongCount);
        setAnswers(result.quizState.answers);
        setIsConfirmed(result.quizState.isConfirmed ?? false);
        setTempChoice(result.quizState.tempChoice ?? null);

        // 영상 일시정지 메시지 전송
        chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
          if (tabs[0]) chrome.tabs.sendMessage(tabs[0].id, { type: 'SET_QUIZ_MODE', active: true }).catch(() => {});
        });
      }

      // 복원 완료 체크
      setIsRestoring(false);
    });

    // content.jsx에서 새 퀴즈 받기
    const listener = (message) => {
      if (message.type === 'QUIZ_READY') {
        // 새 퀴즈로 초기화
        setSessionId(message.sessionId);
        setQuizzes(message.quizzes);
        setCurrentIndex(0);
        setCorrectCount(0);
        setWrongCount(0);
        setAnswers([]);
        setIsConfirmed(false);
        setTempChoice(null);

      // 영상 일시정지 메시지 전송
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        if (tabs[0]) chrome.tabs.sendMessage(tabs[0].id, { type: 'SET_QUIZ_MODE', active: true }).catch(() => {});
      });
    }
  };
  chrome.runtime.onMessage.addListener(listener);

  // cleanup
  return () => chrome.runtime.onMessage.removeListener(listener);
  }, [videoId]);


  // 진행 상황 자동 저장 (상태 바뀔 때마다)
  useEffect(() => {
    // 복원 중 아닐 때만 저장
    if (sessionId && !isRestoring) {
      chrome.storage.local.set({
        quizState: {
          videoId,
          sessionId,
          quizzes,
          currentIndex,
          correctCount,
          wrongCount,
          answers,
          isConfirmed,
          tempChoice
        }
      });
    }
  }, [currentIndex, correctCount, wrongCount, answers, isConfirmed, sessionId, tempChoice, videoId, isRestoring, quizzes]);


  // 답안 제출
  const handleSubmit = async () => {
    // 선택 안 했거나 이미 제출했으면 무시
    if (!tempChoice || isConfirmed) return;
    try {
      // 서버에 정답 제출
      const result = await apiFetch('/api/quiz/submit', {
        method: 'POST',
        body: JSON.stringify({
          sessionId,
          quizId: currentQuiz?.quizId,
          word: currentQuiz?.word,
          quizType: 'BLANK',
          question: currentQuiz?.question,
          correctAnswer: currentQuiz?.correctAnswer,
          userAnswer: tempChoice
        })
      });

      setFeedback(result.data);
      setIsConfirmed(true);

      // 정답/오답 카운트
      if (result.data?.isCorrect) {
        setCorrectCount(prev => prev + 1);
      } else {
        setWrongCount(prev => prev + 1);
      }

      // 문제별 기록 저장
      setAnswers(prev => [...prev, {
        quizId: currentQuiz?.quizId,
        userAnswer: tempChoice,
        isCorrect: result.data?.isCorrect
      }]);
    } catch (error) {
      console.error('제출 실패', error);
    }
  };


  // 다음 문제 또는 정산 완료
  const handleNext = async () => {
    if (currentIndex < quizzes.length - 1) {
      // 다음 문제로 이동
      setCurrentIndex(prev => prev + 1);
      setTempChoice(null);
      setIsConfirmed(false);
      setFeedback(null);
      setDictionaryData(null);
    } else {
      // 마지막 문제였으면 퀴즈 종료
      try {
        const finalResult = await apiFetch(`/api/quiz/sessions/${sessionId}/complete`, {
          method: 'POST'
        });
        // 저장된 진행 상황 삭제
        chrome.storage.local.remove('quizState');
        // 정산 페이지로 이동
        onSettlementPage(finalResult.data);
      } catch (error) {
        console.error('세션 종료 실패', error);
      }
    }
  };


  // 퀴즈 보기 버튼 선택
  const handleChoice = (choice) => {
    // 이미 제출했으면 변경 막기
    if (isConfirmed) return;
    setTempChoice(choice);
  };


  // 퀴즈 중간에 나가기
  const handleExit = async () => {
    // 디폴트페이지로 이동
    onExitPage();
  };


  return (
    // 사이드패널 고정
    <div>

      {/* 최상위 박스 */}
      <div style={{
        display: 'flex',
        width: '402px',
        flexDirection: 'column',
        alignItems: 'flex-start',
        gap: '32px'
      }}>

        {/* 상단 박스 퀴즈 ~ 지나간 자막 어휘 */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          alignSelf: 'stretch'
        }}>

          {/* 비슷한 박스 */}
          <div style={{
            display: 'flex',
            width: '402px',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}>

            {/* 비슷한 박스2 */}
            <div style={{
              display: 'flex',
              width: '402px',
              flexDirection: 'column',
              alignItems: 'center',
              gap: '36px',
              flexShrink: '0'
            }}>

              {/* 상단 중간 박스 (퀴즈 ~ 빈칸 채우기) */}
              <div style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: '28px',
                alignSelf: 'stretch',
              }}>

                {/* 상단 퀴즈 전체 박스 */}
                <div style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  gap: '29px',
                  alignSelf: 'stretch',
                }}>


                  <div style={{
                    display: 'flex',
                    height: '54px',
                    padding: '8px 177px 8px 16px',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    gap: '10px',
                    alignSelf: 'stretch',
                    background: '#C084FC'
                  }}>


                    <div style={{
                      display: 'flex',
                      width: '209px',
                      justifyContent: 'space-between',
                      alignItems: 'center'
                    }}>


                      <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                      }}>

                        {/* 왼쪽 네모 */}
                        <div style={{
                        display: 'flex',
                        width: '24px',
                        height: '24px',
                        alignItems: 'center',
                        background: '#E1E1E1'
                        }}>
                        </div>

                        {/* 오른쪽 네모 */}
                        <div style={{
                          display: 'flex',
                          width: '24px',
                          height: '24px',
                          alignItems: 'center',
                          background: '#E1E1E1'
                        }}>
                        </div>
                      </div>


                      <div style={{
                        display: 'flex',
                        height: '38px',
                        padding: '10px',
                        justifyContent: 'center',
                        alignItems: 'center',
                        gap: '10px'
                      }}>


                        <p style={{
                          color: '#FFF',
                          textAlign: 'center',
                          fontFamily: 'Pretendard',
                          fontSize: '16px',
                          fontStyle: 'normal',
                          fontWeight: '700',
                          lineHeight: 'normal',
                          letterSpacing: '-0.032px'
                        }}>
                          퀴즈
                        </p>
                      </div>
                    </div>
                  </div>

                  {/* 진행바 전체 박스 */}
                  <div style={{
                    display: 'flex',
                    padding: '4px 0',
                    alignItems: 'center',
                    gap: '12px'
                  }}>


                    <div style={{
                      width: '36px',
                      height: '14px'
                    }}>

                      {/* 왼쪽 현재 퀴즈 번호 / 전체 퀴즈 개수 */}
                      <div style={{
                        display: 'flex',
                        width: '36px',
                        height: '14px',
                        flexDirection: 'row',  // 가로 배치로 변경
                        alignItems: 'center',  // 세로 가운데 정렬로 변경
                      }}>


                        <p style={{
                          color: '#0D0C34',
                          textAlign: 'center',
                          fontFamily: 'Pretendard',
                          fontSize: '16px',
                          fontStyle: 'normal',
                          fontWeight: '700',
                          lineHeight: 'normal',
                          letterSpacing: '-0.032px'
                        }}>
                          {(currentIndex + 1)}
                        </p>


                        <p style={{
                          color: '#9F9EB0',
                          fontFamily: 'Pretendard',
                          fontSize: '16px',
                          fontStyle: 'normal',
                          fontWeight: '500',
                          lineHeight: 'normal',
                          letterSpacing: '-0.032px'
                        }}>
                          /{quizzes.length}
                        </p>
                      </div>
                    </div>

                    <div style={{
                      display: 'flex',
                      width: '274px',
                      flexDirection: 'column',  // 가로 배치로 변경
                      alignItems: 'center',  // 세로 가운데 정렬로 변경
                    }}>

                      <div style={{
                        display: 'flex',
                        width: '274px',
                        flexDirection: 'column',
                        alignItems: 'flex-start',
                        alignSelf: 'stretch'
                      }}>

                        {/* 전체 회색 진행바 */}
                        <div style={{
                          display: 'flex',
                          width: '274px',
                          height: '14px',
                          flexDirection: 'column',
                          alignItems: 'flex-start',
                          position: 'absolute',
                          borderRadius: '999px',
                          backgroundColor: '#D8D8E2',
                        }}>
                        </div>

                        {/* 채워지는 보라색 바 */}
                        <div style={{ 
                          display: 'flex',
                          width: `${274 * ((currentIndex + 1) / quizzes.length)}px`,
                          height: '14px',
                          flexDirection: 'column',
                          alignItems: 'flex-start',
                          borderRadius: '999px',
                          background: '#9B87E8', 
                        }}>
                        </div>
                      </div>

                      {/* 개구리 이미지 넣기? */}
                      <div style={{
                        width: '36px',
                        height: '36px',
                        aspectRatio: '1/1',
                        position: 'absolute',
                        left: '75px',
                        top: '-14px',
                        // background: url(<path-to-image>) lightgray -42.788px -1.005px / 228.637% 225.776% no-repeat;
                        }}>
                      </div>
                    </div>

                  {/* 상단 나가기 버튼 */}
                  <button
                  onClick={() => setExitModal(true)}
                  style={{
                    display: 'flex',
                    width: '36px',
                    height: '36px',
                    alignItems: 'center',
                    background: '#E1E1E1'
                  }}>
                  </button>
                </div>
              </div>


              <div style={{
                display: 'flex',
                width: '370px',
                height: '291px',
                padding: '5px 14px',
                flexDirection: 'column',
                alignItems: 'flex-start',
                gap: '10px',
                position: 'relative'  // 추가함
              }}>


                <div style={{
                  display: 'flex',
                  width: '370px',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                }}>

                  {/* 개구리 마스코트1 */}
                  <div style={{ 
                    width: '152px',
                    height: '103px',
                    aspectRatio: '152/103',
                    // lightgray 대신 transparent 사용해서 투명처리
                    background: `url(${frog}) transparent -226.694px -200.414px / 255.502% 375.791% no-repeat`,
                    // 비율 유지 + 전체 보이기
                    backgroundSize: 'contain',
                    // 가운데 정렬
                    backgroundPosition: 'center',
                  }}>
                  </div>


                  <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    alignSelf: 'stretch'
                  }}>

                    {/* 전체 문제 박스 */}
                    <div style={{
                      display: 'flex',
                      padding: '24px 20px 20px 20px',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: '24px',
                      alignSelf: 'stretch',
                      borderRadius: '24px',
                      background: '#FFF',
                    }}>


                      <div style={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'flex-start',
                        gap: '16px',
                        alignSelf: 'stretch'
                      }}>


                        {/* 방금 자막 */}
                        <div style={{
                          display: 'flex',
                          padding: '6px 12px',
                          justifyContent: 'center',
                          alignItems: 'center',
                          borderRadius: '999px',
                          background: '#F5F3FF',
                        }}>


                          <p style={{
                            color: '#01030D',
                            fontFamily: 'Pretendard',
                            fontSize: '12px',
                            fontStyle: 'normal',
                            fontWeight: '400',
                            lineHeight: 'normal'
                          }}>
                            방금 자막
                          </p>
                        </div>


                        {/* 중간 중간 패딩박스 */}
                        <div style={{
                          display: 'flex',
                          width: '330px',
                          flexDirection: 'column',
                          alignItems: 'flex-start',
                          gap: '8px',
                        }}>

                          {/* 영어 자막 박스 */}
                          <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '4px',
                            alignSelf: 'stretch'
                          }}>

                            {/* 영어 자막 - 단어별 호버 */}
                            <p style={{
                              color: '#01030D',
                              fontFamily: 'Pretendard',
                              fontSize: '16px',
                              fontStyle: 'normal',
                              fontWeight: '700',
                              lineHeight: 'normal',
                              letterSpacing: '-0.32px'
                            }}>
                              {/* 자막 문장을 공백으로 나눠서 단어별로 처리 */}
                              {currentSubtitle.text.split(' ').map((word, index) => {
                                // 특수문자만 제거
                                const cleaned = word.replace(/[^a-zA-Z']/g, '');

                                // 해당 단어를 영문자만 소문자로 바꾸고 불용어 아니면 true, 불용어면 false
                                // map이 모든 단어 순회하면서 isHoverable 여러 번 찍힘
                                const isHoverable = cleaned && !stopWords.includes(cleaned.toLowerCase());
                                console.log('호버1:', isHoverable);

                                return (
                                  <span
                                  key={index}
                                  // 클릭 시 불용어 아니면 팝업 고정
                                  onClick={(e) => isHoverable && togglePin(e, cleaned)}>
                                    {/* 원본 단어 그대로 표시 + 공백 추가 */}
                                    {word}{' '} 
                                  </span>
                                );
                              })}
                            </p>
                          </div>

                          {/* 한글 번역 */}
                          <p style={{
                            alignSelf: 'stretch',
                            color: '#9198A3',
                            fontFamily: 'Pretendard',
                            fontSize: '16px',
                            fontStyle: 'normal',
                            fontWeight: '700',
                            lineHeight: 'normal'
                          }}>
                            {currentSubtitle.translation}
                          </p>
                        </div>
                      </div>

                      {/* 버튼 박스 */}
                      <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '30px',
                      }}>

                        {/* 전 스크립트 보기 */}
                        <button
                        onClick= {() => goPrev()}
                        style={{
                          display: 'flex',
                          width: '32px',
                          height: '32px',
                          justifyContent: 'center',
                          alignItems: 'center',
                          borderRadius: '999px',
                          background: '#E1E1E1',
                        }}>
                        </button>

                        {/* 현재 스크립트 구간 반복 */}
                        <button 
                        onClick={() => toggleLoop()}
                        style={{
                          display: 'flex',
                          width: '32px',
                          height: '32px',
                          justifyContent: 'center',
                          alignItems: 'center',
                          background: '#E1E1E1',
                        }}>
                        </button>

                        {/* 다음 스크립트 보기 */}
                        <button
                        onClick={() => goNext()}
                        style={{
                          display: 'flex',
                          width: '32px',
                          height: '32px',
                          justifyContent: 'center',
                          alignItems: 'center',
                          borderRadius: '999px',
                          background: '#E1E1E1',
                        }}>
                        </button>
                      </div>
                    </div>
                  </div>
                </div>

                {/* 말풍선 최상위 박스 */}
                <div style={{
                  display: 'flex',
                  width: '204px',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  gap: '10px',
                  position: 'absolute',
                  right: '14px',
                  top: '5px'
                }}>

                  {/* 말풍선 */}
                  <svg
                  style={{
                    width: '204px',
                    height: '68px',
                    position: 'absolute'
                  }}
                  xmlns="http://www.w3.org/2000/svg"
                  width="204"
                  height="68"
                  viewBox="0 0 204 68"
                  fill="none">
                    <path d="M188 0C196.837 3.2327e-05 204 7.16346 204 16V52C204 60.8365 196.837 68 188 68H4.12501C0.904653 68 -1.03004 64.1901 0.576182 60.8711L0.743174 60.5537C3.27772 57.3476 4.54459 55.7434 5.57911 53.9951C7.88039 50.106 9.39333 45.6025 10 40.8564V16C10 7.16344 17.1635 0 26 0H188Z" fill="white"/>
                  </svg>


                  <p style={{
                    position: 'absolute', // 추가
                    top: '16px',  // 추가
                    left: '26px', // 추가
                    width: '168px',
                    color: '#01030D',
                    fontFamily: 'Pretendard',
                    fontSize: '12px',
                    fontStyle: 'normal',
                    fontWeight: '500',
                    lineHeight: '18px',
                    textTransform: 'uppercase'
                  }}>
                    {!isConfirmed ? "이 문장의 빈칸, 알 것 같지 않아?" : isCorrect ? feedback?.feedback : currentQuiz?.hint}
                  </p>
                </div>
              </div>



              {/* 힌트 박스 */}
              {/* 선택했을 때 */}
              {isConfirmed && (
                // 정답인 경우
                isCorrect ? (
                  <div style={{
                    display: 'flex',
                    width: '370px',
                    height: '68px',
                    padding: '16px 24px 16px 16px',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    gap: '10px',
                    flexShrink: '0',
                    borderRadius: '12px',
                    background: '#CCC',
                  }}>

                    {/* 힌트 박스 gap */}
                    <div style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                    }}>

                      {/* 힌트 전구 */}
                      <div style={{
                        width: '32px',
                        height: '32px',
                        aspectRatio: '1/1',
                        // lightgray 대신 transparent 사용해서 투명처리
                        background: `url(${bulb}) transparent 50% / cover no-repeat`,
                        // 비율 유지 + 전체 보이기
                        backgroundSize: 'contain',
                        // 가운데 정렬
                        backgroundPosition: 'center',
                      }}>
                      </div>

                      {/* 힌트 박스 글 */}
                      <div style={{
                        width: '290px',
                        color: '#FFF',
                        fontFamily: 'Pretendard',
                        fontSize: '12px',
                        fontStyle: 'normal',
                        fontWeight: '700',
                        lineHeight: '18px', /* 150% */
                      }}>
                        {currentQuiz?.correctAnswer}이 정답인 이유!<br></br>
                        {currentQuiz?.correctAnswer}은 정답번역을(를) 뜻하기 때문에 {currentQuiz?.correctAnswer}이 맞아!
                      </div>
                    </div>
                  </div>
                ) : (
                  // 오답인 경우
                  <div style={{
                    display: 'flex',
                    width: '370px',
                    height: '68px',
                    padding: '16px 24px 16px 16px',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    gap: '10px',
                    flexShrink: '0',
                    borderRadius: '12px',
                    background: '#CCC',
                  }}>

                    {/* 힌트 박스 gap */}
                    <div style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '8px',
                    }}>

                      {/* 힌트 전구 */}
                      <div style={{
                        width: '32px',
                        height: '32px',
                        aspectRatio: '1/1',
                        // lightgray 대신 transparent 사용해서 투명처리
                        background: `url(${bulb}) transparent 50% / cover no-repeat`,
                        // 비율 유지 + 전체 보이기
                        backgroundSize: 'contain',
                        // 가운데 정렬
                        backgroundPosition: 'center',
                      }}>
                      </div>

                      <div style={{
                        width: '290px',
                        color: '#FFF',
                        fontFamily: 'Pretendard',
                        fontSize: '12px',
                        fontStyle: 'normal',
                        fontWeight: '700',
                        lineHeight: '18px', /* 150% */
                      }}>
                        {currentQuiz?.correctAnswer}이 정답인 이유! {tempChoice}은(는) 오답번역을(를) 말하고,<br></br>
                        {currentQuiz?.correctAnswer}은(는) 정답번역을(를) 뜻하기 때문에 {currentQuiz?.correctAnswer}이(가) 맞아!
                      </div>
                    </div>
                  </div>
                )
              )}



              {/* 전체 퀴즈 박스 */}
              <div style={{
                display: 'flex',
                width: '370px',
                padding: '24px 16px 20px 16px',
                flexDirection: 'column',
                alignItems: 'flex-start',
                gap: '24px',
                borderRadius: '24px',
                background: '#FFF'
              }}>

                {/* 문제 박스 */}
                <div style={{
                  display: 'flex',
                  width: '334px',
                  padding: '0 4px',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  gap: '12px',
                }}>

                  {/* 빈칸 채우기 박스 */}
                  <div style={{
                    display: 'flex',
                    padding: '6px 12px',
                    justifyContent: 'center',
                    alignItems: 'center',
                    borderRadius: '999px',
                    background: '#F5F3FF',
                  }}>


                    <p style={{
                      color: '#01030D',
                      fontFamily: 'Pretendard',
                      fontSize: '12px',
                      fontStyle: 'normal',
                      fontWeight: '400',
                      lineHeight: 'normal'
                    }}>
                      빈칸채우기
                    </p>
                  </div>

                  {/* 자막, 뜻 들어가는 박스 */}
                  <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    gap: '8px',
                    alignSelf: 'stretch'
                  }}>

                    {/* 자막 전용 박스 */}
                    <div style={{
                      display: 'flex',
                      width: '191px',
                      flexDirection: 'column',
                      alignItems: 'flex-start',
                      gap: '4px'
                    }}>

                      {/* 빈칸 채우기 문제 자막 박스 */}
                      <div style={{
                        display: 'flex',
                        alignItems: 'flex-end',
                        gap: '4px',
                        alignSelf: 'stretch'
                      }}>


                        <p style={{ 
                          alignSelf: 'stretch',
                          color: '#01030D',
                          fontFamily: 'Pretendard',
                          fontSize: '16px',
                          fontStyle: 'normal',
                          fontWeight: '700',
                          lineHeight: 'normal'
                        }}>
                          {/* 회색 박스 대신 _____ 사용 */}
                          {currentQuiz?.question.replace('____', tempChoice || '____')}
                        </p>
                      </div>
                    </div>


                    <p style={{
                      alignSelf: 'stretch',
                      color: '#9198A3',
                      fontFamily: 'Pretendard',
                      fontSize: '16px',
                      fontStyle: 'normal',
                      fontWeight: '700',
                      lineHeight: 'normal'
                    }}>
                      {currentQuiz?.sentence}
                    </p>
                  </div>
                </div>

                {/* 보기 버튼 전체 박스 */}
                <div style={{
                  display: 'flex',
                  alignItems: 'flex-start',
                  alignContent: 'flex-start',
                  gap: '8px',
                  alignSelf: 'stretch',
                  flexWrap: 'wrap'
                }}>

                  {/* 보기 버튼 */}
                  {currentQuiz?.options.map((choice, i) => (
                    <button
                    key={i}
                    disabled={isConfirmed}
                    onClick={() => handleChoice(choice)}
                    style={{
                      display: 'flex',
                      width: '165px',
                      height: '40px',
                      padding: buttonPositions[i].padding,
                      justifyContent: 'center',
                      alignItems: 'center',
                      gap: '10px',
                      borderRadius: '10px',
                      border: borderColor(choice),
                      // border: tempChoice === choice ? '2px solid #EEE' : '1px solid #E7E6EB',
                      background: backgroundColor(choice),
                      // background: tempChoice === choice ? '#F8F8FA' : '#FFF',
                      boxShadow: '0 4px 4px 0 rgba(206, 210, 223, 0.16)',
                      color: textColor(choice),
                      // color: '#4D525C',
                      textAlign: 'center',
                      fontFamily: 'Pretendard',
                      fontSize: '14px',
                      fontStyle: 'normal',
                      fontWeight: '400',
                      lineHeight: 'normal',
                    }}>
                      {choice}
                    </button>
                  ))}
                </div>
              </div>



              {/* 럭키 미스테이크 전체 배경 박스 */}
              {isConfirmed && !feedback?.isCorrect && (
                <div style={{ 
                  display: 'flex',
                  width: '370px',
                  height: '220px',
                  padding: '24px',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  gap: '10px',
                  borderRadius: '24px',
                  background: '#CCC',
                }}>

                  {/* 럭키 미스테이크 전체 내용 박스 */}
                  <div style={{ 
                    display: 'flex',
                    width: '322px',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    gap: '16px',
                  }}>

                    {/* 럭키 미스테이크 안내글 박스 */}
                    <div style={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'flex-start',
                      gap: '8px',
                      alignSelf: 'stretch',
                    }}>

                      {/* 럭키 미스테이크 */}
                      <p style={{
                        alignSelf: 'stretch',
                        color: '#FFF',
                        fontFamily: 'Pretendard',
                        fontSize: '14px',
                        fontStyle: 'normal',
                        fontWeight: '500',
                        lineHeight: '15px', /* 107.143% */
                      }}>
                        럭키 미스테이크
                      </p>

                      {/* 틀린 단어 보상 안내 */}
                      <p style={{
                        alignSelf: 'stretch',
                        color: '#FFF',
                        fontFamily: 'Pretendard',
                        fontSize: '18px',
                        fontStyle: 'normal',
                        fontWeight: '700',
                        lineHeight: '28px', /* 155.556% */
                      }}>
                        틀린 단어는 다음에 다시 나와요!<br></br>그때 맞히면 보상이 더 커요
                      </p>
                    </div>

                    {/* 계속하기 버튼 + 경험치 안내 박스 */}
                    <div style={{
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      gap: '16px',
                      alignSelf: 'stretch',
                    }}>

                      {/* 계속하기 버튼 */}
                      <button
                      onClick={handleNext}
                      style={{
                        display: 'flex',
                        height: '44px',
                        padding: '14px 123px 13px 117px',
                        justifyContent: 'center',
                        alignItems: 'center',
                        gap: '10px',
                        alignSelf: 'stretch',
                        borderRadius: '14.133px',
                        background: '#ABABAB',
                      }}>

                        {/* 퀴즈 계속하기 */}
                        <p style={{
                          color: '#FFF',
                          textAlign: 'center',
                          fontFamily: 'Pretendard',
                          fontSize: '14.133px',
                          fontStyle: 'normal',
                          fontWeight: '700',
                          lineHeight: 'normal',
                          letterSpacing: '-0.028px'
                        }}>
                          퀴즈 계속 하기
                        </p>
                      </button>

                      {/* 경험치 안내 박스 */}
                      <div style={{ 
                        display: 'flex',
                        justifyContent: 'center',
                        alignItems: 'center',
                      }}>

                        {/* 경험치 안내 글 */}
                        <div style={{
                          color: '#F4F4F4',
                          fontFamily: 'Pretendard',
                          fontSize: '14px',
                          fontStyle: 'normal',
                          fontWeight: '500',
                          lineHeight: 'normal'
                        }}>
                          다음 {currentQuiz?.word} 퀴즈 정답 시 × 1.5 EXP
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
                )}



            </div>
          </div>
        </div>
      </div>



      {/* 하단 고정 바 */}
      <div style={{
        display: 'flex',
        padding: '10px 16px',
        justifyContent: 'center',
        alignItems: 'center',
        alignSelf: 'stretch',
        borderTop: '1px solid #E1E6EE',
        background: '#FFF'
      }}>

        {/* 중간 박스 */}
        <div style={{
          display: 'flex',
          width: '370px',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}>

          {/* 건너뛰기 버튼 */}
          <button style={{
            display: 'flex',
            width: '120px',
            height: '40px',
            padding: '11px 30px',
            justifyContent: 'center',
            alignItems: 'center',
            gap: '10px',
            flexShrink: '0',
            borderRadius: '12px',
            border: '1px solid #D9D9D9',
            background: '#FFF'
          }}>


            <p style={{
              color: '#01030D',
              fontFamily: 'Pretendard',
              fontSize: '16px',
              fontStyle: 'normal',
              fontWeight: '500',
              lineHeight: '18px' /* 112.5% */
            }}>
              건너 뛰기
            </p>
          </button>

          {/* 제출 및 넘어가기 버튼 */}
          {!isConfirmed ? (
            <button
            onClick={handleSubmit}
            style={{
              display: 'flex',
              width: '238px',
              height: '40px',
              padding: '11px 90px 11px 89px',
              justifyContent: 'center',
              alignItems: 'center',
              gap: '10px',
              flexShrink: '0',
              borderRadius: '12px',
              background: '#D9D9D9'
            }}>
              <p style={{
                color: '#FFF',
                fontFamily: 'Pretendard',
                fontSize: '16px',
                fontStyle: 'normal',
                fontWeight: '700',
                lineHeight: '18px' /* 112.5% */
              }}>
                문제 확인
              </p>
            </button>
            ) : (
            <button
            onClick={handleNext}
            style={{
              display: 'flex',
              width: '238px',
              height: '40px',
              padding: '11px 90px 11px 89px',
              justifyContent: 'center',
              alignItems: 'center',
              gap: '10px',
              flexShrink: '0',
              borderRadius: '12px',
              background: '#D9D9D9'
            }}>
              <p style={{
                color: '#FFF',
                fontFamily: 'Pretendard',
                fontSize: '16px',
                fontStyle: 'normal',
                fontWeight: '700',
                lineHeight: '18px' /* 112.5% */
              }}>
                계속 하기
              </p>
            </button>
          )}
            </div>
          </div>
        </div>

        {/* 사전 팝업 */}
        {/* dictionaryData 로 렌더링 기다리기 */}
        {dictionaryData && (
          <div style={{
          // absolute 대신 사용
            position: 'fixed',  
            top: popupPosition.top,
            left: popupPosition.left,
            width: '270px',
            height: '188.212px',
            borderRadius: '15.979px',
            border: '0.799px solid #E7E6EB',
            background: '#FFF',
            boxShadow: '0 3.196px 3.196px 0 rgba(206, 210, 223, 0.16)',
            zIndex: '999',
          }}>

            {/* 수집 기능 넣기 */}
            <button 
            onClick={() => collectWord(dictionaryData.word)}
            style={{
              position: 'absolute',
              top: '23.65px',
              bottom: '132.6px',
              left: '15.77px',
              right: '167.94px',
              width: '86.289px',
              height: '31.959px',
              zIndex: '999',
              borderRadius: '7.99px',
              border: '0.799px solid #E7E6EB',
              background: '#FFF',
              boxSizing: 'border-box'
            }}>

              {/* 사전 아이콘 들어갈 자리 */}
              <div style={{
                position: 'absolute',
                top: '5.59x',
                bottom: '7.19px',
                left: '9.59px',
                right: '57.53px',
                width: '19.175px',
                height: '19.175px',
                background: '#CFCFD7',
                zIndex: '999',
                boxSizing: 'border-box'
              }}>
              </div>

              <p style={{
                position: 'absolute',
                top: '9.59x',
                bottom: '11.19px',
                left: '35.16px',
                right: '9.59px',
                display: 'flex',
                width: '41.547px',
                height: '11.186px',
                flexDirection: 'column',
                justifyContent: 'center',
                color: '#01030D',
                fontFamily: 'Pretendard',
                fontSize: '11.186px',
                fontStyle: 'normal',
                fontWeight: '400',
                zIndex: '999',
                lineHeight: 'normal'
              }}>
                단어 수집
              </p>
            </button>

            {/* 닫기 버튼 */}
            <button 
            onClick= {() => closePopup()}
            style={{
              position: 'absolute',
              top: '23.65px',
              bottom: '135.8px',
              left: '224.67px',
              right: '16.57px',
              width: '28.763px',
              height: '28.763px',
              zIndex: '999',
            }}>


              <svg
              xmlns="http://www.w3.org/2000/svg"
              width="29"
              height="29"
              viewBox="0 0 29 29"
              fill="none">
                <rect width="28.7631" height="28.7631" rx="14.3815" fill="#E1E1E1"/>
              </svg>
            </button>

            {/* 명사 */}
            <p style={{
              position: 'absolute',
              top: '71.75px',
              bottom: '105.46px',
              left: '15.95px',
              right: '237.05px',
              zIndex: '999',
              color: '#9198A3',
              fontFamily: 'Pretendard',
              fontSize: '9.588px',
              fontStyle: 'normal',
              fontWeight: '400',
              lineHeight: 'normal'
            }}>
              명사
            </p>

            {/* 단어 */}
            <p style={{
              position: 'absolute',
              top: '82.93x',
              bottom: '77.28px',
              left: '15.95px',
              right: '166.05px',
              zIndex: '999',
              color: '#7C3AED',
              fontFamily: 'Pretendard',
              fontSize: '23.17px',
              fontStyle: 'normal',
              fontWeight: '700',
              lineHeight: 'normal',
              letterSpacing: '-0.463px'
            }}>
              {dictionaryData.word}
            </p>

            {/* 미국식 발음 */}
            <p style={{
              position: 'absolute',
              top: '117.29px',
              bottom: '57.92px',
              left: '15.95px',
              right: '147.05px',
              color: '#9198A3',
              fontFamily: 'Pretendard',
              fontSize: '11.186px',
              fontStyle: 'normal',
              fontWeight: '400',
              lineHeight: 'normal',
              zIndex: '999',
              letterSpacing: '-0.224px'
            }}>
              {dictionaryData.phonetic}
            </p>

            {/* 줄? */}
            <div style={{
              position: 'absolute',
              top: '143.66px',
              bottom: '43.74px',
              left: '15.95px',
              right: '15.95px',
              width: '238.094px',
              height: '0.799px',
              background: '#E7E6EB',
              zIndex: '999',
            }}>
            </div>

            {/* 단어 뜻 번역 */}
            <p style={{
              position: 'absolute',
              top: '157.24px',
              bottom: '15.97px',
              left: '15.95px',
              right: '195.05px',
              color: '#01030D',
              fontFamily: 'Pretendard',
              fontSize: '12.784px',
              fontStyle: 'normal',
              fontWeight: '700',
              lineHeight: 'normal',
              zIndex: '999',
            }}>
              {dictionaryData.translation}
            </p>

            {/* 단어 가리키는 화살표 박스 */}
            <div style={{
              position: 'absolute',
              top: '175.4px',
              bottom: '20.69px',
              left: '50%',  // 좌우 중앙 정렬로 변경
              transform: 'translateX(-50%)',  // 좌우 중앙 정렬로 변경
              width: '29.562px',
              height: '33.504px',
              fill: '#FFF',
              strokeWidth: '0.985px',
              stroke: '#E7E6EB',
              zIndex: '999',
            }}>

              <svg
              xmlns="http://www.w3.org/2000/svg"
              width="21"
              height="21"
              viewBox="0 0 21 21"
              fill="none">
                <path d="M7.24292 18.0095C8.52218 20.5207 12.1101 20.5207 13.3894 18.0095L19.7585 5.50757C20.9275 3.21302 19.2604 0.493045 16.6853 0.49292H3.94702C1.37183 0.49292 -0.295152 3.21297 0.873779 5.50757L7.24292 18.0095Z" fill="white" stroke="#E7E6EB" stroke-width="0.985401"/>
              </svg>
            </div>
          </div>
        )}

        {/* 이탈 모달 */}
        {exitModal && (
          <div  style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            wwidth: '100%', // 너비 고정
            background: 'rgba(0, 0, 0, 0.70)',
          }}>

            {/* 팝업 창 */}
            <div style={{
              position: 'absolute',
              top: 214,
              left: 16,
              right: 16,
              width: '370px',
              background: '#fff',
              borderRadius: '24px',
              border: '1px solid #E7E6EB',
            }}>

              <p style={{
                position: 'absolute',
                top: '24px',
                left: '44px',
                bottom: '374px',
                right: '44px',
                display: 'flex',
                width: '282px',
                height: '48px',
                flexDirection: 'column',
                justifyContent: 'center',
                color: '#0D0C34',
                textAlign: 'center',
                fontFamily: 'Pretendard',
                fontSize: '16px',
                fontStyle: 'normal',
                fontWeight: '700',
                lineHeight: '28px'  /* 175% */
              }}>
                엇, 그만두시게요?<br />
                조금만 더 하면<span
                style={{
                  color: '#9B87E8',
                  fontFamily: 'Pretendard',
                  fontSize: '16px',
                  fontStyle: 'normal',
                  fontWeight: '700',
                  lineHeight: '28px'  /* 175% */
                }}>
                  Silver
                </span>에 가까워져요!
              </p>

              {/* 개구리 마스코트 */}
              <div style={{ 
                position: 'absolute',
                top: '80px',
                bottom: '186px',
                left: '93px',
                right: '93px',
                width: '184px',
                height: '180px',
                aspectRatio: '46/45',
                // lightgray 대신 transparent 사용해서 투명처리
                background: `url(${frog1}) transparent -3.913px -3.002px / 206.475% 210.332% no-repeat`,
                // backgroundSize: 'contain',  // 비율 유지 + 전체 보이기
                // backgroundPosition: 'center', // 가운데 정렬
              }}>
              </div>

              <p style={{
                position: 'absolute',
                top: '268px',
                left: '68px',
                bottom: '154px',
                right: '68px',
                display: 'flex',
                width: '234px',
                height: '24px',
                flexDirection: 'column',
                justifyContent: 'center',
                color: '#9F9EB0',
                textAlign: 'center',
                fontFamily: 'Pretendard',
                fontSize: '14px',
                fontStyle: 'normal',
                fontWeight: '500',
                lineHeight: '24px'  /* 171.429% */
              }}>
                지금 나가도 다음에 다시 이어 할 수 있어요
              </p>

              <button 
              onClick={() => setExitModal(false)}
              style={{
                position: 'absolute',
                top: '316px',
                left: '16px',
                bottom: '81px',
                right: '16px',
                width: '338px',
                height: '49px',
                borderRadius: '14.133px',
              background: '#9B87E8',
              boxShadow: '0 4px 4px 0 rgba(206, 210, 223, 0.16)',
              boxSizing: 'border-box'
            }}>
              <p style={{
                position: 'absolute',
                top: '16px',
                left: '129px',
                bottom: '16px',
                right: '129px',
                width: '80px',
                color: '#FFF',
                fontFamily: 'Pretendard',
                fontSize: '14.133px',
                fontStyle: 'normal',
                fontWeight: '700',
                lineHeight: 'normal',
                letterSpacing: '-0.028px'
              }}>
                퀴즈 계속하기
              </p>
            </button>

            {/* 디폴트 페이지로 이동 */}
            <button
            onClick={handleExit}
            style={{
              position: 'absolute',
              top: '377px',
              left: '16px',
              bottom: '20px',
              right: '16px',
              width: '338px',
              height: '49px',
              borderRadius: '14.133px',
              border: '0.883px solid #E7E6EB',
              boxShadow: '0 3.533px 3.533px 0 rgba(206, 210, 223, 0.16)',
            }}>

              <p style={{
                position: 'absolute',
                top: '16px',
                left: '96px',
                bottom: '16.34px',
                right: '96px',
                display: 'flex',
                width: '146px',
                height: '16.66px',
                flexDirection: 'column',
                justifyContent: 'center',
                color: '#ED5050',
                textAlign: 'center',
                fontFamily: 'Pretendard',
                fontSize: '14.133px',
                fontStyle: 'normal',
                fontWeight: '700',
                lineHeight: 'normal',
                letterSpacing: '-0.028px'
              }}>
                여기까지 저장하고 나가기
              </p>
            </button>
          </div>
        </div>
      )}
    </div>
  );  // return 끝
}  // 함수 끝
export default QuizPage;