import type { Router } from 'vue-router'
import { pinia } from '@/stores/pinia'
import { useAppStore } from '@/stores/app'

export const setupRouterGuards = (router: Router) => {
  router.beforeEach((to) => {
    const store = useAppStore(pinia)
    if (to.meta.requiresAuth && !store.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }

    return true
  })
}
