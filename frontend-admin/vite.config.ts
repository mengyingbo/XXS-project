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
  // 管理端是家长使用，无需 iOS 11 单文件限制；拆分 vendor chunk 加速首屏
  build: {
    target: 'es2015',
    cssTarget: 'safari11',
    minify: 'terser',
    terserOptions: {
      ecma: 2015,
      compress: { ecma: 2015 },
      mangle: { safari10: true }
    },
    chunkSizeWarningLimit: 1500,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['vue', 'vue-router', 'pinia', 'axios'],
          'element-plus': ['element-plus', '@element-plus/icons-vue']
        }
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
