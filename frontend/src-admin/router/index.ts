import { createRouter, createWebHistory } from 'vue-router'
import { useAdminAuthStore } from '@admin/stores/auth'
import { pinia } from '@admin/stores/pinia'

const AdminLoginView = () => import('@admin/views/AdminLoginView.vue')
const AdminLayout = () => import('@admin/views/layout/AdminLayout.vue')
const DashboardView = () => import('@admin/views/DashboardView.vue')
const GoodsView = () => import('@admin/views/GoodsView.vue')
const CategoriesView = () => import('@admin/views/CategoriesView.vue')
const OrdersView = () => import('@admin/views/OrdersView.vue')
const UsersView = () => import('@admin/views/UsersView.vue')

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/login', name: 'admin-login', component: AdminLoginView, meta: { guestOnly: true } },
    {
      path: '/',
      component: AdminLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'admin-dashboard', component: DashboardView },
        { path: 'categories', name: 'admin-categories', component: CategoriesView },
        { path: 'goods', name: 'admin-goods', component: GoodsView },
        { path: 'orders', name: 'admin-orders', component: OrdersView },
        { path: 'users', name: 'admin-users', component: UsersView }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const authStore = useAdminAuthStore(pinia)
  authStore.hydrate()

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    return { name: 'admin-dashboard' }
  }

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    return { name: 'admin-login', query: { redirect: to.fullPath } }
  }

  return true
})

export default router
