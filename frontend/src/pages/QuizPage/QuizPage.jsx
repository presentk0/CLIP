/* global chrome */

import nlp from 'compromise';
import React, { useState, useEffect, useRef, useLayoutEffect } from 'react';
// import frog from '../../imgs/image_710.png';
// import frog1 from '../../imgs/image_712.png';
import { apiFetch } from '../../utils/api';
// import bulb from '../../imgs/image_62.png';
// import frog2 from '../../imgs/image_750.png';
import { log } from '../../utils/logger';
import styles from './QuizPage.module.css';
import frog3 from '../../imgs/image_809.png';
import { useNavigate } from 'react-router-dom';

import SettlementPage from '../SettlementPage/SettlementPage';
import { saveUserData, loadUserData, removeUserData } from '../../utils/userStorage';



// 렌더링해도 1번만 셔플 (비교값을 -0.5 ~ 0.5으로 설정)
const shuffle = (arr) => [...arr].sort(() => Math.random() - 0.5);

function QuizPage({ videoId, videoTitle, channelName, thumbnailUrl, duration }) {

  // 현재 문제 번호 (0부터 시작)
  const [currentIndex, setCurrentIndex] = useState(0);
  // 유저가 선택한 보기 (아직 제출 안 함)
  const [tempChoice, setTempChoice] = useState(null);
  // 정답 확인 버튼을 눌렀는지 여부
  const [isConfirmed, setIsConfirmed] = useState(false);
  // 퀴즈 나가기 확인 팝업 표시 여부
  const [exitModal, setExitModal] = useState(null);

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

  // // 다음 목표 뱃지 저장
  // const [nextBadge, setNextBadge] = useState(null);

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

  // 정산 상태 저장
  const [isSettlement, setIsSettlement] = useState(false);

  const [settlementData, setSettlementData] = useState(null);

  // 팝업 측정 여부 체크
  const [isPositioned, setIsPositioned] = useState(false);

  // 클릭한 단어의 위치/크기 저장 (위치 계산용)
  const [wordRect, setWordRect] = useState(null);

  // useRef = DOM 요소에 직접 접근하기 위한 참조 상자
  // useState와 달리 값이 바뀌어도 리렌더링 안 됨
  // .current로 실제 값(DOM 노드)에 접근
  // JSX의 ref={popupRef} 속성으로 DOM과 연결됨
  // 렌더링 후에 popupRef.current = <div> 실제 DOM이 들어감
  const popupRef = useRef(null);

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


  //  useEffect는 렌더 -> 화면에 그림 -> 실행  (사용자가 변경 전 화면을 봄 = 깜빡임)
  //  useLayoutEffect는 렌더 -> 실행 -> 화면에 그림  (사용자는 변경된 화면만 봄 = 깜빡임 없음)
  //  DOM 크기/위치 측정 후 그 값으로 다시 위치 조정하기 위해 사용
  useLayoutEffect(() => {
    // 필요한 값이 다 준비되지 않았으면 실행 안 함
    // popupRef.current가 null이면 아직 DOM에 팝업이 없음
    // wordRect가 null이면 단어 클릭 정보가 없음
    // dictionaryData가 null이면 사전 데이터가 아직 없음
    if (!popupRef.current || !wordRect || !dictionaryData) return;

    // 실제 렌더링된 팝업의 너비/높이 측정
    // offsetWidth/offsetHeight = 패딩과 테두리를 포함한 실제 픽셀 크기
    const popupWidth = popupRef.current.offsetWidth;
    const popupHeight = popupRef.current.offsetHeight;
    // 단어 중앙 X 좌표 (화면 기준)
    const wordCenter = wordRect.left + wordRect.width / 2;

    // 팝업 좌측 X 좌표 = 단어 중앙에 팝업 중앙을 맞춤
    let popupLeft = wordCenter - popupWidth / 2;
    // 화면 왼쪽으로 삐져나가지 않게 보정
    if (popupLeft < 10) popupLeft = 10;
    // 화면 오른쪽으로 삐져나가지 않게 보정
    if (popupLeft + popupWidth > window.innerWidth - 10) {
      popupLeft = window.innerWidth - popupWidth - 10;
    }

    // 꼬리(말풍선 삼각형) 위치 = 단어 중앙 - 팝업 좌측 - 16(padding 값)
    // .popup의 padding-left가 16이라서 .wrapper는 그만큼 안쪽에 있음
    const arrowLeft = wordCenter - popupLeft - 16;

    // 최종 위치 state 업데이트, JSX에 반영됨
    setPopupPosition({
      // 단어 위에 + 여백 10
      top: wordRect.top - popupHeight - 10,
      left: popupLeft,
      arrowLeft,
    });

    // 측정 완료
    setIsPositioned(true);
  // dictionaryData 바뀌면 팝업 내용 바뀜 -> 너비 변할 수 있음 -> 재계산 필요
  // wordRect 바뀌면 다른 단어 클릭한 것 -> 재계산 필요
  }, [dictionaryData, wordRect]);



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



  // 백그라운드에 단어 전용 번역 API 호출 메시지 전송
  async function fetchTranslation(word, meanings) {
    try {
      const response = await chrome.runtime.sendMessage({
        type: 'TRANSLATE_WORD',
        endpoint: '/translate/word',
        options: {
          method: 'POST',
          body: JSON.stringify({ word, meanings })
        }
      });
      // 배열 전체 반환
      return response?.data?.translations || [];
    } catch (error) {
      log.debug('번역 실패:', error);
      return [];
    }
  }









  // 품사명 매핑 테이블
  const posKor = {
    noun: '명사',
    verb: '동사',
    adjective: '형용사',
    adverb: '부사',
    pronoun: '대명사',
    preposition: '전치사',
    conjunction: '접속사',
    interjection: '감탄사',
  };

  // 단어 클릭 시 사전 팝업 열기
  const togglePin = async (e, word, wordFilter) => {

    // 다른 클릭 이벤트 막기
    e.stopPropagation();

    // 이미 열려있으면 닫기
    if (isPinned) {
      setIsPinned(false);
      setDictionaryData(null);
      setClickedSubtitle(null);
      setIsPositioned(false);

      // 나중에 조회해서 이미 수집했는지 확인 후 대응하기 (오류)
      setIsCollected(false);
      // return 없으면 다른 단어 클릭 시 즉시 이동
    }

    // 클릭 시점의 자막 정보 저장 (단어 수집용)
    // set은 다음 렌더링에 반영되기에 즉시 사용해야 하는 api는 savedSubtitle로 사용
    const savedSubtitle = { ...currentSubtitle };
    setClickedSubtitle({ ...savedSubtitle });

    // 클릭한 단어 위치만 저장 (너비 계산은 useLayoutEffect에서)
    const rect = e.target.getBoundingClientRect();
    // 측정 전
    setIsPositioned(false);
    setWordRect({
      left: rect.left,
      top: rect.top,
      width: rect.width,
    });

    // popupPosition 초기화 (측정 전)
    // 보이지 않게 화면 밖으로 보내기
    setPopupPosition({ top: -9999, left: -9999, arrowLeft: 0 });



    // 서버에서 조회한 단어에서 찾기
    const collected = collectedWords.find(w => w.word === word);
    if (collected) {
      // 자막 문맥 품사로 골라서 표시
    const filterKor = posKor[wordFilter] || wordFilter;
    const target = collected.meaningsByPos.find(m => m.partOfSpeech === filterKor) || collected.meaningsByPos[0];

      setDictionaryData({
        word: collected.word,
        phonetic: collected.phonetic,
        // meaningsByPos에서 추출
        partOfSpeech: target.partOfSpeech,
        // meaningsByPos에서 추출
        meanings: target.meanings,
        // 전체 데이터 보관
        meaningsByPos: collected.meaningsByPos,
        // meaningTranslations: collected.translations,
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

      // API 응답이 없거나, 데이터가 없으면 종료
      if (!data || !data[0]) return;


        // // 자막과 일치하는 품사에 대한 정보만 가져오기 (구동사 혹은 일치하는게 없으면 해당 단어의 첫 번째 품사에 대한 정보만 가져오기)
        // const meaning = data[0].meanings.find(m => m.partOfSpeech === wordFilter) || data[0].meanings[0];



        // 해당 품사의 목록 최대 3개 가져오기
        // const def = meaning.definitions[0];
        const result = {
          word: word,
          // 발음 기호(품사는 있어도 발음이 없는 경우가 있음)
          phonetic: data[0].phonetic || '',
          // 발음 음성
          audio: data[0].phonetics?.find(p => p.audio)?.audio || '',
          // 품사
          // partOfSpeech: meaning.partOfSpeech,
          partOfSpeech: '',
          // 영어 뜻 (최대 3개 배열)
          // definition: def.definition,



          // definitions: meaning.definitions.slice(0, 3).map(d => d.definition),



          // 예문 (최대 3개 배열)
          // example: def.example || '',


          // examples: meaning.definitions.slice(0, 3).map(d => d.example || ''),



          // 유의어
          // synonyms: meaning.synonyms?.slice(0, 5) || [],
          // 반의어
          // antonyms: meaning.antonyms?.slice(0, 5) || [],
          // 단어 번역 (최대 3개 배열)
          // meaningTranslation: '',




          // meaningTranslations: [],
          meanings: [],
          meaningsByPos: [],
        };

        // 모든 품사의 모든 뜻 추출
        const allByPos = data[0].meanings.map(m => ({
          partOfSpeech: m.partOfSpeech,                                  // "noun" 영어
          definitions: m.definitions.slice(0, 3).map(d => d.definition),
        }));

        // 추출된 게 없으면 팝업 닫고 종료
        if (allByPos.length === 0) {
          setIsPinned(false);
          setDictionaryData(null);
          setClickedSubtitle(null);
          setIsPositioned(false);
          return;
        }
        // 모든 뜻을 한 배열로 합치기
        const allDefinitions = allByPos.flatMap(m => m.definitions);
        // 배열 전체 번역 요청
        const translations = await fetchTranslation(word, allDefinitions);

        // // 뜻 번역
        // result.meaningTranslation = await fetchTranslation(result.definition) || '';

        // // 뜻 배열 번역
        // result.meaningTranslations = await fetchTranslation (word, result.definitions);

        // 번역 결과를 품사별로 매칭
        let cursor = 0;
        result.meaningsByPos = allByPos.map(m => {
          const slice = translations.slice(cursor, cursor + m.definitions.length);
          cursor += m.definitions.length;
          return {
            partOfSpeech: posKor[m.partOfSpeech] || m.partOfSpeech,
            meanings: slice
          };
        });



        // 자막 문맥 품사 골라서 result에 담기
        const filterKor = posKor[wordFilter] || wordFilter;
        const target = result.meaningsByPos.find(m => m.partOfSpeech === filterKor) || result.meaningsByPos[0];

        result.partOfSpeech = target.partOfSpeech;
        result.meanings = target.meanings;

        setDictionaryData(result);
        setIsPinned(true);

        // 팝업 열 때 서버에 타입 POPUP으로 보내기
        const collect = await apiFetch('/words/collect', {
          method: 'POST',
          body: JSON.stringify({
            wordType: 'POPUP',
            // videoId: videoId,
            videoId,
            // word: word,
            word,
            // meanings: result.meaningTranslations,
            meaningsByPos: result.meaningsByPos,
            sentence: savedSubtitle.text,
            timestamp: formatTime(savedSubtitle.startTime),
            // set은 다음 렌더링에 반영되기에 즉시 사용해야 하는 api는 dictionaryData.translation 대신 savedSubtitle.translation 사용
            // 또한 dictionaryData가 null이면 에러나기에 result로 ''처리
            translation: savedSubtitle.translation,
            title: videoTitle
          })
        });

        // 응답 성공 후 현재 로그인 유저의 캐시에서 ai챗 허용으로 갱신
        if (collect.success) {
          // await chrome.storage.local.set({ hasWords: true });
          await saveUserData('hasWords', true);
        }
        // 서버에 저장된 단어 추가
        setCollectedWords(prev => [...prev, {
          word: word,
          // translations: result.meaningTranslations
          phonetic: result.phonetic,
          meaningsByPos: result.meaningsByPos,
        }]);

    } catch (error) {
      log.debug('사전 조회 실패:', error);
      setIsPinned(false);
      setDictionaryData(null);
      setClickedSubtitle(null);
      setIsPositioned(false);
      return;
    }
  };







  // 사전 팝업 닫기
  const closePopup = () => {
    setIsPinned(false);
    setDictionaryData(null);
    setClickedSubtitle(null);
    setIsPositioned(false);

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
      const collect = await apiFetch('/words/collect', {
        method: 'POST',
        body: JSON.stringify({
          wordType: 'COLLECT',
          videoId: videoId,
          word: dictionaryData.word,
          meaningsByPos: dictionaryData.meaningsByPos,
          // meaning: dictionaryData.meaningTranslation,
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

      // 응답 성공 후 현재 로그인 유저의 캐시에서 ai챗 허용으로 갱신
      if (collect.success) {
        // await chrome.storage.local.set({ hasWords: true });
        await saveUserData('hasWords', true);
      }

      // 서버에 저장된 단어 추가
      setCollectedWords(prev => [...prev, {
        word: dictionaryData.word,
        // translation: clickedSubtitle.translation,
        meaningsByPos: dictionaryData.meaningsByPos,
      }]);
      setIsCollected(true);
    } catch (error) {
      log.debug('단어 수집 실패:', error);
    }
  };



  // // 해당하는 품사를 한글로 변환
  // const partOfSpeechKo = {
  //   verb: '동사',
  //   noun: '명사',
  //   adjective: '형용사',
  //   adverb: '부사'
  // };



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
    { padding: '14px 59px 14px 56px' },
    // 2번: 오른쪽 위
    { padding: '12px 55px 12px 57px' },
    // 3번: 왼쪽 아래
    { padding: '11px 57px 13px 55px' },
    // 4번: 오른쪽 아래
    { padding: '12px 55px 12px 57px' },
  ];



  // 현재 문제 정보
  const currentQuiz = quizzes[currentIndex];
  // if (currentQuiz?.quizType === 'MATCHING') {
  //   log.debug(feedback?.feedback);
  // }




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








  // 정답 오답 선택 미선택에 따른 background 색상 변화 (빈칸 퀴즈)
  const backgroundColor = (choice) => {
    // feedback에서 정답 가져오기
    const correctAnswer = feedback?.correctAnswer;
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '#45A84D' : '#E9E9DE';
    }

    // 확정 후: 정답이면
    if (choice === correctAnswer) {
      return '#76B9F0';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !== correctAnswer) {
      return '#FFF2D0';
    }

    // 확정 후: 미선택이면
    return '#E9E9DE';
  };



  // 정답 오답 선택 미선택에 따른 color 색상 변화 (빈칸 퀴즈)
  const textColor = (choice) => {
    // feedback에서 정답 가져오기
    const correctAnswer = feedback?.correctAnswer;
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '#FFF' : '#A0A08A';
    }

    // 확정 후: 정답이면
    if (choice === correctAnswer) {
      return '#FFF';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !== correctAnswer) {
      return '#FFC229';
    }

    // 확정 후: 미선택이면
    return '#A0A08A';
  };



  // 정답 오답 선택 미선택에 따른 Color 색상 변화 (ox 퀴즈)
  const oxColor = (choice) => {
    // feedback에서 정답 가져오기
    const correctAnswer = feedback?.correctAnswer;
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '#FFF' : '#A0A08A';
    }

    // 확정 후: 정답이면
    if (choice ===  correctAnswer) {
      return '#FFF';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !==  correctAnswer) {
      return '#FFC229';
    }

    // 확정 후: 미선택이면
    return '#A0A08A';
  };



  // 정답 오답 선택 미선택에 따른 background 색상 변화 (ox 퀴즈)
  const oxBackgroundColor = (choice) => {
    // feedback에서 정답 가져오기
    const correctAnswer = feedback?.correctAnswer;
    if (!isConfirmed) {
      // 확정 전: 선택한 것 : 선택안한 것 표시
      return tempChoice === choice ? '#45A84D' : '#E9E9DE';
    }

    // 확정 후: 정답이면
    if (choice ===  correctAnswer) {
      return '#76B9F0';
    }

    // 확정 후: 내가 선택한 답이 오답이면
    if (choice === tempChoice && tempChoice !==  correctAnswer) {
      return '#FFF2D0';
    }

    // 확정 후: 미선택이면
    return '#E9E9DE';
  };



  // 매칭 퀴즈 영단어 클릭
  const matchingHandleWordClick = (wordQuizId) => {
    // 이미 맞춘 단어면 무시
    if (matchingMatchedPairs.some(pair => pair.wordQuizId === wordQuizId)) return;

    // 다른 단어 클릭하면 선택 변경
    setMatchingSelectedWord(wordQuizId);

    // 뜻이 이미 선택되어 있으면 매칭 시도
    if (matchingSelectedMeaning !== null) {
      checkMatch(wordQuizId, matchingSelectedMeaning);
    }
  };



  // 매칭 퀴즈 뜻 클릭
  const matchingHandleMeaningClick = (meaningQuizId) => {
    // 이미 맞춘 뜻이면 무시
    if (matchingMatchedPairs.some(pair => pair.meaningQuizId === meaningQuizId)) return;

    // 다른 뜻 클릭하면 선택 변경
    setMatchingSelectedMeaning(meaningQuizId);

    // 단어가 이미 선택되어 있으면 매칭 시도
    if (matchingSelectedWord !== null) {
      checkMatch(matchingSelectedWord, meaningQuizId);
    }
  };


  // 매칭 퀴즈 매칭확인
  const checkMatch = async (wordQuizId, meaningQuizId) => {
    // 같은 quizId면 정답 (단어와 뜻이 같은 퀴즈에 속함)
    const isMatchingCorrect = wordQuizId === meaningQuizId;
    const quiz = quizzes.find(q => q.quizId === wordQuizId);

    // 이미 전송한 quizId면 서버 전송 스킵
    const alreadySubmitted = submittedQuizIds.includes(wordQuizId);

    // 정답일 경우
    if (isMatchingCorrect) {
      // 전송하지 않은 quizId인 경우 전송
      if (isMatchingCorrect) {
        try {
          await apiFetch('/quiz/submit', {
            method: 'POST',
            body: JSON.stringify({
              sessionId,
              quizId: wordQuizId,
              userAnswer: quiz.answer
            })
          });
          setSubmittedQuizIds(prev => [...prev, wordQuizId]);
        } catch (error) {
          log.debug('매칭 정답 전송 실패:', error);
        }
      }

    // 화면 업데이트
    setMatchingMatchedPairs(prev => [...prev, { wordQuizId, meaningQuizId }]);
    setMatchingSelectedWord(null);
    setMatchingSelectedMeaning(null);
    } else {
      // 오답일 경우
      // 전송하지 않은 quizId인 경우 전송
      if (!alreadySubmitted) {
        const wrongMeaning = quizzes.find(q => q.quizId === meaningQuizId)?.answer;
        try {
          await apiFetch('/quiz/submit', {
            method: 'POST',
            body: JSON.stringify({
              sessionId,
              quizId: wordQuizId,
              userAnswer: wrongMeaning
            })
          });
          setSubmittedQuizIds(prev => [...prev, wordQuizId]);
        } catch (error) {
          log.debug('매칭 오답 전송 실패:', error);
        }
      }
      setMatchingWrongPair({ wordQuizId, meaningQuizId });

      // 1초 후 초기화
      setTimeout(() => {
        setMatchingWrongPair(null);
        setMatchingSelectedWord(null);
        setMatchingSelectedMeaning(null);
      }, 1000);
    }
  };



  // 단어 버튼의 정답 오답 선택 미선택에 따른 background, border, box-shadow 색상 변화 (매칭)
  const matchingGetWordStyle = (wordQuizId) => {
    // 맞춘 단어 - 파란색 유지
    if (matchingMatchedPairs.some(pair => pair.wordQuizId === wordQuizId)) {
      return { background: '#76B9F0', color: '#FFF'};
    }
    // 틀린 단어 - 주황색
    if (matchingWrongPair?.wordQuizId === wordQuizId) {
      return { background: '#FFF2D0', color: '#FFC229'};
    }
    // 선택된 단어 - 파란색
    if (matchingSelectedWord === wordQuizId) {
      return { background: '#76B9F0', color: '#FFF'};
    }
    // 미선택
    return { background: '#E9E9DE', color: '#A0A08A'};
  };



  // 뜻 버튼의 정답 오답 선택 미선택에 따른 background, color 색상 변화 (매칭)
  const matchingGetMeaningStyle = (meaningQuizId) => {
    // 맞춘 뜻 - 파란색 유지
    if (matchingMatchedPairs.some(pair => pair.meaningQuizId === meaningQuizId)) {
      return { background: '#76B9F0', color: '#FFF'};
    }
    // 틀린 뜻 - 주황색
    if (matchingWrongPair?.meaningQuizId === meaningQuizId) {
      return { background: '#FFF2D0', color: '#FFC229'};
    }
    // 선택된 뜻 - 파란색
    if (matchingSelectedMeaning === meaningQuizId) {
      return { background: '#76B9F0', color: '#FFF'};
    }
    // 미선택
    return { background: '#E9E9DE', color: '#A0A08A'};
  };



  // 매칭 퀴즈 결과 보기 버튼
  const handleMatchingComplete = async () => {
    // 모든 매칭 완료했는지 확인
    if (matchingMatchedPairs.length === quizzes.length) {
      try {
        const finalResult = await apiFetch(`/quiz/sessions/${sessionId}/complete`, {
          method: 'POST'
        });
        // 해당 유저의 저장된 진행 상황 삭제
        // chrome.storage.local.remove('quizState');
        await removeUserData('quizState');
        // 정산 페이지로 이동
        setSettlementData(finalResult.data);
        setIsSettlement(true);
        // onSettlementPage(finalResult.data);
      } catch (error) {
        log.debug('handleMatchingComplete 세션 종료 실패', error);
      }
    }
  };



  // 퀴즈 초기화 및 이전 진행 상황 복원
  useEffect(() => {
    const restoreQuiz = async () => {
      // 크롬 저장소에서 유저 id에 맞는 이전 진행 상황 확인
      const quizState = await loadUserData('quizState');
    // 크롬 저장소에서 이전 진행 상황 확인
    // chrome.storage.local.get('quizState', async (result) => {

      // 같은 영상의 저장된 퀴즈가 있으면 이어하기
      // if (result.quizState?.videoId === videoId) {
      if (quizState && quizState.videoId === videoId) {
        setQuizzes(quizState.quizzes);
        setMatchingShuffledMeanings(quizState.matchingShuffledMeanings ?? []);
        setSessionId(quizState.sessionId);
        setCurrentIndex(quizState.currentIndex);
        setCorrectCount(quizState.correctCount);
        setWrongCount(quizState.wrongCount);
        setAnswers(quizState.answers);
        setIsConfirmed(quizState.isConfirmed ?? false);
        setTempChoice(quizState.tempChoice ?? null);

        setMatchingSelectedWord(quizState.matchingSelectedWord ?? null);
        setMatchingSelectedMeaning(quizState.matchingSelectedMeaning ?? null);
        setMatchingMatchedPairs(quizState.matchingMatchedPairs ?? []);
        setMatchingWrongPair(null);
        setSubmittedQuizIds(quizState.submittedQuizIds ?? []);
        // 영상 일시정지 메시지 전송
        chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
          if (tabs[0]) chrome.tabs.sendMessage(tabs[0].id, { type: 'SET_QUIZ_MODE', active: true }).catch((error) => {log.debug('이거 에러22:', error)});
        });
      }

      // 복원 완료 체크
      setIsRestoring(false);
    };
    // });
    restoreQuiz();

    // content.jsx에서 새 퀴즈 받기
    const listener = (message) => {
      if (message.type === 'QUIZ_READY') {

        // 새 퀴즈로 초기화
        setSessionId(message.sessionId);
        setQuizzes(message.quizzes);
        if (message.quizzes[0]?.quizType === 'MATCHING') {
  // { quizId, meaning } 형태로 저장
  const meaningsWithId = message.quizzes.map(q => ({
    quizId: q.quizId,
    meaning: q.answer
  }));
  setMatchingShuffledMeanings(shuffle(meaningsWithId));
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




  // 진행 상황 자동 저장 (상태 바뀔 때마다)
  useEffect(() => {
    // 복원 중 아닐 때만 저장
    if (sessionId && !isRestoring) {
    // chrome.storage.local.set({
    //   quizState: {
      saveUserData('quizState', {
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
    )
  };
      // });
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
        // chrome.storage.local.remove('quizState');
        // 해당 유저의 저장된 진행 상황 삭제
        await removeUserData('quizState');

        // 정산 페이지로 이동
        setSettlementData(finalResult.data);
        setIsSettlement(true);
        // onSettlementPage(finalResult.data);
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
        // 해당 유저의 저장된 진행 상황 삭제
        // chrome.storage.local.remove('quizState');
        await removeUserData('quizState');

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
  const handleExit = () => {
    if (exitModal === 'back') {
      navigate(-1);
    } else if (exitModal === 'mypage') {
      navigate('/my');
    }
    // 모달 닫기
    setExitModal(null);
  };


  // 뒤로가기 버튼
  const handleBack = () => {
    if (currentQuiz) {
      setExitModal('back');
    } else {
      navigate(-1);
    }
  };

  // 마이페이지 버튼
  const handleMyPage = () => {
    if (currentQuiz) {
      setExitModal('mypage');
    } else {
      navigate('/my');
    }
  };



  // // 테스트 버튼 오류
  // const testButton = async () => {
  //   try {
  //       const finalResult = await apiFetch(`/quiz/sessions/${sessionId}/complete`, {
  //         method: 'POST'
  //       });
  //       // chrome.storage.local.remove('quizState');
  //       // 해당 유저의 저장된 진행 상황 삭제
  //       await removeUserData('quizState');

  //       // 정산 페이지로 이동
  //       setSettlementData(finalResult.data);
  //       setIsSettlement(true);
  //       // onSettlementPage(finalResult.data);
  //     } catch (error) {
  //       log.debug('MATCHING handleNext 세션 종료 실패', error);
  //     }
  // };









  // 퀴즈 기본 화면 테스트용 오류
  const testQuiz = () => {

    return (
      <div className={styles.container}>




        <div className={styles.header}>
          <div className={styles.header2}>
            <button
            onClick={handleBack}
            className={styles['header3-l']}>
              뒤로가기
            </button>

            <button
            onClick={handleMyPage}
            className={styles['header3-r']}>
              마이페이지
            </button>
          </div>
          <div className={styles.header4}>
            <p className={styles['header4-t']}>퀴즈</p>
          </div>
        </div>


        <div className={styles.bar}>
          <div className={styles.bar2}>
            <div 
            className={styles.bar3}
            style={{ backgroundImage: `url(${frog3})` }}
            >
              
            </div>
          </div>

          <div className={styles.bar4}>
          {quizzes && quizzes.length > 0 && (
            <div className={styles.bar5}>
              <p className={styles['bar5-t']}>{currentQuiz?.quizType === 'MATCHING' ? matchingMatchedPairs.length : currentIndex + 1}</p>
              <span className={styles['bar5-t']}> /{quizzes?.length}</span>
            </div>
            )}
            <div className={styles.bar6}>
              <div className={styles.bar7}></div>
              <div 
              className={styles.bar8}
              style={{width: currentQuiz?.quizType === 'MATCHING' ? `${274 * (matchingMatchedPairs.length / quizzes.length)}px` : `${274 * ((currentIndex + 1) / quizzes.length)}px`}}
              ></div>
            </div>
          </div>

        </div>



        {quizzes && quizzes.length > 0 && (
        <div className={styles.balloon}>
          <div className={styles.balloon2}>
            <p className={styles['balloon2-t']}>
                    {currentQuiz?.quizType === 'BLANK' && (!isConfirmed ? "방금 영상에서 나온 문장이에요. 이 자리에 어떤 단어가 들어갈까요?"  : feedback?.feedback)}
                    {currentQuiz?.quizType === 'OX' && (!isConfirmed ? "방금 영상에서 나온 문장이에요. 이 자리에 어떤 단어가 들어갈까요?" : feedback?.feedback)}
                    {currentQuiz?.quizType === 'MATCHING' && (!isConfirmed ? "방금 영상에서 나온 문장이에요. 이 자리에 어떤 단어가 들어갈까요?" : matchingMatchedPairs.length === quizzes.length ? "수고했어! 이제 결과를 볼까?" : "방금 영상에서 나온 문장이에요. 이 자리에 어떤 단어가 들어갈까요?")}
            </p>
          </div>
        </div>
        )}

{/* 정산 상태에 따라 분기, 오류 테스트 끝내고 !지우기 */}
      {isSettlement ? (
        <SettlementPage 
        data={settlementData}
        videoId={videoId}
        videoTitle={videoTitle}
        channelName={channelName}
        duration={duration}
        thumbnailUrl={thumbnailUrl}
        />
      ) : (
        <>
        <div className={styles.subtitles}>
          <div className={styles['subtitles-badge']}>
            <div className={styles['subtitles-badge2']}>
              <p className={styles['subtitles-badge3']}>방금 자막</p>
            </div>
          </div>
          <div className={styles.subtitles2}>
            <div className={styles.subtitles3}>
              <div className={styles.subtitles4}>
                <p className={styles['subtitles4-t']}>
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
              <div className={styles.subtitles5}>
                <p className={styles.subtitles6}>
                  {currentSubtitle.translation}
                </p>
              </div>
            </div>
            <div className={styles.subtitles7}>
              {/* 전 스크립트 보기 */}
              <button
              onClick= {() => goPrev()}
              className={styles.subtitles8}>
                <svg xmlns="http://www.w3.org/2000/svg" width="35" height="35" viewBox="0 0 35 35" fill="none">
                  <path d="M25.4625 8.91042C24.9812 8.6625 24.3979 8.70625 23.9458 9.02708L13.7375 16.3187C13.3583 16.5958 13.125 17.0333 13.125 17.5C13.125 17.9667 13.3583 18.4188 13.7375 18.6813L23.9458 25.9729C24.1938 26.1479 24.5 26.25 24.7917 26.25C25.025 26.25 25.2437 26.1917 25.4625 26.0896C25.9437 25.8417 26.25 25.3458 26.25 24.7917V10.2083C26.25 9.66875 25.9437 9.15833 25.4625 8.91042ZM23.3333 21.9625L17.0917 17.5L23.3333 13.0375V21.9479V21.9625ZM8.75 8.75H11.6667V26.25H8.75V8.75Z" fill="black"/>
                </svg>
              </button>


              {/* 현재 스크립트 구간 반복 */}
              <button 
              onClick={() => toggleLoop()}
              className={styles.subtitles8}>
                <svg xmlns="http://www.w3.org/2000/svg" width="35" height="35" viewBox="0 0 35 35" fill="none">
                  <path d="M8.75001 26.25H14.5833C15.3854 26.25 16.0417 25.5938 16.0417 24.7917V10.2083C16.0417 9.40625 15.3854 8.75 14.5833 8.75H8.75001C7.94792 8.75 7.29167 9.40625 7.29167 10.2083V24.7917C7.29167 25.5938 7.94792 26.25 8.75001 26.25ZM10.2083 11.6667H13.125V23.3333H10.2083V11.6667ZM20.4167 8.75C19.6146 8.75 18.9583 9.40625 18.9583 10.2083V24.7917C18.9583 25.5938 19.6146 26.25 20.4167 26.25H26.25C27.0521 26.25 27.7083 25.5938 27.7083 24.7917V10.2083C27.7083 9.40625 27.0521 8.75 26.25 8.75H20.4167ZM24.7917 23.3333H21.875V11.6667H24.7917V23.3333Z" fill="black"/>
                </svg>
              </button>


              {/* 다음 스크립트 보기 */}
              <button
              onClick={() => goNext()}
              className={styles.subtitles8}>
                <svg xmlns="http://www.w3.org/2000/svg" width="35" height="35" viewBox="0 0 35 35" fill="none">
                  <path d="M21.2625 16.3187L11.0542 9.02708C10.6021 8.70625 10.0187 8.6625 9.5375 8.91042C9.05625 9.15833 8.75 9.65417 8.75 10.2083V24.7917C8.75 25.3313 9.05625 25.8417 9.5375 26.0896C9.74167 26.1917 9.975 26.25 10.2083 26.25C10.5 26.25 10.8062 26.1625 11.0542 25.9729L21.2625 18.6813C21.6417 18.4042 21.875 17.9667 21.875 17.5C21.875 17.0333 21.6417 16.5812 21.2625 16.3187ZM11.6667 21.9625V13.0521L17.9083 17.5146L11.6667 21.9771V21.9625ZM23.3333 8.75H26.25V26.25H23.3333V8.75Z" fill="black"/>
                </svg>
              </button>
            </div>
          </div>
        </div>

        {!['BLANK', 'OX', 'MATCHING'].includes(currentQuiz?.quizType) && (
          <div className={styles.ready}>
            <div className={styles.ready2}>
              <div className={styles.ready3}></div>
              <div className={styles.ready4}></div>
              <div className={styles.ready5}></div>
              <div className={styles.ready6}></div>
            </div>

            <p className={styles.ready7}>
              퀴즈를 준비하고 있어요!
            </p>
          </div>
        )}


        {currentQuiz?.quizType === 'BLANK' && blankQuiz()}
        {currentQuiz?.quizType === 'OX' && oxQuiz()}
        {currentQuiz?.quizType === 'MATCHING' &&  matchingShuffledMeanings?.length > 0 &&  matchingQuiz()}
      </>
      )}
      </div>
    )
  }


  // 실제 빈칸퀴즈 테스트용 오류
  const blankQuiz = () => {
    if (!currentQuiz?.options?.length) return null;
    return (
          isConfirmed && !feedback?.correct ? (
            // 틀린 경우 해설 보이기
      <div className={styles.quiz}>
        <div className={styles['quiz-a']}>
          <div className={styles['quiz-badge-n']}>
            <div className={styles['quiz-badge2']}>
              <p className={styles['quiz-badge3']}>해설</p>
            </div>
          </div>

          <div className={styles['quiz-b']}>
            <div className={styles.explain}>
              <div className={styles.explain2}>
                <p className={styles.explain3}>{feedback?.correctAnswer} 정답인 이유!</p>
              </div>
              <div className={styles.explain4}>
                <p className={styles.explain5}>{feedback?.explanation}</p>
                {/* <p className={styles.explain6}></p> */}
              </div>
            </div>


            <div className={styles.information}>
              <div className={styles.information2}>
                <p className={styles.information3}>💡 함께 알아두면 좋은 표현</p>
              </div>
              <div className={styles.information4}>
                <p className={styles.information5}>stop -ing : ~하는 것을 멈추다 (He stopped smoking. 그는 담배를 끊었다.)</p>
                <p className={styles.information5}>stop to 동사원형 : ~하기 위해 멈추다 (He stopped to smoke. 그는 담배를 피우려고 멈췄다.)</p>
              </div>
            </div>


            <div className={styles['quiz-s']}>
              <div className={styles['quiz-s2']}>
                <div className={styles['quiz-s3']}>
                  <button
                  onClick={handleNext}
                  className={styles['quiz-s4']}>
                    건너뛰기
                  </button>
                </div>




          {/* 제출 및 넘어가기 버튼 */}
            {/* ox, 빈칸 채우기 전용 */}
            {!isConfirmed ? (
              <button
              onClick={handleSubmit}
              className={styles['quiz-s5']}>
                <p className={styles['quiz-s6']}>
                  정답확인
                </p>
              </button>
            ) : (
              <button
              onClick={handleNext}
              className={styles['quiz-s5']}>
                <p className={styles['quiz-s6']}>
                  계속하기
                </p>
              </button>
            )}

              </div>
            </div>
          </div>
        </div>
      </div>
      ) : (
      <div className={styles.quiz}>
        <div className={styles['quiz-a']}>
          <div className={styles['quiz-badge-n']}>
            <div className={styles['quiz-badge2']}>
              <p className={styles['quiz-badge3']}>Q.<span>{currentIndex + 1}</span></p>
            </div>
          </div>

          <div className={styles['quiz-b']}>
            <div className={styles['quiz-c']}>
              <div className={styles['quiz-d']}>
                <div className={styles['quiz-e']}>
                  <p className={styles['quiz2-q']}>
                    {currentQuiz?.content.split('[ ]').map((part, index, array) => (
                      <React.Fragment key={index}>
                        {part}
                        {/* 마지막 조각이 아니면 빈칸 삽입 */}
                        {index < array.length - 1 && (
                          tempChoice ? (
                            <span className={styles['blank-t']}>{tempChoice}</span>
                          ) : (
                            <span className={styles['blank-n']}></span>
                          )
                        )}
                      </React.Fragment>
                    ))}
                  </p>
                </div>


                <div className={styles.quiz5}>
                  <p className={styles['quiz5-t']}>
                    {currentQuiz?.translation}
                  </p>
                </div>
              </div>


              <div className={styles.quiz7}>

          {/* 보기 버튼 */}
          {currentQuiz?.options.map((choice, i) => (
            <button
            key={i}
            disabled={isConfirmed}
            onClick={() => handleChoice(choice)}
            className={styles['quiz8-bt']}
            style={{
              padding: buttonPositions[i].padding,
              // border: borderColor(choice),
              background: backgroundColor(choice),
              color: textColor(choice),
            }}>
              {choice}
            </button>
          ))}
              </div>
            </div>

            <div className={styles['quiz-s']}>
              <div className={styles['quiz-s2']}>
                <div className={styles['quiz-s3']}>
                  <button
                  onClick={handleNext}
                  className={styles['quiz-s4']}>
                    건너뛰기
                  </button>
                </div>



          {/* 제출 및 넘어가기 버튼 */}
            {/* ox, 빈칸 채우기 전용 */}
            {!isConfirmed ? (
              <button
              onClick={handleSubmit}
              className={styles['quiz-s5']}>
                <p className={styles['quiz-s6']}>
                  정답확인
                </p>
              </button>
            ) : (
              <button
              onClick={handleNext}
              className={styles['quiz-s5']}>
                <p className={styles['quiz-s6']}>
                  계속하기
                </p>
              </button>
            )}

              </div>
            </div>
          </div>
        </div>
      </div>
      )
    )
  }




  // 실제 ox 퀴즈문제 테스트용 오류
  const oxQuiz = () => {
    if (!currentQuiz) return null;
    return (
      <div className={styles.quiz}>
        <div className={styles['quiz-a']}>
          <div className={styles['quiz-badge-n']}>
            <div className={styles['quiz-badge2']}>
              <p className={styles['quiz-badge3']}>Q.<span>{currentIndex + 1}</span></p>
            </div>
          </div>

          <div className={styles['quiz-b']}>
            <div className={styles['quiz-c']}>
              <div className={styles['quiz-d']}>
                <div className={styles['quiz-e']}>
                  <p className={styles['quiz2-q']}>
                    {highlightWord(currentQuiz?.content, currentQuiz?.question?.match(/[a-zA-Z]+/)?.[0])}
                  </p>
                </div>


                <div className={styles.quiz5}>
                  <p className={styles['quiz5-t']}>
                    {currentQuiz?.translation}
                  </p>
                </div>
              </div>


              <div className={styles.ox}>
                {/* O 버튼 */}
                <button 
                disabled={isConfirmed}
                onClick={() => handleChoice('O')}
                className={styles.ox2}
                style={{
                  color: oxColor('O'),
                  background: oxBackgroundColor('O'),

                }}>
                  O
                </button>

                {/* X 버튼 */}
                <button
                disabled={isConfirmed}
                onClick={() => handleChoice('X')}
                className={styles.ox3}
                style={{
                  color: oxColor('X'),
                  background: oxBackgroundColor('X'),
                }}>
                  X
                </button>
              </div>
            </div>

            <div className={styles['quiz-s']}>
              <div className={styles['quiz-s2']}>
                <div className={styles['quiz-s3']}>
                  <button
                  onClick={handleNext}
                  className={styles['quiz-s4']}>
                    건너뛰기
                  </button>
                </div>




          {/* 제출 및 넘어가기 버튼 */}
            {/* ox, 빈칸 채우기 전용 */}
            {!isConfirmed ? (
              <button
              onClick={handleSubmit}
              className={styles['quiz-s5']}>
                <p className={styles['quiz-s6']}>
                  정답확인
                </p>
              </button>
            ) : (
              <button
              onClick={handleNext}
              className={styles['quiz-s5']}>
                <p className={styles['quiz-s6']}>
                  계속하기
                </p>
              </button>
            )}

              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }





  // 실제 매칭 퀴즈문제 테스트용 오류
  const matchingQuiz = () => {
    if (!quizzes?.length || !matchingShuffledMeanings?.length) return null;

    return (
      <div className={styles.quiz}>
        <div className={styles['quiz-a']}>
          <div className={styles['quiz-badge-n']}>
            <div className={styles['quiz-badge2']}>
              <p className={styles['quiz-badge3']}>Q.1</p>
            </div>
          </div>

          <div className={styles['quiz-b']}>
            <div className={styles['quiz-c']}>

              <div className={styles.matching}>
                {/* 매칭 퀴즈 보기 버튼 */}
            {quizzes?.map((quiz, i) => {
  const meaningItem = matchingShuffledMeanings[i]; // { quizId, meaning }
  return (
    <React.Fragment key={quiz.quizId}>
      {/* 영어 단어 */}
      <button
        disabled={matchingMatchedPairs.some(p => p.wordQuizId === quiz.quizId)}
        onClick={() => matchingHandleWordClick(quiz.quizId)}
        className={styles.matching2}
        style={{ ...matchingGetWordStyle(quiz.quizId) }}
      >
        {quiz.question}
      </button>

      {/* 단어 뜻 */}
      <button
        disabled={matchingMatchedPairs.some(p => p.meaningQuizId === meaningItem.quizId)}
        onClick={() => matchingHandleMeaningClick(meaningItem.quizId)}
        className={styles.matching3}
        style={{ ...matchingGetMeaningStyle(meaningItem.quizId) }}
      >
        {meaningItem.meaning}
      </button>
    </React.Fragment>
  );
})}
              </div>
            </div>

            <div className={styles['quiz-s']}>
              <div className={styles['quiz-s2']}>
                <div className={styles['quiz-s3']}>
                  <button
                  onClick={handleNext}
                  className={styles['quiz-s4']}>
                    건너뛰기
                  </button>
                </div>




          {/* 제출 및 넘어가기 버튼 */}
            {/* 매칭 퀴즈는 결과 보기만 나옴 */}
            <button
            onClick={handleMatchingComplete}
            disabled={matchingMatchedPairs.length < quizzes.length}
            className={styles['quiz-s5']}
            >
              <p className={styles['quiz-s6']}>
                결과 보기
              </p>
            </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

















// 실제 퀴즈나가기 테스트용 오류
  const testExit= () => {
    return (
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
          onClick={() => setExitModal(null)}>
            <p className={styles.exit8}>퀴즈 계속하기</p>
          </button>
          <button 
          className={styles.exit9}
          onClick={handleExit}
          >
            <p className={styles.exit10}>저장하고 나가기</p>
          </button>
        </div>
      </div>
    )
  }





// 단어 사전
  const testDictionary= () => {
    return (
      <div 
      // ref 속성으로 popupRef와 이 DOM을 연결
      // 렌더링 후 popupRef.current에 이 <div> 요소가 자동으로 들어감
      ref={popupRef}
      className={styles.popup}
      style={{
        position: 'fixed',
        top: popupPosition.top,
        left: popupPosition.left,
        // 위치 계산 전엔 숨기기
        // 측정 끝나면 visibility: visible로 자연스럽게 보임
        visibility: isPositioned ? 'visible' : 'hidden',
      }}
      >
        <div 
        className={styles.wrapper}
        style={{ 
          // 말풍선 꼬리 동적 위치로 이동
          // ::after 위치에만 적용되게 '--arrow-left' 사용
          '--arrow-left': `${popupPosition.arrowLeft}px`,
        }}
        >
          <div className={styles.popup6}>
            <div className={styles.popup7}>
              <div className={styles.popup8}>
                <button 
                onClick={() => collectWord()}
                className={styles.popup9}>
                  <div className={styles.popup10}>
                    <div className={styles.popup11}>
                      <div className={styles.popup12}>
                        {isCollected ? (
                          // 선택함
                          <svg className={styles.popup13} xmlns="http://www.w3.org/2000/svg" width="9" height="12" viewBox="0 0 9 12" fill="none">
                            <path d="M7.3 0.5H1.7C1.03726 0.5 0.5 1.03726 0.5 1.7V10.5686C0.5 11.2814 1.36171 11.6383 1.86568 11.1343L3.93431 9.06569C4.24673 8.75327 4.75327 8.75327 5.06569 9.06569L7.13431 11.1343C7.63829 11.6383 8.5 11.2814 8.5 10.5686V1.7C8.5 1.03726 7.96274 0.5 7.3 0.5Z" stroke="#BDB4AB"
                            fill="#7C3AED"/>
                          </svg>
                        ) : (
                          // 미선택함
                          <svg className={styles.popup13} xmlns="http://www.w3.org/2000/svg" width="9" height="12" viewBox="0 0 9 12" fill="none">
                            <path d="M7.3 0.5H1.7C1.03726 0.5 0.5 1.03726 0.5 1.7V10.5686C0.5 11.2814 1.36171 11.6383 1.86568 11.1343L3.93431 9.06569C4.24673 8.75327 4.75327 8.75327 5.06569 9.06569L7.13431 11.1343C7.63829 11.6383 8.5 11.2814 8.5 10.5686V1.7C8.5 1.03726 7.96274 0.5 7.3 0.5Z" stroke="#BDB4AB"
                            fill="black"/>
                          </svg>
                        )}
                      </div>
                    </div>
                    <div className={styles.popup14}>
                      <p className={styles.popup15}>단어 수집</p>
                    </div>
                  </div>
                </button>


                <button 
                onClick= {() => closePopup()}
                className={styles.popup16}>
                  <div className={styles.popup17}>
                    <svg className={styles.popup18} xmlns="http://www.w3.org/2000/svg" width="9" height="9" viewBox="0 0 9 9" fill="none">
                      <path fill-rule="evenodd" clip-rule="evenodd" d="M6.62682 0.234233C6.93926 -0.0780235 7.44629 -0.078132 7.75866 0.234233C8.0708 0.546675 8.07001 1.05374 7.75768 1.36607L5.17565 3.94712L7.88561 6.65708C8.1979 6.96951 8.19799 7.47654 7.88561 7.78892C7.57316 8.10098 7.06608 8.10024 6.75378 7.78794L4.04479 5.07896L1.3651 7.75865C1.05277 8.07084 0.546641 8.07074 0.234245 7.75865C-0.0781279 7.44627 -0.0780358 6.94022 0.234245 6.62779L2.91296 3.94712L0.361199 1.39537C0.0490364 1.08305 0.0491826 0.576909 0.361199 0.264507C0.673572 -0.0478668 1.17963 -0.0477746 1.49206 0.264507L4.04382 2.81626L6.62682 0.234233Z" fill="#454440"/>
                    </svg>
                  </div>
                </button>
              </div>


              <div className={styles.popup19}>
                <div className={styles.popup20}>
                  <div className={styles.popup21}>
                    <p className={styles.popup22}>{dictionaryData.word}</p>
                  </div>

                  <div className={styles.popup23}>
                    <div className={styles.popup24}>
                      {/* ?. 옵셔널 체이닝으로 meaningTranslations가 없으면 에러없이 넘어가기  */}
                      {dictionaryData.meanings?.map((meaning, index) => (
                        <div key={index} className={styles.popup25}>
                          <p className={styles.popup26}>{index + 1}. </p>
                          <span className={styles.popup28}>{meaning}</span>
                        </div>
                      ))}
                    </div>
                  </div>
                </div>


                <div className={styles.popup29}>
                  <div className={styles.popup30}>
                    <div className={styles.popup31}>
                      <div className={styles.popup32}>
                        <div className={styles.popup33}>
                          <p className={styles.popup34}>US</p>
                        </div>
                        <div className={styles.popup35}>
                          <div className={styles.popup36}>
                            <svg className={styles.popup37} xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 16 16" fill="none">
                              <path d="M7.3335 4.18521C7.3335 3.54151 6.61195 3.1614 6.08107 3.52543L4.1024 4.88223C4.03581 4.9279 3.95694 4.95234 3.87619 4.95234H2.1335C1.69167 4.95234 1.3335 5.31051 1.3335 5.75234V10.2476C1.3335 10.6894 1.69167 11.0476 2.1335 11.0476H3.87619C3.95694 11.0476 4.03581 11.072 4.1024 11.1177L6.08107 12.4745C6.61195 12.8385 7.3335 12.4584 7.3335 11.8147V4.18521Z" fill="#CCC3B4"/>
                              <path d="M9.69336 5.64001C10.3183 6.26511 10.6693 7.1128 10.6693 7.99668C10.6693 8.88056 10.3183 9.72826 9.69336 10.3533" stroke="#CCC3B4" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                              <path d="M11.4878 3.9967C12.7684 5.05768 13.4878 6.49648 13.4878 7.9967C13.4878 9.49693 12.7684 10.9357 11.4878 11.9967" stroke="#CCC3B4" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
                            </svg>
                          </div>
                        </div>
                      </div>


                      <div className={styles.popup38}>
                        <p className={styles.popup39}>{dictionaryData.phonetic}</p>
                      </div>
                    </div>
                  </div>


                  <div className={styles.popup40}>
                    <p className={styles.popup41}>{dictionaryData.partOfSpeech || ''}</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    )
  }

const navigate = useNavigate();






  return (
    // 사이드패널 고정
    <>
      {testQuiz()}

      {/* 이탈 모달 */}
      {exitModal && (
        testExit()
      )}

      {/* 단어 사전 팝업 */}
      {/* dictionaryData 로 렌더링 기다리기 */}
      {dictionaryData && (
        testDictionary()
      )}

      {/* 최종정산 */}
      {/* <SettlementPage 
        data={settlementData}
        videoId={videoId}
        videoTitle={videoTitle}
        channelName={channelName}
        duration={duration}
        thumbnailUrl={thumbnailUrl}
      /> */}
    </>
  );  // return 끝
}  // 함수 끝
export default QuizPage;