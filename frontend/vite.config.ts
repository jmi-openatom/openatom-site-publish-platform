import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const apiTarget = process.env.VITE_API_TARGET || 'http://127.0.0.1:8080'
const sitePort = process.env.VITE_SITE_PORT ?? (new URL(apiTarget).port || '8080')

export default defineConfig({
  plugins: [vue()],
  define: {
    'import.meta.env.VITE_SITE_PORT': JSON.stringify(sitePort),
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      },
      '/published': {
        target: apiTarget,
        changeOrigin: true,
      },
    },
  },
})
