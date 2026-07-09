import { apiFetch } from "../../utils/api";
import styles from './VocabularyPage.module.css';
import { useEffect, useState } from "react";
import { log } from "../../utils/logger";
import { TestSpinner } from "../../components/Spinner/Spinner";
import { useNavigate } from "react-router-dom";
import { useRef } from "react";
import { useMemo } from "react";

// 사진 순서대로
// 단어 10개로 끊어서 조회 - 가장 최근에 수집한 단어부터 순서대로 출력
// ?sort=lastest(따로 설정 안해도 기존 lastest로 설정되어있음) - 가장 최근에 수집한 단어부터 순서대로 출력
// ?sort=oldest(수집한지 오래된 단어부터 출력)
// ?sort=alphabet-asc(알파벳 A-Z 순으로 출력)
// ?sort=alphabet-desc(알파벳 Z-A 순으로 출력)
// ?filter=today (오늘 수집한 단어만), 테스트 상에는 오늘 수집한 단어 데이터가 없기 때문에 빈값 출력
// ?keyword=app (app가 들어간 단어 검색, 수집한 단어중 app가 포함된 appreciate가 출력되는것을 볼 수 있음, 검색창에 영단어 검색할때 사용)
// ?keyword=예약 (예약하다의 뜻을가진 book 출력, 검색창에 단어 뜻 검색해서 찾기 할 때 사용)

function SearchBar({ onSearch }) {
  const [input, setInput] = useState('');
  const isFirst = useRef(true);

  const handleChange = (e) => {
    // 영문, 숫자, 한글, 공백만 허용 (자동 필터링)
    const cleaned = e.target.value.replace(/[^a-zA-Z0-9가-힣\s]/g, '');
    setInput(cleaned);
  };

  useEffect(() => {
    // 첫 마운트 시 스킵 (초기 API 호출 방지)
    if (isFirst.current) {
      isFirst.current = false;
      return;
    }

    // 500ms 후에 검색 실행
    const timer = setTimeout(() => {
      onSearch(input);
    }, 500);

    // 다음 입력 시 타이머 취소
    return () => clearTimeout(timer);
  }, [input, onSearch]);

  return (
    <input
      className={styles.voca20}
      type="text"
      value={input}
      onChange={handleChange}
      placeholder="단어, 뜻, 태그 검색"
    />
  );
}


export function VocabularyPage() {
  // 전체 데이터 (초기 1회 로드)
  const [allWords, setAllWords] = useState([]);
  // 오늘 수집 데이터 (초기 1회 로드)
  const [todayWords, setTodayWords] = useState([]);


  // 화면 표시용
  const [selectedType, setSelectedType] = useState('최신 순');
  // 드롭다운 용
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  // 버튼 구분
  const [mode, setMode] = useState('전체');
  // api 전송용
  const [sort, setSort] = useState('latest');
  // 검색어
  const [keyword, setKeyword] = useState('');
  // 검색 결과 (null이면 검색 안 함)
  const [searchResults, setSearchResults] = useState([]);

  // 오늘 수집한 단어 없을 때 무한 로딩 방지
  const [loading, setLoading] = useState(true);

  const navigate = useNavigate();

  // 초기 1회 (전체 + 오늘 로드)
  useEffect(() => {
    const loadAll = async () => {
      setLoading(true);
      try {
        // 전체 (페이지 순회)
        let all = [];
        let page = 0;
        while (true) {
          const res = await apiFetch(`/words/my-collection?page=${page}&size=10`);
          const w = res?.data?.words || [];
          all = [...all, ...w];
          if (w.length < 10) break;
          page++;
          if (page > 10) break;
        }
        setAllWords(all);

        // 오늘
        const todayRes = await apiFetch('/words/my-collection?filter=today');
        setTodayWords(todayRes?.data?.words || []);
      } catch (error) {
        log.error('로드 실패', error);
      } finally {
        setLoading(false);
      }
    };
    loadAll();
  }, []);

  // 검색어 있을 때만 API 호출 (debounce는 SearchBar에서)
  useEffect(() => {
    const trimmed = keyword.trim();
    if (!trimmed) return;

    const search = async () => {
      try {
        const res = await apiFetch(`/words/my-collection?keyword=${keyword}`);
        setSearchResults(res?.data?.words || []);
      } catch (error) {
        log.error('검색 실패', error);
      }
    };
    search();
  }, [keyword]);

  // 화면에 보여줄 데이터 계산
  const displayWords = useMemo(() => {
    // 검색 결과가 있으면 우선


    const source = keyword.trim()
    ? searchResults
    : (mode === '오늘 수집한 단어' ? todayWords : allWords);

    // 정렬 (원본 안 건드리게 복사)
    const sortFns = {
      'latest':        (a, b) => b.id - a.id,
      'oldest':        (a, b) => a.id - b.id,
      'alphabet-asc':  (a, b) => a.word.localeCompare(b.word),
      'alphabet-desc': (a, b) => b.word.localeCompare(a.word),
    };
    
    return [...source].sort(sortFns[sort]);
  }, [allWords, todayWords, searchResults, mode, sort, keyword]);




const handleBack = () => {
  navigate(-1);
};

const handleHomeClick = () => {
  navigate('/');
};



  
  return(
    <div className={styles.voca}>
      {loading && <TestSpinner />}

      <div className={styles.voca2}>
        <div className={styles.voca3}>
          <div className={styles.voca4}>
            <p className={styles.voca5}>단어장</p>

            <div className={styles.voca6}>
              <button 
              onClick={() => {handleBack()}}
              className={styles.voca7}>
                <svg 
                className={styles.voca8}
                xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M16.5014 4.35565C16.9756 4.82987 16.9756 5.59871 16.5014 6.07292L10.0743 12.5L16.5014 18.9271C16.9756 19.4013 16.9756 20.1702 16.5014 20.6444C16.0272 21.1185 15.2584 21.1185 14.7842 20.6444L7.49848 13.3586C7.02427 12.8845 7.02427 12.1156 7.49848 11.6414L14.7842 4.35565C15.2584 3.88145 16.0272 3.88145 16.5014 4.35565Z" fill="white"/>
                </svg>
              </button>

              <button 
              onClick={() => {handleHomeClick()}}
              className={styles.voca9}>
                <svg 
                className={styles.voca10}
                xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M10.125 16.8572V20.0001C10.125 20.5524 9.67728 21.0001 9.125 21.0001H5.5C4.94772 21.0001 4.5 20.5524 4.5 20.0001V10.3485C4.5 9.76471 4.75512 9.21001 5.19842 8.83005L10.6984 4.11576C11.4474 3.47378 12.5526 3.47378 13.3016 4.11576L18.8016 8.83005C19.2449 9.21001 19.5 9.76471 19.5 10.3485V20.0001C19.5 20.5524 19.0523 21.0001 18.5 21.0001H14.875C14.3227 21.0001 13.875 20.5524 13.875 20.0001V16.8572C13.875 16.305 13.4273 15.8572 12.875 15.8572H11.125C10.5727 15.8572 10.125 16.305 10.125 16.8572Z" fill="white"/>
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>


      <div className={styles.voca14}>
        <div className={styles.voca15}>
          <div className={styles.voca16}>
            <div className={styles.voca17}>
              <div className={styles.voca18}>
                <svg 
                className={styles.voca19}
                xmlns="http://www.w3.org/2000/svg" width="17" height="17" viewBox="0 0 17 17" fill="none">
                  <path d="M7.09488 1.77372C4.15609 1.77372 1.77372 4.15609 1.77372 7.09488C1.77372 10.0337 4.15609 12.416 7.09488 12.416C10.0337 12.416 12.416 10.0337 12.416 7.09488C12.416 4.15609 10.0337 1.77372 7.09488 1.77372ZM0 7.09488C0 3.17649 3.17649 0 7.09488 0C11.0133 0 14.1898 3.17649 14.1898 7.09488C14.1898 8.73443 13.6336 10.2441 12.6997 11.4455L16.4069 15.1527C16.7533 15.499 16.7533 16.0606 16.4069 16.4069C16.0606 16.7533 15.499 16.7533 15.1527 16.4069L11.4455 12.6997C10.2441 13.6336 8.73443 14.1898 7.09488 14.1898C3.17649 14.1898 0 11.0133 0 7.09488Z" fill="#01CF8A"/>
                </svg>
              </div>

              <SearchBar onSearch={setKeyword} />
            </div>
          </div>

          <div className={styles.voca22}>
            <div className={styles.voca23}>
              <div className={styles.voca24}>
                <div className={styles.voca25}>
                  <button 
                  onClick={() => setMode('전체')}
                  disabled={mode === '전체'}
                  className={styles.voca26} style={{ background: mode === '전체' ? '#01CF8A' : '#FEFDF9', color: mode === '전체' ? '#FEFDF9' : '#5E5A4E' }}>
                    <div className={styles.voca27}>
                      <p className={styles.voca28}>전체</p>
                    </div>
                  </button>

                  <button 
                  onClick={() => setMode('오늘 수집한 단어')}
                  disabled={mode === '오늘 수집한 단어'}
                  className={styles.voca29} style={{ background: mode === '오늘 수집한 단어' ? '#01CF8A' : '#FEFDF9', color: mode === '오늘 수집한 단어' ? '#FEFDF9' : '#5E5A4E' }}>
                    <div className={styles.voca30}>
                      <p className={styles.voca31}>오늘 수집한 단어</p>
                    </div>
                  </button>
                </div>

                <div className={styles.filterWrapper}>
                <button
                onClick={() => setIsFilterOpen(!isFilterOpen)}
                className={styles.voca32}>
                  <div className={styles.voca33}>
                    <p className={styles.voca34}>{selectedType}</p>
                  </div>

                  <div className={styles.voca35}>
                    <svg 
                    className={styles.voca36}
                    xmlns="http://www.w3.org/2000/svg" width="11" height="6" viewBox="0 0 11 6" fill="none">
                      <path d="M0.219651 1.28064C-0.0732217 0.987757 -0.0732123 0.512541 0.219651 0.219652C0.512545 -0.0732279 0.987744 -0.0732055 1.28065 0.219652L5.25018 4.18976L9.22031 0.219652C9.51319 -0.0729823 9.98788 -0.0730714 10.2807 0.219652C10.5735 0.512543 10.5735 0.987749 10.2807 1.28064L5.78037 5.78034C5.48754 6.07321 5.01287 6.07323 4.71998 5.78034L0.219651 1.28064Z" fill="#454440"/>
                    </svg>
                  </div>
                </button>

                {isFilterOpen && (
                  <Filter
                    onSelect={(value, label) => {
                    setSort(value);           // API용 value 저장 ('latest')
                    setSelectedType(label);   // 화면 표시용 label 저장 ('최신 순')
                    setIsFilterOpen(false);
                  }}
                />
              )}
                </div>

              </div>
            </div>
          </div>
        </div>

        <div className={styles.voca37}>
          {!loading && (
            displayWords?.length
              ? <VocaCard words={displayWords} />
              : ''
          )}
        </div>
      </div>
    </div>
  );
}

function VocaCard ({ words }) {
  return (
    <>
      {words.map((w, index) => (
        <div key={index} className={styles.card}>
          <div className={styles.card2}>
            <div className={styles.card3}>
              <div className={styles.card4}>
                <div className={styles.card5}>
                  <p className={styles.card6}>{w.word}</p>
                </div>

                <div className={styles.card7}>
                  {w.meanings.map((m, i) => (
                    <p className={styles.card8}>{i}. {m}</p>
                  ))}
                </div>
              </div>
            </div>
          </div>
        </div>
      ))}
    </>

  );
}

function Filter ({ onSelect }) {
  const types = [
    { value: 'latest', label: '최신 순' },
    { value: 'oldest', label: '오래된 순' },
    { value: 'alphabet-asc', label: '알파벳 순(A-Z)' },
    { value: 'alphabet-desc', label: '알파벳 순(Z-A)' },
  ];

  return (
    <div className={styles.filter}>
      <div className={styles.filter2}>
        {types.map((t) => (

          <button 
            key={t.value}
            className={styles.filter3}
            onClick={() => onSelect(t.value, t.label)}  // value와 label 둘 다 전달
          >
            <p className={styles.filter4}>{t.label}</p>
          </button>
        ))}
      </div>
    </div>
  );
}