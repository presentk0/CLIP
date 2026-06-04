import { useAuth } from "../../hooks/useAuth";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/api";
import { useState, useEffect } from "react";
import style from './DefaultPage.module.css';
import { Spinner } from "../../components/Spinner/Spinner";

function truncateEmail(email, maxLength) {
  if (email.length <= maxLength) return email;
  
  const [local, domain] = email.split('@');
  const domainPart = '@' + domain;

  // "..." 3자 빼기
  const remaining = maxLength - domainPart.length - 3;

  if (remaining <= 0) return email.slice(0, maxLength) + '...';

  return local.slice(0, remaining) + '...' + domainPart;
}




// 기본 프로필 색상 (빨강, 파랑, 초록, 주황, 보라, 청럭, 핑크, 갈색)
const AVATAR_COLORS = [
  'var(--semantic-avatar-1)',
  'var(--semantic-avatar-2)',
  'var(--semantic-avatar-3)',
  'var(--semantic-avatar-4)',
  'var(--semantic-avatar-5)',
  'var(--semantic-avatar-6)',
  'var(--semantic-avatar-7)',
  'var(--semantic-avatar-8)',
];


// 한글 이름 대응 첫글자 가져오기
function getInitial(name) {
  // null/undefined 체크
  if (!name) return '?';

  // 공백 제거
  const trimmed = name.trim();
  if (!trimmed) return '?';

  // 대문자 변환 (영어인 경우)
  return [...trimmed][0].toUpperCase();
}



// 기본 프로필  색상 결정
function getAvatarColor(char) {
  if (!char) return AVATAR_COLORS[0];

  // 첫글자 코드 변환
  const code = char.charCodeAt(0);
  // (첫글자 코드 / 색상 종류)를 한 나머지 값으로 색상 결정
  return AVATAR_COLORS[code % AVATAR_COLORS.length];
}



function UserAvatar({ imageUrl, name }) {
  const [hasError, setHasError] = useState(false);

  const initial = getInitial(name);
  const color = getAvatarColor(initial);

  // 이미지 없거나 로드 실패 시 기본 프로필
  if (!imageUrl || hasError) {
    return (
      <div 
      className={style.userAvatar}
      style={{ 
        backgroundColor: color,
        color: 'white',
        fontWeight: 600,
        fontSize: '20px',
        }}>
        {initial}
      </div>
    );
  }
  return (
    <img 
      src={imageUrl}
      alt={`${name} 프로필`}
      onError={() => setHasError(true)}
      className={style.userAvatarTrue}
    />
  );
}





function SectionBadgeCard({ currentBadge, videoId, videoTitle, videoDuration, channelName }) {
  return (
    <article className={style.masteryCard}>

        <div className={style.masteryVideo}>
          <a 
          className={style.masteryThumbnail}
          href={`https://youtube.com/watch?v=${videoId}`}
          target="_blank"
          rel="noopener noreferrer"
          >
            <img
              src={`https://img.youtube.com/vi/${videoId}/maxresdefault.jpg`}
              alt={`${videoTitle} 영상 썸네일`}
              onError={(e) => {
                e.target.src = `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
              }}
            />

            <span className={style.masteryDurationWrapper}>
              <div className={style.masteryDuration}>
                <p className={style.masteryDurationText}>{videoDuration}</p>
              </div>
            </span>
          </a>

          <div className={style.masteryInfo}>
            <h3 className={style.masteryTitle}>{videoTitle}</h3>
            
            <p className={style.masteryChannel}>
              <div>{channelName}</div>
            </p>
          </div>

        <div className={style.sectionBadge}>
          <span className={style.sectionBadgeIcon}>
            {currentBadge}
          </span>

          <span className={style.sectionBadgeLabel}>
            <p className={style.sectionBadgeLabelText}>
              도전 중인 마스터리
            </p>
          </span>
        </div>
      </div>
    </article>
  );
}



function BigVideoCard({ videoId, thumbnailUrl, title, duration, channelName }) {
  return (
    <article className={style.recommendedSectionBox3}>
      <a 
      className={style.recommendedSectionLink}
      href={`https://youtube.com/watch?v=${videoId}`}
      target="_blank"
      rel="noopener noreferrer"
      >
        {/* 썸네일 */}
        <div className={style.recommendedSectionThumbnail}>
          <img 
          src={`https://img.youtube.com/vi/${videoId}/maxresdefault.jpg`} 
          alt={`${title} 영상 썸네일`} 
          // 고화질 실패하면 저화질로
          onError={(e) => {
              e.target.src = `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
          }}
          />

          <div className={style.recommendedSectionDuration}>
            <p className={style.recommendedSectionDuration3}>{duration}</p>
          </div>
        </div>

        {/* 영상 정보 */}
        <div className={style.recommendedSectionInfo}>
          {/* 채널 정보 (프로필 없음 오류) */}
          <img
            className={style.recommendedSectionChannel}
            src={thumbnailUrl}
            alt={`${channelName} 프로필`}
          />
          <div className={style.recommendedSectionTitle}>
            <h3 className={style.recommendedSectionTitle2}>{title}</h3>
            <span className={style.recommendedSectionChannelName}>{channelName}</span>
          </div>
        </div>
      </a>
    </article>
  );
}



function SmallVideoCard({ videoId, title, duration, channelName }) {
  return (
    <a 
    className={style.smallVideoCard2}
    href={`https://youtube.com/watch?v=${videoId}`}
    target="_blank"
    rel="noopener noreferrer"
    >
      <div className={style.smallVideoCardThumbnail}>
        <img 
        src={`https://img.youtube.com/vi/${videoId}/maxresdefault.jpg`} 
        alt={`${title} 영상 썸네일`} 
        // 고화질 실패하면 저화질로
        onError={(e) => {
            e.target.src = `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
        }}
        />
        <span className={style.smallVideoCardDuration}>
          <div className={style.smallVideoCardDuration2}>
            <p className={style.smallVideoCardDuration3}>{duration}</p>
          </div>
        </span>
      </div>

      <span className={style.smallVideoCardInfo}>
        <h3 className={style.smallVideoCardTitle}>{title}</h3>
        <span className={style.smallVideoCardChannel}>{channelName}</span>
      </span>
    </a>
  );
}







function DefaultPage({ onMyPage }) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    // ProtectedRoute가 자동으로 /login 보내지만, 명시적으로 해도 됨
    navigate('/login');
  };

  // // 상태로 대시보드 데이터 관리
  // const [dashboardData, setDashboardData] = useState(null);

  // 상태로 내 정보 조회 데이터 관리 (마스터리 뱃지 영상)
  const [usersData, setUsersData] = useState(null);


  // 칭호 박스 열고 닫기
  const [isBadge, setIsBadge] = useState(false);



  // 추천 영상 관리, 나중에 다시 넣기
  const [recommendedData, setRecommendedData] = useState([]);


  // 첫 번째와 나머지 분리, 나중에 다시 넣기
  const [firstVideo, ...restVideos] = recommendedData;





  // // 컴포넌트 mount 시 대시보드 조회 API 호출
  // useEffect(() => {
  //   const fetchData = async () => {
  //     try {
  //       const result = await apiFetch('/users/me/dashboard', { method: 'GET' });
  //       setDashboardData(result.data);
  //     } catch (error) {
  //       console.error('데이터 로딩 실패:', error);
  //     }
  //   };

  //   fetchData();
  // }, []);



  // 컴포넌트 mount 시 내 정보 조회 API 호출
  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await apiFetch('/users/me', { method: 'GET' });
        setUsersData(result.data);
      } catch (error) {
        console.error('데이터 로딩 실패:', error);
      }
    };

    fetchData();
  }, []);






  // 컴포넌트 mount 시 추천영상 API 호출, 나중에 다시 넣기 오류
  useEffect(() => {
    const fetchData = async () => {
      try {
        // ? 써서 recommended + limit이라는 옵션값 5 로 인식
        const result = await apiFetch('/videos/recommended?limit=5', { method: 'GET' });
        setRecommendedData(result.data.recommendations);
      } catch (error) {
        console.error('데이터 로딩 실패:', error);
      }
    };

    fetchData();
  }, []);








  // 데이터 없으면 무한 로딩 화면
  if (!usersData) {
    return <Spinner showLabel={false} />;
  }



  const MAX_LEVEL = 99;

  // 소수점 첫 자리만 남기기
  const progress = 100 - usersData.progressPercentage;
  // Math.floor는 정수 단위로만 처리해서 10을 곱한 뒤에 나누기
  const display = Math.floor(progress * 10) / 10;


  return (
    // 전체 박스
    <div className={style.main}>

      {/* 테스트 마이페이지 버튼 */}



      {/* 상단 */}
      <div className={style.top}>
        <div className={style.logobox}>
          <svg 
          className={style.logo}
          xmlns="http://www.w3.org/2000/svg" 
          width="76" 
          height="15" 
          viewBox="0 0 76 15" 
          fill="none">
            <path 
            d="M16.71 0C18.32 0 19.7199 1.11035 20.1299 2.61035C22.4998 3.03034 24.2997 5.10992 24.2998 7.58984V9.11035C24.2996 11.9001 22.03 14.1698 19.2402 14.1699H9.87012C8.06012 14.1699 6.58008 12.6899 6.58008 10.8799C6.58015 9.06994 8.06016 7.58984 9.87012 7.58984H13.8701C14.6701 7.5898 15.4201 7.1895 15.8701 6.51953C16.1802 6.04992 16.8097 5.9305 17.2695 6.24023C17.7295 6.5502 17.8597 7.17966 17.5498 7.63965C16.7298 8.86962 15.3501 9.61031 13.8701 9.61035H9.87012C9.17016 9.61035 8.59968 10.1799 8.59961 10.8799C8.59961 11.5799 9.17012 12.1504 9.87012 12.1504H19.2305C20.9001 12.1501 22.2693 10.79 22.2695 9.11035V7.58984C22.2694 5.92008 20.9102 4.55006 19.2305 4.5498C18.6705 4.5498 18.2197 4.10002 18.2197 3.54004C18.2197 2.7001 17.5401 2.01964 16.7002 2.01953C15.8602 2.01953 15.1797 2.70004 15.1797 3.54004C15.1797 4.10002 14.7299 4.5498 14.1699 4.5498H10.1201C9.56013 4.5498 9.11037 4.10002 9.11035 3.54004C9.11035 2.70004 8.42985 2.01953 7.58985 2.01953C6.74992 2.01962 6.07031 2.70009 6.07031 3.54004C6.0703 4.10002 5.61956 4.5498 5.05957 4.5498C3.38982 4.55004 2.01962 5.91007 2.01953 7.58984V9.11035C2.01972 10.78 3.37989 12.1502 5.05957 12.1504C5.61957 12.1504 6.07031 12.6002 6.07031 13.1602C6.07023 13.7201 5.61952 14.1699 5.05957 14.1699C2.26989 14.1697 0.000190344 11.9 0 9.11035V7.58984C8.13357e-05 5.09997 1.81004 3.0304 4.16992 2.61035C4.57991 1.1104 5.95991 7.10023e-05 7.58985 0C9.21985 0 10.5502 1.07027 10.9902 2.53027H13.3096C13.7496 1.07028 15.1 1.69016e-05 16.71 0ZM31.9199 2.9502C32.5599 2.9502 33.1504 3.03973 33.6904 3.21973C34.2303 3.38973 34.7102 3.6503 35.1201 3.99023C35.54 4.32021 35.8699 4.74054 36.1299 5.23047C36.5596 6.05038 36.0395 6.91992 35.0596 6.91992C34.0802 6.9197 34.01 6.18007 33.4004 5.66016C33.2304 5.50018 33.0202 5.38957 32.7803 5.30957L32.7695 5.2998C32.5298 5.21996 32.2701 5.17973 31.9805 5.17969C31.4706 5.17969 31.0301 5.3097 30.6602 5.55957C30.2902 5.80957 30.0096 6.18039 29.8096 6.65039C29.6197 7.12028 29.5195 7.67036 29.5195 8.37012C29.5195 9.07007 29.6203 9.65991 29.8203 10.1299C30.0203 10.5999 30.3099 10.9602 30.6699 11.2002C31.0399 11.4402 31.4805 11.5596 31.9805 11.5596C32.2701 11.5595 32.5298 11.5203 32.7695 11.4404C33.0095 11.3604 33.2097 11.2398 33.3897 11.0898C33.9996 10.5699 34.1 9.83018 35.0498 9.83008C35.9997 9.83008 36.5497 10.6997 36.1201 11.5195C35.8602 12.0094 35.5202 12.4198 35.1104 12.7598C34.7004 13.0998 34.2197 13.3503 33.6797 13.5303C33.1298 13.7102 32.55 13.7998 31.9102 13.7998C30.9602 13.7998 30.1096 13.5899 29.3496 13.1699C28.5999 12.74 28.0102 12.13 27.5703 11.3203C27.1403 10.5103 26.9199 9.53007 26.9199 8.37012C26.9199 7.21012 27.1401 6.22992 27.5801 5.41992C28.0201 4.61001 28.62 4.00006 29.3799 3.58008C30.1299 3.16009 30.98 2.95021 31.9199 2.9502ZM38.5996 3.08984C39.3096 3.08984 39.8799 3.66012 39.8799 4.37012V11.0596C39.8799 11.3496 40.1104 11.5801 40.4004 11.5801H43.2402C43.8101 11.5802 44.2803 12.0402 44.2803 12.6201C44.2802 13.2 43.8201 13.66 43.2402 13.6602H38.8897L38.8799 13.6504C38.02 13.6503 37.3203 12.9498 37.3203 12.0898V4.37012C37.3203 3.67025 37.8898 3.09006 38.5996 3.08984ZM46.3203 3.08984C47.0202 3.09001 47.5996 3.66022 47.5996 4.37012V12.3799C47.5996 13.0798 47.0302 13.66 46.3203 13.6602C45.6103 13.6602 45.0401 13.0899 45.0401 12.3799V4.37012C45.0401 3.67012 45.6103 3.08984 46.3203 3.08984ZM73.8701 3.09961C74.81 3.09968 75.4099 4.10977 74.9502 4.92969L72.2197 9.90039C72.1399 10.0503 72.0899 10.2305 72.0899 10.4004V12.3896C72.0899 13.0895 71.5202 13.66 70.8203 13.6602C70.1203 13.6602 69.5498 13.0896 69.5498 12.3896V10.4004C69.5498 10.2305 69.5098 10.0503 69.4199 9.90039L66.6904 4.92969H66.71C66.2603 4.10974 66.8502 3.09961 67.7901 3.09961C68.26 3.09962 68.6797 3.36028 68.8897 3.78027L70.7598 7.48047C70.762 7.48268 70.801 7.51953 70.8301 7.51953C70.86 7.5195 70.8804 7.50044 70.9004 7.48047L72.7695 3.78027C72.9795 3.36027 73.4101 3.09961 73.8701 3.09961ZM53.5596 3.08984C54.3496 3.08984 55.0304 3.2398 55.6104 3.5498C56.1903 3.8598 56.6302 4.28014 56.9502 4.83008C57.2602 5.38005 57.4199 6.0103 57.4199 6.74023C57.4199 7.47014 57.2604 8.11045 56.9404 8.65039C56.6204 9.19034 56.1701 9.61018 55.5801 9.91016C55.2302 10.0901 54.8403 10.21 54.4004 10.29C54.2204 10.32 53.9901 10.3603 53.6602 10.3604C53.1002 10.3604 52.6299 9.97992 52.5899 9.41992C52.5499 8.86997 52.9503 8.4596 53.5303 8.34961C53.9202 8.2795 54.2603 7.97014 54.3203 7.91016C54.4203 7.82017 54.5003 7.70981 54.5703 7.58984C54.7002 7.34995 54.7695 7.06003 54.7695 6.74023C54.7695 6.42023 54.7003 6.12965 54.5703 5.88965C54.4404 5.64984 54.2402 5.46005 53.9805 5.33008C53.7205 5.20008 53.3995 5.12988 53.0195 5.12988H52.2803C51.9903 5.12988 51.7598 5.36039 51.7598 5.65039V12.3496C51.7598 13.0495 51.1903 13.6296 50.4805 13.6299V13.6504C49.7805 13.6504 49.2003 13.0801 49.2002 12.3701V4.65039C49.2002 3.79047 49.8999 3.08998 50.7598 3.08984H53.5596ZM64.9297 3.08008C65.4997 3.08008 65.9697 3.55012 65.9697 4.12012V4.2002C65.9697 4.4201 65.8995 4.62964 65.7695 4.80957L60.96 11.4004C60.9103 11.4704 60.9602 11.5703 61.0401 11.5703H64.9199C65.4899 11.5703 65.96 12.0304 65.96 12.6104C65.9598 13.1902 65.4998 13.6504 64.9199 13.6504H58.6397C58.0699 13.6502 57.5998 13.1801 57.5996 12.6104V12.5303C57.5996 12.3103 57.6698 12.0999 57.7998 11.9199L62.6104 5.33008C62.6602 5.26016 62.6101 5.16038 62.5303 5.16016H58.6504C58.0804 5.16016 57.6104 4.70007 57.6104 4.12012C57.6104 3.54012 58.0704 3.08008 58.6504 3.08008H64.9297Z" 
            fill="white"
            />
          </svg>
        </div>
        <div className={style.topButtonBox}>
          <div className={style.topLeftButton}>AI 채팅 미구현</div>
          <button 
          className={style.topRightButton}
          onClick={onMyPage}>
            마이페이지
          </button>
        </div>
      </div>




      {/* 전체 내용 */}
      <div className={style.container}>

{isBadge ? (
        // {/* 칭호칸 열린 유저 프로필 */}
        <div className={style['userProfile-t']}>

          {/* 프로필 박스 */}
          <div className={style.userProfileBox}>
          
            {/* 유저 정보 박스 */}
            <div className={style.userInfo}>

              {/* 유저 프로필 이미지 */}
              <div>
                <UserAvatar 
                  imageUrl={usersData?.profileImageUrl}
                  name={usersData?.name}
                />
              </div>

              {/* 유저 정보 박스 */}
              <div className={style.userText}>

                {/* 유저 이름 박스 */}
                <div className={style.userNameRow}>
                  <div className={style.userNameBox}>
                    <p className={style.userName}>
                      {usersData?.name}
                    </p>
                  </div>

                  {/* 님 */}
                  <div className={style.userNameSuffixBox}>
                    <p className={style.userNameSuffix}>
                      님
                    </p>
                  </div>
                </div>

                {/* 이메일 박스 */}
                <div className={style.userEmailBox}>
                  {/* 이메일 */}
                  <p className={style.userEmail}>
                    {truncateEmail(usersData?.email, 15)}
                  </p>
                </div>
              </div>
            </div>

            {/* 로그아웃 박스 */}
            <div className={style.logoutButton}>
              <button 
              onClick={handleLogout}
              className={style.logoutButtonInner}
              >
                <div className={style.logoutIcon}>
                  <p className={style.logoutText}>
                    로그아웃
                  </p>
                </div>
              </button>
            </div>

            {/* 칭호칸 열고 닫기 */}
            <button 
            onClick={() => setIsBadge(false)}
            className={style.logoutButtonBg}>
              {/* 버튼 아이콘? */}
              <svg
                className={style.logoutButtonIcon}
                xmlns="http://www.w3.org/2000/svg"
                width="13"
                height="8"
                viewBox="0 0 13 8"
                fill="none"
              >
                <path 
                  d="M0.75 0.75L6.375 6.375L12 0.75" 
                  stroke="#0F0D0E"
                  stroke-width="1.5"
                  stroke-linecap="round"
                />
              </svg>
            </button>
          </div>

          {/* 레벨 카드 전체 */}
          <div className={style.levelCard}>
            <div className={style.levelCardInner}>
              <div className={style.levelProgress}>
                <div className={style.levelInfoRow}>
                  <div className={style.levelLabel}>
                    <p className={style.levelLabelText}>
                      LV.{usersData.level}
                    </p>
                  </div>

                  <div className={style.levelExp}>
                    <p className={style.levelExpTextLeft}>{usersData.exp.toLocaleString()}</p>
                    <span className={style.levelExpTextRight}> | {usersData.nextLevelExp.toLocaleString()}</span>
                  </div>
                </div>
              </div>

              <div className={style.levelBar}>
                <div className={style.levelBarTrack}></div>
                <div 
                className={style.levelBarFill}
                style={{ width: `${usersData.progressPercentage}%` }}
                ></div>
              </div>
            </div>

            <div className={style.levelMessage}>
              <p className={style.levelMessageText}>
                다음 레벨까지 {display}%남았어요!
              </p>
            </div>
          </div>
        </div>
) : (
        // {/* 유저 프로필 */}
        <div className={style.userProfile}>

          {/* 프로필 박스 */}
          <div className={style.userProfileBox}>
          
            {/* 유저 정보 박스 */}
            <div className={style.userInfo}>

              {/* 유저 프로필 이미지 */}
              <div>
                <UserAvatar 
                  imageUrl={usersData?.profileImageUrl}
                  name={usersData?.name}
                />
              </div>

              {/* 유저 정보 박스 */}
              <div className={style.userText}>

                {/* 유저 이름 박스 */}
                <div className={style.userNameRow}>
                  <div className={style.userNameBox}>
                    <p className={style.userName}>
                      {usersData?.name}
                    </p>
                  </div>

                  {/* 님 */}
                  <div className={style.userNameSuffixBox}>
                    <p className={style.userNameSuffix}>
                      님
                    </p>
                  </div>
                </div>

                {/* 이메일 박스 */}
                <div className={style.userEmailBox}>
                  {/* 이메일 */}
                  <p className={style.userEmail}>
                    {truncateEmail(usersData?.email, 15)}
                  </p>
                </div>
              </div>
            </div>

            {/* 로그아웃 박스 */}
            <div className={style.logoutButton}>
              <button 
              onClick={handleLogout}
              className={style.logoutButtonInner}
              >
                <div className={style.logoutIcon}>
                  <p className={style.logoutText}>
                    로그아웃
                  </p>
                </div>
              </button>
            </div>

            {/* 칭호칸 열고 닫기 */}
            <button 
            onClick={() => setIsBadge(true)}
            className={style.logoutButtonBg}>
              {/* 버튼 아이콘? */}
              <svg
                className={style.logoutButtonIcon}
                xmlns="http://www.w3.org/2000/svg"
                width="13"
                height="8"
                viewBox="0 0 13 8"
                fill="none"
              >
                <path 
                  d="M0.75 0.75L6.375 6.375L12 0.75" 
                  stroke="#0F0D0E"
                  stroke-width="1.5"
                  stroke-linecap="round"
                />
              </svg>
            </button>
          </div>

          {/* 레벨 카드 전체 */}
          <div className={style.levelCard}>
            <div className={style.levelCardInner}>
              <div className={style.levelProgress}>
                <div className={style.levelInfoRow}>
                  <div className={style.levelLabel}>
                    <p className={style.levelLabelText}>
                      LV.{usersData.level}
                    </p>
                  </div>

                  <div className={style.levelExp}>
                    <p className={style.levelExpTextLeft}>{usersData.exp.toLocaleString()}</p>
                    <span className={style.levelExpTextRight}> | {usersData.nextLevelExp.toLocaleString()}</span>
                  </div>
                </div>
              </div>

              <div className={style.levelBar}>
                <div className={style.levelBarTrack}></div>
                <div 
                className={style.levelBarFill}
                style={{ width: `${usersData.progressPercentage}%` }}
                ></div>
              </div>
            </div>

            <div className={style.levelMessage}>
              <p className={style.levelMessageText}>
                다음 레벨까지 {display}%남았어요!
              </p>
            </div>
          </div>
        </div>
)}



        <div className={style.videoSection}>
          <SectionBadgeCard { ...usersData.ongoingMastery }></SectionBadgeCard>

          <div className={style.recommendedSection}>


            <div className={style.recommendedSectionBox}>
              <div className={style.recommendedTitle}>
                <div className={style.recommendedTitle2}></div>
                <div className={style.recommendedTitle3}>
                  <p className={style.recommendedTitle4}>추천 영상</p>
                </div>
              </div>



              <div className={style.recommendedSectionBox2}>
                {firstVideo && <BigVideoCard {...firstVideo} />}
              </div>

                {restVideos.length > 0 && (
                  <div className={style.smallVideoCard}>
                    {restVideos.map(video => (
                      <SmallVideoCard key={video.videoId} {...video} />
                    ))}
                  </div>
                )}



            </div>
          </div>
        </div>
      </div>
    </div>
  );
}



export default DefaultPage;