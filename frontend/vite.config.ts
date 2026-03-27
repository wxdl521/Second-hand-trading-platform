import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

const isAdminApp = process.env.VITE_ADMIN_APP === '1'

const resolveMainChunks = (normalizedId: string) => {
  if (!normalizedId.includes('node_modules')) return
  if (normalizedId.includes('axios')) return 'vendor-axios'
  if (normalizedId.includes('vue') || normalizedId.includes('pinia') || normalizedId.includes('vue-router')) {
    return 'vendor-vue'
  }
}

const resolveAdminChunks = (normalizedId: string) => {
  if (!normalizedId.includes('node_modules')) return
  if (normalizedId.includes('zrender')) return 'admin-zrender'
  if (normalizedId.includes('echarts')) return 'admin-echarts'
  if (normalizedId.includes('@floating-ui')) return 'admin-floating-ui'
  if (normalizedId.includes('async-validator')) return 'admin-validator'
  if (normalizedId.includes('dayjs')) return 'admin-dayjs'
  if (normalizedId.includes('@element-plus/icons-vue')) return 'admin-el-icons'
  if (normalizedId.includes('element-plus')) return 'admin-element-core'
  if (normalizedId.includes('vue') || normalizedId.includes('pinia') || normalizedId.includes('vue-router')) {
    return 'admin-vue'
  }
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
      '@admin': fileURLToPath(new URL('./src-admin', import.meta.url)),
      '@shared': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '0.0.0.0',
    port: isAdminApp ? 5174 : 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/files': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: isAdminApp ? 'dist-admin' : 'dist',
    chunkSizeWarningLimit: isAdminApp ? 1000 : 600,
    rollupOptions: {
      output: {
        manualChunks(id) {
          const normalizedId = id.replace(/\\/g, '/')
          return isAdminApp ? resolveAdminChunks(normalizedId) : resolveMainChunks(normalizedId)
        }
      }
    }
  }
})
