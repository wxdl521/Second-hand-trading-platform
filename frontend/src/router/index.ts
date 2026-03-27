import { createRouter, createWebHistory } from 'vue-router'
import { setupRouterGuards } from '@/router/guards'

const HomeView = () => import('@/views/HomeLandingView.vue')
const LoginView = () => import('@/views/LoginView.vue')
const RegisterView = () => import('@/views/RegisterView.vue')
const GoodsListView = () => import('@/views/goods/GoodsList.vue')
const GoodsDetailView = () => import('@/views/goods/GoodsDetail.vue')
const GoodsManageView = () => import('@/views/goods/GoodsManage.vue')
const GoodsPublishView = () => import('@/views/goods/GoodsPublish.vue')
const EstimateView = () => import('@/views/ai/EstimatePageView.vue')
const OrderListView = () => import('@/views/order/OrderListPageView.vue')
const OrderDetailView = () => import('@/views/order/OrderDetailPageView.vue')
const CarbonView = () => import('@/views/carbon/CarbonAccountPageView.vue')
const AppraiseView = () => import('@/views/appraise/AppraisePageView.vue')
const ProfileView = () => import('@/views/profile/ProfileDashboardView.vue')
const ProfileFavoritesView = () => import('@/views/profile/ProfileFavoritesPageView.vue')
const ProfileMessagesView = () => import('@/views/profile/ProfileMessagesPageView.vue')
const ProfileEditView = () => import('@/views/profile/ProfileEditPageView.vue')
const ProfileSettingsView = () => import('@/views/profile/ProfileSettingsMainPageView.vue')
const ProfilePasswordView = () => import('@/views/profile/ProfilePasswordPageView.vue')
const ProfileKycView = () => import('@/views/profile/ProfileKycPageView.vue')
const NotFoundView = () => import('@/views/NotFoundView.vue')

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/register', name: 'register', component: RegisterView },
    { path: '/goods', name: 'goods-list', component: GoodsListView },
    { path: '/goods/manage', name: 'goods-manage', component: GoodsManageView, meta: { requiresAuth: true } },
    { path: '/goods/:id', name: 'goods-detail', component: GoodsDetailView },
    { path: '/goods/publish', name: 'goods-publish', component: GoodsPublishView, meta: { requiresAuth: true } },
    { path: '/estimate', name: 'estimate', component: EstimateView },
    { path: '/order/list', name: 'order-list', component: OrderListView, meta: { requiresAuth: true } },
    { path: '/order/:id', name: 'order-detail', component: OrderDetailView, meta: { requiresAuth: true } },
    { path: '/carbon', name: 'carbon', component: CarbonView, meta: { requiresAuth: true } },
    { path: '/appraise', name: 'appraise', component: AppraiseView, meta: { requiresAuth: true } },
    { path: '/profile', name: 'profile', component: ProfileView, meta: { requiresAuth: true } },
    { path: '/profile/favorites', name: 'profile-favorites', component: ProfileFavoritesView, meta: { requiresAuth: true } },
    { path: '/profile/messages', name: 'profile-messages', component: ProfileMessagesView, meta: { requiresAuth: true } },
    { path: '/profile/edit', name: 'profile-edit', component: ProfileEditView, meta: { requiresAuth: true } },
    { path: '/profile/settings', name: 'profile-settings', component: ProfileSettingsView, meta: { requiresAuth: true } },
    { path: '/profile/password', name: 'profile-password', component: ProfilePasswordView, meta: { requiresAuth: true } },
    { path: '/profile/kyc', name: 'profile-kyc', component: ProfileKycView, meta: { requiresAuth: true } },
    { path: '/:pathMatch(.*)*', name: 'not-found', component: NotFoundView }
  ]
})

setupRouterGuards(router)

export default router
