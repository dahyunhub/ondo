import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5273,
    proxy: {
      // 개발 중 프론트(5273) → 백엔드(8090) API 프록시
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true,
      },
    },
  },
})
