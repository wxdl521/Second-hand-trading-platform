<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAdminAuthStore } from '@admin/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAdminAuthStore()

const navItems = [
  { label: '数据看板', caption: 'GMV 与运营总览', short: 'DB', to: '/dashboard' },
  { label: '用户管理', caption: 'KYC 审核与封禁', short: 'US', to: '/users' },
  { label: '商品审核', caption: '待审核队列', short: 'GD', to: '/goods' },
  { label: '分类管理', caption: '品类与品牌', short: 'CT', to: '/categories' },
  { label: '订单管理', caption: '订单与成交', short: 'OD', to: '/orders' }
]

const currentNav = computed(
  () => navItems.find((item) => route.path.startsWith(item.to)) ?? navItems[0]
)

const adminName = computed(() => authStore.profile?.name ?? '平台管理员')
const adminAvatar = computed(() => authStore.profile?.avatar ?? '')
const currentDate = computed(() =>
  new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
)

const onLogout = async () => {
  await authStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-sidebar__panel">
        <div class="admin-sidebar__top">
          <span class="admin-sidebar__tag">独立后台</span>
          <div class="admin-sidebar__identity">
            <div class="admin-sidebar__avatar">
              <img v-if="adminAvatar" :src="adminAvatar" :alt="adminName" />
              <span v-else>{{ adminName.slice(0, 2) }}</span>
            </div>
            <div>
              <strong>{{ adminName }}</strong>
              <p>平台管理</p>
            </div>
          </div>

          <div class="admin-sidebar__intro">
            <h2>后台管理</h2>
            <p>聚焦核心数据、内容治理、用户运营与交易履约的独立桌面后台。</p>
          </div>
        </div>

        <nav class="admin-nav">
          <RouterLink
            v-for="item in navItems"
            :key="item.to"
            class="admin-nav__item"
            :to="item.to"
          >
            <span class="admin-nav__icon">{{ item.short }}</span>
            <span class="admin-nav__text">
              <strong>{{ item.label }}</strong>
              <small>{{ item.caption }}</small>
            </span>
          </RouterLink>
        </nav>

        <div class="admin-sidebar__footer">
          <button class="admin-sidebar__button admin-sidebar__button--primary" type="button" @click="onLogout">
            退出登录
          </button>
        </div>
      </div>
    </aside>

    <div class="admin-main">
      <header class="admin-header">
        <div>
          <span class="admin-header__eyebrow">PC Admin</span>
          <h1>{{ currentNav.label }}</h1>
          <p>{{ currentNav.caption }} · {{ currentDate }}</p>
        </div>
      </header>

      <main class="admin-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-shell {
  max-width: 1660px;
  margin: 0 auto;
  padding: 24px;
  min-height: 100vh;
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 24px;
}

.admin-sidebar {
  position: sticky;
  top: 24px;
  align-self: start;
}

.admin-sidebar__panel {
  min-height: calc(100vh - 48px);
  padding: 24px;
  border-radius: 36px;
  border: 1px solid rgba(219, 228, 241, 0.94);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.08);
  display: grid;
  grid-template-rows: auto 1fr auto;
  gap: 28px;
}

.admin-sidebar__top,
.admin-sidebar__intro,
.admin-nav,
.admin-sidebar__footer {
  display: grid;
  gap: 18px;
}

.admin-sidebar__tag {
  width: fit-content;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-sidebar__identity {
  display: grid;
  grid-template-columns: 68px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.admin-sidebar__avatar {
  width: 68px;
  height: 68px;
  border-radius: 24px;
  overflow: hidden;
  background: linear-gradient(135deg, #2563eb, #14b8a6);
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 22px;
  font-weight: 800;
}

.admin-sidebar__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.admin-sidebar__identity strong,
.admin-nav__text strong {
  display: block;
  margin: 0;
}

.admin-sidebar__identity strong {
  font-size: 20px;
}

.admin-sidebar__identity p,
.admin-sidebar__intro p,
.admin-nav__text small,
.admin-header p {
  margin: 0;
  color: #64748b;
}

.admin-sidebar__intro h2,
.admin-header h1 {
  margin: 0;
}

.admin-sidebar__intro h2 {
  font-size: 34px;
  line-height: 1.08;
}

.admin-sidebar__intro p {
  line-height: 1.8;
}

.admin-nav__item {
  min-height: 72px;
  padding: 0 16px;
  border-radius: 22px;
  border: 1px solid transparent;
  background: #f8fbff;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.admin-nav__item:hover {
  transform: translateY(-1px);
  border-color: rgba(147, 197, 253, 0.8);
  box-shadow: 0 12px 24px rgba(37, 99, 235, 0.08);
}

.admin-nav__item.router-link-active {
  border-color: rgba(147, 197, 253, 0.95);
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.98), rgba(236, 254, 255, 0.92));
  box-shadow: 0 16px 32px rgba(37, 99, 235, 0.1);
}

.admin-nav__icon {
  width: 48px;
  height: 48px;
  border-radius: 18px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2563eb, #60a5fa);
  color: #fff;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.admin-nav__item.router-link-active .admin-nav__icon {
  background: linear-gradient(135deg, #1d4ed8, #14b8a6);
}

.admin-nav__text {
  display: grid;
  gap: 4px;
}

.admin-nav__text strong {
  font-size: 16px;
  color: #0f172a;
}

.admin-nav__text small {
  font-size: 13px;
}

.admin-sidebar__footer {
  align-content: end;
}

.admin-sidebar__button {
  min-height: 50px;
  border-radius: 18px;
  font-weight: 700;
  border: none;
}

.admin-sidebar__button--ghost {
  background: #f8fbff;
  border: 1px solid rgba(191, 219, 254, 0.92);
  color: #1d4ed8;
}

.admin-sidebar__button--primary {
  background: linear-gradient(135deg, #2563eb, #14b8a6);
  color: #fff;
  box-shadow: 0 18px 32px rgba(37, 99, 235, 0.2);
}

.admin-main {
  display: grid;
  gap: 24px;
  align-content: start;
}

.admin-header {
  min-height: 114px;
  padding: 28px 30px;
  border-radius: 34px;
  border: 1px solid rgba(219, 228, 241, 0.95);
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 22px 52px rgba(15, 23, 42, 0.07);
  display: flex;
  gap: 16px;
  align-items: center;
}

.admin-header__eyebrow {
  display: inline-block;
  margin-bottom: 10px;
  color: #2563eb;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.admin-header h1 {
  font-size: 34px;
  line-height: 1.12;
}

.admin-header p {
  margin-top: 10px;
  font-size: 14px;
}
</style>
