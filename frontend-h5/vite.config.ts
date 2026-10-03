import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 需求文档 6.3：本地开发由 Vite 代理 /api → Spring Boot 8080
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  // 兼容 iOS 11 Safari 11：转译现代语法 + 禁用动态 import 代码分割
  build: {
    target: 'es2015',
    cssTarget: 'safari11',
    rollupOptions: {
      output: {
        inlineDynamicImports: true,
        manualChunks: undefined
      }
    }
  },
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/api': {
        // 本地起后端时用默认 127.0.0.1:8080；不想起后端时可指定服务器，如
        // PowerShell: $env:XXS_API_TARGET='http://192.168.110.99'; npm run dev
        target: process.env.XXS_API_TARGET || 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
