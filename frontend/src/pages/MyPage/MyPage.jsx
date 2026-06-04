import styles from './MyPage.module.css'
import { useState, useEffect } from 'react';
import { apiFetch } from '../../utils/api';
import { Spinner } from '../../components/Spinner/Spinner';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../hooks/useAuth';
import { log } from '../../utils/logger';

  // 오류
  // 영문 → 한글 매핑
const GOAL_LABELS = {
  TRAVEL: '여행',
  BUSINESS: '비즈니스',
  // 명세서엔 TRAVEL, DAILY, BUSINESS만 있음
  // 디자인엔 자기계발, 영어시험, 일상, 없음 등 추가 항목이 있는데
  // 백엔드에 추가 요청 필요
  SELF_DEVELOPMENT: '자기계발',
  EXAM: '영어시험',
  DAILY: '일상',
  NONE: '없음'
};

const GOALS = ['TRAVEL', 'BUSINESS', 'SELF_DEVELOPMENT', 'EXAM', 'DAILY', 'NONE']; // 명세서 기준


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


// 절대 난이도
const ABSOLUTE_OPTIONS = [
  { value: 'BEGINNER', label: '초보자' },
  { value: 'INTERMEDIATE', label: '중급자' },
  { value: 'ADVANCED', label: '상급자' },
];

// 절대 난이도 레벨
const ABSOLUTE_LEVEL_MAP = {
  BEGINNER: 1,
  INTERMEDIATE: 2,
  ADVANCED: 3,
};


const MIN_LEVEL = 1;
const MAX_LEVEL = 3;












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
                {isLast && (
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
                <div 
                  className={isLast ? styles['chart12-g'] : styles['chart11-g']}
                  style={{ height: `${heightPercent * 1.6}px` }}  
                />
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
  const [selectedAbsolute, setSelectedAbsolute] = useState(null);



  const [isInfor, setIsInfor] = useState(true);
  const [isGrowth, setIsGrowth] = useState(true);
  const [isObjectives, setIsObjectives] = useState(true);
  const [isDifficulty, setIsDifficulty] = useState(true);
  const [isAbsoluteLevel, setIsAbsoluteLevel] = useState(true);


  const { needsOnboarding, completeOnboarding } = useAuth();

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
        log.debug('대시보드', dashboard);
        log.debug('성장지표', growth);
        log.debug('학습목표', preferences);


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

        // 절대 난이도 설정값 업데이트
        if (preferences.data?.difficultyLevel) {
          setIsAbsoluteLevel(preferences.data.isAbsoluteLevel);
        }
      } catch (error) {
        console.error('데이터 로딩 실패:', error);
      }
    };

    fetchAll();
  }, []);

  // 데이터 준비 안 됐으면 일찍 return
  if (!dashboardData || !growthData) {
    return (
      <Spinner />
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
  selectedDifficulty !== preferencesData?.difficultyLevel ||
  selectedAbsolute !== preferencesData?.absoluteLevel;






const handleSave = async () => {
  log.debug('여기실행');
  // 둘 다 선택했는지 검증
  if (!selectedGoal || !selectedDifficulty || !selectedAbsolute) {
    alert('학습 목표와 상대 난이도, 절대 난이도를 모두 선택해주세요');
    return;
  }
log.debug('여기실행1');
  try {
    log.debug('여기실행2');
    if (needsOnboarding) {
      log.debug('여기실행33', selectedGoal, selectedDifficulty, selectedAbsolute);
      // 신규 유저 POST onboarding
      await apiFetch('/preferences/onboarding', {
        method: 'POST',
        body: JSON.stringify({ 
          learningGoal: selectedGoal, 
          difficultyLevel: selectedDifficulty,
          absoluteLevel: selectedAbsolute,
        }),
      });
      log.debug('여기실행4');
      completeOnboarding();
      log.debug('여기실행5');
      alert('온보딩 저장 (임시):', { selectedGoal, selectedDifficulty, selectedAbsolute });
      // 디폴트 페이지로
      navigate('/');
      log.debug('여기실행6');
    } else {
      log.debug('여기실행7');
      if (!isDirty) {
        log.debug('여기실행8');
        alert('변경된 내용이 없습니다');
        return;
      }
log.debug('여기실행9');
      const res = await apiFetch('/users/me/preferences', {
        method: 'PATCH',
        body: JSON.stringify({  
          learningGoal: selectedGoal, 
          difficultyLevel: selectedDifficulty,
          absoluteLevel: selectedAbsolute,
        }),
      });
      log.debug('여기실행10');
      // 새 원본으로 갱신
      setPreferencesData(res.data);  
      alert('저장되었습니다!');
    }
  } catch (error) {
    log.debug('여기실행11');
    console.error('저장 실패:', error);
  }
};




const handleGoalChange = async (goal) => {
  // 같은 거 또 누르면 무시
  if (goal === selectedGoal) return;

  // const prevGoal = selectedGoal;
  setSelectedGoal(goal);

  // try {
    // await apiFetch('/users/me/preferences', {
    //   method: 'PATCH',
    //   body: { learningGoal: goal },
    // });
  // } catch (error) {
  //   console.error('학습 목표 변경 실패:', error);
  //   setSelectedGoal(prevGoal);  // 실패 시 롤백
  // }
};





// 상대 난이도 변경
const handleDifficultyChange = async (level) => {
  if (level === selectedDifficulty) return;

  // const prev = selectedDifficulty;
  setSelectedDifficulty(level);

  // try {
    // await apiFetch('/users/me/preferences', {
    //   method: 'PATCH',
    //   body: { difficultyLevel: level },
    // });
  // } catch (error) {
  //   console.error('난이도 변경 실패:', error);
  //   setSelectedDifficulty(prev);
  //   alert('난이도 변경에 실패했습니다.');
  // }
};



// 절대 난이도 변경
const handleAbsoluteChange = async (level) => {
  if (level === selectedAbsolute) return;

  setSelectedAbsolute(level);
};


  // 비활성화 체크 함수
  const isOptionDisabled = (optionValue) => {
    if (!selectedAbsolute) return false;  // 절대 난이도 선택 전엔 다 활성화
    
    const currentLevel = ABSOLUTE_LEVEL_MAP[selectedAbsolute];
    const offset = DIFFICULTY_LEVEL_MAP[optionValue];
    const targetLevel = currentLevel + offset;
    
    return targetLevel < MIN_LEVEL || targetLevel > MAX_LEVEL;
  };



const handleHomeClick = () => {
  if (needsOnboarding) {
    alert('학습 목표와 상대 난이도, 절대 난이도를 모두 선택하고 저장해주세요');
    return;
  }
  onExitPage();
};


  return (
    <div className={styles.container}>
      <div className={styles.header}>
        <div className={styles.top}>
          <div className={styles.top2}>
            <div className={styles.top3}>
              <div className={styles['top4-l']}>뒤로가기 미구현</div>
              <button 
              className={styles['top5-r']}
              onClick={handleHomeClick}>
                홈
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
        <div className={styles['infor-card33']}></div>
        <div className={styles['infor-card34']}>
          <p 
            className={styles['infor-card35']}
            style={{ color: log.amount >= 0 ? '#6CAEFB' : '#BDBDBD' }}  /* 음수면 회색 */
          >
            {log.amount}P
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
            <div className={styles['collection-card4']}></div>
            <div className={styles['collection-card5']}>
              <p className={styles['collection-card6']}>총 수집한 단어</p>
              <p className={styles['collection-card7']}>{totalWords.toLocaleString()}개</p>
            </div>
          </div>
        </div>



        {/* 완료한 영상 */}
        <div className={styles['collection-card-v']}>
          <div className={styles['collection-card-v2']}>
            <div className={styles['collection-card-v3']}></div>
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
                <div className={styles['growth-card11']}></div>
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
                <div className={styles['growth-card17']}></div>
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
              <p className={styles['growth-card5']}>성장 기록</p>
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
{GOALS.map((goal) => {
    const isSelected = selectedGoal === goal;
    
    return (
      <button
        key={goal}
        type="button"
        onClick={() => handleGoalChange(goal)}
        className={isSelected 
          ? styles['objectives-card8-t']   // 선택 (초록 배경)
          : styles['objectives-card13-f']  // 미선택 (흰 배경)
        }
      >
        <div className={styles['objectives-card9']}>
          <div className={styles['objectives-card10']}></div>  {/* 아이콘 */}
          <div className={styles['objectives-card11']}>
            <p 
              className={styles['objectives-card12']}
              style={{ color: isSelected ? '#FFF' : '#87735A' }}  // 선택 여부에 따라 색
            >
              {GOAL_LABELS[goal]}
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
            {/* 오류 절대 난이도가 상대 난이도보다 먼저 설정하게 바꾸기, 절대 난이도 변경 시 상대난이도 조건에 따라 상대난이도 초기화 여부 결정하기  */}
{DIFFICULTY_OPTIONS.map(({ value, label }) => {
    const isSelected = selectedDifficulty === value;
    // 절대 난이도 기준 활성화 조건 설정
    const isDisabled = isOptionDisabled(value);

    return (
      <button
        key={value}
        type="button"
        // 절대 난이도 기준 +2, -2 단계 범위 제한
        disabled={isDisabled}
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











{/* 절대 난이도 */}
{isAbsoluteLevel ? (
      // {/* 난이도 카드 */}
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
{ABSOLUTE_OPTIONS.map(({ value, label }) => {
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
  })}
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
)}



















    </div>
  );
}



