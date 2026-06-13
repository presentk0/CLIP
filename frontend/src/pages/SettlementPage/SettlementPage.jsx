// import happy from '../../imgs/image_703.png';
// import frog2 from '../../imgs/image_750.png';
import { apiFetch } from '../../utils/api';
import { useState, useEffect } from 'react';
import { openYoutubeVideo } from '../../utils/openInTab';
import styles from './SettlementPage.module.css';
import { log } from '../../utils/logger';


function QuizResultCard(data, videoId, videoTitle, channelName, thumbnailUrl, duration) {
  const [wordCount, setWordCount] = useState(0);
  // 내 정보 조회 데이터 관리 (마스터리 뱃지 영상)
  const [usersData, setUsersData] = useState(null);

  // 썸네일 클릭 시 메인 탭에서 유튜브 영상 열기
  const handleClick = (e) => {
    // e.preventDefault() = HTML 요소의 기본 동작(default behavior)을 막는 함수
    // <a> 태그는 클릭하면 href로 이동하는게 기본 동작
    e.preventDefault();
    openYoutubeVideo(videoId);
  };

  useEffect(() => {
    const fetchWordCount = async () => {
      const result = await apiFetch('/words/my-collection');
      setWordCount(result.data.pagination.totalCount);
    };
    fetchWordCount();
  }, []);

  // 컴포넌트 mount 시 내 정보 조회 API 호출
  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await apiFetch('/users/me', { method: 'GET' });
        setUsersData(result.data);
      } catch (error) {
        log.debug('데이터 로딩 실패', error);
      }
    };

    fetchData();
  }, []);

  const nextLevelExp = usersData?.nextLevelExp ?? 0;
  const currentExp = usersData?.exp ?? 0;
  const earnedExp = data?.earnedExp ?? 0;

  const remainingCount = earnedExp > 0 ? Math.ceil((nextLevelExp - currentExp) / earnedExp) : 0;

  return  (
    <div className={styles['quiz-result-card']}>
      <a 
      className={styles['quiz-result-card2']}
      href={`https://youtube.com/watch?v=${videoId}`}
      onClick={handleClick}
      rel="noopener noreferrer"
      >
        <div className={styles['quiz-result-card3']}>
          <img 
          className={styles['quiz-result-thumbnail']}
          src={`https://img.youtube.com/vi/${videoId}/maxresdefault.jpg`}
          alt={`${videoTitle} 영상 썸네일`}
          onError={(e) => {
            e.target.src = `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
          }}
          />
          <div className={styles['quiz-result-card4']}>
            <div className={styles['quiz-result-card5']}>
              <p className={styles['quiz-result-card6']}>{duration}</p>
            </div>
          </div>

          <svg 
          className={styles['quiz-result-card10']}
          xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M0 12C0 5.37258 5.37258 0 12 0C18.6274 0 24 5.37258 24 12C24 18.6274 18.6274 24 12 24C5.37258 24 0 18.6274 0 12Z" fill="#D9D9D9"/>
          </svg>
        </div>

        <div className={styles['quiz-result-card7']}>
          <p className={styles['quiz-result-card8']}>{videoTitle}</p>
          <p className={styles['quiz-result-card9']}>{channelName}</p>
        </div>


      </a>


      <div className={styles['quiz-result-card11']}>
        <div className={styles['quiz-result-card12']}>
          <div className={styles['quiz-result-card14']}>
            <div className={styles['quiz-result-card15']}>
              <div className={styles['quiz-result-card16']}>
                <p className={styles['quiz-result-card17']}>{usersData?.ongoingMastery.currentBadge} 도전 중</p>
              </div>


              <div className={styles['quiz-result-card18']}>
                <p className={styles['quiz-result-card19']}>{usersData?.exp.toLocaleString()}<span>/{usersData?.nextLevelExp.toLocaleString()}</span></p>
              </div>
            </div>


            <div className={styles['quiz-result-card20']}>
              <div className={styles['quiz-result-card21']}></div>
              <div 
              className={styles['quiz-result-card22']}
              style={{ width: `${usersData?.progressPercentage}%` }}></div>
            </div>
          </div>


          <div className={styles['quiz-result-card23']}>
            <p className={styles['quiz-result-card24']}>{remainingCount} 번 더 시청하면 {data?.newBadge?.badgeType}에요!</p>
          </div>
        </div>


        <div className={styles['quiz-result-card25']}>
          <div className={styles['quiz-result-card26']}>
            <div className={styles['quiz-result-card27']}>
              <div className={styles['quiz-result-card28']}></div>
              <div className={styles['quiz-result-card29']}>
                <p className={styles['quiz-result-card30']}>퀴즈 정답수</p>
                <p className={styles['quiz-result-card31']}>{data?.correctCount}<span>/{data?.totalQuizCount}</span></p>
              </div>
            </div>
          </div>


          <div className={styles['quiz-result-card26']}>
            <div className={styles['quiz-result-card27']}>
              <div className={styles['quiz-result-card28']}></div>
              <div className={styles['quiz-result-card29']}>
                <p className={styles['quiz-result-card30']}>단어 수집</p>
                <p className={styles['quiz-result-card31']}>{wordCount}개</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}





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
        <div className={styles['recommendation-card4']}></div>
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





function MessageBox() {
  // 성장지표 관리
  const [growthData, setGrowthData] = useState(null);
  const { thisWeek = 0, lastWeek = 0 } = growthData?.weeklyAccuracy ?? {};
  const diff = thisWeek - lastWeek;

  let message;
  if (diff > 0) {
    message = `이번 주 ${lastWeek}%→${thisWeek}% 올랐어요`;
  } else if (diff < 0) {
    message = `이번 주 ${lastWeek}%→${thisWeek}% 떨어졌어요`;
  } else {
    message = `이번 주도 ${thisWeek}% 유지하고 있어요`;
  }

  // 컴포넌트 mount 시 성장지표 API 호출
  useEffect(() => {
    const fetchData = async () => {
      try {
        const growth = await apiFetch('/users/me/growth', { method: 'GET' });
        setGrowthData(growth);
      } catch (error) {
        log.debug('성장지표 데이터 로딩 실패:', error);
      }
    };
    fetchData();
  }, []);

  return (
    <div className={styles.messageWrapper}>
      <svg 
      className={styles.message}
      xmlns="http://www.w3.org/2000/svg" 
      width="402" 
      height="108" 
      viewBox="0 0 402 108" 
      fill="none">
        <g filter="url(#filter0_d_1_978)">
          <path d="M-29 32C-29 18.7452 -18.2548 8 -5 8H407C420.255 8 431 18.7452 431 32V68C431 81.2548 420.255 92 407 92H-5C-18.2548 92 -29 81.2548 -29 68V32Z" fill="#70C1EE"/>
        </g>
        <defs>
          <filter id="filter0_d_1_978" x="-41" y="0" width="484" height="108" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
            <feFlood flood-opacity="0" result="BackgroundImageFix"/>
            <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
            <feOffset dy="4"/>
            <feGaussianBlur stdDeviation="6"/>
            <feComposite in2="hardAlpha" operator="out"/>
            <feColorMatrix type="matrix" values="0 0 0 0 0.619608 0 0 0 0 0.607843 0 0 0 0 0.537255 0 0 0 0.2 0"/>
            <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow_1_978"/>
            <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow_1_978" result="shape"/>
          </filter>
        </defs>
      </svg>

      {/* SVG 위에 겹치는 용도 추가 */}
      <div className={styles.messageContent}>
        {/* 아이콘 */}
        <div className={styles.message2}></div>

        {/* 텍스트 묶음 */}
        <div className={styles.messageText}>
          <p className={styles.message3}>알림!</p>
          <p className={styles.message4}>정확도 {thisWeek}% 달성! {message}</p>
        </div>
      </div>
    </div>
  );
}



// // 오류 다시 주석 넣기
// function SettlementPage({ data, videoId, videoTitle, channelName, duration, onExitPage, thumbnailUrl }) {
function SettlementPage({ data, videoId, videoTitle, channelName, thumbnailUrl, duration }) {
  // // 영상 전체 길이를 00:00:00 형태로 바꿔야 함(오류)
  // // 해당 영상의 썸네일 URL (480x360)
  // const thumbnailUrl = `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;


  

  return (
    <div className={styles['main-box']}>
      <p className={styles['mastery']}>
        마스터리 진행률
      </p>
      {QuizResultCard(data, videoId, videoTitle, channelName, thumbnailUrl, duration)}
      {MessageBox()}

      {Recommendation()}

    </div>
  );
}
export default SettlementPage;