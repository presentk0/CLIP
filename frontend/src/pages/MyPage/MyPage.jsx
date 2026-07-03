import styles from './MyPage.module.css'
import { useState, useEffect } from 'react';
import { apiFetch } from '../../utils/api';
// import { Spinner } from '../../components/Spinner/Spinner';
import { useNavigate } from 'react-router-dom';
// import { useAuth } from '../../hooks/useAuth';
import { log } from '../../utils/logger';
import { TestSpinner } from '../../components/Spinner/Spinner';


// import frog from '../../imgs/frog.png';
// import frog2 from '../../imgs/frog2.png';




  // 오류
  // 영문 → 한글 매핑
const GOAL_LABELS = {
  TRAVEL: '여행',
  BUSINESS: '비즈니스',
  SELF_DEVELOPMENT: '자기계발',
  EXAM: '영어시험',
  DAILY: '일상',
  NONE: '없음'
};

// const GOALS = ['TRAVEL', 'BUSINESS', 'SELF_DEVELOPMENT', 'EXAM', 'DAILY', 'NONE']; // 명세서 기준





const GOALS = [
  { value: 'TRAVEL',           icon: (<svg xmlns="http://www.w3.org/2000/svg" width="50" height="36" viewBox="0 0 50 36" fill="none">
  <g clip-path="url(#clip0_1439_799)">
    <path d="M8.29829 4.45696C6.12902 -3.02 19.82 0.513582 27.8567 3.53537C23.0875 5.42701 18.0133 7.82738 13.7716 10.5877C10.2965 8.13019 8.68494 5.53701 8.29829 4.45696Z" fill="#A8D2F1"/>
    <path d="M4.04914 24.4545C2.19347 13.3459 25.4409 3.71162 37.2965 0.283047C46.5749 -1.7741 61.7813 7.64182 34.2038 20.0831C10.4925 30.7802 4.22097 27.4545 4.04914 24.4545Z" fill="#E7E6EB"/>
    <path d="M33.6402 13.1571L21.5745 18.9938C20.8584 19.3402 20.3373 19.9961 20.202 20.7785C19.4913 24.8905 19.4406 29.4984 19.6408 32.8853C19.7821 35.277 22.1511 36.6113 24.1022 35.2146C30.4564 30.6661 34.4165 21.6401 36.3599 15.3896C36.8692 13.7516 35.187 12.4089 33.6402 13.1571Z" fill="#A8D2F1"/>
    <path d="M19.8018 14.9131C19.3386 13.8752 19.8065 12.6592 20.8468 12.1971L21.7886 11.7787C22.8288 11.3166 24.0477 11.7834 24.5108 12.8213C24.974 13.8592 24.5061 15.0752 23.4659 15.5373L22.524 15.9556C21.4837 16.4178 20.265 15.951 19.8018 14.9131Z" fill="#3D91B3"/>
    <path d="M26.4327 11.6853C25.9695 10.6474 26.4374 9.4314 27.4776 8.96926L28.4194 8.55094C29.4597 8.0888 30.6785 8.55557 31.1417 9.5935C31.6049 10.6314 31.137 11.8474 30.0967 12.3095L29.1549 12.7279C28.1146 13.19 26.8958 12.7232 26.4327 11.6853Z" fill="#3D91B3"/>
    <path d="M35.1954 7.57104C34.7322 6.53311 35.2001 5.31713 36.2403 4.85501L41.8911 2.34487C42.9314 1.88276 44.1502 2.34954 44.6134 3.38744C45.0765 4.42535 44.6087 5.64134 43.5684 6.10347L37.9176 8.6136C36.8773 9.07568 35.6585 8.60892 35.1954 7.57104Z" fill="#3D91B3"/>
    <path d="M4.53403 21.8925L3.94445 23.6572L0.270137 15.2256C-0.948645 12.4288 2.20444 9.75113 4.77692 11.3984L11.0164 15.3936C12.0623 16.0634 11.9292 17.6281 10.7851 18.1124L6.26021 20.0277C5.4433 20.3734 4.81463 21.0526 4.53403 21.8925Z" fill="#3D91B3"/>
  </g>
  <defs>
    <clipPath id="clip0_1439_799">
      <rect width="50" height="36" fill="white"/>
    </clipPath>
  </defs>
</svg>) },
  { value: 'BUSINESS',         icon: (<svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 36 36" fill="none">
  <path d="M0 12.5582C0 9.78391 2.27545 7.53491 5.08235 7.53491H30.9176C33.7245 7.53491 36 9.78391 36 12.5582V30.9768C36 33.751 33.7245 36 30.9176 36H5.08235C2.27545 36 0 33.751 0 30.9768V12.5582Z" fill="#CBDDF1"/>
  <path d="M0 12.5582C0 9.78391 2.27545 7.53491 5.08235 7.53491H30.9176C33.7245 7.53491 36 9.78391 36 12.5582V17.5814C36 21.2805 32.9661 24.2791 29.2235 24.2791H6.77647C3.03393 24.2791 0 21.2805 0 17.5814V12.5582Z" fill="#76B9F0"/>
  <path d="M14.4004 23.0233C14.4004 21.6362 15.5381 20.5117 16.9416 20.5117H18.6357C20.0391 20.5117 21.1769 21.6362 21.1769 23.0233V25.535C21.1769 26.9221 20.0391 28.0466 18.6357 28.0466H16.9416C15.5381 28.0466 14.4004 26.9221 14.4004 25.535V23.0233Z" fill="#3D91B3"/>
  <path d="M21.6004 0C24.4073 0 26.6828 2.24899 26.6828 5.02326V7.53488H22.871V5.44186C22.871 4.51712 22.1125 3.76744 21.1769 3.76744H14.4004C13.4648 3.76744 12.7063 4.51712 12.7063 5.44186V7.53488H8.89453V5.02326C8.89453 2.24899 11.17 1.2136e-07 13.9769 0H21.6004Z" fill="#3D91B3"/>
</svg>) },
  { value: 'SELF_DEVELOPMENT', icon: (<svg xmlns="http://www.w3.org/2000/svg" width="34" height="34" viewBox="0 0 34 34" fill="none">
  <path d="M12.4412 7.18685C12.9005 6.19781 14.2995 6.19781 14.7588 7.18685L16.9097 11.8195C17.9258 14.0083 19.676 15.7676 21.8533 16.7891L26.4621 18.9511C27.446 19.4127 27.446 20.8191 26.4621 21.2808L21.8533 23.4429C19.676 24.4643 17.9258 26.2236 16.9097 28.4123L14.7588 33.0451C14.2995 34.0341 12.9005 34.0341 12.4412 33.0451L10.2903 28.4123C9.27416 26.2236 7.52397 24.4643 5.34666 23.4429L0.737949 21.2808C-0.245983 20.8191 -0.245983 19.4127 0.737949 18.9511L5.34666 16.7891C7.52402 15.7676 9.2742 14.0083 10.2903 11.8195L12.4412 7.18685Z" fill="#FFC229"/>
  <path d="M26.578 0.370897C26.8076 -0.123633 27.5072 -0.123632 27.7368 0.370898L28.8122 2.68725C29.3203 3.78161 30.1954 4.66127 31.2841 5.17202L33.5885 6.25306C34.0804 6.48383 34.0804 7.18706 33.5885 7.41782L31.2841 8.49887C30.1954 9.00961 29.3203 9.88929 28.8122 10.9836L27.7368 13.3C27.5072 13.7945 26.8076 13.7945 26.578 13.3L25.5026 10.9836C24.9945 9.88929 24.1194 9.00961 23.0307 8.49887L20.7264 7.41782C20.2344 7.18706 20.2344 6.48383 20.7264 6.25306L23.0307 5.17202C24.1194 4.66127 24.9945 3.78161 25.5026 2.68725L26.578 0.370897Z" fill="#FFD26C"/>
</svg>) },
  { value: 'EXAM',             icon: (<svg xmlns="http://www.w3.org/2000/svg" width="28" height="36" viewBox="0 0 28 36" fill="none">
  <path d="M28 10.5V32.25C28 34.321 26.3059 36 24.2162 36H3.78378C1.69406 36 0 34.321 0 32.25V3.75C3.90048e-07 1.67893 1.69406 0 3.78378 0H17.4054L28 10.5Z" fill="#CBDDF1"/>
  <path d="M19.7161 10.6466L27.9999 10.5L17.4053 0V8.39696C17.4053 9.65531 18.4466 10.6691 19.7161 10.6466Z" fill="#70C1EE"/>
  <path d="M16.6491 13.5H6.05453C5.21864 13.5 4.54102 14.1716 4.54102 15C4.54102 15.8284 5.21864 16.5 6.05453 16.5H16.6491C17.485 16.5 18.1626 15.8284 18.1626 15C18.1626 14.1716 17.485 13.5 16.6491 13.5Z" fill="#3D91B3"/>
  <path d="M16.6491 18.75H6.05453C5.21864 18.75 4.54102 19.4216 4.54102 20.25C4.54102 21.0784 5.21864 21.75 6.05453 21.75H16.6491C17.485 21.75 18.1626 21.0784 18.1626 20.25C18.1626 19.4216 17.485 18.75 16.6491 18.75Z" fill="#3D91B3"/>
  <path d="M12.1086 24H6.05453C5.21864 24 4.54102 24.6716 4.54102 25.5C4.54102 26.3284 5.21864 27 6.05453 27H12.1086C12.9445 27 13.6221 26.3284 13.6221 25.5C13.6221 24.6716 12.9445 24 12.1086 24Z" fill="#3D91B3"/>
</svg>) },
  { value: 'DAILY',            icon: (<svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 36 36" fill="none">
  <path d="M17.9999 27.6922C23.3528 27.6922 27.6922 23.3528 27.6922 17.9999C27.6922 12.647 23.3528 8.30762 17.9999 8.30762C12.647 8.30762 8.30762 12.647 8.30762 17.9999C8.30762 23.3528 12.647 27.6922 17.9999 27.6922Z" fill="#FFD26C"/>
  <path d="M16.2695 4.15385V1.73077C16.2695 0.774893 17.0444 0 18.0003 0C18.9562 0 19.7311 0.774893 19.7311 1.73077V4.15385C19.7311 5.10972 18.9562 5.88462 18.0003 5.88462C17.0444 5.88462 16.2695 5.10972 16.2695 4.15385Z" fill="#FFC229"/>
  <path d="M16.2695 34.2693V31.8462C16.2695 30.8904 17.0444 30.1155 18.0003 30.1155C18.9562 30.1155 19.7311 30.8904 19.7311 31.8462V34.2693C19.7311 35.2251 18.9562 36.0001 18.0003 36.0001C17.0444 36.0001 16.2695 35.2251 16.2695 34.2693Z" fill="#FFC229"/>
  <path d="M4.15385 16.2693C5.10972 16.2693 5.88462 17.0442 5.88462 18.0001C5.88462 18.9559 5.10972 19.7308 4.15385 19.7308H1.73077C0.774893 19.7308 0 18.9559 0 18.0001C0 17.0442 0.774893 16.2693 1.73077 16.2693H4.15385Z" fill="#FFC229"/>
  <path d="M34.2691 16.2693C35.2248 16.2693 35.9998 17.0442 35.9998 18.0001C35.9998 18.9559 35.2248 19.7308 34.2691 19.7308H31.846C30.8901 19.7308 30.1152 18.9559 30.1152 18.0001C30.1152 17.0442 30.8901 16.2693 31.846 16.2693H34.2691Z" fill="#FFC229"/>
  <path d="M4.90125 5.46884C5.51318 4.73461 6.60457 4.63551 7.33886 5.24744L9.41579 6.97821C10.15 7.59014 10.2491 8.68153 9.63719 9.41582C9.02525 10.15 7.93387 10.2492 7.19957 9.63722L5.12265 7.90645C4.38842 7.29452 4.28932 6.20313 4.90125 5.46884Z" fill="#FFC229"/>
  <path d="M26.3632 26.5843C26.9751 25.8501 28.0665 25.751 28.8008 26.3629L30.8777 28.0937C31.6119 28.7056 31.711 29.797 31.0991 30.5313C30.4872 31.2655 29.3958 31.3646 28.6615 30.7527L26.5846 29.0219C25.8503 28.41 25.7512 27.3186 26.3632 26.5843Z" fill="#FFC229"/>
  <path d="M7.19957 26.3629C7.93387 25.751 9.02525 25.8501 9.63719 26.5843C10.2491 27.3186 10.15 28.41 9.41579 29.0219L7.33886 30.7527C6.60457 31.3646 5.51318 31.2655 4.90125 30.5313C4.28932 29.797 4.38842 28.7056 5.12265 28.0937L7.19957 26.3629Z" fill="#FFC229"/>
  <path d="M28.6615 5.24744C29.3958 4.63551 30.4872 4.73461 31.0991 5.46884C31.711 6.20313 31.6119 7.29452 30.8777 7.90645L28.8008 9.63722C28.0665 10.2492 26.9751 10.15 26.3632 9.41582C25.7512 8.68153 25.8503 7.59014 26.5846 6.97821L28.6615 5.24744Z" fill="#FFC229"/>
</svg>) },
  { value: 'NONE',             icon: (<svg xmlns="http://www.w3.org/2000/svg" width="29" height="32" viewBox="0 0 29 32" fill="none">
  <path d="M22.6106 1.145C23.8713 -0.274811 26.0289 -0.390066 27.4292 0.888242C28.8295 2.16668 28.9424 4.35436 27.6816 5.77423L18.8698 15.6974L27.635 25.5682C28.8956 26.9879 28.7826 29.1748 27.3826 30.4533C25.9823 31.7317 23.8248 31.6172 22.564 30.1974L14.2786 20.8671L5.99407 30.1974C4.73324 31.6172 2.57573 31.7317 1.17545 30.4533C-0.224494 29.1748 -0.337519 26.9879 0.92306 25.5682L9.68736 15.6974L0.876421 5.77423C-0.384402 4.35436 -0.27148 2.16668 1.12881 0.888242C2.52909 -0.390107 4.68663 -0.274826 5.94742 1.145L14.2786 10.5268L22.6106 1.145Z" fill="#FA5757"/>
</svg>) },
];











// 상대 난이도
const DIFFICULTY_OPTIONS = [
  { value: 'CURRENT', label: '지금이 좋아요' },
  { value: 'RELAXED', label: '여유롭게 갈게요' },
  { value: 'EASIER', label: '지금 보다 가볍게 갈게요' },
  { value: 'HARDER', label: '한 발 더 내딛어볼게요' },
  { value: 'MAX', label: '전력으로 해볼게요' },
];


// 상대 난이도 레벨 조절용
const DIFFICULTY_LEVEL_MAP = {
  EASIER: -2,
  RELAXED: -1,
  CURRENT: 0,
  HARDER: 1,
  MAX: 2,
};


// // 절대 난이도
// const ABSOLUTE_OPTIONS = [
//   { value: 'BEGINNER', label: '초보자' },
//   { value: 'INTERMEDIATE', label: '중급자' },
//   { value: 'ADVANCED', label: '상급자' },
// ];

// // 절대 난이도 레벨
// const ABSOLUTE_LEVEL_MAP = {
//   BEGINNER: 1,
//   INTERMEDIATE: 2,
//   ADVANCED: 3,
// };


// const MIN_LEVEL = 1;
// const MAX_LEVEL = 3;












function CheckIcon({ className, fill = "#A0A08A" }) {
  return (
    <svg 
      className={className}
      xmlns="http://www.w3.org/2000/svg" 
      width="16" 
      height="16" 
      viewBox="0 0 16 16" 
      fill="none"
    >
      <path 
        d="M12.1369 3.77577C12.5659 3.30106 13.2974 3.26015 13.7766 3.68593C14.2578 4.11393 14.3019 4.85091 13.8743 5.33241L13.8674 5.34023L7.08803 12.6732L7.08705 12.6723C6.86608 12.9169 6.5527 13.0578 6.22278 13.058C5.88972 13.058 5.57208 12.9152 5.3507 12.6664L1.79499 8.6664C1.36695 8.18486 1.41019 7.44702 1.89167 7.01894C2.37326 6.59109 3.11115 6.63509 3.53915 7.11659L6.22962 10.1439L12.1369 3.7748V3.77577Z" 
        fill={fill}
      />
    </svg>
  );
}




function GrowthChart({ weeklyData = [], growthRate = 0 }) {
  // Y축 라벨 (정확도는 %니까 0~100, 적절한 간격으로)
  const yAxisLabels = [100, 75, 50, 25, 0];
  const maxValue = 100;

  return (
    <div className={styles.chart0}>
      <div className={styles.chart1}>
        <div className={styles.chart2}>
          <div className={styles.chart3}>
            <div className={styles.chart4}>
              {/* Y축 라벨 */}
              <div className={styles.chart5}>
                {yAxisLabels.map((label) => (
                  <div 
                  key={label}
                  className={styles.chart6}>
                    <p className={styles['chart6-T']}>
                      {label}
                    </p>
                  </div>
                ))}
              </div>
            
              {/* 점선 4개 (각 라벨 사이) */}
              <div className={styles.chart7}>
                <div className={styles['chart8-g']}></div>
                <div className={styles['chart8-g']}></div>
                <div className={styles['chart8-g']}></div>
                <div className={styles['chart8-g']}></div>
                <div className={styles['chart8-g']}></div>
              </div>
            </div>
          </div>
        </div>

        {/* 막대 영역 */}
        <div className={styles.chart9}>
          {weeklyData.map((item, index) => {
            const isLast = index === weeklyData.length - 1;
            const heightPercent = (item.accuracy / maxValue) * 100;

            return (
              <div key={item.week} className={styles['chart10-g']}>
                <div
    className={isLast ? styles['chart12-g'] : styles['chart11-g']}
    style={{ height: `${heightPercent * 1.6}px` }}  
  >
                {isLast && item.accuracy > 0 && growthRate !== 0 && (
                  <div className={styles.chart15}>
                    <div className={styles.chart18}>
                      <p className={styles['chart18-t']}>
                        {growthRate >= 0 ? '+' : ''}{growthRate}%p
                      </p>
                    </div>
                    
                    <svg 
                      className={styles.chart16}
                      xmlns="http://www.w3.org/2000/svg" 
                      width="52" 
                      height="36" 
                      viewBox="0 0 52 36" 
                      fill="none"
                    >
                      <path d="M40 0C46.6274 0 52 5.37258 52 12V20C52 26.6274 46.6274 32 40 32H29.4639L27.7324 35C26.9626 36.3333 25.0374 36.3333 24.2676 35L22.5361 32H12C5.37258 32 0 26.6274 0 20V12C0 5.37258 5.37258 3.22129e-08 12 0H40Z" fill="#45A84D"/>
                    </svg>

                  </div>
                )}
                {/* <div 
                  className={isLast ? styles['chart12-g'] : styles['chart11-g']}
                  style={{ height: `${heightPercent * 1.6}px` }}  
                /> */}
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* X축 라벨 */}
      <div className={styles.chart13}>
        {weeklyData.map((item) => (
          <div key={item.week} className={styles.chart14}>
            <p className={styles['chart14-t']}>{item.week}</p>
          </div>
        ))}
      </div>
    </div>
  );
}








export function MyPage ({ onExitPage }) {
  const [dashboardData, setDashboardData] = useState(null);
  // 성장지표 관리
  const [growthData, setGrowthData] = useState(null);



  const [preferencesData, setPreferencesData] = useState(null);





  const [selectedGoal, setSelectedGoal] = useState(null);
  const [selectedDifficulty, setSelectedDifficulty] = useState(null);
  // const [selectedAbsolute, setSelectedAbsolute] = useState(null);



  const [isInfor, setIsInfor] = useState(true);
  const [isGrowth, setIsGrowth] = useState(true);
  const [isObjectives, setIsObjectives] = useState(true);
  const [isDifficulty, setIsDifficulty] = useState(true);
  // const [isAbsoluteLevel, setIsAbsoluteLevel] = useState(true);


  // const { needsOnboarding, completeOnboarding } = useAuth();

  const navigate = useNavigate();


  // 컴포넌트 mount 시 성장지표, 대시보드, 학습 목표 조회 API 호출
  useEffect(() => {
    const fetchAll = async () => {
      try {
        // 병렬 호출

        const [dashboard, growth, preferences] = await Promise.all([
        // const [dashboard, growth] = await Promise.all([
          // apiFetch('/users/me', { method: 'GET' }),
          apiFetch('/users/me/dashboard', { method: 'GET' }),
          apiFetch('/users/me/growth', { method: 'GET' }),

          apiFetch('/users/me/preferences', { method: 'GET' }),
        ]);


        // 대시보드, 성장 데이터
        setDashboardData(dashboard.data);

        setGrowthData(growth.data);

        // preferences는 온보딩 완료한 경우에만 의미 있음
        setPreferencesData(preferences.data);

        // 목표 설정값 업데이트 
        if (preferences.data?.learningGoal) {
          setSelectedGoal(preferences.data.learningGoal);
        }

        // 상대 난이도 설정값 업데이트
        if (preferences.data?.difficultyLevel) {
          setSelectedDifficulty(preferences.data.difficultyLevel);
        }

        // // 절대 난이도 설정값 업데이트
        // if (preferences.data?.difficultyLevel) {
        //   setIsAbsoluteLevel(preferences.data.isAbsoluteLevel);
        // }
      } catch (error) {
        log.debug('데이터 로딩 실패', error);
      }
    };

    fetchAll();
  }, []);

  // 데이터 준비 안 됐으면 일찍 return
  if (!dashboardData || !growthData) {
    return (
      <TestSpinner />
    );
  }



  const { levelInfo, expLogs } = dashboardData;
  const { totalWords, conqueredVideos } = dashboardData.stats;

  // 소수점 첫 자리만 남기기
  const progress = 100 - levelInfo.progressPercentage;
  // Math.floor는 정수 단위로만 처리해서 10을 곱한 뒤에 나누기
  const display = Math.floor(progress * 10) / 10;

const isDirty = 
  selectedGoal !== preferencesData?.learningGoal ||
  selectedDifficulty !== preferencesData?.difficultyLevel;
  // selectedAbsolute !== preferencesData?.absoluteLevel;






const handleSave = async () => {

  // 둘 다 선택했는지 검증
  if (!selectedGoal || !selectedDifficulty) {
    alert('학습 목표와 상대 난이도를 모두 선택해주세요');
    return;
  }

  try {

    // if (needsOnboarding) {

    //   // 신규 유저 POST onboarding
    //   await apiFetch('/preferences/onboarding', {
    //     method: 'POST',
    //     body: JSON.stringify({ 
    //       learningGoal: selectedGoal, 
    //       difficultyLevel: selectedDifficulty,
    //       absoluteLevel: selectedAbsolute,
    //     }),
    //   });

    //   completeOnboarding();

    //   alert('온보딩 저장 (임시):', { selectedGoal, selectedDifficulty, selectedAbsolute });
    //   // 디폴트 페이지로
    //   navigate('/');

    // } else {

      if (!isDirty) {

        alert('변경된 내용이 없습니다');
        return;
      }

      const res = await apiFetch('/users/me/preferences', {
        method: 'PATCH',
        body: JSON.stringify({  
          learningGoal: selectedGoal, 
          difficultyLevel: selectedDifficulty,
          // absoluteLevel: selectedAbsolute,
          // 서버값 쓰기 (기본 초보자)
          absoluteLevel: preferencesData.absoluteLevel ?? 'BEGINNER',
        }),
      });
      // 새 원본으로 갱신
      setPreferencesData(res.data);  
      alert('저장되었습니다!');
    // }
  } catch (error) {
    log.debug('저장 실패:', error);
  }
};




const handleGoalChange = async (goal) => {
  // 같은 거 또 누르면 무시
  if (goal === selectedGoal) return;


  setSelectedGoal(goal);


};





// 상대 난이도 변경
const handleDifficultyChange = async (level) => {
  if (level === selectedDifficulty) return;


  setSelectedDifficulty(level);

};



// // 절대 난이도 변경
// const handleAbsoluteChange = async (level) => {
//   if (level === selectedAbsolute) return;

//   setSelectedAbsolute(level);
// };


  // // 비활성화 체크 함수
  // const isOptionDisabled = (optionValue) => {
  //   if (!selectedAbsolute) return false;  // 절대 난이도 선택 전엔 다 활성화
    
  //   const currentLevel = ABSOLUTE_LEVEL_MAP[selectedAbsolute];
  //   const offset = DIFFICULTY_LEVEL_MAP[optionValue];
  //   const targetLevel = currentLevel + offset;
    
  //   return targetLevel < MIN_LEVEL || targetLevel > MAX_LEVEL;
  // };


const handleBack = () => {
  navigate(-1);
};

const handleHomeClick = () => {
  onExitPage();
};


  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div className={styles.top}>
          <div className={styles.top2}>
            <div className={styles.top3}>
              <button 
              onClick={handleBack}
              className={styles['top4-l']}
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="10" height="17" viewBox="0 0 10 17" fill="none">
  <path d="M9.35862 0.35565C9.83282 0.82987 9.83282 1.59871 9.35862 2.07292L2.93152 8.5L9.35862 14.9271C9.83282 15.4013 9.83282 16.1702 9.35862 16.6444C8.88442 17.1185 8.11562 17.1185 7.64142 16.6444L0.355657 9.3586C-0.118553 8.8845 -0.118553 8.1156 0.355657 7.6414L7.64142 0.35565C8.11562 -0.11855 8.88442 -0.11855 9.35862 0.35565Z" fill="#454440"/>
</svg>
              </button>
              <button 
              className={styles['top5-r']}
              onClick={handleHomeClick}
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
  <path d="M10.125 16.8572V20.0001C10.125 20.5524 9.67728 21.0001 9.125 21.0001H5.5C4.94772 21.0001 4.5 20.5524 4.5 20.0001V10.3485C4.5 9.76471 4.75512 9.21001 5.19842 8.83005L10.6984 4.11576C11.4474 3.47378 12.5526 3.47378 13.3016 4.11576L18.8016 8.83005C19.2449 9.21001 19.5 9.76471 19.5 10.3485V20.0001C19.5 20.5524 19.0523 21.0001 18.5 21.0001H14.875C14.3227 21.0001 13.875 20.5524 13.875 20.0001V16.8572C13.875 16.305 13.4273 15.8572 12.875 15.8572H11.125C10.5727 15.8572 10.125 16.305 10.125 16.8572Z" fill="#454440"/>
</svg>
              </button>
            </div>

            <div className={styles.top6}>
              <p className={styles.top7}>MY</p>
            </div>

            <button 
            className={styles.top8}
            onClick={handleSave}
            disabled={!isDirty}
            >
              <p className={styles.top9}>저장하기</p>
            </button>
          </div>
        </div>




      <div className={styles.today}>
        <p className={styles['today-t']}>Today</p>

<div className={styles['week-list']}>

{dashboardData.weeklyAttendance?.days.map((dayData) => {
    const { dayOfWeek, attended, today } = dayData;

    return (
      <div 
        key={dayOfWeek}
        className={today ? styles['current-date'] : styles.week}
      >
        <p className={today ? styles['current-date-t'] : styles['week-t']}>
          {dayOfWeek}
        </p>
        <div className={styles['week-attendance']}>
          {attended && (
            <svg 
            className={styles.check}
            xmlns="http://www.w3.org/2000/svg" 
            width="16" 
            height="16" 
            viewBox="0 0 16 16" 
            fill="none">
              <path d="M12.1369 3.77577C12.5659 3.30106 13.2974 3.26015 13.7766 3.68593C14.2578 4.11393 14.3019 4.85091 13.8743 5.33241L13.8674 5.34023L7.08803 12.6732L7.08705 12.6723C6.86608 12.9169 6.5527 13.0578 6.22278 13.058C5.88972 13.058 5.57208 12.9152 5.3507 12.6664L1.79499 8.6664C1.36695 8.18486 1.41019 7.44702 1.89167 7.01894C2.37326 6.59109 3.11115 6.63509 3.53915 7.11659L6.22962 10.1439L12.1369 3.7748V3.77577Z" fill="#A0A08A"/>
            </svg>
          )}
        </div>
      </div>
    );
  })}

        </div>
      </div>
      </div>




      {isInfor ? (
      // {/* 레벨 카드 */}
      <div className={styles['infor-card']}>
        {/* 레벨 박스 */}
        <div className={styles['infor-card2']}>

          {/* 레벨과 열고 닫기 */}
          <div className={styles['infor-card3']}>

            <div className={styles['infor-card4']}>
              <p className={styles['infor-card5']}>Level</p>
            </div>

            {/* 열고 닫기 버튼 */}
            <button
            onClick={() => setIsInfor(false)}
            className={styles['infor-card6']}>
              <svg 
              className={styles['infor-card7']}
              xmlns="http://www.w3.org/2000/svg" 
              width="13" 
              height="8" 
              viewBox="0 0 13 8" 
              fill="none">
                <path d="M0.75 6.68565L6.375 1.06065L12 6.68565" stroke="#0F0D0E" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
          </div>




          {/* 경험치 바 */}
          <div className={styles['infor-card8']}>
            <div className={styles['infor-card9']}>
              <div className={styles['infor-card10']}>
                <div className={styles['infor-card11']}>



                  <div className={styles['infor-card12']}>
                    <p className={styles['infor-card13']}>
                      LV.{levelInfo.currentLevel}
                      </p>
                  </div>








                  <div className={styles['infor-card14']}>
                    <p className={styles['infor-card15-l']}>
                      {levelInfo.currentExp.toLocaleString()}

                    </p>
                    <span className={styles['infor-card15-r']}> | {levelInfo.nextLevelExp.toLocaleString()}</span>
                  </div>
                </div>

                <div className={styles['infor-card16']}>
                  {/* 전체 바 */}
                  <div className={styles['infor-card17']}></div>

                  {/* 진행 바 */}
                  <div 
                  className={styles['infor-card18']}
                  style={{ width: `${levelInfo.progressPercentage}%` }}
                  >
                  </div>
                  
                </div>
              </div>



              <div className={styles['infor-card19']}>
                <p className={styles['infor-card20']}>다음 레벨까지 {display}%남았어요!</p>
              </div>





            </div>
          </div>




        </div>



        {/* 경험치 박스 */}
        <div className={styles['infor-card21']}>
          <div className={styles['infor-card22']}>
            <p className={styles['infor-card23']}>획득 경험치</p>
          </div>



          <div className={styles['infor-card24']}>
  {expLogs.map((log, idx) => (
    <div key={idx} className={styles['infor-card25']}>
      <div className={styles['infor-card26']}>
        <div className={styles['infor-card27']}>
          <div className={styles['infor-card28']}>
            <p className={styles['infor-card29']}>{log.date}</p>
          </div>
        </div>

        <div className={styles['infor-card30']}>
          <p className={styles['infor-card31']}>{log.title}</p>
        </div>
      </div>

      <div className={styles['infor-card32']}>
        {/* <div className={styles['infor-card33']}></div> */}
        <div className={styles['infor-card34']}>
          <p 
            className={styles['infor-card35']}
            style={{ color: log.amount >= 0 ? '#6CAEFB' : '#BDBDBD' }}  /* 음수면 회색 */
          >
            {log.amount >= 0 ? '+' : ''}{log.amount}P
          </p>
        </div>
      </div>
    </div>
  ))}


          </div>

        </div>
      </div>
) : (
  // {/* 레벨 카드 */}
      <div className={styles['infor-f']}>
        {/* 레벨 박스 */}
        <div className={styles['infor-card2']}>

          {/* 레벨과 열고 닫기 */}
          <div className={styles['infor-card3']}>

            <div className={styles['infor-card4']}>
              <p className={styles['infor-card5']}>Level</p>
            </div>

            {/* 열고 닫기 버튼 */}
            <button
            onClick={() => setIsInfor(true)}
            className={styles['infor-card6']}>
              <svg 
              className={styles['infor-card7']}
              xmlns="http://www.w3.org/2000/svg" 
              width="13" 
              height="8" 
              viewBox="0 0 13 8" 
              fill="none">
                <path d="M0.75 0.75L6.375 6.375L12 0.75" stroke="#0F0D0E" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
          </div>




          {/* 경험치 바 */}
          <div className={styles['infor-card8']}>
            <div className={styles['infor-card9']}>
              <div className={styles['infor-card10']}>
                <div className={styles['infor-card11']}>



                  <div className={styles['infor-card12']}>
                    <p className={styles['infor-card13']}>
                      LV.{levelInfo.currentLevel}
                      </p>
                  </div>








                  <div className={styles['infor-card14']}>
                    <p className={styles['infor-card15-l']}>
                      {levelInfo.currentExp.toLocaleString()}

                    </p>
                    <span className={styles['infor-card15-r']}> | {levelInfo.nextLevelExp.toLocaleString()}</span>
                  </div>
                </div>

                <div className={styles['infor-card16']}>
                  {/* 전체 바 */}
                  <div className={styles['infor-card17']}></div>

                  {/* 진행 바 */}
                  <div 
                  className={styles['infor-card18']}
                  style={{ width: `${levelInfo.progressPercentage}%` }}
                  >
                  </div>
                  
                </div>
              </div>



              <div className={styles['infor-card19']}>
                <p className={styles['infor-card20']}>다음 레벨까지 {display}%남았어요!</p>
              </div>
            </div>
          </div>
        </div>
      </div>
)}





      {/* 총 수집한 단어 + 완료한 영상 박스 */}
      <div className={styles['collection-card']}>
        {/* 총 수집한 단어 */}
        <div className={styles['collection-card2']}>
          <div className={styles['collection-card3']}>
            <div className={styles['collection-card4']}>
              <svg xmlns="http://www.w3.org/2000/svg" width="18" height="28" viewBox="0 0 18 28" fill="none">
  <path d="M16 0H2C0.895431 0 0 0.895431 0 2V24.0357C0 25.4752 1.75253 26.1826 2.75175 25.1463L7.56031 20.1597C8.34682 19.344 9.65318 19.344 10.4397 20.1597L15.2482 25.1463C16.2475 26.1826 18 25.4752 18 24.0357V2C18 0.895431 17.1046 0 16 0Z" fill="#454440"/>
</svg>
            </div>
            <div className={styles['collection-card5']}>
              <p className={styles['collection-card6']}>총 수집한 단어</p>
              <p className={styles['collection-card7']}>{totalWords.toLocaleString()}개</p>
            </div>
          </div>
        </div>



        {/* 완료한 영상 */}
        <div className={styles['collection-card-v']}>
          <div className={styles['collection-card-v2']}>
            <div className={styles['collection-card-v3']}>
              <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 36 36" fill="none">
  <path d="M25.8193 14.4883C27.9325 14.5956 29.6132 16.3435 29.6133 18.4834V25.9678L29.6074 26.1738C29.5036 28.2188 27.8643 29.858 25.8193 29.9619L25.6133 29.9678H10.3867L10.1807 29.9619C8.1356 29.8581 6.49546 28.2189 6.3916 26.1738L6.38672 25.9678V18.4834C6.38681 16.3434 8.06734 14.5955 10.1807 14.4883L10.3867 14.4834H25.6133L25.8193 14.4883ZM10.3867 16.4834C9.28221 16.4834 8.38682 17.3789 8.38672 18.4834V25.9678C8.38682 27.0723 9.28221 27.9678 10.3867 27.9678H25.6133C26.7177 27.9676 27.6132 27.0722 27.6133 25.9678V18.4834C27.6132 17.379 26.7177 16.4835 25.6133 16.4834H10.3867ZM16 20.0684C16.0003 19.27 16.8903 18.7936 17.5547 19.2363L20.5586 21.2393C21.1521 21.6351 21.1522 22.5075 20.5586 22.9033L17.5547 24.9053C16.8901 25.3483 16 24.8719 16 24.0732V20.0684ZM24.7422 9.90332C25.2944 9.90344 25.7422 10.3511 25.7422 10.9033C25.7419 11.4553 25.2942 11.9032 24.7422 11.9033H11.2578C10.7057 11.9033 10.2581 11.4554 10.2578 10.9033C10.2578 10.351 10.7055 9.90332 11.2578 9.90332H24.7422ZM22.1621 6.03223C22.714 6.03268 23.1621 6.48022 23.1621 7.03223C23.1619 7.58405 22.7139 8.03177 22.1621 8.03223H13.8389C13.2867 8.03223 12.8391 7.58433 12.8389 7.03223C12.8389 6.47994 13.2866 6.03223 13.8389 6.03223H22.1621Z" fill="#454440"/>
</svg>
            </div>
            <div className={styles['collection-card-v4']}>
              <p className={styles['collection-card-v5']}>완료한 영상</p>
              <p className={styles['collection-card-v6']}>{conqueredVideos.toLocaleString()}편</p>
            </div>
          </div>
        </div>
      </div>




{isGrowth ? (
      // {/* 성장 지표 카드 */}
      <div className={styles['growth-card']}>
        <div className={styles['growth-card2']}>
          <div className={styles['growth-card3']}>
            <div className={styles['growth-card4']}>
              <p className={styles['growth-card5']}>성장 지표</p>
            </div>

            <button 
            onClick={() => setIsGrowth(false)}
            className={styles['growth-card6']}>
              <svg 
              className={styles['growth-card7']}
              xmlns="http://www.w3.org/2000/svg" 
              width="13" 
              height="8" 
              viewBox="0 0 13 8" 
              fill="none">
                <path d="M0.75 6.68567L6.375 1.06067L12 6.68567" stroke="#5E5A4E" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
          </div>




          <div className={styles['growth-card8']}>
            <div className={styles['growth-card9']}>
              <div className={styles['growth-card10']}>
                <div className={styles['growth-card11']}>
                  <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 28 28" fill="none">
  <path d="M14 0C15.0881 0 16.1474 0.12515 17.1641 0.360352C17.6639 0.476204 18 0.933159 18 1.44629C18 2.25287 17.1936 2.8155 16.4043 2.64941C15.6287 2.48601 14.8242 2.40039 14 2.40039C7.5935 2.40039 2.40039 7.5935 2.40039 14C2.40039 20.4065 7.5935 25.5996 14 25.5996C20.4065 25.5996 25.5996 20.4065 25.5996 14C25.5996 13.1759 25.5132 12.372 25.3496 11.5967C25.183 10.807 25.7467 10 26.5537 10C27.0671 10.0001 27.5239 10.3367 27.6396 10.8369C27.8748 11.8534 28 12.9121 28 14C28 21.732 21.732 28 14 28C6.26801 28 0 21.732 0 14C0 6.26801 6.26801 0 14 0ZM23.0215 5.14355C23.4943 4.60336 24.3162 4.54874 24.8564 5.02148C25.3966 5.49429 25.4513 6.31616 24.9785 6.85645L15.2383 17.9873C14.3158 19.0412 12.6734 19.033 11.7617 17.9697L8.0127 13.5957C7.54575 13.0506 7.60928 12.2299 8.1543 11.7627C8.69942 11.2957 9.52015 11.3593 9.9873 11.9043L13.5098 16.0146L23.0215 5.14355Z" fill="#454440"/>
</svg>
                </div>
                <div className={styles['growth-card12']}>
                  <p className={styles['growth-card13']}>평균 정확도</p>
                  <p className={styles['growth-card14']}>
                    {growthData?.averageAccuracy ?? 0}%
                  </p>
                </div>
              </div>
            </div>




            <div className={styles['growth-card15']}>
              <div className={styles['growth-card16']}>
                <div className={styles['growth-card17']}>
                  <svg xmlns="http://www.w3.org/2000/svg" width="24" height="28" viewBox="0 0 24 28" fill="none">
  <path d="M10.5 0C13.3168 0 16.1336 4.54771 17.0879 8.7627C17.1714 9.1304 17.5924 9.27976 17.8506 9.00488C18.1729 8.66156 18.5552 8.21746 18.9502 7.73438C19.9183 6.5505 21.839 6.69566 22.3672 8.13086C23.2944 10.6502 24 13.6626 24 16.9473C24 21.38 21.1473 25.1023 17.0675 26.8789C16.5891 27.0872 16.1129 26.384 16.3238 25.9068C16.4891 25.5328 16.6119 25.1332 16.6855 24.7158C17.8506 20.9292 15 16.9292 14 16.9473C12.9307 16.9976 12 18.4292 11.0322 20.0273C10.9312 20.2748 10.5829 20.3904 10.3857 20.21C9.82794 19.6992 9.5 18.9292 8.84277 20.168C8.41062 20.9848 8.03627 21.9389 7.82227 23.1523C7.62175 24.2895 7.83022 25.3846 8.33321 26.2968C8.58786 26.7586 8.17766 27.5339 7.67764 27.3663C3.0555 25.8171 0 21.6139 0 16.9473C6.35363e-05 8.84203 7.50001 5.62125e-06 10.5 0Z" fill="#454440"/>
</svg>
                </div>
                <div className={styles['growth-card18']}>
                  <p className={styles['growth-card19']}>연속학습</p>
                  <p className={styles['growth-card20']}>
                    {growthData?.streak.toLocaleString() ?? 0}일
                  </p>
                </div>
              </div>
            </div>
          </div>



          {/* 성장 지표 그래프 */}
          <div className={styles['growth-card21']}>
            <div className={styles['growth-card22']}>
              <div className={styles['growth-card23']}>
                <p className={styles['growth-card24']}>This week's</p>
              </div>
              <div className={styles['growth-card25']}>
                <p className={styles['growth-card26']}>
                  {growthData?.weeklyAccuracy?.thisWeek ?? 0}%
                </p>
              </div>
            </div>


            {/* 실제 그래프 */}
            {/* <div className={styles['growth-card27']}> */}
            {growthData && (
              <GrowthChart
                weeklyData={growthData.weeklyAccuracy.weeklyData}
                growthRate={growthData.weeklyAccuracy.growthRate}
              />
            )}
            {/* </div> */}




          </div>
        </div>
      </div>
) : (
  <div className={styles['growth-card2-f']}>
          <div className={styles['growth-card3']}>
            <div className={styles['growth-card4']}>
              <p className={styles['growth-card5']}>성장 지표</p>
            </div>

            <button 
            onClick={() => setIsGrowth(true)}
            className={styles['growth-card6']}>
              <svg 
              className={styles['growth-card7']}
              xmlns="http://www.w3.org/2000/svg" 
              width="13" 
              height="8" 
              viewBox="0 0 13 8" 
              fill="none">
                <path d="M0.75 0.75L6.375 6.375L12 0.75" stroke="#0F0D0E" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
        </div>
)}







{isObjectives ? (
      // {/* 학습 목표 카드 */}
      <div className={styles['objectives-card']}>
        <div className={styles['objectives-card2']}>
          <div className={styles['objectives-card3']}>
            <p className={styles['objectives-card4']}>학습 목표</p>
          </div>

          <button 
          onClick={() => setIsObjectives(false)}
          className={styles['objectives-card5']}>
            <svg 
            className={styles['objectives-card6']}
            xmlns="http://www.w3.org/2000/svg" 
            width="13" 
            height="8" 
            viewBox="0 0 13 8" 
            fill="none">
              <path d="M0.75 6.68567L6.375 1.06067L12 6.68567" stroke="#5E5A4E" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </div>


        {/* 목표 선택 */}
        <div className={styles['objectives-card7']}>
{GOALS.map(({ value, icon }) => {
    const isSelected = selectedGoal === value;
    
    return (
      <button
        key={value}
        type="button"
        onClick={() => handleGoalChange(value)}
        className={isSelected 
          ? styles['objectives-card8-t']   // 선택 (초록 배경)
          : styles['objectives-card13-f']  // 미선택 (흰 배경)
        }
      >
        <div className={styles['objectives-card9']}>
          <div className={styles['objectives-card10']}
        //   style={{
        //   backgroundImage: 
        //     goal === 'DAILY' ? `url(${frog2})` :
        //     goal === 'SELF_DEVELOPMENT' ? `url(${frog})` :
        //     'none',
        //   backgroundSize: 'cover',
        //   backgroundPosition: 'center',
        // }}
          >
            {icon}
          </div>
          <div className={styles['objectives-card11']}>
            <p 
              className={styles['objectives-card12']}
              style={{ color: isSelected ? '#FFF' : '#87735A' }}  // 선택 여부에 따라 색
            >
              {GOAL_LABELS[value]}
            </p>
          </div>
        </div>
      </button>
    );
  })}
        </div>
      </div>
) : (
      // {/* 학습 목표 카드 */}
      <div className={styles['objectives-card-f']}>
        <div className={styles['objectives-card2']}>
          <div className={styles['objectives-card3']}>
            <p className={styles['objectives-card4']}>학습 목표</p>
          </div>

          <button 
          onClick={() => setIsObjectives(true)}
          className={styles['objectives-card5']}>
            <svg 
            className={styles['objectives-card6']}
            xmlns="http://www.w3.org/2000/svg" 
            width="13" 
            height="8" 
            viewBox="0 0 13 8" 
            fill="none">
              <path d="M0.75 0.75L6.375 6.375L12 0.75" stroke="#0F0D0E" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </button>
        </div>
      </div>

)}




{/* 상대 난이도 */}
{isDifficulty ? (
      // {/* 난이도 카드 */}
      <div className={styles['difficulty-card']}>
        <div className={styles['difficulty-card2']}>
          <div className={styles['difficulty-card3']}>
            <div className={styles['difficulty-card4']}>
              <p className={styles['difficulty-card5']}>난이도</p>
            </div>

            <button 
            onClick={() => setIsDifficulty(false)}
            className={styles['difficulty-card6']}>
              <svg 
              className={styles['difficulty-card7']}
              xmlns="http://www.w3.org/2000/svg" 
              width="13" 
              height="8" 
              viewBox="0 0 13 8" 
              fill="none">
                <path d="M0.75 6.68567L6.375 1.06067L12 6.68567" stroke="#5E5A4E" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
          </button>


          </div>

          <div className={styles['difficulty-card8']}>

{DIFFICULTY_OPTIONS.map(({ value, label }) => {
    const isSelected = selectedDifficulty === value;


    return (
      <button
        key={value}
        type="button"
        onClick={() => handleDifficultyChange(value)}
        className={isSelected 
          ? styles['difficulty-card9']     // 선택
          : styles['difficulty-card15-f']  // 미선택
        }
      >
        {isSelected ? (
          <div className={styles['difficulty-card10']}>
            <p className={styles['difficulty-card11']}>{label}</p>
            <div className={styles['difficulty-card12']}>
              <div className={styles['difficulty-card13']}>
                <CheckIcon className={styles['difficulty-card14']} />
              </div>
            </div>
          </div>
        ) : (
          <p className={styles['difficulty-card16-f']}>{label}</p>
        )}
      </button>
    );
  })}
          </div>
        </div>
      </div>
      ) : (
        <div className={styles['difficulty-card-f']}>
          <div className={styles['difficulty-card3']}>
            <div className={styles['difficulty-card4']}>
              <p className={styles['difficulty-card5']}>난이도</p>
            </div>

            <button 
            onClick={() => setIsDifficulty(true)}
            className={styles['difficulty-card6']}>
              <svg 
              className={styles['difficulty-card7']}
              xmlns="http://www.w3.org/2000/svg" 
              width="13" 
              height="8" 
              viewBox="0 0 13 8" 
              fill="none">
                <path d="M0.75 0.75L6.375 6.375L12 0.75" stroke="#0F0D0E" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
          </button>
        </div>
      </div>
)}










{/* 
절대 난이도

isAbsoluteLevel ? (

      <div className={styles['difficulty-card']}>
        <div className={styles['difficulty-card2']}>
          <div className={styles['difficulty-card3']}>
            <div className={styles['difficulty-card4']}>
              <p className={styles['difficulty-card5']}>절대 난이도(임시)</p>
            </div>

            <button 
            onClick={() => setIsAbsoluteLevel(false)}
            className={styles['difficulty-card6']}>
              <svg 
              className={styles['difficulty-card7']}
              xmlns="http://www.w3.org/2000/svg" 
              width="13" 
              height="8" 
              viewBox="0 0 13 8" 
              fill="none">
                <path d="M0.75 6.68567L6.375 1.06067L12 6.68567" stroke="#5E5A4E" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
          </button>
          </div>

          <div className={styles['difficulty-card8']}>
ABSOLUTE_OPTIONS.map(({ value, label }) => {
    const isSelected = selectedAbsolute === value;
    
    return (
      <button
        key={value}
        type="button"
        onClick={() => handleAbsoluteChange(value)}
        className={isSelected 
          ? styles['difficulty-card9']     // 선택
          : styles['difficulty-card15-f']  // 미선택
        }
      >
        {isSelected ? (
          <div className={styles['difficulty-card10']}>
            <p className={styles['difficulty-card11']}>{label}</p>
            <div className={styles['difficulty-card12']}>
              <div className={styles['difficulty-card13']}>
                <CheckIcon className={styles['difficulty-card14']} />
              </div>
            </div>
          </div>
        ) : (
          <p className={styles['difficulty-card16-f']}>{label}</p>
        )}
      </button>
    );
  })
          </div>
        </div>
      </div>
      ) : (
        <div className={styles['difficulty-card-f']}>
          <div className={styles['difficulty-card3']}>
            <div className={styles['difficulty-card4']}>
              <p className={styles['difficulty-card5']}>절대 난이도(임시)</p>
            </div>

            <button 
            onClick={() => setIsAbsoluteLevel(true)}
            className={styles['difficulty-card6']}>
              <svg 
              className={styles['difficulty-card7']}
              xmlns="http://www.w3.org/2000/svg" 
              width="13" 
              height="8" 
              viewBox="0 0 13 8" 
              fill="none">
                <path d="M0.75 0.75L6.375 6.375L12 0.75" stroke="#0F0D0E" stroke-width="1.5" stroke-linecap="round"/>
              </svg>
          </button>
        </div>
      </div>
) */}


















    </div>
  );
}



