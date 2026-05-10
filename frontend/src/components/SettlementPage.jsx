import happy from '../imgs/image_703.png';
import frog2 from '../imgs/image_750.png';
import { apiFetch } from '../utils/api';
import { useState, useEffect } from 'react';

function SettlementPage({ data, videoId, videoTitle, channelName, duration, onExitPage , onGoBackToQuiz }) {
  // 영상 전체 길이를 00:00:00 형태로 바꿔야 함(오류)
  // 해당 영상의 썸네일 이미지
  const thumbnailUrl = `https://img.youtube.com/vi/${videoId}/mqdefault.jpg`;
  
  const [wordCount, setWordCount] = useState(0);

  useEffect(() => {
    const fetchWordCount = async () => {
      const result = await apiFetch('/words/my-collection');
      setWordCount(result.data.pagination.totalCount);
    };
    fetchWordCount();
  }, []);
  
  return (
    // 전체 박스
    <div style={{
      display: 'flex',
      width: '402px',
      flexDirection: 'column',
      justifyContent: 'center',
      alignItems: 'center',
      background: '#F5F5F5'
    }}>

      {/* 전체 내용 박스 */}
      <div
      style={{
        display: 'flex',
        width: '402px',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '24px',
        flexShrink: '0'
      }}>

        {/* 뱃지 진행률까지 담는 박스 */}
        <div style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: '62px',
          alignSelf: 'stretch',
        }}>

          {/* 퀴즈 정산 바 전체 박스 */}
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '23px',
            alignSelf: 'stretch',
          }}>

            {/* 상단 보라색 퀴즈 정산 바 */}
            <div style={{
              display: 'flex',
              height: '54px',
              padding: '9px 161px 9px 16px',
              flexDirection: 'column',
              alignItems: 'flex-start',
              gap: '10px',
              alignSelf: 'stretch',
              background: '#C084FC',
            }}>

              {/* 버튼과 페이지 제목 박스 */}
              <div style={{
                display: 'flex',
                width: '225px',
                justifyContent: 'space-between',
                alignItems: 'center',
              }}>

                {/* 왼쪽 버튼? 2개 박스 */}
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                }}>

                  {/* 이전페이지로 돌아가기 */}
                  <button
                  // 나중에 페이지별로 고치기 (오류)
                  onClick={() => onGoBackToQuiz()}
                  style={{
                    display: 'flex',
                    width: '24px',
                    height: '24px',
                    alignItems: 'center',
                    // background: '#E1E1E1',
                    background: 'transparent',
                    border: 'none',
                  }}>
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                      <path d="M7.84 13.75L9.17 12.26L6.64 10.01H15.01C17.22 10.01 19.01 11.8 19.01 14.01C19.01 16.22 17.22 18.01 15.01 18.01H12.01V20.01H15.01C18.32 20.01 21.01 17.32 21.01 14.01C21.01 10.7 18.32 8.01 15.01 8.01H6.63L9.16 5.76L7.83 4.27L2.49 9.02L7.83 13.77L7.84 13.75Z" fill="black"/>
                    </svg>
                  </button>

                  {/* 디폴트페이지로 이동 */}
                  <button
                  onClick={() => {
                        console.log('1. 클릭됨');
    console.log('2. onExitPage:', onExitPage);
    console.log('3. typeof:', typeof onExitPage);
                    onExitPage()}}
                  style={{
                    display: 'flex',
                    width: '24px',
                    height: '24px',
                    alignItems: 'center',
                    // background: '#E1E1E1',
                    background: 'transparent',
                    border: 'none',
                  }}>
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                      <path d="M12.71 2.29C12.6175 2.1973 12.5076 2.12375 12.3866 2.07357C12.2657 2.02339 12.136 1.99756 12.005 1.99756C11.874 1.99756 11.7444 2.02339 11.6234 2.07357C11.5024 2.12375 11.3925 2.1973 11.3 2.29L3.29 10.29C3.19732 10.3834 3.12399 10.4943 3.07423 10.6161C3.02447 10.7379 2.99924 10.8684 3 11V20C3 21.1 3.9 22 5 22H9C9.55 22 10 21.55 10 21V15H14V21C14 21.55 14.45 22 15 22H19C20.1 22 21 21.1 21 20V11C21 10.73 20.89 10.48 20.71 10.29L12.71 2.29ZM16 20V15C16 13.9 15.1 13 14 13H10C8.9 13 8 13.9 8 15V20H5V11.41L12 4.41L19 11.41V20H16Z" fill="black"/>
                    </svg>
                  </button>
                </div>

                {/* 페이지 제목 박스 */}
                <div style={{
                  display: 'flex',
                  width: '80px',
                  height: '36px',
                  justifyContent: 'center',
                  alignItems: 'center',
                  flexShrink: '0',
                }}>

                  {/* 페이지 제목 */}
                  <p style={{
                    color: '#FFF',
                    textAlign: 'center',
                    fontFamily: 'Pretendard',
                    fontSize: '16px',
                    fontStyle: 'normal',
                    fontWeight: '700',
                    lineHeight: 'normal',
                    letterSpacing: '-0.032px',
                  }}>
                    퀴즈 정산
                  </p>
                </div>
              </div>
            </div>

            {/* 퀴즈 진행 전체 바 */}
            <div style={{
              display: 'flex',
              padding: '4px 0',
              alignItems: 'center',
              gap: '12px',
            }}>

              {/* 푼 문제 / 전체 문제 박스 */}
              <div style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                // width: '36px',
                // height: '14px',
              }}>

                {/* 푼 문제 */}
                <p style={{
                  // display: 'flex',
                  // width: '36px',
                  // height: '14px',
                  // flexDirection: 'column',
                  // justifyContent: 'center',
                  color: '#0D0C34',
                  textAlign: 'center',
                  fontFamily: 'Pretendard',
                  // 16에서 12로 변경
                  fontSize: '12px',
                  fontStyle: 'normal',
                  fontWeight: '700',
                  lineHeight: 'normal',
                  letterSpacing: '-0.032px',
                  whiteSpace: 'nowrap',
                }}>
                  {data.correctCount + data.wrongCount}
                </p>

                {/* /전체 문제 */}
                <p style={{
                  // display: 'flex',
                  // width: '36px',
                  // height: '14px',
                  // flexDirection: 'column',
                  // justifyContent: 'center',
                  color: '#9F9EB0',
                  fontFamily: 'Pretendard',
                  // 16에서 12로 변경
                  fontSize: '12px',
                  fontStyle: 'normal',
                  fontWeight: '500',
                  lineHeight: 'normal',
                  letterSpacing: '-0.032px',
                  whiteSpace: 'nowrap',
                  textAlign: 'center',
                }}>
                  /{data.totalQuizCount}
                </p>
              </div>

              {/* 진행도 바 전체 박스 */}
              <div style={{
                display: 'flex',
                width: '274px',
                flexDirection: 'column',
                alignItems: 'flex-start',
                // alignItems: 'center',
                alignSelf: 'stretch',
                position: 'relative',
              }}>

                {/* 진행도 바 색상 박스 */}
                <div style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  alignSelf: 'stretch',
                }}>

                  {/* 회색 전체 진행도 바 */}
                  <div style={{
                    display: 'flex',
                    width: '274px',
                    height: '14px',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    position: 'absolute',
                    // top: 0,
                    // left: 0,
                    borderRadius: '999px',
                    background: '#D8D8E2',
                  }}>
                  </div>

                  {/* 보라색 현재 진행도 바 */}
                  <div style={{
                    display: 'flex',
                    width: `${274 * ((data.correctCount + data.wrongCount) / data.totalQuizCount)}px`,
                    height: '14px',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    borderRadius: '999px',
                    background: '#9B87E8',
                    position: 'relative',
                    zIndex: 1,
                  }}>
                  </div>
                </div>

                {/* 현재 진행도에 붙는 개구리 이미지 */}
                <div style={{
                  width: '36px',
                  height: '36px',
                  aspectRatio: '1/1',
                  position: 'absolute',
                  right: '-18px',
                  top: '-11px',
                  background: `url(${frog2}) transparent -42.788px -1.005px / 228.637% 225.776% no-repeat`,
                }}>
                </div>
              </div>

              {/* 완주 표시 아이콘 */}
              <div
              style={{
                display: 'flex',
                width: '36px',
                height: '36px',
                alignItems: 'center',
                // background: '#E1E1E1',
                        background: 'transparent',
                        border: 'none',
              }}>
                    <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none">
                      <path d="M8.00415 2.05813C9.47139 2.09829 10.9059 2.50116 12.1799 3.23C14.0199 4.28 16.2099 4.43991 18.1799 3.64993L19.6301 3.06985C19.7819 3.00944 19.9463 2.98768 20.1086 3.00539C20.271 3.02314 20.4267 3.07971 20.5618 3.17141C20.6969 3.26317 20.8076 3.38736 20.884 3.53176C20.9602 3.67592 21.0003 3.83648 21.0002 3.99953V14.9995C21.0004 15.1998 20.9407 15.3962 20.8284 15.562C20.7161 15.7278 20.5563 15.8561 20.3704 15.9302L18.9202 16.5103C17.8402 16.9403 16.7199 17.1499 15.5999 17.1499C14.0699 17.1499 12.5499 16.7502 11.1799 15.9702C9.28314 14.8821 7.00757 14.7701 5.01001 15.6353V21.9898H3.01001V3.98977C3.01061 3.80518 3.06238 3.62434 3.15942 3.46731C3.25646 3.31031 3.39501 3.18321 3.55981 3.10012L3.76978 2.99953C5.08184 2.34136 6.53681 2.018 8.00415 2.05813ZM11.1799 4.97024C9.27995 3.88025 7.00023 3.76042 5.00024 4.63039H5.01001V13.4995C5.89997 13.2096 6.83006 13.0601 7.76001 13.0601C9.31063 13.0639 10.8337 13.4702 12.1799 14.2398C13.0821 14.7616 14.0915 15.0712 15.1311 15.1441C16.1709 15.2168 17.2138 15.0508 18.1799 14.6597L19.0002 14.3296V5.48L18.9202 5.51028C16.3702 6.52027 13.5499 6.33021 11.1799 4.97024Z" fill="black"/>
                      <path d="M8.00415 2.05813C9.47139 2.09829 10.9059 2.50116 12.1799 3.23C14.0199 4.28 16.2099 4.43991 18.1799 3.64993L19.6301 3.06985C19.7819 3.00944 19.9463 2.98768 20.1086 3.00539C20.271 3.02314 20.4267 3.07971 20.5618 3.17141C20.6969 3.26317 20.8076 3.38736 20.884 3.53176C20.9602 3.67592 21.0003 3.83648 21.0002 3.99953V14.9995C21.0004 15.1998 20.9407 15.3962 20.8284 15.562C20.7161 15.7278 20.5563 15.8561 20.3704 15.9302L18.9202 16.5103C17.8402 16.9403 16.7199 17.1499 15.5999 17.1499C14.0699 17.1499 12.5499 16.7502 11.1799 15.9702C9.28314 14.8821 7.00757 14.7701 5.01001 15.6353V21.9898H3.01001V3.98977C3.01061 3.80518 3.06238 3.62434 3.15942 3.46731C3.25646 3.31031 3.39501 3.18321 3.55981 3.10012L3.76978 2.99953C5.08184 2.34136 6.53681 2.018 8.00415 2.05813ZM11.1799 4.97024C9.27995 3.88025 7.00023 3.76042 5.00024 4.63039H5.01001V13.4995C5.89997 13.2096 6.83006 13.0601 7.76001 13.0601C9.31063 13.0639 10.8337 13.4702 12.1799 14.2398C13.0821 14.7616 14.0915 15.0712 15.1311 15.1441C16.1709 15.2168 17.2138 15.0508 18.1799 14.6597L19.0002 14.3296V5.48L18.9202 5.51028C16.3702 6.52027 13.5499 6.33021 11.1799 4.97024Z" stroke="black"/>
                    </svg>
              </div>
            </div>
          </div>

          {/* 뱃지 진행률 박스 */}
          <div style={{
            display: 'flex',
            width: '370px',
            flexDirection: 'column',
            alignItems: 'flex-end',
            position: 'relative',
          }}>

            {/* 뱃지 진행률 중간 박스 */}
            <div style={{
              display: 'flex',
              flexDirection: 'column',
              alignItems: 'flex-start',
              alignSelf: 'stretch'
            }}>

              {/* 최종 정산 마스코드 */}
              <div style={{
                width: '152px',
                height: '76px',
                aspectRatio: '2/1',
                // 투명으로 변경
                background: `url(${happy}) transparent -16.875px -18.483px / 230.642% 458.647% no-repeat`,
                // 비율 유지 + 전체 보이기
                backgroundSize: 'contain',
                // 가운데 정렬
                backgroundPosition: 'center',
              }}>
              </div>

              {/* 뱃지 진행률 중간중간 박스 */}
              <div style={{
                display: 'flex',
                padding: '24px 16px 20px 16px',
                flexDirection: 'column',
                alignItems: 'flex-start',
                gap: '16px',
                alignSelf: 'stretch',
                borderRadius: '24px',
                background: '#FFF',
              }}>

                {/* 뱃지 진행률 칸(제목 + 영상) */}
                <div style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  gap: '24px',
                  alignSelf: 'stretch',
                }}>

                  {/* 벳지 진행률 제목 칸 */}
                  <p style={{
                    alignSelf: 'stretch',
                    color: '#01030D',
                    fontFamily: 'Pretendard',
                    fontSize: '18px',
                    fontStyle: 'normal',
                    fontWeight: '700',
                    lineHeight: '18px', /* 100% */
                  }}>
                    뱃지 진행률
                  </p>

                  {/* 영상 + 영상 제목 박스 */}
                  <div style={{
                    display: 'flex',
                    flexDirection: 'column',
                    alignItems: 'flex-start',
                    alignSelf: 'stretch',
                    position: 'relative',
                  }}>

                    {/* 영상과 제목 사이에 gap추가 */}
                    <div style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '10px',
                    alignSelf: 'stretch',
                    }}>

                      {/* 영상 박스 */}
                      <div style={{
                        display: 'flex',
                        width: '156.279px',
                        height: '88px',
                        // padding: '65px 6px 6px 121px',
                        flexDirection: 'column',
                        alignItems: 'flex-start',
                        gap: '10px',
                        borderRadius: '10px',
                        background: '#CCC',
                        overflow: 'hidden',
                        position: 'relative',
                      }}>

                        {/* 실제 영상 썸네일 */}
                        <img 
                          src={thumbnailUrl} 
                          style={{
                            width: '100%',
                            height: '100%',
                            // 비율 유지하며 채우기
                            objectFit: 'cover',
                            borderRadius: '10px',
                            }}
                          />

                        {/* 영상의 길이 */}
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
                          position: 'absolute',
                          bottom: '6px', 
                          right: '6px',
                          }}>
                            {duration}
                        </div>
                      </div>

                      {/* 영상 제목과 채널 이름 박스 */}
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
                          // 폰트 추가하기
                          fontFamily: 'Roboto',
                          fontSize: '14px',
                          fontStyle: 'normal',
                          fontWeight: '700',
                          lineHeight: '20px', /* 142.857% */
                          letterSpacing: '-0.5px',
                        }}>
                          {videoTitle}
                        </p>

                        {/* 채널 이름 */}
                        <p style={{
                          display: 'flex',
                          height: '20.021px',
                          flexDirection: 'column',
                          justifyContent: 'center',
                          alignSelf: 'stretch',
                          color: '#01030D',
                          // 폰트 추가하기
                          fontFamily: 'Roboto',
                          fontSize: '12px',
                          fontStyle: 'normal',
                          fontWeight: '400',
                          lineHeight: '20px', /* 142.857% */
                          letterSpacing: '-0.5px',
                        }}>
                          {channelName}
                        </p>
                      </div>
                    </div>

                    {/* 이건 뭐지? */}
                    {/* <div style={{
                      display: 'flex',
                      width: '20px',
                      height: '20px',
                      alignItems: 'center',
                      position: 'absolute',
                      left: '8px',
                      top: '8px',
                      background: '#B0B0B0',
                      
                    }}>
                    </div> */}
                  </div>
                </div>

                {/* 퀴즈 정답 수 + 단어 수집 + 뱃지 도전률 보여주는 박스 */}
                <div style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  gap: '8px',
                  alignSelf: 'stretch',
                }}>

                  {/* 퀴즈 정답 수 + 단어 수집 사이에 gap */}
                  <div style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '8px',
                    alignSelf: 'stretch',
                  }}>

                    {/* 퀴즈 정답 수 배경 박스 */}
                    <div style={{
                      display: 'flex',
                      width: '165px',
                      height: '60px',
                      padding: '14px 36px 12px 16px',
                      flexDirection: 'column',
                      alignItems: 'flex-start',
                      gap: '10px',
                      borderRadius: '16px',
                      border: '1px solid #E7E6EB',
                      background: '#FFF',
                      boxShadow: '0 4px 4px 0 rgba(206, 210, 223, 0.16)',
                    }}>

                      {/* 퀴즈 정답 수 내부 박스 */}
                      <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                      }}>

                        {/* 네모 아이콘? */}
                        <div style={{
                          display: 'flex',
                          width: '32px',
                          height: '32px',
                          alignItems: 'center',
                          background: '#CFCFD7',
                        }}>
                        </div>

                        {/* 퀴즈 정답 수 표시 박스 */}
                        <div style={{
                          display: 'flex',
                          width: '73px',
                          flexDirection: 'column',
                          alignItems: 'flex-start',
                          gap: '6px',
                        }}>

                          {/* 퀴즈 정답 수 */}
                          <p style={{
                          display: 'flex',
                          height: '14px',
                          flexDirection: 'column',
                          justifyContent: 'center',
                          alignSelf: 'stretch',
                          color: '#0D0C34',
                          fontFamily: 'Pretendard',
                          fontSize: '14px',
                          fontStyle: 'normal',
                          fontWeight: '400',
                          lineHeight: 'normal',
                          letterSpacing: '-0.028px',
                          }}>
                            퀴즈 정답 수
                          </p>

                          {/* 실제 수치 */}
                          <p style={{
                            display: 'flex',
                            height: '14px',
                            flexDirection: 'column',
                            justifyContent: 'center',
                            alignSelf: 'stretch',
                            color: '#0D0C34',
                            fontFamily: 'Pretendard',
                            fontSize: '16px',
                            fontStyle: 'normal',
                            fontWeight: '700',
                            lineHeight: 'normal',
                            letterSpacing: '-0.032px',
                          }}>
                            {data.correctCount}/{data.totalQuizCount}
                          </p>
                        </div>
                      </div>
                    </div>

                    {/* 단어 수집 배경 칸 */}
                    <div style={{
                      display: 'flex',
                      width: '165px',
                      height: '60px',
                      padding: '13px 36px 13px 16px',
                      flexDirection: 'column',
                      alignItems: 'flex-start',
                      gap: '10px',
                      borderRadius: '16px',
                      border: '1px solid #E7E6EB',
                      background: '#FFF',
                      boxShadow: '0 4px 4px 0 rgba(206, 210, 223, 0.16)',
                    }}>

                      {/* 단어 수집 gap 주기 */}
                      <div style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '8px',
                      }}>

                        {/* 네모 아이콘 */}
                        <div style={{
                          display: 'flex',
                          width: '32px',
                          height: '32px',
                          alignItems: 'center',
                          background: '#CFCFD7',
                        }}>
                        </div>

                        {/* 단어 수집과 실제 수치 박스 */}
                        <div style={{
                          display: 'flex',
                          width: '73px',
                          flexDirection: 'column',
                          alignItems: 'flex-start',
                          gap: '6px',
                        }}>

                          {/* 단어 수집 */}
                          <p style={{
                            display: 'flex',
                            height: '14px',
                            flexDirection: 'column',
                            justifyContent: 'center',
                            alignSelf: 'stretch',

                            color: '#0D0C34',
                            fontFamily: 'Pretendard',
                            fontSize: '14px',
                            fontStyle: 'normal',
                            fontWeight: '400',
                            lineHeight: 'normal',
                            letterSpacing: '-0.028px',
                          }}>
                            단어 수집
                          </p>

                          {/* 실제 수치 */}
                          <p style={{
                            display: 'flex',
                            height: '14px',
                            flexDirection: 'column',
                            justifyContent: 'center',
                            alignSelf: 'stretch',
                            color: '#0D0C34',
                            fontFamily: 'Pretendard',
                            fontSize: '16px',
                            fontStyle: 'normal',
                            fontWeight: '700',
                            lineHeight: 'normal',
                            letterSpacing: '-0.032px',
                          }}>
                            {wordCount}개
                          </p>
                        </div>
                      </div>
                    </div>
                  </div>


                  {/* 뱃지 도전률 박스 */}
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

                    {/* 뱃지 실제 진행상황 박스 */}
                    <div style={{
                      display: 'flex',
                      alignItems: 'flex-start',
                      alignContent: 'flex-start',
                      gap: '8px 230px',
                      alignSelf: 'stretch',
                      flexWrap: 'wrap',
                    }}>


                      {/* 뱃지 등급 + 진행률 박스 */}
                      <div style={{
                        display: 'flex',
                        width: '306px',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                      }}>

                        {/* 뱃지 등급 */}
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
                          GOLD 도전 중
                        </p>

                        {/* 퍼센트 */}
                        <p style={{
                          display: 'flex',
                          width: '38px',
                          height: '14px',
                          flexDirection: 'column',
                          justifyContent: 'center',
                          flexShrink: '0',

                          color: '#0D0C34',
                          textAlign: 'right',
                          fontFamily: 'Pretendard',
                          fontSize: '14px',
                          fontStyle: 'normal',
                          fontWeight: '500',
                          lineHeight: 'normal',
                          letterSpacing: '-0.028px',
                        }}>
                          45%
                        </p>
                      </div>

                      {/* 남은 상태 바 */}
                      <div style={{
                        display: 'flex',
                        width: '306px',
                        alignItems: 'flex-start',
                        position: 'relative',
                      }}>

                        {/* 전체 회색 바 */}
                        <div style={{
                          width: '306px',
                          height: '6px',
                          position: 'absolute',
                          top: 0,
                          left: 0,
                          borderRadius: '999px',
                          background: '#EDEDF2',
                        }}>
                        </div>

                        {/* 현재 진행률 바 */}
                        <div style={{
                          width: '114px',
                          height: '6px',
                          flexShrink: '0',
                          borderRadius: '999px',
                          background: '#9B87E8',
                        }}>
                        </div>
                      </div>

                      {/* 도달까지 남은 횟수 알림 */}
                      <p style={{
                        alignSelf: 'stretch',

                        color: '#9F9EB0',
                        fontFamily: 'Pretendard',
                        fontSize: '14px',
                        fontStyle: 'normal',
                        fontWeight: '400',
                        lineHeight: 'normal',
                      }}>
                        도달까지 5개 더!
                      </p>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* 말풍선 박스 */}
            <div style={{
              display: 'flex',
              width: '204px',
              flexDirection: 'column',
              alignItems: 'flex-start',
              gap: '10px',
              position: 'absolute',
              right: '0',
              top: '-55px',
            }}>

              {/* 말풍선
              <div style={{
                width: '204px',
                height: '115px',
                fill: '#FFF',
                position: 'absolute',
                        background: 'transparent',
                        border: 'none',
              }}> */}

                <svg
                xmlns="http://www.w3.org/2000/svg"
                width="204"
                height="115"
                viewBox="0 0 204 115"
                fill="none"
                style={{
                  position: 'absolute',
                  top: 0,
                  left: 0,
                }}>
                  <path
                  fill-rule="evenodd"
                  clip-rule="evenodd"
                  d="M188 0C196.836 3.2327e-05 204 7.16346 204 16V99C204 107.837 196.836 115 188 115H4.12495C0.904592 115 -1.0301 111.19 0.576121 107.871L0.743113 107.554C3.27766 104.348 4.54453 102.743 5.57905 100.995C7.88033 97.106 9.39327 92.6025 9.99995 87.8564V16C9.99995 7.16344 17.1634 0 25.9999 0H188Z"
                  fill="white"
                  />
                </svg>
{/* 
                네모 말풍선
                <div style={{
                  // position: 'absolute', // 추가
                  // top: '16px',  // 추가
                  // left: '26px', // 추가
                  width: '194px',
                  height: '115px',
                  borderRadius: '16px 16px 16px 0',
                  background: '#FFF',
                }}>
                </div> */}

                {/* 말풍선 꼬리
                <div style={{
                  // position: 'absolute', // 추가
                  // top: '16px',  // 추가
                  // left: '26px', // 추가
                  width: '10.294px',
                  height: '32.882px',
                  fill: '#FFF',
                }}>
                </div> */}
              {/* </div> */}

              {/* 말풍선 글 박스 */}
              <div style={{
                display: 'flex',
                padding: '16px 16px 16px 26px',
                justifyContent: 'center',
                alignItems: 'center',
                alignSelf: 'stretch',
                position: 'relative',
                zIndex: 1,
              }}>

                {/* 말풍선 글 */}
                <p style={{
                  width: '168px',
                  color: '#01030D',
                  fontFamily: 'Pretendard',
                  fontSize: '12px',
                  fontStyle: 'normal',
                  fontWeight: '500',
                  lineHeight: '16.5px', /* 137.5% */
                  textTransform: 'uppercase',
                }}>
                  오늘
                  <span style={{
                    color: '#01030D',
                    fontFamily: 'Pretendard',
                    fontSize: '12px',
                    fontStyle: 'normal',
                    fontWeight: '700',
                    lineHeight: '16.5px', /* 137.5% */
                    textTransform: 'uppercase',
                  }}>
                    여행 동사
                  </span> 위주로 잘 맞혔어!<br></br>아쉬운 점은
                  <span style={{
                    color: '#7C3AED',
                    fontFamily: 'Pretendard',
                    fontSize: '12px',
                    fontStyle: 'normal',
                    fontWeight: '700',
                    lineHeight: '16.5px', /* 137.5% */
                    textTransform: 'uppercase',
                  }}>
                    문맥 적용
                  </span>이 약했다는 점이야<br></br>다음에 같은 주제로 다시 해볼래?
                </p>
              </div>
            </div>
          </div>
        </div>

        {/* 정확도, 수집 결과 안내 박스 */}
        <div style={{
          display: 'flex',
          width: '370px',
          height: '68px',
          padding: '16px 45px 16px 16px',
          flexDirection: 'column',
          alignItems: 'flex-start',
          gap: '10px',
          flexShrink: '0',
          borderRadius: '12px',
          background: '#CCC',
        }}>

          {/* 아이콘과 안내 글 박스 */}
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
          }}>

            {/* 왼쪽 회색 아이콘 */}
            <div style={{
              width: '32px',
              height: '32px',
              aspectRatio: '1/1',
              background: '#E0E0E0',
            }}>
            </div>

            {/* 안내 글 박스 */}
            <p style={{
              width: '269px',
              color: '#FFF',
              fontFamily: 'Pretendard',
              fontSize: '12px',
              fontStyle: 'normal',
              fontWeight: '500',
              lineHeight: '18px', /* 150% */
            }}>
              퀴즈 정확도 80% 달성! 여행 영어 어휘 12개 수집 완료!
              <span style={{
                color: '#F5F6FA',
                fontFamily: 'Pretendard',
                fontSize: '12px',
                fontStyle: 'normal',
                fontWeight: '700',
                lineHeight: '18px',
              }}>
                이번 주 정확도 78%→80% 향상!
              </span>
            </p>
          </div>
        </div>

        {/* AI 추천 영상 박스 */}
        <div style={{
          display: 'flex',
          width: '370px',
          padding: '24px 16px 20px 16px',
          flexDirection: 'column',
          alignItems: 'flex-end',
          gap: '24px',
          borderRadius: '24px',
          background: '#FFF',
        }}>

          {/* AI 추천 영상 안내 글 박스 */}
          <div style={{
            display: 'flex',
            paddingLeft: '4px',
            flexDirection: 'column',
            alignItems: 'flex-start',
            gap: '8px',
            alignSelf: 'stretch',
          }}>

            {/* AI가 다음으로 추천하는 영상 안내 글 */}
            <p style={{
              alignSelf: 'stretch',
              color: '#01030D',
              fontFamily: 'Pretendard',
              fontSize: '18px',
              fontStyle: 'normal',
              fontWeight: '700',
              lineHeight: 'normal',
              letterSpacing: '0.5px',
              textTransform: 'uppercase',
            }}>
              AI가 다음으로 추천하는 영상
            </p>

            {/* 추천하는 영상 설명 글 */}
            <p style={{
              alignSelf: 'stretch',
              color: '#9198A3',
              fontFamily: 'Pretendard',
              fontSize: '12px',
              fontStyle: 'normal',
              fontWeight: '500',
              lineHeight: '18px', /* 150% */
              textTransform: 'uppercase',
            }}>
              문맥 적용이 약한 너에게 보완할 영상을 추천할게!
              <span>영상에는 ~~ 다루고 있어서 ~~도움이 될거야</span>
            </p>
          </div>

          {/* 추천 영상 박스 */}
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'flex-start',
            gap: '10px',
            alignSelf: 'stretch',
          }}>

            {/* 추천 영상 섬네일 */}
            <div style={{
              display: 'flex',
              height: '190px',
              padding: '152px 0 0 288px',
              flexDirection: 'column',
              alignItems: 'flex-start',
              alignSelf: 'stretch',
              borderRadius: '14px',
              background: '#CCC',
            }}>
              {/* 추천 영상 전체 길이 박스 */}
              <div style={{
                display: 'flex',
                width: '50px',
                padding: '10px 10px 11px 10px',
                flexDirection: 'column',
                alignItems: 'flex-start',
                gap: '10px',
              }}>

                {/* 추천 영상 전체 길이 */}
                <div style={{
                  display: 'flex',
                  height: '17px',
                  flexDirection: 'column',
                  alignItems: 'flex-start',
                  gap: '10px',
                  alignSelf: 'stretch',
                  borderRadius: '4px',
                  background: 'rgba(15, 15, 15, 0.50)',
                }}>
                </div>
              </div>
            </div>

            {/* 채널 이미지, 추천 영상 제목, 채널명 박스 */}
            <div style={{
              display: 'flex',
              alignItems: 'flex-start',
              gap: '8px',
            }}>

              {/* 채널 이미지 */}
              <div style={{
                display: 'flex',
                width: '40.004px',
                height: '40.004px',
                alignItems: 'center',
                borderRadius: '999px',
                background: '#CCC',
              }}>
              </div>

              {/* 추천 영상 제목, 채널명 박스 */}
              <div style={{
                display: 'flex',
                width: '275px',
                flexDirection: 'column',
                alignItems: 'flex-start',
                gap: '8px',
              }}>

                {/* 추천 영상 제목 */}
                <p style={{
                  display: 'flex',
                  height: '32px',
                  flexDirection: 'column',
                  justifyContent: 'center',
                  alignSelf: 'stretch',
                  color: '#01030D',
                  fontFamily: 'Roboto',
                  fontSize: '14px',
                  fontStyle: 'normal',
                  fontWeight: '700',
                  lineHeight: '16px', /* 114.286% */
                  letterSpacing: '-0.5px',
                }}>
                  It is a long established fact that a reader will be
                  <span>distracted by the readable content of a page...</span>
                </p>

                {/* 채널명 박스 */}
                <div style={{
                  width: '78px',
                  height: '16px',
                  }}>

                  {/* 채널명 */}
                  <p style={{
                    display: 'flex',
                    width: '78px',
                    height: '16px',
                    flexDirection: 'column',
                    justifyContent: 'center',
                    color: '#01030D',
                    fontFamily: 'Roboto',
                    fontSize: '12px',
                    fontStyle: 'normal',
                    fontWeight: '400',
                    lineHeight: '20px', /* 166.667% */
                    letterSpacing: '-0.5px',
                  }}>
                    Adventure Time
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
export default SettlementPage;