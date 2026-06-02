/* global chrome */

// 커스텀 에러 클래스
class ApiError extends Error {
  constructor(message, code) {
    // 부모(Error)한테 message 전달
    super(message);
    // code도 같이 저장
    this.code = code;
    this.name = 'ApiError';
  }
}



// 토큰 검증 헬퍼
function checkAuth(headers) {
  const authHeader = headers?.['Authorization'];
  
  if (!authHeader?.startsWith('Bearer ')) {
    return {
      success: false,
      error: {
        code: "UNAUTHORIZED",
        message: "인증 정보가 유효하지 않습니다. 다시 로그인해주세요."
      }
    };
  }
  
  return null;  // 검증 통과
}



// body를 객체로 변환하는 헬퍼
// 진짜 서버는 fetch 요청 시 body를 문자열로 받아야하기에 AuthContext에서 보낼 때 JSON.stringify로 변환해야 함
// 그래서 목업에서 받을 때 JSON.parse 변환이 필요
function parseBody(body) {
  if (!body) return {};
  if (typeof body === 'string') {
    try { return JSON.parse(body); }
    catch { return {}; }
  }
  return body;
}



// 서버 주소가 생기면 false로 바꾸고 URL을 실제 서버 주소로 변경하기
const IS_MOCK = false; 
const BASE_URL = import.meta.env.VITE_API_BASE_URL;

// 토큰 조회 헬퍼
async function getAccessToken() {
  // 백그라운드(서비스워커) 컨텍스트인지 확인
  // chrome.runtime.sendMessage는 자기 자신(같은 컨텍스트)에겐 메시지 못 보냄
  // 백그라운드는 저장소에서 조회하게 변경
  const isServiceWorker = typeof window === 'undefined';
  
  try {
    if (isServiceWorker) {
      // 백그라운드면 storage 직접 조회
      const result = await chrome.storage.session.get(['accessToken']);
      return result.accessToken;
    } else {
      // 콘텐츠/사이드패널이면 메시지로 조회
      const auth = await chrome.runtime.sendMessage({ type: 'GET_AUTH' });
      return auth?.accessToken;
    }
  } catch (error) {
    console.warn('토큰 조회 실패', error);
    return null;
  }
}



// 토큰 저장 헬퍼
async function saveAccessToken(accessToken) {
  const isServiceWorker = typeof window === 'undefined';
  
  if (isServiceWorker) {
    await chrome.storage.session.set({ accessToken });
  } else {
    await chrome.runtime.sendMessage({
      type: 'SET_AUTH',
      accessToken,
    });
  }
}


// 인증 정리 헬퍼
async function clearAuthLocal() {
  // 백그라운드(서비스워커) 컨텍스트인지 확인
  const isServiceWorker = typeof window === 'undefined';
  
  if (isServiceWorker) {
    
    await chrome.storage.session.remove(['accessToken', 'user']);
  } else {
    
    await chrome.runtime.sendMessage({ type: 'CLEAR_AUTH' });
  }
}



export const apiFetch = async (endpoint, options = {}) => {

  // 백그라운드에서 토큰 가져오기
  const accessToken = await getAccessToken();

  // headers에 토큰 자동 추가 (Mock/실제 공통)
  const headersWithAuth = {
    'Content-Type': 'application/json',
    ...(accessToken && { 'Authorization': `Bearer ${accessToken}` }),
    // 호출자가 명시적으로 헤더 주면 그걸로 덮어쓰기
    ...options.headers,
  };



  if (IS_MOCK) {
    // 네트워크 지연 시뮬레이션
    await new Promise(resolve => setTimeout(resolve, 500)); 

    const { method = 'GET', body = null } = options;
    const headers = headersWithAuth;

    // 로그인 요청
    if (endpoint === '/auth/google/login' && method === 'POST') {
      const idToken = parseBody(body)?.idToken;

      // 토큰 빈값 검증
      if (!idToken) {
        return {
          success: false,
          error: {
            code: "INVALID_TOKEN",
            message: "유효하지 않은 Google 토큰입니다."
          }
        };
      }

      return {
        success: true,
        data: {
          // JWT 공식 사이트(jwt.io)의 예시 토큰
          accessToken: "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
          user: {
            id: 1,
            email: "user@gmail.com",
            name: "홍길동",
            profileImageUrl: "https://...",
            level: 1,
            exp: 0,
            isNewUser: true
          }
        },
        message: "환영합니다! 🎉",
        "Set-Cookie": "refreshToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; Path=/api/auth; HttpOnly; Secure; SameSite=Strict; Max-Age=604800"
      };
    }



    // 로그아웃 요청
    if (endpoint === '/auth/logout'  && method === 'POST') {
      const authHeader = headers?.['Authorization'];
      // mock 리프레쉬 토큰 (실제는 쿠키로 자동 전송)
      const refreshToken = parseBody(body)?.refreshToken;

      // Authorization 헤더 무효/만료
      if (!authHeader?.startsWith('Bearer ')) {
        return {
          success: false,
          error: {
            code: "REFRESH_TOKEN_EXPIRED",
            message: "다시 로그인해주세요."
          }
        };
      }

      // refreshToken 누락
      if (!refreshToken) {
        return {
          success: false,
          error: {
            code: "TOKEN_EXPIRED",
            message: "토큰이 만료되었습니다."
          }
        };
      }

      return {
        success: true,
        message: "로그아웃되었습니다.",
        "Set-Cookie": "refreshToken=; Path=/api/auth; HttpOnly; Secure; SameSite=Strict; Max-Age=0"
      };
    }



    // 토큰 갱신 요청
    if (endpoint === '/auth/refresh'  && method === 'POST') {
      // mock 리프레쉬 토큰
      const refreshToken = parseBody(body)?.refreshToken;

      // 토큰 없음
      if (!refreshToken) {
        return {
          success: false,
          error: {
            code: "INVALID_REFRESH_TOKEN",
            message: "유효하지 않은 토큰입니다1."
          }
        };
      }

      // JWT 디코딩해서 exp 체크
      try {
        const payload = JSON.parse(atob(refreshToken.split('.')[1]));
        const now = Math.floor(Date.now() / 1000);

        if (payload.exp < now) {
          return {
            success: false,
            error: {
              code: "REFRESH_TOKEN_EXPIRED",
              message: "다시 로그인해주세요."
            }
          };
        }
      } catch {
        return {
          success: false,
          error: {
            code: "INVALID_REFRESH_TOKEN",
            message: "유효하지 않은 토큰입니다2."
          }
        };
      }

      // 성공한 경우
      return {
        success: true,
        data: {
          // JWT 공식 사이트(jwt.io)의 예시 토큰
          accessToken: "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjMifQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c",
          expiresIn: 3600,
        },
        message: "토큰이 갱신되었습니다.",
        "Set-Cookie": "refreshToken=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...; Path=/api/auth; HttpOnly; Secure; SameSite=Strict; Max-Age=604800"
      };
    }



    // 내 정보 조회
    if (endpoint === '/users/me' && method === 'GET') {
      const authHeader = headers?.['Authorization'];

      // 토큰 검증
      if (!authHeader?.startsWith('Bearer ')) {
        return {
          success: false,
          data: null,
          error: {
            code: "UNAUTHORIZED",
            message: "인증 정보가 유효하지 않습니다. 다시 로그인해주세요."
          }
        };
      }

      // 사용자를 찾을 수 없는 경우
      // 토큰에 'notfound' 단어 포함되면 사용자 없음으로 처리
      if (authHeader.includes('notfound')) {
        return {
          success: false,
          error: {
            code: "USER_NOT_FOUND",
            message: "사용자를 찾을 수 없습니다."
          }
        };
      }

      return {
        success: true,
        data: {
          id: 1,
          email: "user@gmail.com",
          name: "홍길동",
          profileImageUrl: "https://lh3.googleusercontent.com/...",
          level: 12,
          exp: 3450,

          nextLevelExp: 37140, // 다음 레벨을 위해 획득해야하는 EXP
          progressPercentage: 65, // 다음 레벨까지 진행한 퍼센드(사용시에는 100-퍼센트)로 사용

          createdAt: "2024.01.15",
          needsOnboarding: true,
          ongoingMastery: {
            videoId: "dQw4w9WgXcQ",
  	        videoTitle: "Contrary to popular belief, Lorem Ipsum is not simply ...", // 영상 제목
  	        videoDuration: "14:23", // 영상 재생 시간
  	        channelName: "BBC Learning English", // 채널명
            channelProfileImageUrl: "https://api.dicebear.com/7.x/notionists/svg?seed=BBC", // 테스트용 채널 프로필 URL
            thumbnailUrl: "https://yt3...",
            currentBadge: "SILVER"
          },
        }
      };
    }



    // 대시보드 조회
    if (endpoint === '/users/me/dashboard' && method === 'GET') {
      const authError = checkAuth(headers);
      if (authError) return authError;

      return {
        success: true,
        data: {
          levelInfo: {
            currentLevel: 12,
            currentExp: 12345,  // 현재 exp
            nextLevelExp: 37140,  // 다음 레벨까지 exp
            progressPercentage: 35, // 다음 레벨까지 진행률(35%)
          },
          stats: {
            totalWords: 247, // 수집한 단어
            conqueredVideos: 12, // 완료된 영상
          },
          expLogs: [
            {
              date: "12.31",
              title: "마스터리 골드 획득",
              amount: 500
            },
            {
              date: "12.28",
              title: "10일 이상 미접속",
              amount: -150
            },
            {
              date: "12.13",
              title: "마스터리 실버 획득",
              amount: 500
            }
          ],
          weeklyAttendance: {
            today: "Thu",
            "days": [
              { "dayOfWeek":"Mon", "date": "2026-05-26", "attended": true,  "today": false },
              { "dayOfWeek":"Tue", "date": "2026-05-27", "attended": true,  "today": false },
              { "dayOfWeek":"Wed", "date": "2026-05-28", "attended": true,  "today": false },
              { "dayOfWeek":"Thu", "date": "2026-05-29", "attended": true,  "today": true  },
              { "dayOfWeek":"Fri", "date": "2026-05-30", "attended": false, "today": false },
              { "dayOfWeek":"Sat", "date": "2026-05-31", "attended": false, "today": false },
              { "dayOfWeek":"Sun", "date": "2026-06-01", "attended": false, "today": false }
            ]
          }
        },
      };
    }



    // 성장 지표 조회 (마이페이지)
    if (endpoint.startsWith('/users/me/growth') && method === 'GET') {
      const authError = checkAuth(headers);
      if (authError) return authError;

      return {
        success: true,
        data: {
          averageAccuracy: 100, // 평균 정확도
          streak: 999, // 연속 학습습
          weeklyAccuracy: {
            thisWeek: 84,
            lastWeek: 80,
            growthRate: 4,
            weeklyData: [
              { "week": "4주 전",  "accuracy": 76 },
              { "week": "3주 전",  "accuracy": 70 },
              { "week": "저번 주", "accuracy": 80 },
              { "week": "이번 주", "accuracy": 84 }
            ]
          }
        }
      };
    }



    // 마이페이지 속 학습 목표 조회
    if (endpoint.startsWith('/users/me/preferences') && method === 'GET') {
      const authError = checkAuth(headers);
      if (authError) return authError;

      return {
        success: true,
        data: {
          learningGoal: null,
          difficultyLevel: null,
          absoluteLevel: null,
          updatedAt: "2024-01-15T10:30:00Z"
        }
      };
    }



    // 학습 설정 수정
    if (endpoint.startsWith('/users/me/preferences') && method === 'PATCH') {
      const authError = checkAuth(headers);
      if (authError) return authError;

      // 요청 본문 가져오기
      const { learningGoal, difficultyLevel, absoluteLevel } = body || {};

      // 유효성 검사
      const validGoals = ['TRAVEL', 'BUSINESS', 'SELF_DEVELOPMENT', 'EXAM', 'DAILY', 'NONE'];
      const validLevels = ['CURRENT', 'RELAXED', 'EASIER', 'HARDER', 'MAX'];
      const validAbsoluteLevels = [ 'BEGINNER', 'INTERMEDIATE', 'ADVANCED']

      // 학습 목표 변경
      if (learningGoal && !validGoals.includes(learningGoal)) {
        return {
          success: false,
          error: {
            code: 'INVALID_PREFERENCE',
            message: '올바르지 않은 학습 목표입니다. (TRAVEL, BUSINESS, SELF_DEVELOPMENT, EXAM, DAILY, NONE 중 선택)'
          }
        };
      }

      // 상대적 난이도 변경
      if (difficultyLevel && !validLevels.includes(difficultyLevel)) {
        return {
          success: false,
          error: {
            code: 'INVALID_PREFERENCE',
            message: '올바르지 않은 상대적 난이도입니다. (CURRENT, RELAXED, EASIER, HARDER, MAX 중 선택)'
          }
        };
      }


      // 절대적 난이도 변경
      if (absoluteLevel && !validAbsoluteLevels.includes(absoluteLevel)) {
        return {
          success: false,
          error: {
            code: 'INVALID_PREFERENCE',
            message: '올바르지 않은 절대적 난이도입니다. (BEGINNER, INTERMEDIATE, ADVANCED 중 선택)'
          }
        };
      }


      // 성공 응답
      return {
        success: true,
        data: {
          userId: 1,
          learningGoal: learningGoal ?? "BUSINESS", // 왼쪽이 null 또는 undefined면 오른쪽 값을 사용
          difficultyLevel: difficultyLevel ?? "CURRENT", // 왼쪽이 null 또는 undefined면 오른쪽 값을 사용
          absoluteLevel: absoluteLevel ?? "BEGINNER",
          updatedAt: new Date().toISOString() // 현재 시간을 "2024-11-15T14:23:45.123Z" 형식으로 변환
        },
        message: '학습 설정이 변경되었습니다.'
      };
    }



    // 학습 목표 및 난이도 설정
    if (endpoint.startsWith('/preferences/onboarding') && method === 'POST') {
      const authError = checkAuth(headers);
      if (authError) return authError;

      // 요청 본문 변환해서 가져오기
      const parsedBody = parseBody(body);
      const { learningGoal, difficultyLevel, absoluteLevel } = parsedBody || {};

      // 유효성 검사
      const validGoals = ['TRAVEL', 'DAILY', 'BUSINESS'];
      const validLevels = ['CURRENT', 'RELAXED', 'EASIER', 'HARDER', 'MAX'];
      const validAbsoluteLevels = ['BEGINNER', 'INTERMEDIATE', 'ADVANCED'];

      // 에러 케이스 (임의 추가}
      if (learningGoal && !validGoals.includes(learningGoal)) {
        return {
          success: false,
          error: {
            code: 'INVALID_PREFERENCE',
            message: '올바르지 않은 학습 목표입니다. (TRAVEL, DAILY, BUSINESS 중 선택)'
          }
        };
      }

      // 에러 케이스 (임의 추가}
      if (difficultyLevel && !validLevels.includes(difficultyLevel)) {
        return {
          success: false,
          error: {
            code: 'INVALID_PREFERENCE',
            message: '올바르지 않은 상대적 난이도입니다. (CURRENT, RELAXED, EASIER, HARDER, MAX 중 선택)'
          }
        };
      }

      // 에러 케이스 (임의 추가}
      if (absoluteLevel && !validAbsoluteLevels.includes(absoluteLevel)) {
        return {
          success: false,
          error: {
            code: 'INVALID_PREFERENCE',
            message: '올바르지 않은 절대적 난이도입니다. (BEGINNER, INTERMEDIATE, ADVANCED 중 선택)'
          }
        };
      }

      return {
        success: true,
        data: {
          userId: 1,
          learningGoal: learningGoal ?? "TRAVEL",
          difficultyLevel: difficultyLevel ?? "CURRENT",
          absoluteLevel: absoluteLevel ?? "BEGINNER",
          updatedAt: new Date().toISOString()
        },
        message: "학습 설정 완료되었습니다!"
      };
    }



    // 추천 영상 조회
    if (endpoint.startsWith('/videos/recommended') && method === 'GET') {
      const authError = checkAuth(headers);
      if (authError) return authError;

      // URL에서 query parameter 추출
      const url = new URL(endpoint, 'http://dummy.com');  // 더미 base URL
      const limit = parseInt(url.searchParams.get('limit')) || 5;  // 기본값 5
      
      
      // 데이터 풀
      const allRecommendations = [
        { videoId: "abc123", title: "1Travel Tips for Beginners", thumbnailUrl: "https://...", duration: 480, channelName: "English Journey", recommendationReason: "당신의 'travel' 학습 목표와 중급 수준에 맞는 영상이에요", relevanceScore: 95.5, estimatedDifficulty: "INTERMEDIATE" },
        { videoId: "abc123", title: "2Travel Tips for Beginners", thumbnailUrl: "https://...", duration: 480, channelName: "English Journey", recommendationReason: "당신의 'travel' 학습 목표와 중급 수준에 맞는 영상이에요", relevanceScore: 95.5, estimatedDifficulty: "INTERMEDIATE" },
        { videoId: "abc123", title: "3Travel Tips for Beginners", thumbnailUrl: "https://...", duration: 480, channelName: "English Journey", recommendationReason: "당신의 'travel' 학습 목표와 중급 수준에 맞는 영상이에요", relevanceScore: 95.5, estimatedDifficulty: "INTERMEDIATE" },
        { videoId: "abc123", title: "4Travel Tips for Beginners", thumbnailUrl: "https://...", duration: 480, channelName: "English Journey", recommendationReason: "당신의 'travel' 학습 목표와 중급 수준에 맞는 영상이에요", relevanceScore: 95.5, estimatedDifficulty: "INTERMEDIATE" },
        { videoId: "abc123", title: "5Travel Tips for Beginners", thumbnailUrl: "https://...", duration: 480, channelName: "English Journey", recommendationReason: "당신의 'travel' 학습 목표와 중급 수준에 맞는 영상이에요", relevanceScore: 95.5, estimatedDifficulty: "INTERMEDIATE" },
      ];

      return {
        success: true,
        data: {
          // 추천 영상 정보
          recommendations: allRecommendations.slice(0, limit)
        }
      };
    }







    // 영상 배지 조회 요청 (뱃지로 중복 시청 여부 확인)
    // startsWith	/api/badges/video/로 시작하는지
    // endpoint.split('/').pop()으로 동적 추출
    if (endpoint.startsWith('/badges/video/')) {
        const authError = checkAuth(headers);
  if (authError) return authError;
      const videoId = endpoint.split('/').pop();
  
      return {
        success: true,
        data: {
          videoId: videoId,
          videoTitle: "TED Talk - Success",
          currentBadge: "SILVER",
          nextBadge: "GOLD",
          earnedBadges: [
            {
              badgeType: "BRONZE",
              earnedAt: "2026-05-04T16:30:00.758275"
            },
            {
              badgeType: "SILVER",
              earnedAt: "2026-05-06T15:55:31.62448"
            }
          ]
        },
        message: "영상 배지 등급이 성공적으로 조회되었습니다."
      };
    }

    // 단어 수집 요청
    if (endpoint === '/words/my-collection') {
        const authError = checkAuth(headers);
  if (authError) return authError;
      
      return {
        success: true,
        data: {
          words: [
            {
              id: 1,
              word: "travel",
              sentence: "I love to travel to new places every year.",
              timestamp: "00:01:23",
              translation: "여행하다",
              videoId: "dQw4w9WgXcQ",
              videoTitle: "Amazing Travel Video",
              collectedAt: "2024-01-15T10:30:00Z"
            }
          ],
          pagination: {
            totalCount: 247,
            currentPage: 0,
            totalPages: 13,
            pageSize: 20
          }
        }
      }
    }




    // (빈칸 / ox) 퀴즈 요청
if (endpoint === '/quiz/sessions/generate/section') {
    const authError = checkAuth(headers);
  if (authError) return authError;
  const body = JSON.parse(options?.body || '{}');
  console.log('퀴즈 시작 요청:', body);

  return {
    success: true,
    data: {
      sessionId: 9,
      videoId: "abc123",
      sessionType: "NORMAL",
      totalQuizCount: 6,
      quizzes: [
        {
          quizId: 33,
          quizType: "OX",
          content: "The only way to achieve success is to keep going.",
          translation: "성공을 이루는 유일한 방법은 끈기이다.",
          question: "achieve가 이 해석이랑 잘 맞을까?",
          options: null,
          videoTimestamp: "00:00"
        },
        {
          quizId: 34,
          quizType: "BLANK",
          content: "Every [ ] will be rewarded someday.",
          translation: "모든 노력은 언젠가 보상받을 것이다.",
          question: "개구리가 물어볼게! 빈칸에 들어갈 가장 알맞은 단어는 뭘까?",
          options: [
            "effort",
            "achievement",
            "patience",
            "success"
          ],
          videoTimestamp: "00:00"
        }
      ]
    },
    message: "퀴즈 섹션 생성에 성공하였습니다."
  };
}


    // 매칭 퀴즈 요청
    if (endpoint === '/quiz/sessions/generate/matching') {
        const authError = checkAuth(headers);
  if (authError) return authError;
      return{
        success: true,
        data: {
          sessionId: 9,
          videoId: "abc123",
          sessionType: "NORMAL",
          totalQuizCount: 6,
          quizzes: [
            {
              quizId: 49,
              quizType: "MATCHING",
              content: "She worked hard to achieve her dream.",
              translation: "그녀는 꿈을 이루기 위해 열심히 노력했다.",
              question: "achieve",
              options: null,
              answer: "이루다",
              videoTimeStamp: "00:00"
            },
          ]
        },
        message: "매칭 퀴즈 요청에 성공하였습니다."
      };
    }



    // 번역 API (먼저 체크 - 더 구체적인 것)
    if (endpoint === '/translate/subtitles' && options?.method === 'POST') {
        const authError = checkAuth(headers);
  if (authError) return authError;
      return {
        success: true,
        data: {
          translatedTexts: [
            "안녕하세요, 튜토리얼에 오신 것을 환영합니다.",
            "오늘은 JPA에 대해 배워보겠습니다.",
            "JPA는 Java Persistence API의 약자입니다.",
            "기본부터 시작해 봅시다."
          ]
        },
        message: "해당 영상의 전체 자막이 성공적으로 번역되었습니다."
      };
    }







    // 자막 저장
    // POST 저장
    // includes는 부분 일치
    if (endpoint.includes('/subtitles') && options?.method === 'POST') {
        const authError = checkAuth(headers);
  if (authError) return authError;
      const body = JSON.parse(options?.body || '{}');
      const videoId = endpoint.split('/')[3];

      return {
        success: true,
        data: {
          videoId: videoId || "v123",
          subtitleId: Date.now(),
          text: body.text || "This is the second challenge for you.",
          translation: body.translation || "이것은 당신을 위한 두 번째 도전입니다.",
          startTime: body.startTime || 170.0,
          endTime: body.endTime || 330.0,
          isQuizGenerate: true,
          section: 1,
          totalSections: 3
        },
        message: "자막이 성공적으로 저장되었습니다."
      };
    }


    // 자막 불러오기
    if (endpoint.includes('/subtitles') && options?.method === 'GET') {
        const authError = checkAuth(headers);
  if (authError) return authError;
      const videoId = endpoint.split('/')[3];
  
      return {
        success: true,
        data: {
          videoId: videoId,
          subtitles: [
            {
              id: 123,
              text: "I want to travel around the world.",
              translation: "나는 세계를 여행하고 싶어.",
              startTime: 10.5,
              endTime: 13.2
            },
            {
              id: 124,
              text: "Travel opens your mind.",
              translation: "여행은 마음을 열어준다.",
              startTime: 13.5,
              endTime: 16.0
            }
          ]
        }
      };
    }


    // 빈칸 퀴즈
    if (endpoint === '/quiz/generate/blank') {
        const authError = checkAuth(headers);
  if (authError) return authError;
      return {
        success: true,
        data: {
          sessionId: 456,
          quizType: "BLANK",
          quizzes: [
            {
              quizId: "q1",
              word: "travel",
              question: "I want to ____ around the world.",
              sentence: "나는 세계를 여행하고 싶어.",
              options: ["study", "travel", "move", "go"],
              correctAnswer: "travel",
              hint: "travel은 여행하다 라는 뜻이에요"
            },
            {
              quizId: "q2",
              word: "study",
              question: "I ____ English every day.",
              sentence: "나는 매일 영어를 공부해.",
              options: ["study", "play", "eat", "sleep"],
              correctAnswer: "study",
              hint: "study는 공부하다 라는 뜻이에요"
            },
            {
              quizId: "q3",
              word: "enjoy",
              question: "I ____ playing games.",
              sentence: "나는 게임하는 것을 즐겨.",
              options: ["hate", "enjoy", "stop", "finish"],
              correctAnswer: "enjoy",
              hint: "enjoy는 즐기다 라는 뜻이에요"
            },
            {
              quizId: "q4",
              word: "learn",
              question: "I want to ____ Korean.",
              sentence: "나는 한국어를 배우고 싶어.",
              options: ["teach", "learn", "forget", "ignore"],
              correctAnswer: "learn",
              hint: "learn은 배우다 라는 뜻이에요"
            }
          ]
        }
      };
    }




    // 퀴즈 답안 제출
    if (endpoint === '/quiz/submit') {
        const authError = checkAuth(headers);
  if (authError) return authError;
      const body = JSON.parse(options?.body || '{}');
  
      // 테스트용: 특정 답이면 정답 처리
      const isCorrect = body.userAnswer === '용기' || body.userAnswer === 'courage';

      return {
        success: true,
        data: {
          correct: isCorrect,
          earnedExp: isCorrect ? 10 : 0,
          currentExp: isCorrect ? 30 : 10,
          feedback: isCorrect 
            ? "와 이건 거의 원어민인데?" 
            : "앗! 이건 살짝 헷갈렸지 😅",
          explanation: isCorrect
            ? "courage는 두려워도 앞으로 나아가는 힘이에요."
            : "\"effort\"는 목표를 이루기 위해 들이는 '노력'을 뜻해요.",
          correctAnswer: isCorrect ? "용기" : "effort"
        },
        message: "퀴즈 제출 및 채점이 완료되었습니다."
      };
    }



    // 퀴즈 완료 (정산)
    if (endpoint.includes('/quiz/sessions/') && endpoint.includes('/complete')) {
        const authError = checkAuth(headers);
  if (authError) return authError;

      return {
        success: true,
        data: {
          sessionId: 456,
          totalQuizCount: 10,
          correctCount: 8,
          wrongCount: 2,
          accuracy: 80.0,
          earnedExp: 80,
          levelUp: true,
          currentLevel: 13,
          newBadge: {
            videoId: "dQw4w9WgXcQ",
            badgeType: "SILVER",
            // masteryPercentage: 50
          },
          completedAt: "2024-03-15T15:15:00Z"
        },
        message: "축하합니다! 레벨 13이 되었습니다! 🎉"
      };
    }



    // 단어 수집
    if (endpoint === '/words/collect') {
        const authError = checkAuth(headers);
  if (authError) return authError;

      const body = JSON.parse(options?.body || '{}');
  
      // 테스트용: "duplicate"는 중복 에러 반환
      if (body.word === 'duplicate') {
        return {
          success: false,
          error: {
            code: "WORD_ALREADY_COLLECTED",
            message: "이미 수집한 단어입니다."
          }
        };
      }
  
      // 성공 응답
      return {
        success: true,
        data: {
          wordId: Date.now(),
          collectedAt: new Date().toISOString(),
          totalCollectedWords: 248
        },
        message: "단어가 성공적으로 수집되었습니다."
      };
    }


    // 기본 응답
    return { success: true };
  }

  // // 실제 서버 통신 로직 (나중에 사용)
  // const { accessToken } = await chrome.storage.session.get(['accessToken']);

  // 첫 호출
  // response = 서버 응답 객체 (아직 텍스트 상태)
  // fetch는 HTTP 응답이 오면 무조건 성공으로 처리해서 토큰 만료에도 멈추지 않음
  let response;
  try {
    response = await fetch(`${BASE_URL}${endpoint}`, {
      ...options,
      // include의 경우 일반 웹사이트는 CSRF 공격 위험이 있다?
      // 확장프로그램과 서버 간 도메인이 다르기에 같은 도메인만 보내는 기본값인 'same-origin'는 쿠키를 보낼 수 없음
      // include를 사용해서 도메인이 달라도 항상 브라우저에서 쿠키를 보내게 함
      credentials: 'include',
      headers: headersWithAuth,
    });
  } catch {
    throw new ApiError('네트워크 오류', 'NETWORK_ERROR');
  }



  // 토큰 만료 = 401 응답
  if (response.status === 401 && endpoint !== '/auth/refresh') {
    try {
      // refresh 토큰으로 새 토큰 발급
      const refreshRes = await fetch(`${BASE_URL}/auth/refresh`, {
        method: 'POST',
        credentials: 'include',
      });

      if (refreshRes.ok) {
        const refreshData = await refreshRes.json();
        const newAccessToken = refreshData.data.accessToken;

        // 새 토큰 저장
        await saveAccessToken(newAccessToken);
        // await chrome.runtime.sendMessage({
        //   type: 'SET_AUTH',
        //   accessToken: newAccessToken,
        // });

        // 원래 요청 재시도
        response = await fetch(`${BASE_URL}${endpoint}`, {
          ...options,
          credentials: 'include',
          headers: {
            ...headersWithAuth,
            Authorization: `Bearer ${newAccessToken}`,
          },
        });
      } else {
        throw new ApiError('인증 만료', 'UNAUTHORIZED');
      }
    } catch (error) {
      await clearAuthLocal();
      throw error;
    }
  }


  // 응답 처리
  const text = await response.text();

  // 빈 응답 처리
  let result = null;
  if (text) {
    try {
      result = JSON.parse(text);
    } catch {
      // JSON 파싱 실패
      throw new ApiError(
        `잘못된 응답 형식 (status: ${response.status})`,
        'INVALID_RESPONSE'
      );
    }
  }


  // HTTP 에러 처리
  if (!response.ok) {
    throw new ApiError(
      result?.error?.message || `HTTP ${response.status} 에러`,
      result?.error?.code || `HTTP_${response.status}`
    );
  }


  // success 필드 체크
  if (result && result.success === false) {
    throw new ApiError(
      result.error?.message || 'API Error',
      result.error?.code || 'UNKNOWN'
    );
  }

  // // result = JSON으로 파싱된 데이터
  // const result = await response.json();
  // if (!result.success) {
  //   throw new ApiError(
  //     result.error?.message || 'API Error',
  //     result.error?.code || 'UNKNOWN'
  //   );
  // }

  return result;
};