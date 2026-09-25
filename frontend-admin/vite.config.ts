import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 部署在 /admin/ 子路径（需求文档 9.2）；本地开发由 Vite 代理 /api → Spring Boot 8080
export default defineConfig({
  base: '/admin/',
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
    minify: 'terser',
    terserOptions: {
      ecma: 2015,
      compress: { ecma: 2015 },
      mangle: { safari10: true }
    },
    rollupOptions: {
      output: {
        inlineDynamicImports: true,
        manualChunks: undefined
      }
    }
  },
  server: {
    host: true,
    port: 5174,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
