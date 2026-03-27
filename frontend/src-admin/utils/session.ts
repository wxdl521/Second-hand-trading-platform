import type { AdminSessionSnapshot } from '@admin/types'

export const ADMIN_STORAGE_KEY = 'syxs-admin-site-session'
let isBeforeUnloadBound = false

const getStorage = () => {
  if (typeof window === 'undefined') return null

  if (!isBeforeUnloadBound) {
    window.addEventListener('beforeunload', clearAdminSession)
    isBeforeUnloadBound = true
  }

  return window.sessionStorage
}

export const readAdminSession = (): AdminSessionSnapshot | null => {
  const storage = getStorage()
  if (!storage) return null

  window.localStorage.removeItem(ADMIN_STORAGE_KEY)
  const raw = storage.getItem(ADMIN_STORAGE_KEY)
  if (!raw) return null

  try {
    return JSON.parse(raw) as AdminSessionSnapshot
  } catch (error) {
    console.warn('Failed to parse admin session.', error)
    return null
  }
}

export const writeAdminSession = (snapshot: AdminSessionSnapshot) => {
  const storage = getStorage()
  if (!storage) return

  window.localStorage.removeItem(ADMIN_STORAGE_KEY)
  storage.setItem(ADMIN_STORAGE_KEY, JSON.stringify(snapshot))
}

export const clearAdminSession = () => {
  if (typeof window === 'undefined') return

  window.localStorage.removeItem(ADMIN_STORAGE_KEY)
  window.sessionStorage.removeItem(ADMIN_STORAGE_KEY)
}
