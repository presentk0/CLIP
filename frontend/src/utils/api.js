/* global chrome */

// 서버 주소가 생기면 false로 바꾸고 URL을 실제 서버 주소로 변경하기
const IS_MOCK = true; 
const BASE_URL = 'https://api.clip.example.com';

export const apiFetch = async (endpoint, options = {}) => {
  if (IS_MOCK) {
    // 네트워크 지연 시뮬레이션
    await new Promise(resolve => setTimeout(resolve, 500)); 

    // 영상 배지 조회 요청
    // 동적 videoId 매칭
    if (endpoint.startsWith('/api/badges/video/')) {
      const videoId = endpoint.split('/').pop();  // videoId 추출
      return {
        success: true,
        data: {
          videoId: videoId,  // 동적으로 사용
          videoTitle: "Amazing Travel Video",
          currentBadge: "SILVER",
          masteryPercentage: 65,
          nextBadge: "GOLD",
          nextBadgeRequirement: 80,
          earnedBadges: [
            {
              badgeType: "BRONZE",
              masteryPercentage: 30,
              earnedAt: "2024-01-10T14:20:00Z"
            },
            {
              badgeType: "SILVER",
              masteryPercentage: 65,
              earnedAt: "2024-01-15T10:45:00Z"
            }
          ]
        }
      };
    }



    // 퀴즈 세션 시작
    if (endpoint === '/api/quiz/sessions/start') {
      // 요청 데이터 확인
      const body = JSON.parse(options?.body || '{}');
      console.log('퀴즈 시작 요청:', body);

      return {
        success: true,
        data: {
          sessionId: 456,
          videoId: "dQw4w9WgXcQ",
          sessionType: "NORMAL",
          totalQuizCount: 10,
          startedAt: "2024-03-15T15:00:00Z"
        },
        message: "퀴즈를 시작합니다!"
      };
    }




    // 자막 저장
    // POST 저장
    // includes는 부분 일치
    if (endpoint.includes('/subtitles') && options?.method === 'POST') {
      // POST: body에서 데이터 꺼냄 (프론트가 보낸 것)
      const body = JSON.parse(options?.body || '{}');
      const videoId = endpoint.split('/')[3];

      return {
        success: true,
        data: { videoId, ...body },
        message: "영상 자막이 저장되었습니다."
      };
    }


    // 자막 불러오기
    if (endpoint.includes('/subtitles') && options?.method === 'GET') {
      return {
        success: true,
        // 빈 배열 또는 가짜 데이터
        data: []
      };
    }


    // 빈칸 퀴즈
    if (endpoint === '/api/quiz/generate/blank') {
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
    if (endpoint === '/api/quiz/submit') {
      const body = JSON.parse(options?.body || '{}');
      const isCorrect = body.userAnswer === body.correctAnswer;

      return {
        success: true,
        data: {
          isCorrect: isCorrect,
          earnedExp: isCorrect ? 10 : 0,
          currentExp: isCorrect ? 3460 : 3450,
          feedback: isCorrect 
            ? "정답이에요! 🎉" 
            : "아쉬워요! 다음에 맞히면 2배 보상을 드릴게요! 💪",
          correctAnswer: body.correctAnswer,
          doubleRewardNext: !isCorrect
        }
      };
    }



    // 퀴즈 완료 (정산)
    if (endpoint.includes('/api/quiz/sessions/') && endpoint.includes('/complete')) {
      return {
        success: true,
        data: {
          sessionId: 456,
          totalQuizCount: 10,
          correctCount: 8,
          wrongCount: 2,
          accuracy: 80,
          earnedExp: 80,
          levelUp: true,
          newLevel: 13,
          newBadge: {
            videoId: "dQw4w9WgXcQ",
            badgeType: "SILVER",
            masteryPercentage: 50
          },
          completedAt: "2024-03-15T15:15:00Z"
        },
        message: "축하합니다! 레벨 13이 되었습니다! 🎉"
      };
    }



    // 단어 수집
    if (endpoint === '/api/words/collect') {
      return {
        success: true,
        data: {
          id: Date.now(),
          word: "boarding",
          collectedAt: new Date().toISOString(),
          totalCollectedWords: 248
        },
        message: "단어가 수집되었습니다!"
      };
    }


    // 기본 응답
    return { success: true };
  }


  // 실제 서버 통신 로직 (나중에 사용)
  const { accessToken } = await chrome.storage.local.get(['accessToken']);
  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${accessToken}`,
      ...options.headers,
    }
  });

  const result = await response.json();
  if (!result.success) throw new Error(result.error?.message || 'API Error');
  return result.data;
};