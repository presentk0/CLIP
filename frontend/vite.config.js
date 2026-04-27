import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import { crx } from '@crxjs/vite-plugin'
import manifest from './manifest.json'

export default defineConfig({
  plugins: [react(), crx({ manifest })],
  build: {
    chunkSizeWarningLimit: 1000,
    rollupOptions: {
      input: {
        sidepanel: 'sidepanel.html'
      }
    }
  }
})