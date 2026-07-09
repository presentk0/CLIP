/* global chrome */

import { useAuth } from "../../hooks/useAuth";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../../utils/api";
import { useState, useEffect } from "react";
import style from './DefaultPage.module.css';
import { Spinner } from "../../components/Spinner/Spinner";
import { openYoutubeVideo } from "../../utils/openInTab";
import { log } from "../../utils/logger";

import { saveUserData, loadUserData } from "../../utils/userStorage";
import { TestSpinner } from "../../components/Spinner/Spinner";
import { Toast } from "../../contexts/Toast";

const BADGE_ICONS = {
  BRONZE: (<svg className={style.bronze} xmlns="http://www.w3.org/2000/svg" width="21" height="24" viewBox="0 0 21 24" fill="none">
  <g filter="url(#filter0_i_1435_11641)">
    <path d="M10.3921 0L20.7844 6V18L10.3921 24L-0.000214577 18V6L10.3921 0Z" fill="#F7D4AE"/>
  </g>
  <path d="M19.7847 6.57715V17.4219L10.3921 22.8447L0.999512 17.4219V6.57715L10.3921 1.1543L19.7847 6.57715Z" stroke="#E3CEB9" stroke-width="2"/>
  <defs>
    <filter id="filter0_i_1435_11641" x="0" y="0" width="20.7842" height="24" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
      <feFlood flood-opacity="0" result="BackgroundImageFix"/>
      <feBlend mode="normal" in="SourceGraphic" in2="BackgroundImageFix" result="shape"/>
      <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
      <feOffset/>
      <feGaussianBlur stdDeviation="6"/>
      <feComposite in2="hardAlpha" operator="arithmetic" k2="-1" k3="1"/>
      <feColorMatrix type="matrix" values="0 0 0 0 0.358974 0 0 0 0 0.353279 0 0 0 0 0.33999 0 0 0 0.7 0"/>
      <feBlend mode="normal" in2="shape" result="effect1_innerShadow_1435_11641"/>
    </filter>
  </defs>
</svg>),
  SILVER: (<svg className={style.silver} xmlns="http://www.w3.org/2000/svg" width="29" height="32" viewBox="0 0 29 32" fill="none">
  <path d="M24.2847 6.28809V17.7109L14.3921 23.4229L4.49951 17.7109V6.28809L14.3921 0.576172L24.2847 6.28809Z" fill="#DCD6C6" stroke="#E5E0D2"/>
  <g filter="url(#filter0_di_1435_11627)">
    <path d="M14.3921 0L24.7844 6V18L14.3921 24L3.99979 18V6L14.3921 0Z" fill="#CDC6B9"/>
    <path d="M23.7847 6.57715V17.4219L14.3921 22.8447L4.99951 17.4219V6.57715L14.3921 1.1543L23.7847 6.57715Z" stroke="#D9D5CA" stroke-width="2"/>
  </g>
  <g filter="url(#filter1_d_1435_11627)">
    <path d="M14.3921 15L11.0171 16.5L11.3921 12.75L9.14209 9.75L12.8921 9.375L14.3921 6L16.2671 9.375L20.3921 10.125L17.3921 12.75L17.7671 16.5L14.3921 15Z" fill="#EFEADB"/>
  </g>
  <path d="M10.4707 10.3335L13.367 10.1242L12.2412 8.62479L7.26701 9.375L10.4707 10.3335Z" fill="#C9C2B3"/>
  <path d="M11.8671 15.3734L12.6167 12.3741L10.7417 13.1243L9.89223 17.9998L11.8671 15.3734Z" fill="#C9C2B3"/>
  <path d="M10.4717 10.3291L12.6173 12.3714L10.7414 13.1244L7.26712 9.375L10.4717 10.3291Z" fill="#A6A293"/>
  <path d="M18.6171 10.2768L15.6171 10.1268L16.7173 8.62562L21.5177 9.3772L18.6171 10.2768Z" fill="#C9C2B3"/>
  <path d="M18.6166 10.2745L16.5166 12.3745L18.2416 13.1245L21.5171 9.375L18.6166 10.2745Z" fill="#A6A293"/>
  <path d="M14.4917 7.5003L15.6167 10.1253L16.7417 8.6253L14.4917 4.125V7.5003Z" fill="#A6A293"/>
  <path d="M11.8664 15.3754L14.4914 13.8751L14.4914 15.7504L9.89196 18L11.8664 15.3754Z" fill="#837D6D"/>
  <path d="M17.1158 15.3736L16.5158 12.3739L18.2408 13.1237L18.8919 17.9998L17.1158 15.3736Z" fill="#C9C2B3"/>
  <path d="M17.1184 15.3781L14.4924 13.8766L14.4924 15.7513L18.8924 18.0002L17.1184 15.3781Z" fill="#999484"/>
  <path d="M14.4917 7.49864L13.3667 10.1236L12.2417 8.62364L14.4917 4.125V7.49864Z" fill="#FDF8E9"/>
  <defs>
    <filter id="filter0_di_1435_11627" x="0" y="0" width="28.7842" height="32" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
      <feFlood flood-opacity="0" result="BackgroundImageFix"/>
      <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
      <feOffset dy="4"/>
      <feGaussianBlur stdDeviation="2"/>
      <feComposite in2="hardAlpha" operator="out"/>
      <feColorMatrix type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.12 0"/>
      <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow_1435_11627"/>
      <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow_1435_11627" result="shape"/>
      <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
      <feOffset/>
      <feGaussianBlur stdDeviation="6"/>
      <feComposite in2="hardAlpha" operator="arithmetic" k2="-1" k3="1"/>
      <feColorMatrix type="matrix" values="0 0 0 0 0.358974 0 0 0 0 0.353279 0 0 0 0 0.33999 0 0 0 0.7 0"/>
      <feBlend mode="normal" in2="shape" result="effect2_innerShadow_1435_11627"/>
    </filter>
    <filter id="filter1_d_1435_11627" x="5.14209" y="4" width="19.25" height="18.5" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
      <feFlood flood-opacity="0" result="BackgroundImageFix"/>
      <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
      <feOffset dy="2"/>
      <feGaussianBlur stdDeviation="2"/>
      <feComposite in2="hardAlpha" operator="out"/>
      <feColorMatrix type="matrix" values="0 0 0 0 0.448718 0 0 0 0 0.427145 0 0 0 0 0.366021 0 0 0 0.6 0"/>
      <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow_1435_11627"/>
      <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow_1435_11627" result="shape"/>
    </filter>
  </defs>
</svg>),
  GOLD: (<svg className={style.gold} xmlns="http://www.w3.org/2000/svg" width="29" height="32" viewBox="0 0 29 32" fill="none">
  <g filter="url(#filter0_di_1435_11642)">
    <path d="M14.3921 0L24.7844 6V18L14.3921 24L3.99979 18V6L14.3921 0Z" fill="#FFC229"/>
    <path d="M23.7847 6.57715V17.4219L14.3921 22.8447L4.99951 17.4219V6.57715L14.3921 1.1543L23.7847 6.57715Z" stroke="#F4D78E" stroke-width="2"/>
  </g>
  <g filter="url(#filter1_d_1435_11642)">
    <path d="M10.6421 16.125V13.875L8.76709 12L10.2671 10.5V7.875H12.8921L14.3921 6.375L15.8921 7.875H18.5171V10.5L20.0171 12L18.5171 13.5V16.125H15.8921L14.3921 17.625L12.8921 16.125H10.6421Z" fill="#FEEEC5"/>
  </g>
  <path d="M17.6156 8.77477L15.7046 8.84977L16.3046 7.31227L19.1696 7.22241L17.6156 8.77477Z" fill="#FEE3A3"/>
  <path d="M11.1686 8.77477L13.0796 8.84977L12.4796 7.31227L9.61459 7.22241L11.1686 8.77477Z" fill="#FEFDF9"/>
  <path d="M11.1671 8.7675L11.2421 10.6875L9.7046 10.0875L9.61475 7.2225L11.1671 8.7675Z" fill="#FEE3A3"/>
  <path d="M17.6171 8.7675L17.5421 10.6875L19.0796 10.0875L19.1694 7.2225L17.6171 8.7675Z" fill="#E1A131"/>
  <path d="M11.1686 15.2252L13.0796 15.1502L12.4796 16.6877L9.61459 16.7776L11.1686 15.2252Z" fill="#B77E25"/>
  <path d="M17.6156 15.2252L15.7046 15.1502L16.3046 16.6877L19.1696 16.7776L17.6156 15.2252Z" fill="#D09232"/>
  <path d="M11.1671 15.2325L11.2421 13.3125L9.7046 13.9125L9.61475 16.7775L11.1671 15.2325Z" fill="#FEE3A3"/>
  <path d="M17.6171 15.2325L17.5421 13.3125L19.0796 13.9125L19.1694 16.7775L17.6171 15.2325Z" fill="#D09232"/>
  <path d="M14.3922 7.5V5.25L16.3047 7.3125L15.7047 8.85L14.3922 7.5Z" fill="#E1A131"/>
  <path d="M14.3922 16.5V18.75L16.3047 16.6875L15.7047 15.15L14.3922 16.5Z" fill="#B77E25"/>
  <path d="M14.392 7.5V5.25L12.4795 7.3125L13.0795 8.85L14.392 7.5Z" fill="#FEE3A3"/>
  <path d="M14.392 16.5V18.75L12.4795 16.6875L13.0795 15.15L14.392 16.5Z" fill="#D09232"/>
  <path d="M9.89209 12.0001L7.64209 12.0001L9.70459 10.0876L11.2421 10.6876L9.89209 12.0001Z" fill="#FEFDF9"/>
  <path d="M18.8921 12.0001L21.1421 12.0001L19.0796 10.0876L17.5421 10.6876L18.8921 12.0001Z" fill="#FEE3A3"/>
  <path d="M9.89209 11.9999L7.64209 11.9999L9.70459 13.9124L11.2421 13.3124L9.89209 11.9999Z" fill="#E1A131"/>
  <path d="M18.8921 11.9999L21.1421 11.9999L19.0796 13.9124L17.5421 13.3124L18.8921 11.9999Z" fill="#B77E25"/>
  <defs>
    <filter id="filter0_di_1435_11642" x="0" y="0" width="28.7842" height="32" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
      <feFlood flood-opacity="0" result="BackgroundImageFix"/>
      <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
      <feOffset dy="4"/>
      <feGaussianBlur stdDeviation="2"/>
      <feComposite in2="hardAlpha" operator="out"/>
      <feColorMatrix type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0.12 0"/>
      <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow_1435_11642"/>
      <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow_1435_11642" result="shape"/>
      <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
      <feOffset/>
      <feGaussianBlur stdDeviation="6"/>
      <feComposite in2="hardAlpha" operator="arithmetic" k2="-1" k3="1"/>
      <feColorMatrix type="matrix" values="0 0 0 0 0.778846 0 0 0 0 0.545221 0 0 0 0 0.183478 0 0 0 1 0"/>
      <feBlend mode="normal" in2="shape" result="effect2_innerShadow_1435_11642"/>
    </filter>
    <filter id="filter1_d_1435_11642" x="4.76709" y="4.375" width="19.25" height="19.25" filterUnits="userSpaceOnUse" color-interpolation-filters="sRGB">
      <feFlood flood-opacity="0" result="BackgroundImageFix"/>
      <feColorMatrix in="SourceAlpha" type="matrix" values="0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 127 0" result="hardAlpha"/>
      <feOffset dy="2"/>
      <feGaussianBlur stdDeviation="2"/>
      <feComposite in2="hardAlpha" operator="out"/>
      <feColorMatrix type="matrix" values="0 0 0 0 0.669872 0 0 0 0 0.459726 0 0 0 0 0.132042 0 0 0 0.6 0"/>
      <feBlend mode="normal" in2="BackgroundImageFix" result="effect1_dropShadow_1435_11642"/>
      <feBlend mode="normal" in="SourceGraphic" in2="effect1_dropShadow_1435_11642" result="shape"/>
    </filter>
  </defs>
</svg>),
};




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






function TutorialPopup({setShowTutorial}) {
  // 지금 보기
  const handleTutorialConfirm = async () => {
    try {
      await apiFetch('/users/me/ui-state', { 
        method: 'PATCH',
        body: JSON.stringify({ tutorialCompleted: true }),
      });

      setShowTutorial(false);
      openExternalLink(TUTORIAL_URL);
    } catch (error) {
      log.debug('튜토리얼 여부 전송 실패', error);
    }
  };
  

  // 나중에 보기
  const handleTutorialSkip = async () => {
    await apiFetch('/users/me/ui-state', {
      method: 'PATCH',
      body: JSON.stringify({ tutorialCompleted: true }),
    });
    setShowTutorial(false);
  };

  // 튜토리얼 이동
  const TUTORIAL_URL = 'https://clipzyguide.netlify.app/';

const openExternalLink = (url) => {
  if (url !== TUTORIAL_URL) {  // 단일 URL이면 직접 비교가 더 명확
    log.error('허용되지 않은 URL');
    return;
  }

  if (typeof chrome !== 'undefined' && chrome.tabs) {
    chrome.tabs.create({ url });
  } else {
    window.open(url, '_blank', 'noopener,noreferrer');
  }
};


  return (
      <>
      {/* 어두워지기 */}
      <div className={style['exit-overlay']}></div>
      
      <div className={style.exit}>
        <div className={style.exit2}>
          <p className={style.exit3}>시작하기 전에 간단한 안내 도와드릴까요?</p>
          <div className={style.exit4}>
            <p className={style.exit5}>클립지의 기본적인 사용법을 적어두었어요! </p>
          </div>
        </div>


        <div className={style.exit6}>
          <button 
          className={style.exit7}
          onClick={handleTutorialConfirm}>
            <p className={style.exit8}>도움말 바로가기</p>
          </button>
          <button 
          className={style.exit9}
          onClick={handleTutorialSkip}
          >
            <p className={style.exit10}>나중에 구경할게요</p>
          </button>
        </div>
      </div>
      </>
  );
}









function SectionBadgeCard({ currentBadge, videoId, videoTitle, videoDuration, channelName }) {
  if (!currentBadge) return null;

  // 썸네일 클릭 시 메인 탭에서 유튜브 영상 열기
  const handleClick = (e) => {
    // e.preventDefault() = HTML 요소의 기본 동작(default behavior)을 막는 함수
    // <a> 태그는 클릭하면 href로 이동하는게 기본 동작
    e.preventDefault();
    openYoutubeVideo(videoId);
  };



  return (
    <article className={style.masteryCard}>

        <div className={style.masteryVideo}>
          <a 
          className={style.masteryThumbnail}
          href={`https://youtube.com/watch?v=${videoId}`}
          onClick={handleClick}
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
                <p className={style.masteryDurationText}>{formatDuration(videoDuration)}</p>
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
          {/* <span className={style.sectionBadgeIcon}> */}
          {/* <span> */}
            {BADGE_ICONS[currentBadge] || null}
          {/* </span> */}

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
  // 썸네일 클릭 시 메인 탭에서 유튜브 영상 열기
  const handleClick = (e) => {
    // e.preventDefault() = HTML 요소의 기본 동작(default behavior)을 막는 함수
    // <a> 태그는 클릭하면 href로 이동하는게 기본 동작
    e.preventDefault();
    openYoutubeVideo(videoId);
  };

  return (
    <article className={style.recommendedSectionBox3}>
      <a 
      className={style.recommendedSectionLink}
      href={`https://youtube.com/watch?v=${videoId}`}
      onClick={handleClick}
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
            <p className={style.recommendedSectionDuration3}>{formatDuration(duration)}</p>
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
  // 썸네일 클릭 시 메인 탭에서 유튜브 영상 열기
  const handleClick = (e) => {
    // e.preventDefault() = HTML 요소의 기본 동작(default behavior)을 막는 함수
    // <a> 태그는 클릭하면 href로 이동하는게 기본 동작
    e.preventDefault();
    openYoutubeVideo(videoId);
  };

  return (
    <a 
    className={style.smallVideoCard2}
    href={`https://youtube.com/watch?v=${videoId}`}
    onClick={handleClick}
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
            <p className={style.smallVideoCardDuration3}>{formatDuration(duration)}</p>
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

  const [showTutorial, setShowTutorial] = useState(false);


  // 추천 영상 관리, 나중에 다시 넣기
  const [recommendedData, setRecommendedData] = useState([]);


  // 첫 번째와 나머지 분리, 나중에 다시 넣기
  const [firstVideo, ...restVideos] = recommendedData;

  // ai 채팅방 중복 클릭 막기
  const [isAiBlocked, setIsAiBlocked] = useState(false);


  // 팝업 메시지
  const [popupMessage, setPopupMessage] = useState('');
  const [popupCount, setPopupCount] = useState(0);

  const showPopup = (msg) => {
    setPopupMessage(msg);
    setPopupCount(prev => prev + 1);
  };


  // 컴포넌트 mount 시 내 정보 조회 API 호출
  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await apiFetch('/users/me', { method: 'GET' });
        // log.debug('/users/me 값', result);

        setUsersData(result.data);
      } catch (error) {
        log.debug('데이터 로딩 실패:', error);
      }
    };

    fetchData();
  }, []);







  // 컴포넌트 mount 시 추천영상 API 호출, 나중에 다시 넣기 오류
  useEffect(() => {
    const fetchData = async () => {
      try {
        // // ? 써서 recommended + limit이라는 옵션값 5 로 인식
        // const result = await apiFetch('/videos/recommended?limit=5', { method: 'GET' });
        const result = await apiFetch('/videos/recommended', { method: 'GET' });
        setRecommendedData(result.data.recommendations);
      } catch (error) {
        log.debug('데이터 로딩 실패:', error);
      }
    };

    fetchData();
  }, []);



  // 컴포넌트 mount 시 팝업 확인 여부 조회
  useEffect(() => {
    const check = async () => {
      const res = await apiFetch('/users/me/ui-state', { method: 'GET' });

      if (!res.data?.tutorialCompleted) {
        setShowTutorial(true);
      }
    };
  check();
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


// 디폴트 페이지의 AI 방 입장 버튼
const onAiChatPage = async () => {
  if (isAiBlocked) return;


  // 버튼 비활성화
  setIsAiBlocked(true);
  // log.debug('디폴트에서 채팅방 들어감');

try {
  // 유저별 캐시 확인
  const hasWords = await loadUserData('hasWords');
  // const { hasWords } = await chrome.storage.local.get('hasWords');

  // true면 바로 진입하기 (API 호출 없음)
  if (hasWords === true) {
    navigate('/ai');
    return;
  }

  // false면 API 호출 없이 바로 차단 (나중에 단어 삭제 생기면 전부 삭제됐을 때 false로 되돌리는것도 필요함)
  if (hasWords === false) {
    showPopup('수집된 단어가 없습니다!');

    
    return;
  }

  // undefined면 API로 확인 최초 1회
  try {
    const res = await apiFetch('/chats/words', { method: 'GET' });

    // 결과를 유저별 캐시에 저장
    // await chrome.storage.local.set({ hasWords: res.data.hasWords });
    await saveUserData('hasWords', res.data.hasWords);
    // await setHasWordsCache(res.data.hasWords);

    if (!res.data.hasWords) {
      showPopup(res.data.guideMessage || '수집된 단어가 없습니다!');
      return;
    }

    // 단어 있으면 AI 방으로 이동 (받은 데이터도 같이 전달)
    navigate('/ai', { state: { wordsData: res.data } });
    
  } catch (error) {
    showPopup('단어 조회에 실패했습니다');
    log.debug('단어 조회 실패', error);
  }
} catch (error) {
  showPopup('AI 채팅방 입장에 실패했습니다');
  log.debug('ai 방 입장 실패', error)
} finally {
  // 버튼 활성화
  setIsAiBlocked(false);
}
};






  return (
    // 전체 박스
    <div className={style.main}>

      {/* ai 채팅 진입 시 로딩 */}
      {isAiBlocked && <TestSpinner />}

      {popupMessage && 
      <Toast 
        message={popupMessage}
        count={popupCount}
        onClose={() => setPopupMessage('')}
      />}

      {showTutorial && <TutorialPopup setShowTutorial={setShowTutorial} />}

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
          <button 
          className={style.tutorial}
          onClick={() => setShowTutorial(true)}>
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path d="M12 3C7.041 3 3 7.041 3 12C3 16.959 7.041 21 12 21C16.959 21 21 16.959 21 12C21 7.041 16.959 3 12 3ZM12 19.2C8.031 19.2 4.8 15.969 4.8 12C4.8 8.031 8.031 4.8 12 4.8C15.969 4.8 19.2 8.031 19.2 12C19.2 15.969 15.969 19.2 12 19.2Z" fill="#F9F8F1"/>
              <path d="M11.2471 14.4983H12.7758V16H11.2471V14.4983ZM12.9822 7.17725C11.3924 6.61409 9.56561 7.44005 9 8.98685L10.4369 9.49744C10.5745 9.12201 10.865 8.82166 11.2548 8.64896C11.6446 8.47626 12.0803 8.45373 12.4624 8.58889C12.7548 8.69385 13.0079 8.8836 13.188 9.133C13.3682 9.38241 13.467 9.67971 13.4713 9.98551C13.4713 10.7664 12.2025 11.3821 11.7592 11.5398C11.4535 11.6449 11.2471 11.9303 11.2471 12.2456V14H12.7758V12.7412C13.5707 12.3583 15 11.4722 15 9.978C14.9934 9.36498 14.7968 8.7685 14.4362 8.2681C14.0757 7.7677 13.5685 7.38716 12.9822 7.17725Z" fill="#F9F8F1"/>
            </svg>
          </button>

          
          <button className={style.topLeftButton}
          disabled={isAiBlocked}
          onClick={onAiChatPage}
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
  <path d="M14 5C17.866 5 21 8.13401 21 12C21 15.866 17.866 19 14 19H10.9883C10.8533 19.0676 10.7192 19.1432 10.582 19.2295C9.99809 19.6424 9.58506 20.2014 8.83203 21.0166C8.09716 21.4857 7.09185 21.0021 7.0918 20.1797V18.3662C4.67828 17.2617 3.00098 14.8278 3.00098 12C3.00098 8.13417 6.13423 5.00026 10 5H14Z" fill="#FEFDF9"/>
  <path d="M13.3047 14.8281H12.0469L11.6211 13.5234H9.57422L9.14844 14.8281H7.88281L9.84375 9.17188H11.3516L13.3047 14.8281ZM15.1172 14.8281H13.9453V9.17188H15.1172V14.8281ZM9.87109 12.6094H11.3242L10.6172 10.4453H10.5781L9.87109 12.6094Z" fill="#08B682"/>
</svg>
          </button>
          <button 
          className={style.topRightButton}
          onClick={onMyPage}
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="15" height="16" viewBox="0 0 15 16" fill="none">
  <path d="M8.45801 9.1426C11.9924 9.1428 14.8574 12.0085 14.8574 15.543C14.8574 15.7954 14.6528 16 14.4004 16H0.45801C0.20556 16 5e-05 15.7954 0 15.543C0 12.0084 2.86581 9.1426 6.40041 9.1426H8.45801ZM7.42871 0C9.63781 0 11.4287 1.79086 11.4287 4C11.4287 6.2091 9.63781 8 7.42871 8C5.21972 7.9998 3.42871 6.209 3.42871 4C3.42871 1.79096 5.21972 0.00016 7.42871 0Z" fill="#FEFDF9"/>
</svg>
          </button>
        </div>
      </div>




      {/* 전체 내용 */}
      <div className={style.container}>

        {/* 유저 프로필 */}
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




        <div className={style.videoSection}>
          {usersData?.ongoingMastery?.currentBadge ? <SectionBadgeCard { ...usersData.ongoingMastery }></SectionBadgeCard> : null }

          <div className={style.recommendedSection}>


            <div className={style.recommendedSectionBox}>
              <div className={style.recommendedTitle}>
                <div className={style.recommendedTitle2}>
                  <svg xmlns="http://www.w3.org/2000/svg" width="23" height="23" viewBox="0 0 23 23" fill="none">
  <path d="M11.6471 0.41721C17.744 0.417582 22.6862 5.36163 22.6862 11.4592C22.6858 17.5565 17.7438 22.4994 11.6471 22.4997C5.55012 22.4997 0.606941 17.5567 0.606581 11.4592C0.606581 11.1755 0.618045 10.8936 0.640273 10.6155C0.665945 10.2954 0.940283 10.0573 1.26137 10.0573C1.64929 10.0577 1.94593 10.4012 1.91908 10.7883C1.90366 11.0097 1.89564 11.2339 1.89564 11.4592C1.896 16.8453 6.26146 21.2107 11.6471 21.2107C17.0324 21.2103 21.3967 16.8451 21.3971 11.4592C21.3971 6.07304 17.0327 1.70665 11.6471 1.70627C11.422 1.70627 11.1975 1.71454 10.9762 1.72971C10.589 1.75601 10.2453 1.45871 10.2453 1.07053C10.2454 0.749618 10.4835 0.476337 10.8034 0.450901C11.0815 0.428973 11.3635 0.41721 11.6471 0.41721ZM13.736 15.1404H12.3239L11.7086 13.321H8.93861L8.32777 15.1404H6.91566L9.50697 7.77951H11.1344L13.736 15.1404ZM15.9318 15.1404H14.6105V7.77951H15.9318V15.1404ZM9.29457 12.2531H11.3527L10.3507 9.28537H10.2907L9.29457 12.2531ZM4.4767 0.311741C4.54544 -0.0972571 5.13007 -0.106529 5.21205 0.300022L5.27943 0.635472C5.68492 2.64634 7.33556 4.16964 9.37221 4.41477C9.7578 4.46151 9.80116 5.00459 9.42787 5.11203L8.96058 5.2468C7.10696 5.77891 5.68432 7.26854 5.23842 9.14475L5.21937 9.21946C5.12358 9.62014 4.54946 9.60923 4.46937 9.20481C4.09333 7.30606 2.6786 5.78225 0.813124 5.26584L0.260878 5.11203C-0.115769 5.00773 -0.0731594 4.46129 0.315077 4.41623C2.41283 4.17267 4.09713 2.57156 4.4474 0.488987L4.4767 0.311741Z" fill="#F5F9F8"/>
</svg>
                </div>
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