// /* global chrome */

// 서버 주소가 생기면 false로 바꾸고 URL을 실제 서버 주소로 변경하기
const IS_MOCK = false; 
const BASE_URL = 'https://clip-server.com/api';

export const apiFetch = async (endpoint, options = {}) => {
  if (IS_MOCK) {
    // 네트워크 지연 시뮬레이션
    await new Promise(resolve => setTimeout(resolve, 500)); 

    // 영상 배지 조회 요청 (뱃지로 중복 시청 여부 확인)
    // startsWith	/api/badges/video/로 시작하는지
    // endpoint.split('/').pop()으로 동적 추출
    if (endpoint.startsWith('/badges/video/')) {
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

    // 단어 수집 요청 (미사용)
    if (endpoint === '/words/my-collection') {
      
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


  // 실제 서버 통신 로직 (나중에 사용)
  // const { accessToken } = await chrome.storage.local.get(['accessToken']);
  // response = 서버 응답 객체 (아직 텍스트 상태)
  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      // 'Authorization': `Bearer ${accessToken}`,
      ...options.headers,
    }
  });

  // result = JSON으로 파싱된 데이터
  const result = await response.json();
  if (!result.success) throw new Error(result.error?.message || 'API Error');
  return result;
};