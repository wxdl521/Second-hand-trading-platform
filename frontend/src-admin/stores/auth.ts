import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { AdminSessionSnapshot } from '@admin/types'
import { adminLogin, adminLogout } from '@admin/api/admin'
import { clearAdminSession, readAdminSession, writeAdminSession } from '@admin/utils/session'

export const useAdminAuthStore = defineStore('admin-auth', () => {
  const profile = ref<AdminSessionSnapshot['profile']>(null)
  const accessToken = ref<string | null>(null)
  const refreshToken = ref<string | null>(null)
  const hydrated = ref(false)

  const isLoggedIn = computed(() => Boolean(profile.value && accessToken.value))

  const persist = () => {
    writeAdminSession({
      profile: profile.value,
      accessToken: accessToken.value,
      refreshToken: refreshToken.value
    })
  }

  const hydrate = () => {
    if (hydrated.value) return

    const session = readAdminSession()
    if (session) {
      profile.value = session.profile
      accessToken.value = session.accessToken
      refreshToken.value = session.refreshToken
    }
    hydrated.value = true
  }

  const setSession = (snapshot: AdminSessionSnapshot) => {
    profile.value = snapshot.profile
    accessToken.value = snapshot.accessToken
    refreshToken.value = snapshot.refreshToken
    persist()
  }

  const login = async (phone: string, password: string) => {
    const session = await adminLogin(phone, password)
    setSession(session)
  }

  const logout = async () => {
    await adminLogout(refreshToken.value)
    profile.value = null
    accessToken.value = null
    refreshToken.value = null
    clearAdminSession()
  }

  return {
    profile,
    accessToken,
    refreshToken,
    hydrated,
    isLoggedIn,
    hydrate,
    login,
    logout
  }
})
