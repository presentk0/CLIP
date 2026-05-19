import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { crx } from '@crxjs/vite-plugin'
import manifest from './manifest.json'

// 현재 빌드 모드(development/production)에 따라 옵션을 다르게 적용하기 위해 mode 사용
export default defineConfig(({ mode }) => ({
  plugins: [react(), crx({ manifest })],
  // pure = console.error 같이 결과를 쓰는게 아니면 삭제하는 옵션
  // drop = 빌드 결과물에서 특정 호출을 코드 자체를 삭제하는 옵션
  // production 빌드에서 console.log/info/warn/debug 모두 제거(error는 유지), debugger 문 제거
  esbuild: {
    pure: mode === 'production' 
      ? ['console.log', 'console.debug', 'console.info', 'console.warn']
      : [],
    drop: mode === 'production' ? ['debugger'] : [],
  },
  build: {
    chunkSizeWarningLimit: 1000,
    // 운영 시 소스맵 설정 false (배포본에 소스맵을 포함하면 원본 코드가 그대로 노출됨)
    sourcemap: mode !== 'production',
    rollupOptions: {
      input: {
        sidepanel: 'sidepanel.html'
      }
    }
  }
}));