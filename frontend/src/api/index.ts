import axios from 'axios'
import { pinia } from '@/stores/pinia'
import { useAppStore } from '@/stores/app'

export const getAppStore = () => useAppStore(pinia)

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 8000
})

apiClient.interceptors.request.use((config) => {
  const token = getAppStore().authToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(error)
)
