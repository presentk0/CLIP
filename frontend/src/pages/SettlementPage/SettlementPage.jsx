// import happy from '../../imgs/image_703.png';
// import frog2 from '../../imgs/image_750.png';
import { apiFetch } from '../../utils/api';
import { useState, useEffect } from 'react';
import { openYoutubeVideo } from '../../utils/openInTab';
import styles from './SettlementPage.module.css';
import { log } from '../../utils/logger';

function formatDuration (duration) {
  if (duration == null) return '00:00';

  let totalSeconds;

  if (typeof duration === 'number') {
    // 863 같은 숫자 (초 단위)
    totalSeconds = duration;
  } else if (typeof duration === 'string') {
    if (duration.includes(':')) {
      // "07:57" 같이 이미 포맷된 문자열
      return duration;
    }
    // "863" 같은 문자열 숫자
    totalSeconds = parseInt(duration, 10);
  }

  if (isNaN(totalSeconds)) return '00:00';

  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;

  const pad = (n) => String(n).padStart(2, '0');

  return hours > 0
    ? `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`
    : `${pad(minutes)}:${pad(seconds)}`;
};


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
              <p className={styles['quiz-result-card6']}>{formatDuration(duration)}</p>
            </div>
          </div>

          {/* 오류!!!!!! 뱃지를 넣으려면 api쓰는걸로 고쳐야하는데 이거 고치려면 구조바꿔야 하는데 시간 없으니 일단 주석처리 */}
          {/* <svg 
          className={styles['quiz-result-card10']}
          xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
            <path d="M0 12C0 5.37258 5.37258 0 12 0C18.6274 0 24 5.37258 24 12C24 18.6274 18.6274 24 12 24C5.37258 24 0 18.6274 0 12Z" fill="#D9D9D9"/>
          </svg> */}
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
              <div className={styles['quiz-result-card28']}>
                <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 28 28" fill="none">
  <path d="M14 0C15.0881 0 16.1474 0.12515 17.1641 0.360352C17.6639 0.476204 18 0.933159 18 1.44629C18 2.25287 17.1936 2.8155 16.4043 2.64941C15.6287 2.48601 14.8242 2.40039 14 2.40039C7.5935 2.40039 2.40039 7.5935 2.40039 14C2.40039 20.4065 7.5935 25.5996 14 25.5996C20.4065 25.5996 25.5996 20.4065 25.5996 14C25.5996 13.3742 25.566 12.9581 25.4883 12.5791C25.326 11.7885 25.8277 11 26.6348 11C27.1481 11.0001 27.6064 11.3362 27.7432 11.8311C27.932 12.5144 28 12.9689 28 14C28 21.732 21.732 28 14 28C6.26801 28 0 21.732 0 14C0 6.26801 6.26801 0 14 0ZM22.9463 5.07812C23.4554 4.49626 24.34 4.4372 24.9219 4.94629C25.5037 5.45542 25.5628 6.33998 25.0537 6.92188L15.3145 18.0527C14.3518 19.1529 12.6369 19.1441 11.6855 18.0342L7.9375 13.6611C7.43438 13.0742 7.50203 12.1907 8.08887 11.6875C8.67584 11.1844 9.55927 11.252 10.0625 11.8389L13.5107 15.8613L22.9463 5.07812Z" fill="#454440"/>
</svg>
              </div>
              <div className={styles['quiz-result-card29']}>
                <p className={styles['quiz-result-card30']}>퀴즈 정답수</p>
                <p className={styles['quiz-result-card31']}>{data?.correctCount}<span>/{data?.totalQuizCount}</span></p>
              </div>
            </div>
          </div>


          <div className={styles['quiz-result-card26']}>
            <div className={styles['quiz-result-card27']}>
              <div className={styles['quiz-result-card28']}>
                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="28" viewBox="0 0 18 28" fill="none">
  <path d="M16 0H2C0.895431 0 0 0.895431 0 2V24.0357C0 25.4752 1.75253 26.1826 2.75175 25.1463L7.56031 20.1597C8.34682 19.344 9.65318 19.344 10.4397 20.1597L15.2482 25.1463C16.2475 26.1826 18 25.4752 18 24.0357V2C18 0.895431 17.1046 0 16 0Z" fill="#454440"/>
</svg>
              </div>
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