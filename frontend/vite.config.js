import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 前端开发端口 8085；/api /images /ws 全部代理到后端 8082
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 8085,
    host: true,
    proxy: {
      '/api': { target: 'http://localhost:8082', changeOrigin: true },
      '/images': { target: 'http://localhost:8082', changeOrigin: true },
      '/ws': { target: 'ws://localhost:8082', ws: true }
    }
  }
})
