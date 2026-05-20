import { useAuth } from "../hooks/useAuth";
import { useNavigate } from "react-router-dom";
import { apiFetch } from "../utils/api";
import { useState, useEffect } from "react";


function truncateEmail(email, maxLength) {
  if (email.length <= maxLength) return email;
  
  const [local, domain] = email.split('@');
  const domainPart = '@' + domain;

  // "..." 3자 빼기
  const remaining = maxLength - domainPart.length - 3;

  if (remaining <= 0) return email.slice(0, maxLength) + '...';

  return local.slice(0, remaining) + '...' + domainPart;
}



function DefaultPage() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    // ProtectedRoute가 자동으로 /login 보내지만, 명시적으로 해도 됨
    navigate('/login');
  };

  // 상태로 데이터 관리
  const [data, setData] = useState(null);


  // 컴포넌트 mount 시 API 호출
  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await apiFetch('/users/me/dashboard', { method: 'GET' });
        setData(result.data);
      } catch (error) {
        console.error('데이터 로딩 실패:', error);
      }
    };

    fetchData();
  }, []);




  // 데이터 없으면 에러 화면
  if (!data) {
    return <div>데이터를 불러올 수 없습니다.</div>;
  }

  // 데이터에서 꺼내기
  const { user, levelInfo, stats } = data;

  const MAX_LEVEL = 99;
  const nextLevel = levelInfo.currentLevel >= MAX_LEVEL ? 'MAX' : `LV.${levelInfo.currentLevel + 1}`;








  return (
    // 전체 박스
    <div style={{
      display: 'flex',
      width: '402px',
      height: '1247px',
      flexDirection: 'column',
      alignItems: 'center',
      gap: '24px',
      flexShrink: '0',
    }}>

      {/* 상단 보라색 전체 바 */}
      <div style={{
        display: 'flex',
        height: '54px',
        padding: '15px 16px',
        flexDirection: 'column',
        alignItems: 'flex-start',
        gap: '10px',
        flexShrink: '0',
        alignSelf: 'stretch',
        background: '#C084FC',
      }}>

        {/* 상단 양쪽 버튼 박스 */}
        <div style={{
          display: 'flex',
          width: '370px',
          justifyContent: 'space-between',
          alignItems: 'center',
        }}>

          {/* 상단 왼쪽 버튼 */}
          <div style={{
            display: 'flex',
            width: '80px',
            height: '24px',
            flexDirection: 'column',
            alignItems: 'flex-start',
            flexShrink: '0',
            background: '#E1E1E1',
          }}>
          </div>

          {/* 상단 오른쪽 버튼 박스 */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
          }}>

            {/* 왼 버튼1 */}
            <div style={{
              display: 'flex',
              width: '24px',
              height: '24px',
              alignItems: 'center',
              background: '#E1E1E1',
            }}>
            </div>

            {/* 오른 버튼1 */}
            <div style={{
              display: 'flex',
              width: '24px',
              height: '24px',
              alignItems: 'center',
              background: '#E1E1E1',
            }}>
            </div>
          </div>
        </div>
      </div>

      {/* 내 정보 및 레벨 전체 박스 */}
      <div style={{
        display: 'flex',
        width: '370px',
        height: '244px',
        padding: '24px 16px 20px 16px',
        flexDirection: 'column',
        alignItems: 'flex-start',
        gap: '16px',
        flexShrink: '0',
        borderRadius: '24px',
        background: '#FFF',
      }}>

        {/* 내 정보 박스 */}
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'flex-start',
          gap: '10px',
          alignSelf: 'stretch',
        }}>

          {/* 칭호 및 로그아웃 박스 */}
          <div style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '94px',
            alignSelf: 'stretch',
          }}>

            {/* 칭호 박스 */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '8px',
            }}>

              {/* 여행 수집가 칭호 박스 */}
              <div style={{
                display: 'flex',
                padding: '6px 8px',
                justifyContent: 'center',
                alignItems: 'center',
                borderRadius: '999px',
                background: '#E9E5FF',
              }}>

                {/* 여행 수집가 칭호 이름 */}
                <p style={{
                  color: '#7C5CBF',
                  fontFamily: 'Pretendard',
                  fontSize: '12px',
                  fontStyle: 'normal',
                  fontWeight: '400',
                  lineHeight: 'normal',
                }}>
                  여행 영어 수집가
                </p>
              </div>

              {/* 연속 출석 박스 */}
              <div style={{
                display: 'flex',
                padding: '6px 8px',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '4px',
                borderRadius: '999px',
                background: '#FFE8D6',
              }}>

                {/* 7일 연속 칭호 아이콘 */}
                <div style={{
                  // display: 'flex',
                  width: '12px',
                  height: '12px',
                  // alignItems: 'center',
                  // gap: '10px',
                }}>
                </div>

                {/* 7일 연속 칭호 이름 */}
                <p style={{
                  color: '#A03030',
                  fontFamily: 'Pretendard',
                  fontSize: '12px',
                  fontStyle: 'normal',
                  fontWeight: '400',
                  lineHeight: 'normal',
                }}>
                  {stats.streak}일 연속
                </p>
              </div>
            </div>

            {/* 로그아웃 전체 박스 */}
            <button 
            onClick={handleLogout}
            style={{
              display: 'flex',
              width: '66px',
              height: '32px',
              borderRadius: '999px',
              border: '1px solid #E7E6EB',
              justifyContent: 'center',
              alignItems: 'center',
            }}>

              {/* 로그아웃 문구 */}
              <p style={{
                display: 'flex',
                // width: '42px',
                // height: '12.444px',
                // flexDirection: 'column',
                justifyContent: 'center',
                color: '#01030D',
                fontFamily: 'Pretendard',
                fontSize: '12px',
                fontStyle: 'normal',
                fontWeight: '400',
                lineHeight: 'normal',
              }}>
                로그아웃
              </p>
            </button>
          </div>

          {/* 프로필 전체 박스 */}
          <div style={{
            display: 'flex',
            alignItems: 'flex-start',
            gap: '8px',
          }}>

            {/* 프로필 이미지 */}
            <div style={{
              display: 'flex',
              width: '60px',
              height: '60px',
              alignItems: 'center',
              gap: '10px',
              borderRadius: '999px',
              background: '#E1E1E1',
            }}>
            </div>

            {/* 프로필 이름 및 이메일 및 출석 박스 */}
            <div style={{
              display: 'flex',
              width: '135px',
              flexDirection: 'column',
              alignItems: 'flex-start',
              gap: '4px',
            }}>

              {/* 프로필 이름 */}
              <p style={{
                alignSelf: 'stretch',
                color: '#01030D',
                fontFamily: 'Pretendard',
                fontSize: '16px',
                fontStyle: 'normal',
                fontWeight: '700',
                lineHeight: 'normal',
              }}>
                {user.name}
              </p>

              {/* 이메일 및 출석 박스 */}
              <div style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'flex-start',
                gap: '4px',
                alignSelf: 'stretch',
              }}>

                {/* 이메일 */}
                <p style={{
                  alignSelf: 'stretch',
                  color: '#9198A3',
                  fontFamily: 'Pretendard',
                  fontSize: '14px',
                  fontStyle: 'normal',
                  fontWeight: '400',
                  lineHeight: 'normal',
                }}>
                  {truncateEmail(user.email, 15)}
                </p>

                {/* 연속 출석일 */}
                <p style={{
                  alignSelf: 'stretch',
                  color: '#3D434F',
                  fontFamily: 'Pretendard',
                  fontSize: '14px',
                  fontStyle: 'normal',
                  fontWeight: '400',
                  lineHeight: 'normal',
                }}>
                  연속 {stats.streak}일 출석 (총 30일)
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* 다음 레벨 안내 박스 */}
        <div style={{
          display: 'flex',
          padding: '16px',
          flexDirection: 'column',
          alignItems: 'flex-start',
          gap: '8px',
          alignSelf: 'stretch',
          borderRadius: '16px',
          border: '1px solid #E7E6EB',
          background: '#FFF',
          boxShadow: '0 4px 12px 0 rgba(206, 210, 223, 0.10)',
        }}>

          {/* 레벨바 전체 박스 */}
          <div style={{
            display: 'flex',
            alignItems: 'flex-start',
            alignContent: 'flex-start',
            gap: '8px 230px',
            alignSelf: 'stretch',
            flexWrap: 'wrap',
          }}>

            {/* 현재 레벨, 다음 레벨 박스 */}
            <div style={{
              display: 'flex',
              width: '306px',
              justifyContent: 'space-between',
              alignItems: 'center',
            }}>

              {/* 현재 레벨 */}
              <p style={{
                display: 'flex',
                width: '114px',
                height: '14px',
                flexDirection: 'column',
                justifyContent: 'center',
                flexShrink: '0',
                color: '#01030D',
                fontFamily: 'Pretendard',
                fontSize: '14px',
                fontStyle: 'normal',
                fontWeight: '500',
                lineHeight: 'normal',
                letterSpacing: '-0.028px',
              }}>
                LV. {levelInfo.currentLevel}
              </p>

              {/* 다음 레벨 */}
              <p style={{
                display: 'flex',
                width: '38px',
                height: '14px',
                flexDirection: 'column',
                justifyContent: 'center',
                flexShrink: '0',
                color: '#9F9EB0',
                textAlign: 'right',
                fontFamily: 'Pretendard',
                fontSize: '14px',
                fontStyle: 'normal',
                fontWeight: '500',
                lineHeight: 'normal',
                letterSpacing: '-0.028px',
              }}>
                {nextLevel}
              </p>
            </div>

            {/* 레벨 바 박스 */}
            <div style={{
              display: 'flex',
              width: '306px',
              alignItems: 'flex-start',
            }}>

              {/* 회색 전체 레벨바 */}
              <div style={{
                width: '306px',
                height: '6px',
                // position: 'absolute',
                borderRadius: '999px',
                background: '#EDEDF2',
              }}>

                {/* 보라색 현재 레벨바 */}
                <div style={{
                  width: `${levelInfo.progressPercentage}%`,
                  height: '6px',
                  flexShrink: '0',
                  borderRadius: '999px',
                  background: '#9B87E8',
                }}>
                </div>
              </div>


            </div>

            {/* 다음 레벨 안내 */}
            <p style={{
              alignSelf: 'stretch',
              color: '#9F9EB0',
              fontFamily: 'Pretendard',
              fontSize: '14px',
              fontStyle: 'normal',
              fontWeight: '400',
              lineHeight: 'normal',
            }}>
              다음 레벨까지 {levelInfo.progressPercentage}%남았어요!
            </p>
          </div>
        </div>
      </div>

      {/* 도전 중인 마스터리 뱃지 박스 */}
      <div style={{
        display: 'flex',
        width: '370px',
        padding: '24px 16px 20px 16px',
        flexDirection: 'column',
        alignItems: 'flex-start',
        gap: '16px',
        borderRadius: '24px',
        background: '#FFF',
      }}>

        {/* 마스터리 뱃지 안내 박스 */}
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
        }}>

          {/* 마스터리 뱃지 아이콘 박스 */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
          }}>

            {/* 마스터리 뱃지 아이콘 박스 */}
            <div style={{
              width: '24px',
              height: '24px',
              // aspectRatio: '1/1',
              // background: '#E1E1E1',
                        background: 'transparent',
                        border: 'none',
            }}>
              <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path d="M13.08 8.63L12 6.44L10.92 8.63L8.5 8.98L10.25 10.69L9.84 13.1L12 11.96L14.16 13.1L13.75 10.69L15.5 8.98L13.08 8.63Z" fill="#E2C4AE"/>
                <path d="M17.16 3.01C16.9835 2.70429 16.73 2.45017 16.4247 2.27296C16.1194 2.09575 15.773 2.00164 15.42 2H8.58002C7.86002 2 7.20002 2.39 6.84002 3.01L3.41002 9.01C3.06002 9.62 3.06002 10.38 3.41002 10.99L6.84002 16.99C6.88002 17.07 6.94002 17.13 7.00002 17.19V20.99C7.00002 21.34 7.18002 21.66 7.47002 21.84C7.76002 22.02 8.13002 22.04 8.44002 21.88L11.99 20.1L15.54 21.88C15.6922 21.9576 15.8617 21.9948 16.0323 21.9881C16.203 21.9814 16.369 21.9309 16.5146 21.8416C16.6601 21.7522 16.7803 21.627 16.8635 21.4779C16.9468 21.3288 16.9903 21.1608 16.99 20.99V17.19C17.04 17.12 17.1 17.06 17.15 16.99L20.58 10.99C20.93 10.38 20.93 9.62 20.58 9.01L17.15 3.01H17.16ZM15.42 16H8.58002L5.15002 10L8.58002 4H15.42L18.85 10L15.42 16Z" fill="#E2C4AE"/>
              </svg>
            </div>
          </div>

          {/* 마스터리 뱃지 안내 문구 */}
          <div style={{
            display: 'flex',
            width: '175px',
            height: '20px',
            flexDirection: 'column',
            justifyContent: 'center',
            color: '#01030D',
            fontFamily: 'Pretendard',
            fontSize: '18px',
            fontStyle: 'normal',
            fontWeight: '700',
            lineHeight: 'normal',
            letterSpacing: '0.5px',
            textTransform: 'uppercase',
          }}>
            도전 중인 마스터리 뱃지
          </div>
        </div>

        {/* 실제 영상 전체 박스 */}
        <div style={{
          display: 'flex',
          alignItems: 'flex-start',
          gap: '10px',
          alignSelf: 'stretch',
        }}>

          {/* 영상 전체 길이 박스 */}
          <div style={{
            display: 'flex',
            width: '156.279px',
            height: '88px',
            padding: '65px 6px 6px 121px',
            flexDirection: 'column',
            alignItems: 'flex-start',
            gap: '10px',
            borderRadius: '10px',
            background: '#CCC',
          }}>

            {/* 영상 전체 길이 */}
            <div style={{
              display: 'flex',
              width: '29.767px',
              height: '16.762px',
              flexDirection: 'column',
              alignItems: 'flex-start',
              gap: '10px',
              flexShrink: '0',
              borderRadius: '4px',
              background: 'rgba(15, 15, 15, 0.50)',
            }}>
            </div>
          </div>

          {/* 영상 제목 및 채널명 박스 */}
          <div style={{
            display: 'flex',
            width: '168px',
            flexDirection: 'column',
            alignItems: 'flex-start',
          }}>

            {/* 영상 제목 */}
            <p style={{
              display: 'flex',
              height: '40.042px',
              flexDirection: 'column',
              justifyContent: 'center',
              alignSelf: 'stretch',
              color: '#01030D',
              fontFamily: 'Roboto',
              fontSize: '14px',
              fontStyle: 'normal',
              fontWeight: '700',
              lineHeight: '20px',
              letterSpacing: '-0.5px',
            }}>
              Contrary to popular belief, <span>Lorem Ipsum is not simply ...</span>
            </p>

            {/* 채널명 */}
            <p style={{
              display: 'flex',
              height: '20.021px',
              flexDirection: 'column',
              justifyContent: 'center',
              alignSelf: 'stretch',
              color: '#01030D',
              fontFamily: 'Roboto',
              fontSize: '12px',
              fontStyle: 'normal',
              fontWeight: '400',
              lineHeight: '20px',
              letterSpacing: '-0.5px',
            }}>
              Adventure Time
            </p>
          </div>
        </div>
      </div>

      {/* 추천하는 영상 전체 박스 */}
      {/* <div style={{
        display: 'flex',
        width: '370px',
        height: '673px',
        padding: '24px 16px 20px 16px',
        alignItems: 'flex-start',
        gap: '10px',
        flexShrink: '0',
        borderRadius: '24px',
        background: '#FFF',
      }}> */}

        {/* 추천하는 영상 하위 박스 */}
        {/* <div style={{
          display: 'flex',
          width: '338px',
          flexDirection: 'column',
          alignItems: 'flex-start',
          gap: '16px',
          flexShrink: '0',
        }}> */}

          {/* 추천하는 영상 하위 하위 박스 */}
          {/* <div style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'flex-start',
            gap: '28px',
            alignSelf: 'stretch',
          }}> */}

            {/* 추천하는 영상 문구 박스
            <div style={{
              display: 'flex',
              paddingLeft: '4px',
              justifyContent: 'center',
              alignItems: 'center',
            }}>

              추천하는 영상 문구
              <p style={{
                color: '#01030D',
                fontFamily: 'Pretendard',
                fontSize: '18px',
                fontStyle: 'normal',
                fontWeight: '700',
                lineHeight: 'normal',
                letterSpacing: '0.5px',
                textTransform: 'uppercase',
              }}>
                추천하는 영상
              </p>
            </div> */}

            {/* 추천하는 영상 전체 */}
          {/* </div> */}
        {/* </div> */}
      {/* </div> */}
    </div>
  );
}

export default DefaultPage;