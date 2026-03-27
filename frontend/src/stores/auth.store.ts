import { computed } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import { useAppStore } from '@/stores/app'

export const useAuthStore = defineStore('auth', () => {
  const appStore = useAppStore()

  const currentUser = computed(() => appStore.currentUser)
  const isLoggedIn = computed(() => appStore.isLoggedIn)
  const isAdmin = computed(() => appStore.isAdmin)

  const sendOtp = (phone: string) => authApi.sendOtp(phone)
  const login = (phone: string, otpCode: string) => authApi.login(phone, otpCode)
  const adminLogin = (phone: string, password: string) => authApi.adminLogin(phone, password)
  const register = (nickname: string, phone: string, otpCode: string, password: string) =>
    authApi.register(nickname, phone, otpCode, password)
  const logout = () => authApi.logout()
  const updateProfile = (payload: { name: string; city: string; bio: string; avatar?: string }) =>
    authApi.updateProfile(payload)
  const changePassword = (payload: { currentPassword: string; nextPassword: string }) =>
    authApi.changePassword(payload)
  const submitKyc = (payload: { realName: string; idNumber: string }) => authApi.submitKyc(payload)

  return {
    currentUser,
    isLoggedIn,
    isAdmin,
    sendOtp,
    login,
    adminLogin,
    register,
    logout,
    updateProfile,
    changePassword,
    submitKyc
  }
})
