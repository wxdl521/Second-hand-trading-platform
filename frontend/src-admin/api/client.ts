import axios from 'axios'
import { readAdminSession } from '@admin/utils/session'

export const adminApiClient = axios.create({
  baseURL: import.meta.env.VITE_ADMIN_API_BASE_URL || import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 8000
})

adminApiClient.interceptors.request.use((config) => {
  const session = readAdminSession()
  if (session?.accessToken) {
    config.headers.Authorization = `Bearer ${session.accessToken}`
  }
  return config
})
