import { apiFetch } from "../../utils/api";
import styles from './VocabularyPage.module.css';
import { useEffect, useState } from "react";
import { log } from "../../utils/logger";

// 사진 순서대로
// 단어 10개로 끊어서 조회 - 가장 최근에 수집한 단어부터 순서대로 출력
// ?sort=lastest(따로 설정 안해도 기존 lastest로 설정되어있음) - 가장 최근에 수집한 단어부터 순서대로 출력
// ?sort=oldest(수집한지 오래된 단어부터 출력)
// ?sort=alphabet-asc(알파벳 A-Z 순으로 출력)
// ?sort=alphabet-desc(알파벳 Z-A 순으로 출력)
// ?filter=today (오늘 수집한 단어만), 테스트 상에는 오늘 수집한 단어 데이터가 없기 때문에 빈값 출력
// ?keyword=app (app가 들어간 단어 검색, 수집한 단어중 app가 포함된 appreciate가 출력되는것을 볼 수 있음, 검색창에 영단어 검색할때 사용)
// ?keyword=예약 (예약하다의 뜻을가진 book 출력, 검색창에 단어 뜻 검색해서 찾기 할 때 사용)


export function VocabularyPage() {
  const [words, setWords] = useState([]);
  const [selectedType, setSelectedType] = useState('최신 순');
  const [isFilterOpen, setIsFilterOpen] = useState(false);

  useEffect(() => {
    const load = async () => {
      try {
        // 전체 순회로 모든 단어 로드
        let all = [];
        let page = 0;
        while (true) {
          const res = await apiFetch(
            `/words/my-collection?page=${page}&size=10`
          );
          const w = res?.data?.words || [];
          all = [...all, ...w];
          if (w.length < 10) break;
          page++;
          if (page > 100) break;
        }
        setWords(all);
      } catch (error) {
        log.error('로드 실패', error);
      }
    };
    load();
  }, []);


  return(
    <div className={styles.voca}>
      <div className={styles.voca}>
        <div className={styles.voca}>
          <div className={styles.voca}>
            <p className={styles.voca}>단어장</p>

            <div className={styles.voca}>
              <div className={styles.voca}>
                <svg 
                className={styles.voca}
                xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M16.5014 4.35565C16.9756 4.82987 16.9756 5.59871 16.5014 6.07292L10.0743 12.5L16.5014 18.9271C16.9756 19.4013 16.9756 20.1702 16.5014 20.6444C16.0272 21.1185 15.2584 21.1185 14.7842 20.6444L7.49848 13.3586C7.02427 12.8845 7.02427 12.1156 7.49848 11.6414L14.7842 4.35565C15.2584 3.88145 16.0272 3.88145 16.5014 4.35565Z" fill="white"/>
                </svg>
              </div>

              <div className={styles.voca}>
                <svg 
                className={styles.voca}
                xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                  <path d="M10.125 16.8572V20.0001C10.125 20.5524 9.67728 21.0001 9.125 21.0001H5.5C4.94772 21.0001 4.5 20.5524 4.5 20.0001V10.3485C4.5 9.76471 4.75512 9.21001 5.19842 8.83005L10.6984 4.11576C11.4474 3.47378 12.5526 3.47378 13.3016 4.11576L18.8016 8.83005C19.2449 9.21001 19.5 9.76471 19.5 10.3485V20.0001C19.5 20.5524 19.0523 21.0001 18.5 21.0001H14.875C14.3227 21.0001 13.875 20.5524 13.875 20.0001V16.8572C13.875 16.305 13.4273 15.8572 12.875 15.8572H11.125C10.5727 15.8572 10.125 16.305 10.125 16.8572Z" fill="white"/>
                </svg>
              </div>
            </div>
          </div>

          <div className={styles.voca}>
            <div className={styles.voca}>
              <svg 
              className={styles.voca}
              xmlns="http://www.w3.org/2000/svg" width="15" height="16" viewBox="0 0 15 16" fill="none">
                <path d="M4.2998 5.99948C4.68647 5.99948 4.99993 6.31306 5 6.69966C5 7.08636 4.68651 7.39986 4.2998 7.39986H2C1.66867 7.39986 1.40046 7.66816 1.40039 7.99946V13.9995C1.40039 14.3309 1.66863 14.5991 2 14.5991H13C13.3314 14.5991 13.5996 14.3309 13.5996 13.9995V7.99946C13.5995 7.66816 13.3313 7.39986 13 7.39986H10.7002C10.3135 7.39986 10 7.08636 10 6.69966C10.0001 6.31306 10.3135 5.99948 10.7002 5.99948H13C14.1045 5.99948 14.9999 6.89496 15 7.99946V13.9995C15 15.0352 14.2128 15.8865 13.2041 15.9888L13 15.9995H2L1.7959 15.9888C0.85435 15.8933 0.1062 15.1452 0.0107398 14.2036L0 13.9995V7.99946C7e-05 6.89496 0.89547 5.99948 2 5.99948H4.2998ZM6.6191 0.386198C7.1056 -0.128733 7.8944 -0.128733 8.3809 0.386198L10.8174 2.9653C11.0604 3.22282 11.0606 3.6405 10.8174 3.89791C10.5742 4.1548 10.1796 4.15491 9.9365 3.89791L8.1826 1.82565L8.2656 10.3408C8.2655 10.7048 7.9865 11 7.6426 11C7.2989 10.9996 7.0206 10.7046 7.0205 10.3408V1.82565L5.06348 3.89791C4.82036 4.15484 4.42577 4.15478 4.18262 3.89791C3.93947 3.64049 3.93962 3.22281 4.18262 2.9653L6.6191 0.386198Z" fill="white"/>
              </svg>
            </div>
          </div>
        </div>
      </div>


      <div className={styles.voca}>
        <div className={styles.voca}>
          <div className={styles.voca}>
            <div className={styles.voca}>
              <div className={styles.voca}>
                <svg 
                className={styles.voca}
                xmlns="http://www.w3.org/2000/svg" width="17" height="17" viewBox="0 0 17 17" fill="none">
                  <path d="M7.09488 1.77372C4.15609 1.77372 1.77372 4.15609 1.77372 7.09488C1.77372 10.0337 4.15609 12.416 7.09488 12.416C10.0337 12.416 12.416 10.0337 12.416 7.09488C12.416 4.15609 10.0337 1.77372 7.09488 1.77372ZM0 7.09488C0 3.17649 3.17649 0 7.09488 0C11.0133 0 14.1898 3.17649 14.1898 7.09488C14.1898 8.73443 13.6336 10.2441 12.6997 11.4455L16.4069 15.1527C16.7533 15.499 16.7533 16.0606 16.4069 16.4069C16.0606 16.7533 15.499 16.7533 15.1527 16.4069L11.4455 12.6997C10.2441 13.6336 8.73443 14.1898 7.09488 14.1898C3.17649 14.1898 0 11.0133 0 7.09488Z" fill="#01CF8A"/>
                </svg>
              </div>

              <div className={styles.voca}>
                <p className={styles.voca}>단어, 뜻, 태그 검색</p>
              </div>
            </div>
          </div>

          <div className={styles.voca}>
            <div className={styles.voca}>
              <div className={styles.voca}>
                <div className={styles.voca}>
                  <div className={styles.voca}>
                    <div className={styles.voca}>
                      <div className={styles.voca}>
                        <p className={styles.voca}>전체</p>
                      </div>
                    </div>
                  </div>

                  <div className={styles.voca}>
                    <div className={styles.voca}>
                      <p className={styles.voca}>오늘 수집한 단어</p>
                    </div>
                  </div>
                </div>

                <div className={styles.voca}>
                  <button 
                  className={styles.voca}
                  onClick={() => setIsFilterOpen(!isFilterOpen)}>
                    <p className={styles.voca}>{selectedType}</p>
                  </button>

                  <div className={styles.voca}>
                    <svg 
                    className={styles.voca}
                    xmlns="http://www.w3.org/2000/svg" width="11" height="6" viewBox="0 0 11 6" fill="none">
                      <path d="M0.219651 1.28064C-0.0732217 0.987757 -0.0732123 0.512541 0.219651 0.219652C0.512545 -0.0732279 0.987744 -0.0732055 1.28065 0.219652L5.25018 4.18976L9.22031 0.219652C9.51319 -0.0729823 9.98788 -0.0730714 10.2807 0.219652C10.5735 0.512543 10.5735 0.987749 10.2807 1.28064L5.78037 5.78034C5.48754 6.07321 5.01287 6.07323 4.71998 5.78034L0.219651 1.28064Z" fill="#454440"/>
                    </svg>
                  </div>
                </div>

                {isFilterOpen && (
                  <Filter 
                  setWords={setWords}
                  words={words}
                  onSelect={(value) => {
                    setSelectedType(value);      // 선택 값 저장
                    setIsFilterOpen(false);       // 필터 닫기
                  }}
                />
              )}
              </div>
            </div>
          </div>
        </div>

        <div className={styles.voca}>
          {<VocaCard />}
        </div>
      </div>
    </div>
  );
}

function VocaCard () {
  return (
    <div className={styles.card}>
      <div className={styles.card}>
        <div className={styles.card}>
          <div className={styles.card}>
            <div className={styles.card}>
              <p className={styles.card}>Pneumonoultramicroscopicsilicovolcanoconiosis</p>
            </div>

            <div className={styles.card}>
              <p className={styles.card}>1. 진폐증</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function Filter ({ setWords, words, onSelect }) {
  const type = [
    {value: '최신 순', sortFn: (a, b) => b.id - a.id},
    {value: '오래된 순', sortFn: (a, b) => a.id - b.id},
    {value: '알파벳 순(A-Z)', sortFn: (a, b) => a.word.localeCompare(b.word)},
    {value: '알파벳 순(Z-A)', sortFn: (a, b) => b.word.localeCompare(a.word)},
  ];

  return (
    <div className={styles.filter}>
      <div className={styles.filter}>
        {type.map((t, index) => (

          <button 
            key={index}
            className={styles.filter}
            onClick={() => {
              const sorted = [...words].sort(t.sortFn);
              setWords(sorted);
              onSelect(t.value);
            }}
          >
            <p className={styles.filter}>{t.value}</p>
          </button>
        ))}
      </div>
    </div>
  );
}