
/* global chrome */

import { useState, useEffect } from 'react';
import { apiFetch } from '../utils/api';
import { AuthContext } from './AuthContextDefinition';
import { log, IS_DEV } from '../utils/logger';

// ============ 인증 API ============
const authApi = {
  // 구글로 로그인
  googleLogin: (input) =>
    apiFetch('/auth/google/login', {
      method: 'POST',
      body: JSON.stringify(input),
    }),

  // 로그아웃
  logout: () =>
    apiFetch('/auth/logout', { method: 'POST' }),

  // 내 정보 조회
  me: (accessToken) =>
    apiFetch('/users/me', {
      method: 'GET',
      headers: { Authorization: `Bearer ${accessToken}` },
    }),

  // 리프레쉬 토큰 재발급
  refresh: (refreshToken) =>
    apiFetch('/auth/refresh', {
      method: 'POST',
      body: JSON.stringify({ refreshToken }),
    }),
};




export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  // const [token, setToken] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  // !!은 값을 boolean으로 변환
  const isAuthenticated = !!user;

  // 백그라운드에서 토큰 가져오기
  useEffect(() => {
    const initAuth = async () => {
      try {
        // 백그라운드에 저장된 인증 확인 (창 다시 열 때 빠르게)
        const cached = await chrome.runtime.sendMessage({ type: 'GET_AUTH' });
        if (cached?.accessToken && cached?.user) {
          setUser(cached.user);
          setIsLoading(false);
          return;
        }

        // 캐시 없으면 refresh 시도 (HttpOnly 쿠키 자동 전송)
        const refreshRes = await apiFetch('/auth/refresh', { method: 'POST' });
          if (!refreshRes.success) {
            // 401 → 비로그인 상태 유지
            setIsLoading(false);
            return;
          }

        const newAccessToken = refreshRes.data.accessToken;

        // 새 토큰으로 내 정보 가져오기
        // 백그라운드에 토큰 저장하기 전이라서 토큰을 직접 헤더에 써서 직접 넘겨줘야 함
        const meRes = await apiFetch('/users/me', {
          method: 'GET',
          headers: { Authorization: `Bearer ${newAccessToken}` },
        });

        // 사용자 못 찾으면 토큰 정리
        if (!meRes.success) {
          if (meRes.error?.code === 'USER_NOT_FOUND') {
            console.warn('사용자 정보 없음 - 로그아웃 처리');
            await chrome.runtime.sendMessage({ type: 'CLEAR_AUTH' });
          }
          return;
        }

        // 상태 + 백그라운드 저장
        setUser(meRes.data);
        await chrome.runtime.sendMessage({
          type: 'SET_AUTH',
          accessToken: newAccessToken,
          user: meRes.data,
        });
      } catch (error) {
        log.debug('인증 초기화 실패', error);
      } finally {
        setIsLoading(false);
      }
    };




    //     const auth = await chrome.runtime.sendMessage({ type: 'GET_AUTH' });

    //     if (auth?.accessToken && auth?.user) {
    //       setToken(auth.accessToken);
    //       setUser(auth.user);
    //     }
    //   } catch (error) {
    //     console.error('인증 초기화 실패', error);
    //   } finally {
    //     setIsLoading(false);
    //   }
    // };

    initAuth();



    // // 백그라운드에서 동기화 신호 받기
    // const handleMessage = (message) => {
    //   if (message.type === 'SYNC_AUTH') {
    //     if (message.accessToken && message.user) {
    //       setUser(message.user);
    //     } else {
    //       setUser(null);
    //     }
    //   }
    // };

    // chrome.runtime.onMessage.addListener(handleMessage);
    // return () => chrome.runtime.onMessage.removeListener(handleMessage);
  }, []);




  // const login = async (input) => {
  //   const response = await authApi.login(input);

  //   if (response.success && response.accessToken && response.user) {
  //     localStorage.setItem('token', response.accessToken);
  //     if (response.refreshToken) {
  //       localStorage.setItem('refreshToken', response.refreshToken);
  //     }
  //     setToken(response.accessToken);
  //     setUser(response.user);
  //   } else {
  //     throw new Error(response.message || '로그인에 실패했습니다');
  //   }
  // };


  // Google 로그인
  const loginWithGoogle = async (idToken) => {
    const response = await authApi.googleLogin({ idToken });

    // 응답 구조
    if (response.success && response.data?.accessToken && response.data?.user) {
      const { accessToken, user } = response.data;


      setUser(user);

      // 백그라운드에 저장
      await chrome.runtime.sendMessage({
        type: 'SET_AUTH',
        accessToken,
        user,
      });
    } else {
      const error = new Error(response.error?.message || '로그인 실패');
      error.code = response.error?.code;
      throw error;
    }
  };



  // const signup = async (input) => {
  //   const response = await authApi.signup(input);

  //   if (response.success && response.accessToken && response.user) {
  //     localStorage.setItem('token', response.accessToken);
  //     if (response.refreshToken) {
  //       localStorage.setItem('refreshToken', response.refreshToken);
  //     }
  //     setToken(response.accessToken);
  //     setUser(response.user);
  //   } else {
  //     throw new Error(response.message || '회원가입에 실패했습니다');
  //   }
  // };

  // const logout = () => {
  //   authApi.logout().catch(() => {});
  //   localStorage.removeItem('token');
  //   localStorage.removeItem('refreshToken');
  //   setToken(null);
  //   setUser(null);
  // };


  const logout = async () => {
    try {
      await authApi.logout();
    } catch (error) {
      console.warn('로그아웃 실패', error);
    }

    // 서버 로그아웃 실패해도 클라이언트 상태는 정리

    setUser(null);

    // 백그라운드 클리어
    try {
      await chrome.runtime.sendMessage({ type: 'CLEAR_AUTH' });
    } catch (error) {
      log.error('백그라운드 토큰 정리 실패', error);
    }
  };

  const updateUser = (updatedUser) => {
    setUser(updatedUser);
  };

  return (
    <AuthContext.Provider
      value={{
        user,

        isAuthenticated,
        isLoading,
        loginWithGoogle,
        // login,
        // signup,
        logout,
        updateUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

















