import happy from '../imgs/image_703.png';

function SettlementPage({ onGoHome }) {
  return (
    // 전체 박스
    <div>

      {/* 전체 내용 박스 */}
      <div
      style={{
        display: 'flex',
        width: '402px',
        flexDirection: 'column',
        alignItems: 'center',
        gap: '24px',
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


          </div>







          {/* 뱃지 진행률 박스 */}
          <div style={{
            display: 'flex',
            width: '370px',
            flexDirection: 'column',
            alignItems: 'flex-end',
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
                        padding: '65px 6px 6px 121px',
                        flexDirection: 'column',
                        alignItems: 'flex-start',
                        gap: '10px',
                        borderRadius: '10px',
                        background: '#CCC',
                      }}>

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
                          }}>
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
                          영상 제목...
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
                          채널 이름
                        </p>
                      </div>
                    </div>

                    {/* 이건 뭐지? */}
                    <div style={{
                      display: 'flex',
                      width: '20px',
                      height: '20px',
                      alignItems: 'center',
                      position: 'absolute',
                      left: '8px',
                      top: '8px',
                      background: '#B0B0B0',
                    }}>
                    </div>
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
                            실제 수치 8/10
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
                            실제 수치 12개
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
                      }}>

                        {/* 전체 회색 바 */}
                        <div style={{
                          width: '306px',
                          height: '6px',
                          position: 'absolute',
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

              {/* 말풍선 */}
              <div style={{
                width: '204px',
                height: '115px',
                fill: '#FFF',
                position: 'absolute',
              }}>

                <svg xmlns="http://www.w3.org/2000/svg" width="204" height="115" viewBox="0 0 204 115" fill="none">
                  <path fill-rule="evenodd" clip-rule="evenodd" d="M188 0C196.836 3.2327e-05 204 7.16346 204 16V99C204 107.837 196.836 115 188 115H4.12495C0.904592 115 -1.0301 111.19 0.576121 107.871L0.743113 107.554C3.27766 104.348 4.54453 102.743 5.57905 100.995C7.88033 97.106 9.39327 92.6025 9.99995 87.8564V16C9.99995 7.16344 17.1634 0 25.9999 0H188Z" fill="white"/>
                </svg>

                {/* 네모 말풍선 */}
                <div style={{
                  // position: 'absolute', // 추가
                  // top: '16px',  // 추가
                  // left: '26px', // 추가
                  width: '194px',
                  height: '115px',
                  borderRadius: '16px 16px 16px 0',
                  background: '#FFF',
                }}>
                </div>

                {/* 말풍선 꼬리 */}
                <div style={{
                  // position: 'absolute', // 추가
                  // top: '16px',  // 추가
                  // left: '26px', // 추가
                  width: '10.294px',
                  height: '32.882px',
                  fill: '#FFF',
                }}>
                </div>
              </div>

              {/* 말풍선 글 박스 */}
              <div style={{
                display: 'flex',
                padding: '16px 16px 16px 26px',
                justifyContent: 'center',
                alignItems: 'center',
                alignSelf: 'stretch',
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
      </div>
      {/* 나가는 박스 */}
      <div>
        <button
        onClick={onGoHome}
        style={{}}>
          홈으로 돌아가기
        </button>
    </div>

    </div>
  );
}
export default SettlementPage;