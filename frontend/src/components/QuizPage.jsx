/* global chrome */

import nlp from 'compromise';
import React, { useState, useEffect } from 'react';
import frog from '../imgs/image_710.png';
import frog1 from '../imgs/image_712.png';
import { apiFetch } from '../utils/api';
import bulb from '../imgs/image_62.png';
import frog2 from '../imgs/image_750.png';
import { log, IS_DEV } from '../utils/logger';

// 렌더링해도 1번만 셔플 (비교값을 -0.5 ~ 0.5으로 설정)
const shuffle = (arr) => [...arr].sort(() => Math.random() - 0.5);

function QuizPage({ videoId, videoTitle, duration, onExitPage, onSettlementPage }) {

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

  // 이미 전송한 quizId 저장
  const [submittedQuizIds, setSubmittedQuizIds] = useState([]);


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


  // 현재 구간 반복 중인지 여부
  const [isLooping, setIsLooping] = useState(false);

  // 수집한 단어 목록 상태 저장
  const [collectedWords, setCollectedWords] = useState([]);


  // 팝업이 고정되어 있는지 여부
  const [isPinned, setIsPinned] = useState(false);
  // 현재 팝업에 표시할 단어 정보
  const [dictionaryData, setDictionaryData] = useState(null);
  // 팝업 위치 (클릭한 단어 위에 표시)
  const [popupPosition, setPopupPosition] = useState({ top: 0, left: 0 });
  // 단어 클릭 시점의 자막 정보 (단어 수집용)
  const [clickedSubtitle, setClickedSubtitle] = useState(null);
  // 단어 수집 버튼 누름 여부
  const [isCollected, setIsCollected] = useState(false);

  // 다음 목표 뱃지 저장
  const [nextBadge, setNextBadge] = useState(null);

  // 매칭 퀴즈 선택한 영단어
  const [matchingSelectedWord, setMatchingSelectedWord] = useState(null);
  // 매칭 퀴즈 선택한 뜻
  const [matchingSelectedMeaning, setMatchingSelectedMeaning] = useState(null);
  // 매칭 퀴즈 맞춘 쌍들
  const [matchingMatchedPairs, setMatchingMatchedPairs] = useState([]);
  // 매칭 퀴즈 틀린 쌍 (1초 표시용)
  const [matchingWrongPair, setMatchingWrongPair] = useState(null);

  // 퀴즈 셔플용
  const [matchingShuffledMeanings, setMatchingShuffledMeanings] = useState([]);



  // content.jsx에서 자막 수신
  useEffect(() => {
    const listener = (message) => {

      // 전체 자막 수신
      if (message.type === 'SUBTITLES_LOADED') {
        const sorted = message.subtitles.sort((a, b) => a.startTime - b.startTime);
        setSubtitles(sorted);
        setSubtitleIndex(0);
        setCurrentSubtitle(sorted[0]);
      }

      // 새 자막 도착
      if (message.type === 'SUBTITLE_UPDATE') {
        const newSubtitle = {
          text: message.text,
          startTime: message.startTime,
          endTime: message.endTime || null,
          translation: message.translation,
          // 내가 본 자막 위치 찾기용 고유 id 부여
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



  // 수집한 단어 목록 조회
  useEffect(() => {
    const fetchCollectedWords = async () => {
      try {
        const response = await apiFetch('/words/my-collection?page=0&size=100', {
          method: 'GET'
        });
        if (response?.data?.words) {
          setCollectedWords(response.data.words);
        }
      } catch (error) {log.debug('단어 목록 조회 에러', error)}
    };
    fetchCollectedWords();
  }, []);



  // 이전 자막 스크립트 이동
  const goPrev = () => {
    console.log('현재 subtitleIndex:', subtitleIndex);
    console.log('현재 currentSubtitle:', currentSubtitle);
    console.log('subtitles 길이:', subtitles.length);
    if (subtitleIndex > 0) {
      const newIndex = subtitleIndex - 1;
      setSubtitleIndex(newIndex);
      setCurrentSubtitle(subtitles[newIndex]);
    }
  };



  // 다음 자막 스크립트 이동
  const goNext = () => {
    console.log('현재 subtitleIndex', subtitleIndex);
    console.log('현재 currentSubtitle', currentSubtitle);
    console.log('subtitles 길이', subtitles.length);
    if (subtitleIndex < subtitles.length - 1) {
      const newIndex = subtitleIndex + 1;
      setSubtitleIndex(newIndex);
      setCurrentSubtitle(subtitles[newIndex]);
    }
  };



  // 구간 반복 재생 (startTime ~ endTime)
  const toggleLoop = () => {
    // chrome.tabs.query({ 조건 }, (tabs) => { 로 조건에 맞는 탭 배열 찾기
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



  // 단서 사전 번역 API 호출
  async function fetchTranslation(word) {
    try {
      const response = await apiFetch('/translate/subtitles', {
        method: 'POST',
        body: JSON.stringify({
          videoId: videoId,
          title: videoTitle,
          duration: duration,
          subtitleRequests: [
            {
              text: word,
              startTime: currentSubtitle.startTime,
              endTime: currentSubtitle.endTime,
            }
          ]
        })
      });
      return response?.data?.translatedTexts?.[0] || '';
    } catch (error) {
      log.debug('번역 실패', error);
      return '';
    }
  }



  // 단어 클릭 시 사전 팝업 열기
  const togglePin = async (e, word, wordFilter) => {
    // 다른 클릭 이벤트 막기
    e.stopPropagation();

    // 이미 열려있으면 닫기
    if (isPinned) {
      setIsPinned(false);
      setDictionaryData(null);
      setClickedSubtitle(null);

      // 나중에 조회해서 이미 수집했는지 확인 후 대응하기 (오류)
      setIsCollected(false);
      // return 없으면 다른 단어 클릭 시 즉시 이동
    }

    // 클릭 시점의 자막 정보 저장 (단어 수집용)
    // set은 다음 렌더링에 반영되기에 즉시 사용해야 하는 api는 savedSubtitle로 사용
    const savedSubtitle = { ...currentSubtitle };
    setClickedSubtitle({ ...savedSubtitle });

    // 클릭한 단어 위치 계산
    const rect = e.target.getBoundingClientRect();
    // 팝업 너비
    const popupWidth = 270;

    // 단어 중앙 위치
    const wordCenter = rect.left + (rect.width / 2);

    // 팝업 left 계산
    let popupLeft = wordCenter - popupWidth / 2;

    // 화면 왼쪽 밖으로 나가면 조정
    if (popupLeft < 10) {
      popupLeft = 10;
    }

    // 화면 오른쪽 밖으로 나가면 조정
    if (popupLeft + popupWidth > window.innerWidth - 10) {
      popupLeft = window.innerWidth - popupWidth - 10;
    }

    // 화살표 위치 = 단어 중앙 - 팝업 left (조정 후 기준)
    const arrowLeft = rect.left + (rect.width / 2) - popupLeft;

    // let left = rect.left + (rect.width / 2);

    // // 화면 왼쪽 밖으로 나가면 조정
    // if (left - popupWidth / 2 < 10) {
    //   left = popupWidth / 2 + 10;
    // }
  
    // // 화면 오른쪽 밖으로 나가면 조정
    // if (left + popupWidth / 2 > window.innerWidth - 10) {
    //   left = window.innerWidth - popupWidth / 2 - 10;
    // }

    setPopupPosition({
      // 단어 위쪽에 표시
      top: rect.top - 215,
      // 중앙 정렬
      left: popupLeft,
      arrowLeft: arrowLeft,
    });

    // 서버에서 조회한 단어에서 찾기
    const collected = collectedWords.find(w => w.word === word);
    if (collected) {
      setDictionaryData({
        word: collected.word,
        meaningTranslation: collected.translation,
      });
      setIsPinned(true);
      return;
    }

    // 사전 API 호출
    try {
      const response = await fetch(`https://api.dictionaryapi.dev/api/v2/entries/en/${word}`);
      
      // 에러날 경우 기존 팝업 유지
      if (!response.ok) {
        return;
      }

      const data = await response.json();

      // API 응답이 있고, 데이터가 있으면 실행
      if (data && data[0]) {
        // 자막과 일치하는 품사에 대한 정보만 가져오기 (구동사 혹은 일치하는게 없으면 해당 단어의 첫 번째 품사에 대한 정보만 가져오기)
        const meaning = data[0].meanings.find(m => m.partOfSpeech === wordFilter) || data[0].meanings[0];

        // 품사 없으면 팝업 닫고 종료
        if (!meaning?.partOfSpeech) {
          setIsPinned(false);
          setDictionaryData(null);
          setClickedSubtitle(null);
          return;
        }

        // 해당 품사의 첫 번째 뜻 목록 가져오기
        const def = meaning.definitions[0];
        const result = {
          word: word,
          // 발음 기호(품사는 있어도 발음이 없는 경우가 있음)
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
          // 단어 번역
          meaningTranslation: '',
        };

        // 뜻 번역
        result.meaningTranslation = await fetchTranslation(result.definition) || '';

        setDictionaryData(result);
        setIsPinned(true);

        // 팝업 열 때 서버에 타입 POPUP으로 보내기
        await apiFetch('/words/collect', {
          method: 'POST',
          body: JSON.stringify({
            wordType: 'POPUP',
            videoId: videoId,
            word: word,
            meaning: result.meaningTranslation,
            sentence: savedSubtitle.text,
            timestamp: formatTime(savedSubtitle.startTime),
            // set은 다음 렌더링에 반영되기에 즉시 사용해야 하는 api는 dictionaryData.translation 대신 savedSubtitle.translation 사용
            // 또한 dictionaryData가 null이면 에러나기에 result로 ''처리
            translation: savedSubtitle.translation,
            title: videoTitle
          })
        });

        // 서버에 저장된 단어 추가
        setCollectedWords(prev => [...prev, {
          word: word,
          translation: result.meaningTranslation
        }]);
      }
    } catch (error) {
      log.debug('사전 조회 실패:', error);
      setIsPinned(false);
      setDictionaryData(null);
      setClickedSubtitle(null);
      return;
    }
  };



  // 사전 팝업 닫기
  const closePopup = () => {
    setIsPinned(false);
    setDictionaryData(null);
    setClickedSubtitle(null);

    // 나중에 조회해서 이미 수집했는지 확인 후 대응하기 (오류)
    setIsCollected(false);
  };



  // 시간 형식 변환 (초 → 00:00:00)
  const formatTime = (seconds) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);
    return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };



  // 단어 수집 버튼
  const collectWord = async () => {
    try {
      await apiFetch('/words/collect', {
        method: 'POST',
        body: JSON.stringify({
          wordType: 'COLLECT',
          videoId: videoId,
          word: dictionaryData.word,
          meaning: dictionaryData.meaningTranslation,
          // 단어가 포함된 문장
          sentence: clickedSubtitle.text,
          // 영상 시간
          timestamp: formatTime(currentSubtitle.startTime),
          // 단어가 포함된 문장 뜻
          translation: clickedSubtitle.translation,
          // 영상 제목
          title: videoTitle
        })
      });

      // 서버에 저장된 단어 추가
      setCollectedWords(prev => [...prev, {
        word: dictionaryData.word,
        translation: clickedSubtitle.translation,
      }]);
      setIsCollected(true);
    } catch (error) {
      log.debug('단어 수집 실패:', error);
    }
  };



  // 해당하는 품사를 한글로 변환
  const partOfSpeechKo = {
    verb: '동사',
    noun: '명사',
    adjective: '형용사',
    adverb: '부사'
  };



  // 단어의 품사 구분 및 일부 단어 필터링
  const getWordFilter = (sentence, word) => {
    // 원본 자막 넣기
    const doc = nlp(sentence);
    // 해당 단어를 자막에서 찾고 관련 태그들을 부여
    const match = doc.match(word);

    // 구동사 체크
    const phrasalVerb = doc.match('#PhrasalVerb').text();
    if (phrasalVerb && phrasalVerb.includes(word)) {
      // 구동사 전체 반환
      return phrasalVerb;
    }

    // 제외 필터링 먼저 체크
    // 이름 제외
    if (match.has('#Person')) return false;
    // 장소 제외
    if (match.has('#Place')) return false;
    // 회사명 제외
    if (match.has('#Organization')) return false;
    // 고유 명사 제외
    // if (match.has('#ProperNoun')) return false;
    // 대명사 제외
    if (match.has('#Pronoun')) return false;
    // 전치사 제외
    if (match.has('#Preposition')) return false;
    // 접속사 제외
    if (match.has('#Conjunction')) return false;
    // 관사 제외
    if (match.has('#Determiner')) return false;
    // 의문사 제외
    if (match.has('#QuestionWord')) return false;
    // 감탄사 제외
    if (match.has('#Interjection')) return false;

    // 가장 처음 품사 가져오기
    const tags = match.json()[0]?.terms?.[0]?.tags;
      if (tags && tags.length > 0) {
        // Set → Array
        const firstTag = [...tags][0];
        // 'Verb' → 'verb'
        return firstTag.toLowerCase();
      }

    // 품사를 모르면 null
    return null;
  };



  // 빈칸 퀴즈 보기 버튼 위치
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
  const isCorrect = feedback?.correct;



  // ox퀴즈용 특정 단어 강조하기
  const highlightWord = (text, word) => {
    // 퀴즈 문장 또는 단어 없으면 빈 화면 또는 퀴즈 문장 보이기
    if (!text || !word) return text;
  
    // 퀴즈 문장에서 강조 단어 위치 찾기
    const index = text.toLowerCase().indexOf(word.toLowerCase());
    // 단어가 문장에 없으면 퀴즈 문장 보이기
    if (index === -1) return text;
  
    // 위치별로 분리
    const before = text.slice(0, index);
    const highlight = text.slice(index, index + word.length);
    const after = text.slice(index + word.length);

    return (
      <div style={{
        color: '#01030D',
        fontFamily: 'Pretendard',
        fontSize: '16px',
        fontStyle: 'normal',
        fontWeight: '700',
        lineHeight: 'normal',
      }}>
        {before}
        <span style={{
          color: '#667EEA',
          fontFamily: 'Pretendard',
          fontSize: '16px',
          fontStyle: 'normal',
          fontWeight: '700',
          lineHeight: 'normal',
        }}>
          {highlight}
        </span>
        {after}
      </div>
    );
  };



  // 빈칸 퀴즈 타입별 나타나는 박스
  const blankQuiz = () => {
    if (!currentQuiz?.options?.length) return null;
    return(
      // {/* 전체 퀴즈 박스 */}
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
              빈칸 채우기
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

                {/* 빈칸 채우기 문제 질문 */}
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
                  {currentQuiz?.content.replace('[ ]', tempChoice || '[ ]')}
                </p>
              </div>
            </div>

            {/* 빈칸 채우기 문제 번역 */}
            <p style={{
              alignSelf: 'stretch',
              color: '#9198A3',
              fontFamily: 'Pretendard',
              fontSize: '16px',
              fontStyle: 'normal',
              fontWeight: '700',
              lineHeight: 'normal'
            }}>
              {currentQuiz?.translation}
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
              background: backgroundColor(choice),
              boxShadow: '0 4px 4px 0 rgba(206, 210, 223, 0.16)',
              color: textColor(choice),
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
    );
  }



  // ox 퀴즈 타입별 나타나는 박스
  const oxQuiz = () => {
    if (!currentQuiz) return null;
    return(
      // {/* 전체 퀴즈 박스 */}
      <div style={{
        display: 'flex',
        width: '370px',
        padding: '24px 16px 20px 16px',
        flexDirection: 'column',
        justifyContent: 'flex-end',
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
              OX 퀴즈
            </p>
          </div>

          {/* ox 문제 질문, 뜻 들어가는 박스 */}
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'flex-start',
            gap: '8px',
            alignSelf: 'stretch'
          }}>

            {/* ox 문제 질문 전용 박스 */}
            <div style={{
              display: 'flex',
              width: '191px',
              flexDirection: 'column',
              alignItems: 'flex-start',
              gap: '4px'
            }}>

              {/* ox 문제 질문 박스 */}
              <div style={{
                display: 'flex',
                alignItems: 'flex-end',
                gap: '4px',
                alignSelf: 'stretch'
              }}>

                {/* ox 문제 질문 */}
                <p>
                  {highlightWord(currentQuiz?.content, currentQuiz?.question?.match(/[a-zA-Z]+/)?.[0])}
                </p>
              </div>
            </div>

            {/* ox 문제 질문 뜻 */}
            <p style={{
              alignSelf: 'stretch',
              color: '#9198A3',
              fontFamily: 'Pretendard',
              fontSize: '16px',
              fontStyle: 'normal',
              fontWeight: '700',
              lineHeight: 'normal'
            }}>
              {currentQuiz?.translation}
            </p>
          </div>
        </div>

        {/* 보기 버튼 전체 박스 */}
        <div style={{
          display: 'inline-grid',
          height: '60px',
          columnGap: '8px',
          gridTemplateRows: 'repeat(1,fit-content(100%))',
          gridTemplateColumns: 'repeat(2,fit-content(100%))',
        }}>

          {/* O 버튼 */}
          <button
          disabled={isConfirmed}
          onClick={() => handleChoice('O')}
          style={{
            display: 'flex',
            width: '165px',
            height: '60px',
            padding: '11px 63px 12px 63px',
            justifyContent: 'center',
            alignItems: 'center',
            gap: '10px',
            gridRow: '1 / span 1',
            gridColumn: '1 / span 1',
            borderRadius: '14px',
            border: oxBorderColor('O'),
            background: oxBackgroundColor('O'),
            boxShadow: tempChoice === 'O' ? '0 4px 4px 0 rgba(206, 210, 223, 0.16)' : 'none',
          }}>
            O
          </button>

          {/* X 버튼 */}
          <button
          disabled={isConfirmed}
          onClick={() => handleChoice('X')}
          style={{
            display: 'flex',
            width: '165px',
            height: '60px',
            padding: '11px 63px 12px 63px',
            justifyContent: 'center',
            alignItems: 'center',
            gap: '10px',
            gridRow: '1 / span 1',
            gridColumn: '2 / span 1',
            borderRadius: '14px',
            border: oxBorderColor('X'),
            background: oxBackgroundColor('X'),
            boxShadow: tempChoice === 'X' ? '0 4px 4px 0 rgba(206, 210, 223, 0.16)' : 'none',
          }}>
            X
          </button>
        </div>
      </div>
    );
  }



  // 매칭 퀴즈 타입별 나타나는 박스
  const matchingQuiz = () => {
    if (!quizzes?.length || !matchingShuffledMeanings?.length) return null;
    return(
      // {/* 전체 퀴즈 박스 */}
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

          {/* 단어와 뜻 매칭하기 박스 */}
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
              단어와 뜻 매칭하기
            </p>
          </div>

          {/* 단어와 뜻 들어가는 박스 */}
          <div style={{
            display: 'inline-grid',
            rowGap: '8px',
            columnGap: '8px',
            alignSelf: 'stretch',
            gridTemplateRows: 'repeat(3,fit-content(100%))',
            gridTemplateColumns: 'repeat(2,minmax(0,1fr))',
          }}>

            {/* 매칭 퀴즈 보기 버튼 */}
            {quizzes?.map((quiz, i) => (
              <React.Fragment key={i}>
                {/* 영어 단어 */}
                <button
                  disabled={matchingMatchedPairs.some(p => p.word === quiz.question)}
                  onClick={() => matchingHandleWordClick(quiz.question)}
                  style={{
                    display: 'flex',
                    width: '165px',
                    height: '40px',
                    justifyContent: 'center',
                    alignItems: 'center',
                    borderRadius: '10px',
                    ...matchingGetWordStyle(quiz.question),
                }}>
                  {quiz.question}
                </button>

                {/* 단어 뜻 */}
                <button
                  disabled={matchingMatchedPairs.some(p => p.meaning === matchingShuffledMeanings[i])}
                  onClick={() => matchingHandleMeaningClick(matchingShuffledMeanings[i])}
                  style={{
                    display: 'flex',
                    width: '165px',
                    height: '40px',
                    justifyContent: 'center',
                    alignItems: 'center',
                    borderRadius: '10px',
                    ...matchingGetMeaningStyle(matchingShuffledMeanings[i]),
                }}>
                  {matchingShuffledMeanings[i]}
                </button>
              </React.Fragment>
            ))}
          </div>
        </div>
      </div>
    );
  }



  // 정답 오답 선택 미선택에 따른 border 색상 변화 (빈칸 퀴즈)
  const borderColor = (choice) => {
    // feedback에서 정답 가져오기
    const correctAnswer = feedback?.correctAnswer;
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '1px solid #9B87E8' : 'none';
    }

    // 확정 후: 정답이면
    if (choice === correctAnswer) {
      return '1px solid #C1C0FC';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !== correctAnswer) {
      return '1px solid #FFB484';
    }

    // 확정 후: 미선택이면
    return 'none';
  };



  // 정답 오답 선택 미선택에 따른 background 색상 변화 (빈칸 퀴즈)
  const backgroundColor = (choice) => {
    // feedback에서 정답 가져오기
    const correctAnswer = feedback?.correctAnswer;
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '#F0F3FF' : '#F7F7F7';
    }

    // 확정 후: 정답이면
    if (choice === correctAnswer) {
      return '#F0F3FF';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !== correctAnswer) {
      return '#FFF4EE';
    }

    // 확정 후: 미선택이면
    return '#F7F7F7';
  };



  // 정답 오답 선택 미선택에 따른 color 색상 변화 (빈칸 퀴즈)
  const textColor = (choice) => {
    // feedback에서 정답 가져오기
    const correctAnswer = feedback?.correctAnswer;
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '#5559D9' : '#4D525C';
    }

    // 확정 후: 정답이면
    if (choice === correctAnswer) {
      return '#5559D9';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !== correctAnswer) {
      return '#F7731E';
    }

    // 확정 후: 미선택이면
    return '#4D525C';
  };



  // 정답 오답 선택 미선택에 따른 border 색상 변화 (ox 퀴즈)
  const oxBorderColor = (choice) => {
    // feedback에서 정답 가져오기
    const correctAnswer = feedback?.correctAnswer;
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '1px solid #9B87E8' : 'none';
    }

    // 확정 후: 정답이면
    if (choice ===  correctAnswer) {
      return '1px solid #C1C0FC';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !==  correctAnswer) {
      return '1px solid #F7731E';
    }

    // 확정 후: 미선택이면
    return 'none';
  };



  // 정답 오답 선택 미선택에 따른 background 색상 변화 (ox 퀴즈)
  const oxBackgroundColor = (choice) => {
    // feedback에서 정답 가져오기
    const correctAnswer = feedback?.correctAnswer;
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '#FFF' : '#F7F7F7';
    }

    // 확정 후: 정답이면
    if (choice ===  correctAnswer) {
      return '#F0F3FF';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !==  correctAnswer) {
      return '#FFF4EE';
    }

    // 확정 후: 미선택이면
    return '#F7F7F7';
  };



  // 매칭 퀴즈 영단어 클릭
  const matchingHandleWordClick = (word) => {
    // 이미 맞춘 단어면 무시
    if (matchingMatchedPairs.some(pair => pair.word === word)) return;

    // 다른 단어 클릭하면 선택 변경
    setMatchingSelectedWord(word);

    // 뜻이 이미 선택되어 있으면 매칭 시도
    if (matchingSelectedMeaning) {
      checkMatch(word, matchingSelectedMeaning);
    }
  };



  // 매칭 퀴즈 뜻 클릭
  const matchingHandleMeaningClick = (meaning) => {
    // 이미 맞춘 뜻이면 무시
    if (matchingMatchedPairs.some(pair => pair.meaning === meaning)) return;

    // 다른 뜻 클릭하면 선택 변경
    setMatchingSelectedMeaning(meaning);

    // 단어가 이미 선택되어 있으면 매칭 시도
    if (matchingSelectedWord) {
      checkMatch(matchingSelectedWord, meaning);
    }
  };



  // 매칭 퀴즈 매칭확인
  const checkMatch = async (word, meaning) => {
    // 정답 찾기
    const quiz = quizzes.find(q => q.question === word);
    const isMatchingCorrect = quiz?.answer === meaning;

    // 이미 전송한 quizId면 서버 전송 스킵
    const alreadySubmitted = submittedQuizIds.includes(quiz?.quizId);

    // 정답일 경우
    if (isMatchingCorrect) {
      // 전송하지 않은 quizId인 경우 전송
      if (!alreadySubmitted) {
        try {
          await apiFetch('/quiz/submit', {
            method: 'POST',
            body: JSON.stringify({
              sessionId,
              quizId: quiz?.quizId,
              userAnswer: meaning
            })
          });
          setSubmittedQuizIds(prev => [...prev, quiz?.quizId]);
        } catch (error) {
          log.debug('매칭 정답 전송 실패:', error);
        }
      }

    // 화면 업데이트
    setMatchingMatchedPairs(prev => [...prev, { word, meaning }]);
    setMatchingSelectedWord(null);
    setMatchingSelectedMeaning(null);
    } else {
      // 오답일 경우
      // 전송하지 않은 quizId인 경우 전송
      if (!alreadySubmitted) {
        try {
          await apiFetch('/quiz/submit', {
            method: 'POST',
            body: JSON.stringify({
              sessionId,
              quizId: quiz?.quizId,
              userAnswer: meaning
            })
          });
          setSubmittedQuizIds(prev => [...prev, quiz?.quizId]);
        } catch (error) {
          log.debug('매칭 오답 전송 실패:', error);
        }
      }
      setMatchingWrongPair({ word, meaning });

      // 1초 후 초기화
      setTimeout(() => {
        setMatchingWrongPair(null);
        setMatchingSelectedWord(null);
        setMatchingSelectedMeaning(null);
      }, 1000);
    }
  };



  // 단어 버튼의 정답 오답 선택 미선택에 따른 background, border, box-shadow 색상 변화 (매칭)
  const matchingGetWordStyle = (word) => {
    // 맞춘 단어 - 파란색 유지
    if (matchingMatchedPairs.some(pair => pair.word === word)) {
      return { background: '#F0F3FF', border: '1px solid #C1C0FC', boxShadow: '0 4px 4px 0 rgba(206, 210, 223, 0.16)' };
    }
    // 틀린 단어 - 주황색
    if (matchingWrongPair?.word === word) {
      return { background: '#FFF4EE', border: '1px solid #FFB484', boxShadow: '0 4px 4px 0 rgba(206, 210, 223, 0.16)'};
    }
    // 선택된 단어 - 파란색
    if (matchingSelectedWord === word) {
      return { background: '#FFF', border: '1px solid #9B87E8', boxShadow: '0 4px 4px 0 rgba(206, 210, 223, 0.16)' };
    }
    // 미선택
    return { background: '#F7F7F7', border: 'none', boxShadow: 'none' };
  };



  // 뜻 버튼의 정답 오답 선택 미선택에 따른 background, border, box-shadow 색상 변화 (매칭)
  const matchingGetMeaningStyle = (meaning) => {
    // 맞춘 뜻 - 파란색 유지
    if (matchingMatchedPairs.some(pair => pair.meaning === meaning)) {
      return { background: '#F0F3FF', border: '1px solid #C1C0FC', boxShadow: '0 4px 4px 0 rgba(206, 210, 223, 0.16)'};
    }
    // 틀린 뜻 - 주황색
    if (matchingWrongPair?.meaning === meaning) {
      return { background: '#FFF4EE', border: '1px solid #FFB484', boxShadow: '0 4px 4px 0 rgba(206, 210, 223, 0.16)'};
    }
    // 선택된 뜻 - 파란색
    if (matchingSelectedMeaning === meaning) {
      return { background: '#FFF', border: '1px solid #9B87E8', boxShadow: '0 4px 4px 0 rgba(206, 210, 223, 0.16)'};
    }
    // 미선택
    return { background: '#F7F7F7', border: 'none', boxShadow: 'none' };
  };



  // 매칭 퀴즈 결과 보기 버튼
  const handleMatchingComplete = async () => {
    // 모든 매칭 완료했는지 확인
    if (matchingMatchedPairs.length === quizzes.length) {
      try {
        const finalResult = await apiFetch(`/quiz/sessions/${sessionId}/complete`, {
          method: 'POST'
        });
        // 저장된 진행 상황 삭제
        chrome.storage.local.remove('quizState');
        // 정산 페이지로 이동
        onSettlementPage(finalResult.data);
      } catch (error) {
        log.debug('handleMatchingComplete 세션 종료 실패', error);
      }
    }
  };



  // 퀴즈 초기화 및 이전 진행 상황 복원
  useEffect(() => {
    // 크롬 저장소에서 이전 진행 상황 확인
    chrome.storage.local.get('quizState', async (result) => {
      // 같은 영상의 저장된 퀴즈가 있으면 이어하기
      if (result.quizState?.videoId === videoId) {
        log.debug('사이드패널 퀴즈 복원', result, Date.now());
        setQuizzes(result.quizState.quizzes);
        setMatchingShuffledMeanings(result.quizState.matchingShuffledMeanings ?? []);
        setSessionId(result.quizState.sessionId);
        setCurrentIndex(result.quizState.currentIndex);
        setCorrectCount(result.quizState.correctCount);
        setWrongCount(result.quizState.wrongCount);
        setAnswers(result.quizState.answers);
        setIsConfirmed(result.quizState.isConfirmed ?? false);
        setTempChoice(result.quizState.tempChoice ?? null);

        setMatchingSelectedWord(result.quizState.matchingSelectedWord ?? null);
        setMatchingSelectedMeaning(result.quizState.matchingSelectedMeaning ?? null);
        setMatchingMatchedPairs(result.quizState.matchingMatchedPairs ?? []);
        setMatchingWrongPair(null);
        setSubmittedQuizIds(result.quizState.submittedQuizIds ?? []);
        // 영상 일시정지 메시지 전송
        chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
          if (tabs[0]) chrome.tabs.sendMessage(tabs[0].id, { type: 'SET_QUIZ_MODE', active: true }).catch((error) => {log.debug('이거 에러22:', error)});
        });
      }

      // 복원 완료 체크
      setIsRestoring(false);
    });

    // content.jsx에서 새 퀴즈 받기
    const listener = (message) => {
      if (message.type === 'QUIZ_READY') {
        log.debug('사이드패널 퀴즈 받음', message, Date.now());
        // 새 퀴즈로 초기화
        setSessionId(message.sessionId);
        setQuizzes(message.quizzes);
        if (message.quizzes[0]?.quizType === 'MATCHING') {
          setMatchingShuffledMeanings(shuffle(message.quizzes.map(q => q.answer)));
        } else {
          setMatchingShuffledMeanings([]);
        }
        setCurrentIndex(0);
        setCorrectCount(0);
        setWrongCount(0);
        setAnswers([]);
        setIsConfirmed(false);
        setTempChoice(null);
        setMatchingSelectedWord(null);
        setMatchingSelectedMeaning(null);
        setMatchingMatchedPairs([]);
        setMatchingWrongPair(null);
        setSubmittedQuizIds([]);

      // 영상 일시정지 메시지 전송
      chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
        if (tabs[0]) chrome.tabs.sendMessage(tabs[0].id, { type: 'SET_QUIZ_MODE', active: true }).catch((error) => {log.debug('이거 에러33:', error)});
      });
    }
  };
  chrome.runtime.onMessage.addListener(listener);

  // cleanup
  return () => chrome.runtime.onMessage.removeListener(listener);
  }, [videoId]);



  // 다음 목표 뱃지 조회하기
  useEffect(() => {
    const fetchBadge = async () => {
      try {
        const badge = await apiFetch(`/badges/video/${videoId}`, {
          method: 'GET'
        });
        setNextBadge(badge?.data?.nextBadge);
      } catch (error) {
        log.debug('목표 뱃지 조회 에러', error);
      }}
    fetchBadge();
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
          tempChoice,
          matchingSelectedWord,
          matchingSelectedMeaning,
          matchingMatchedPairs,
          matchingShuffledMeanings,
          submittedQuizIds,
        }
      });
    }
  }, [currentIndex, correctCount, wrongCount, answers, isConfirmed, sessionId, tempChoice, videoId, isRestoring, quizzes, matchingSelectedWord, matchingSelectedMeaning, matchingMatchedPairs, matchingShuffledMeanings, submittedQuizIds]);



  // 답안 제출
  const handleSubmit = async () => {
    // 선택 안 했거나 이미 제출했으면 무시
    if (!tempChoice || isConfirmed) return;
    try {
      // 서버에 정답 제출
      const result = await apiFetch('/quiz/submit', {
        method: 'POST',
        body: JSON.stringify({
          sessionId,
          quizId: currentQuiz?.quizId,
          userAnswer: tempChoice
        })
      });
      setFeedback(result.data);
      setIsConfirmed(true);

      // 정답/오답 카운트
      if (result.data?.correct) {
        setCorrectCount(prev => prev + 1);
      } else {
        setWrongCount(prev => prev + 1);
      }

      // 문제별 기록 저장
      setAnswers(prev => [...prev, {
        quizId: currentQuiz?.quizId,
        userAnswer: tempChoice,
        isCorrect: result.data?.correct
      }]);
    } catch (error) {
      log.debug('제출 실패', error);
    }
  };



  // 다음 문제 또는 정산 완료
  const handleNext = async () => {
    // 매칭 퀴즈면 바로 정산 페이지
    if (currentQuiz?.quizType === 'MATCHING') {
      try {
        const finalResult = await apiFetch(`/quiz/sessions/${sessionId}/complete`, {
          method: 'POST'
        });
        chrome.storage.local.remove('quizState');
        onSettlementPage(finalResult.data);
      } catch (error) {
        log.debug('MATCHING handleNext 세션 종료 실패', error);
      }
      return;
    }

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
        // 저장된 진행 상황 삭제
        chrome.storage.local.remove('quizState');

        // 영상 재생 신호
        chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
          if (tabs[0]) {
            chrome.tabs.sendMessage(tabs[0].id, { type: 'RESUME_VIDEO' });
          }
        });

        // 퀴즈 상태만 초기화
        setCurrentIndex(0);
        setQuizzes([]);
        setTempChoice(null);
        setIsConfirmed(false);
        setFeedback(null);
        setDictionaryData(null);
        setSubmittedQuizIds([]);
      } catch (error) {
        log.debug('빈칸,ox handleNext 세션 종료 실패', error);
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

                        {/* 이전 페이지로 돌아가기 */}
                        <button
                        onClick={() => {
                          if (currentQuiz) {
                            // 퀴즈 중이면 확인 팝업
                            setExitModal(true);
                          } else {
                            // 퀴즈 없으면 바로 디폴트로 (나중에 기능 추가하기)
                            onExitPage(); 
                          }
                        }}
                        style={{
                        display: 'flex',
                        width: '24px',
                        height: '24px',
                        alignItems: 'center',
                        justifyContent: 'center',
                        background: 'transparent',
                        border: 'none', 
                        }}>
                            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                              <path d="M7.84 13.75L9.17 12.26L6.64 10.01H15.01C17.22 10.01 19.01 11.8 19.01 14.01C19.01 16.22 17.22 18.01 15.01 18.01H12.01V20.01H15.01C18.32 20.01 21.01 17.32 21.01 14.01C21.01 10.7 18.32 8.01 15.01 8.01H6.63L9.16 5.76L7.83 4.27L2.49 9.02L7.83 13.77L7.84 13.75Z" fill="black"/>
                            </svg>
                        </button>

                        {/* 디폴트 페이지로 이동 */}
                        <button
                        onClick={() => {
                          if (currentQuiz) {
                            // 퀴즈 중이면 확인 팝업
                            setExitModal(true);
                          } else {
                            // 퀴즈 없으면 바로 디폴트로
                            onExitPage(); 
                          }
                        }}
                        style={{
                          display: 'flex',
                          width: '24px',
                          height: '24px',
                          alignItems: 'center',
                          justifyContent: 'center',
                        background: 'transparent',
                        border: 'none',
                        }}>
                          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                            <path d="M12.71 2.29C12.6175 2.1973 12.5076 2.12375 12.3866 2.07357C12.2657 2.02339 12.136 1.99756 12.005 1.99756C11.874 1.99756 11.7444 2.02339 11.6234 2.07357C11.5024 2.12375 11.3925 2.1973 11.3 2.29L3.29 10.29C3.19732 10.3834 3.12399 10.4943 3.07423 10.6161C3.02447 10.7379 2.99924 10.8684 3 11V20C3 21.1 3.9 22 5 22H9C9.55 22 10 21.55 10 21V15H14V21C14 21.55 14.45 22 15 22H19C20.1 22 21 21.1 21 20V11C21 10.73 20.89 10.48 20.71 10.29L12.71 2.29ZM16 20V15C16 13.9 15.1 13 14 13H10C8.9 13 8 13.9 8 15V20H5V11.41L12 4.41L19 11.41V20H16Z" fill="black"/>
                          </svg>
                        </button>
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
                  {currentQuiz && (
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
                          {currentQuiz?.quizType === 'MATCHING' ? matchingMatchedPairs.length : currentIndex + 1}
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
                          /{quizzes?.length}
                        </p>
                      </div>
                    </div>

                    <div style={{
                      display: 'flex',
                      width: '274px',
                      flexDirection: 'column',  // 가로 배치로 변경
                      alignItems: 'center',  // 세로 가운데 정렬로 변경
                      position: 'relative',
                    }}>

                      <div style={{
                        display: 'flex',
                        width: '274px',
                        flexDirection: 'column',
                        alignItems: 'flex-start',
                        alignSelf: 'stretch',
                        position: 'relative',
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
                          width: currentQuiz?.quizType === 'MATCHING' ? `${274 * (matchingMatchedPairs.length / quizzes.length)}px` : `${274 * ((currentIndex + 1) / quizzes.length)}px`,
                          height: '14px',
                          flexDirection: 'column',
                          alignItems: 'flex-start',
                          borderRadius: '999px',
                          background: '#9B87E8', 
                          position: 'relative',
                          zIndex: 1,
                        }}>

                          {/* 개구리 이미지 넣기? */}
                          <div style={{
                            width: '36px',
                            height: '36px',
                            aspectRatio: '1/1',
                            position: 'absolute',
                            right: '-18px',
                            top: '-11px',
                            background: `url(${frog2}) center / contain no-repeat`,
                            }}>
                          </div>
                        </div>
                      </div>
                    </div>

                  {/* 완주 표시 아이콘 */}
                  <div
                  style={{
                    display: 'flex',
                    width: '36px',
                    height: '36px',
                    alignItems: 'center',
                        background: 'transparent',
                        border: 'none',
                  }}>
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                      <path d="M8.00415 2.05813C9.47139 2.09829 10.9059 2.50116 12.1799 3.23C14.0199 4.28 16.2099 4.43991 18.1799 3.64993L19.6301 3.06985C19.7819 3.00944 19.9463 2.98768 20.1086 3.00539C20.271 3.02314 20.4267 3.07971 20.5618 3.17141C20.6969 3.26317 20.8076 3.38736 20.884 3.53176C20.9602 3.67592 21.0003 3.83648 21.0002 3.99953V14.9995C21.0004 15.1998 20.9407 15.3962 20.8284 15.562C20.7161 15.7278 20.5563 15.8561 20.3704 15.9302L18.9202 16.5103C17.8402 16.9403 16.7199 17.1499 15.5999 17.1499C14.0699 17.1499 12.5499 16.7502 11.1799 15.9702C9.28314 14.8821 7.00757 14.7701 5.01001 15.6353V21.9898H3.01001V3.98977C3.01061 3.80518 3.06238 3.62434 3.15942 3.46731C3.25646 3.31031 3.39501 3.18321 3.55981 3.10012L3.76978 2.99953C5.08184 2.34136 6.53681 2.018 8.00415 2.05813ZM11.1799 4.97024C9.27995 3.88025 7.00023 3.76042 5.00024 4.63039H5.01001V13.4995C5.89997 13.2096 6.83006 13.0601 7.76001 13.0601C9.31063 13.0639 10.8337 13.4702 12.1799 14.2398C13.0821 14.7616 14.0915 15.0712 15.1311 15.1441C16.1709 15.2168 17.2138 15.0508 18.1799 14.6597L19.0002 14.3296V5.48L18.9202 5.51028C16.3702 6.52027 13.5499 6.33021 11.1799 4.97024Z" fill="black"/>
                      <path d="M8.00415 2.05813C9.47139 2.09829 10.9059 2.50116 12.1799 3.23C14.0199 4.28 16.2099 4.43991 18.1799 3.64993L19.6301 3.06985C19.7819 3.00944 19.9463 2.98768 20.1086 3.00539C20.271 3.02314 20.4267 3.07971 20.5618 3.17141C20.6969 3.26317 20.8076 3.38736 20.884 3.53176C20.9602 3.67592 21.0003 3.83648 21.0002 3.99953V14.9995C21.0004 15.1998 20.9407 15.3962 20.8284 15.562C20.7161 15.7278 20.5563 15.8561 20.3704 15.9302L18.9202 16.5103C17.8402 16.9403 16.7199 17.1499 15.5999 17.1499C14.0699 17.1499 12.5499 16.7502 11.1799 15.9702C9.28314 14.8821 7.00757 14.7701 5.01001 15.6353V21.9898H3.01001V3.98977C3.01061 3.80518 3.06238 3.62434 3.15942 3.46731C3.25646 3.31031 3.39501 3.18321 3.55981 3.10012L3.76978 2.99953C5.08184 2.34136 6.53681 2.018 8.00415 2.05813ZM11.1799 4.97024C9.27995 3.88025 7.00023 3.76042 5.00024 4.63039H5.01001V13.4995C5.89997 13.2096 6.83006 13.0601 7.76001 13.0601C9.31063 13.0639 10.8337 13.4702 12.1799 14.2398C13.0821 14.7616 14.0915 15.0712 15.1311 15.1441C16.1709 15.2168 17.2138 15.0508 18.1799 14.6597L19.0002 14.3296V5.48L18.9202 5.51028C16.3702 6.52027 13.5499 6.33021 11.1799 4.97024Z" stroke="black"/>
                    </svg>
                  </div>
                </div>
                  )}
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
                  width: '100%',
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

                  {/* 자막 박스 감싸는 div */}
                  <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    alignSelf: 'stretch',
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
                                // 해당 종류를 일반 따옴표로 변환(백틱, 오른쪽 작은따옴표, 왼쪽 작은따옴표, 수정 문자 아포스트로피)
                                const normalized = word.replace(/[`''ʼ]/g, "'");
                                // 특수문자만 제거
                                const cleaned = normalized.replace(/[^a-zA-Z']/g, '');
                                // 단어의 품사 구분 및 일부 단어 필터링
                                const wordFilter = getWordFilter(currentSubtitle.text, cleaned);

                                // 구동사는 있으면 클릭 시 구동사로 보이기
                                const searchWord = wordFilter?.type === 'phrasal' ? wordFilter.phrase : cleaned;

                                return (
                                  <span
                                    key={index}
                                    // 클릭 시 필터링이 아니면 팝업 고정
                                    onClick={(e) => wordFilter && togglePin(e, searchWord, wordFilter)}>
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
                          background: 'transparent',
                          border: 'none',
                        }}>
                          <svg xmlns="http://www.w3.org/2000/svg" width="35" height="35" viewBox="0 0 35 35" fill="none">
                            <path d="M25.4625 8.91042C24.9812 8.6625 24.3979 8.70625 23.9458 9.02708L13.7375 16.3187C13.3583 16.5958 13.125 17.0333 13.125 17.5C13.125 17.9667 13.3583 18.4188 13.7375 18.6813L23.9458 25.9729C24.1938 26.1479 24.5 26.25 24.7917 26.25C25.025 26.25 25.2437 26.1917 25.4625 26.0896C25.9437 25.8417 26.25 25.3458 26.25 24.7917V10.2083C26.25 9.66875 25.9437 9.15833 25.4625 8.91042ZM23.3333 21.9625L17.0917 17.5L23.3333 13.0375V21.9479V21.9625ZM8.75 8.75H11.6667V26.25H8.75V8.75Z" fill="black"/>
                          </svg>
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
                        background: 'transparent',
                        border: 'none',
                        }}>
                          <svg xmlns="http://www.w3.org/2000/svg" width="35" height="35" viewBox="0 0 35 35" fill="none">
                            <path d="M8.75001 26.25H14.5833C15.3854 26.25 16.0417 25.5938 16.0417 24.7917V10.2083C16.0417 9.40625 15.3854 8.75 14.5833 8.75H8.75001C7.94792 8.75 7.29167 9.40625 7.29167 10.2083V24.7917C7.29167 25.5938 7.94792 26.25 8.75001 26.25ZM10.2083 11.6667H13.125V23.3333H10.2083V11.6667ZM20.4167 8.75C19.6146 8.75 18.9583 9.40625 18.9583 10.2083V24.7917C18.9583 25.5938 19.6146 26.25 20.4167 26.25H26.25C27.0521 26.25 27.7083 25.5938 27.7083 24.7917V10.2083C27.7083 9.40625 27.0521 8.75 26.25 8.75H20.4167ZM24.7917 23.3333H21.875V11.6667H24.7917V23.3333Z" fill="black"/>
                          </svg>
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
                        background: 'transparent',
                        border: 'none',
                        }}>
                          <svg xmlns="http://www.w3.org/2000/svg" width="35" height="35" viewBox="0 0 35 35" fill="none">
                            <path d="M21.2625 16.3187L11.0542 9.02708C10.6021 8.70625 10.0187 8.6625 9.5375 8.91042C9.05625 9.15833 8.75 9.65417 8.75 10.2083V24.7917C8.75 25.3313 9.05625 25.8417 9.5375 26.0896C9.74167 26.1917 9.975 26.25 10.2083 26.25C10.5 26.25 10.8062 26.1625 11.0542 25.9729L21.2625 18.6813C21.6417 18.4042 21.875 17.9667 21.875 17.5C21.875 17.0333 21.6417 16.5812 21.2625 16.3187ZM11.6667 21.9625V13.0521L17.9083 17.5146L11.6667 21.9771V21.9625ZM23.3333 8.75H26.25V26.25H23.3333V8.75Z" fill="black"/>
                          </svg>
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
                    {currentQuiz?.quizType === 'BLANK' && (!isConfirmed ? <div>이 문장의 빈칸, 알 것 같지 않아?<br/> 한 번 맞춰봐!</div>  : feedback?.feedback)}
                    {currentQuiz?.quizType === 'OX' && (!isConfirmed ? "문장에 들어간 단어로 저게 맞을까?" : feedback?.feedback)}
                    {currentQuiz?.quizType === 'MATCHING' && (!isConfirmed ? "문장에 들어간 단어로 저게 맞을까?" : matchingMatchedPairs.length === quizzes.length ? "수고했어! 이제 결과를 볼까?" : "문장에 들어간 단어로 저게 맞을까?")}
                  </p>
                </div>
              </div>



              {/* 힌트 박스 */}
              {/* 선택했을 때 */}
              {currentQuiz?.quizType !== 'MATCHING' && isConfirmed && (
                // 정답인 경우
                isCorrect ? (
                  <div style={{
                    display: 'flex',
                    width: '370px',
                    minHeight: '68px',
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
                        {feedback?.explanation}
                      </div>
                    </div>
                  </div>
                ) : (
                  // 오답인 경우
                  <div style={{
                    display: 'flex',
                    width: '370px',
                    minHeight: '68px',
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
                        {feedback?.explanation}
                      </div>
                    </div>
                  </div>
                )
              )}

              {currentQuiz?.quizType === 'BLANK' && blankQuiz()}
              {currentQuiz?.quizType === 'OX' && oxQuiz()}
              {currentQuiz?.quizType === 'MATCHING' &&  matchingShuffledMeanings?.length > 0 &&  matchingQuiz()}

              {/* 럭키 미스테이크 전체 배경 박스 */}
              {currentQuiz?.quizType === 'BLANK' && isConfirmed && !feedback?.correct && (
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
      {currentQuiz && (
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
          <button
          onClick={handleNext}
          style={{
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
              lineHeight: '18px',
              whiteSpace: 'nowrap'
            }}>
              건너 뛰기
            </p>
          </button>

          {/* 제출 및 넘어가기 버튼 */}
          {currentQuiz?.quizType === 'MATCHING' ? (
            // 매칭 퀴즈는 결과 보기만 나옴
            <button
            onClick={handleMatchingComplete}
            disabled={matchingMatchedPairs.length < quizzes.length}
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
              background: matchingMatchedPairs.length === quizzes.length ? '#7C3AED' : '#D9D9D9',
            }}>

              <p style={{
                color: '#FFF',
                fontFamily: 'Pretendard',
                fontSize: '16px',
                fontStyle: 'normal',
                fontWeight: '700',
                lineHeight: '18px',
                whiteSpace: 'nowrap'
              }}>
              결과 보기
              </p>
            </button>
          ) : (
            // ox, 빈칸 채우기 전용
            !isConfirmed ? (
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
                  lineHeight: '18px',
                  whiteSpace: 'nowrap'
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
            )
          )}
        </div>
      </div>
      )}
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
            minHeight: '188px',
            // height: '188.212px',
            borderRadius: '15.979px',
            border: '0.799px solid #E7E6EB',
            background: '#FFF',
            boxShadow: '0 3.196px 3.196px 0 rgba(206, 210, 223, 0.16)',
            zIndex: '999',
            display: 'flex',
            flexDirection: 'column',
            gap: '8px',
            padding: '16px',
          }}>

            {/* 상단 버튼 영역 - 가로 배치 */}
            <div style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              width: '100%',
            }}>

            {/* 수집 기능 넣기 */}
            <button 
            onClick={() => collectWord()}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              justifyContent: 'center',
              width: '86.289px',
              height: '31.959px',
              zIndex: '999',
              borderRadius: '7.99px',
              border: '0.799px solid #E7E6EB',
              background: '#FFF',
              boxSizing: 'border-box',
              padding: '0 8px',
            }}>

              {/* 사전 아이콘 들어갈 자리 */}
              <div style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '19.175px',
                height: '19.175px',
                background: 'transparent',
                border: 'none',
                zIndex: '999',
                flexShrink: 0,
              }}>
                {isCollected ? (
                  <svg xmlns="http://www.w3.org/2000/svg" width="9" height="11" viewBox="0 0 9 11" fill="none">
                    <path d="M7.58301 0C8.17884 0 8.66602 0.488151 8.66602 1.08398V10.292C8.66602 10.487 8.56342 10.6662 8.39551 10.7637C8.31439 10.8123 8.22231 10.8339 8.125 10.834C8.0275 10.834 7.93477 10.8124 7.85352 10.7637L4.33301 8.74805L0.8125 10.7637C0.644639 10.8611 0.438394 10.8611 0.270508 10.7637C0.102591 10.6662 0 10.487 0 10.292V1.08398C0 0.488158 0.487184 1.11154e-05 1.08301 0H7.58301Z" fill="#7C3AED"/>
                  </svg>
                ) : (
                  <svg xmlns="http://www.w3.org/2000/svg" width="9" height="11" viewBox="0 0 9 11" fill="none">
                    <path d="M7.58333 0H1.08333C0.4875 0 0 0.4875 0 1.08333V10.2917C0 10.4867 0.102917 10.6654 0.270833 10.7629C0.43875 10.8604 0.644583 10.8604 0.8125 10.7629L4.33333 8.74792L7.85417 10.7629C7.93542 10.8117 8.0275 10.8333 8.125 10.8333C8.2225 10.8333 8.31458 10.8117 8.39583 10.7629C8.56375 10.6654 8.66667 10.4867 8.66667 10.2917V1.08333C8.66667 0.4875 8.17917 0 7.58333 0ZM7.58333 4.33333V9.36L4.60417 7.65917C4.52283 7.61156 4.43028 7.58647 4.33604 7.58647C4.2418 7.58647 4.14925 7.61156 4.06792 7.65917L1.08875 9.36V1.08333H7.58875V4.33333H7.58333Z" fill="black"/>
                  </svg>
                )}
              </div>

              <span style={{
                color: '#01030D',
                fontFamily: 'Pretendard',
                fontSize: '11.186px',
                fontStyle: 'normal',
                fontWeight: '400',
                zIndex: '999',
                lineHeight: 'normal',
                whiteSpace: 'nowrap',
              }}>
                단어 수집
              </span>
            </button>

            {/* 닫기 버튼 */}
            <button 
            onClick= {() => closePopup()}
            style={{
              width: '28.763px',
              height: '28.763px',
              zIndex: '999',
              background: 'transparent',
              border: 'none',
            }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M14.83 7.76001L12 10.59L9.17001 7.76001L7.76001 9.17001L10.59 12L7.76001 14.83L9.17001 16.24L12 13.41L14.83 16.24L16.24 14.83L13.41 12L16.24 9.17001L14.83 7.76001Z" fill="black"/>
                <path d="M12 2.00001C9.33 2.00001 6.82 3.04001 4.93 4.93001C3.04 6.82001 2 9.33001 2 12C2 14.67 3.04 17.18 4.93 19.07C6.88 21.02 9.44 21.99 12 21.99C14.56 21.99 17.12 21.02 19.07 19.07C21.02 17.12 22 14.67 22 12C22 9.33001 20.96 6.82001 19.07 4.93001C18.1439 3.99824 17.0422 3.25949 15.8286 2.75654C14.615 2.25359 13.3137 1.99645 12 2.00001ZM17.66 17.66C14.54 20.78 9.47 20.78 6.35 17.66C4.84 16.15 4.01 14.14 4.01 12C4.01 9.86001 4.84 7.85001 6.35 6.34001C7.86 4.83001 9.87 4.00001 12.01 4.00001C14.15 4.00001 16.16 4.83001 17.67 6.34001C19.18 7.85001 20.01 9.86001 20.01 12C20.01 14.14 19.18 16.15 17.67 17.66H17.66Z" fill="black"/>
              </svg>
            </button>
          </div>

            {/* 해당하는 품사 반환 */}
            <p style={{
              zIndex: '999',
              color: '#9198A3',
              fontFamily: 'Pretendard',
              fontSize: '9.588px',
              fontStyle: 'normal',
              fontWeight: '400',
              lineHeight: 'normal'
            }}>
              {partOfSpeechKo[dictionaryData.partOfSpeech] || ''}
            </p>

            {/* 단어 */}
            <p style={{
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
              width: '238.094px',
              height: '0.799px',
              background: '#E7E6EB',
              zIndex: '999',
            }}>
            </div>

            {/* 단어 뜻 번역 */}
            <p style={{
              color: '#01030D',
              fontFamily: 'Pretendard',
              fontSize: '12.784px',
              fontStyle: 'normal',
              fontWeight: '700',
              lineHeight: 'normal',
              zIndex: '999',
              // 자동 줄바꿈 추가
              wordBreak: 'break-word',
            }}>
              {dictionaryData.meaningTranslation}
            </p>

            {/* 단어 가리키는 화살표 박스 */}
            <div style={{
              position: 'absolute',
              // top: '175.4px',
              bottom: '-32px',
              left: `${popupPosition.arrowLeft}px`,   // 동적 위치로 변경
              transform: 'translateX(-50%)',  // 좌우 중앙 정렬로 변경
              width: '29.562px',
              height: '33.504px',
              fill: '#FFF',
              strokeWidth: '0.985px',
              stroke: '#E7E6EB',
              zIndex: '999',
              background: 'transparent',
              border: 'none',
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
                  {nextBadge}
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
                // 비율 유지 + 전체 보이기
                backgroundSize: 'contain',
                // 가운데 정렬
                backgroundPosition: 'center',
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