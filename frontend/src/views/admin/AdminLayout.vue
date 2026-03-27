<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const store = useAppStore()

const navItems = [
  { label: '工作台', to: '/admin/dashboard', name: 'admin-dashboard' },
  { label: '商品管理', to: '/admin/goods', name: 'admin-goods' },
  { label: '订单中心', to: '/admin/orders', name: 'admin-orders' },
  { label: '用户管理', to: '/admin/users', name: 'admin-users' }
]

const adminName = computed(() => store.currentUser?.name ?? '平台管理员')
const overview = computed(() => [
  { label: '在售商品', value: store.goods.filter((item) => item.status === '在售中').length },
  { label: '订单总数', value: store.orders.length },
  { label: '用户规模', value: store.accounts.length }
])

const onLogout = async () => {
  await authStore.logout()
  router.push('/admin/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-sidebar__brand">
        <span class="admin-sidebar__logo">S</span>
        <div>
          <strong>尚有新生</strong>
          <small>管理系统</small>
        </div>
      </div>

      <section class="admin-sidebar__profile">
        <span class="admin-sidebar__tag">ADMIN</span>
        <strong>{{ adminName }}</strong>
        <small>平台审核 · 运营巡检 · 用户管理</small>
      </section>

      <nav class="admin-sidebar__nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.name"
          class="admin-sidebar__link"
          :class="{ 'admin-sidebar__link--active': route.name === item.name }"
          :to="item.to"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <section class="admin-sidebar__summary">
        <div v-for="item in overview" :key="item.label" class="admin-sidebar__summary-item">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>
    </aside>

    <div class="admin-main">
      <header class="admin-topbar">
        <div>
          <span class="admin-topbar__eyebrow">Desktop Admin</span>
          <h1>运营管理后台</h1>
        </div>
        <div class="admin-topbar__actions">
          <RouterLink class="admin-topbar__ghost" to="/">返回前台</RouterLink>
          <button class="admin-topbar__primary" type="button" @click="onLogout">退出登录</button>
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
  min-height: 100vh;
  display: grid;
  grid-template-columns: 260px minmax(0, 1fr);
  background:
    radial-gradient(circle at top left, rgba(201, 151, 42, 0.12), transparent 28%),
    linear-gradient(180deg, #f7faf4 0%, #eef5ee 100%);
  color: var(--text);
}

.admin-sidebar {
  padding: 28px 20px;
  background: linear-gradient(180deg, #203225 0%, #14512b 100%);
  color: #f4f7ef;
  display: grid;
  gap: 24px;
  align-content: start;
  border-right: 1px solid rgba(255, 255, 255, 0.08);
}

.admin-sidebar__brand,
.admin-sidebar__profile,
.admin-sidebar__summary-item {
  display: grid;
  gap: 8px;
}

.admin-sidebar__brand {
  grid-template-columns: auto 1fr;
  align-items: center;
  gap: 14px;
}

.admin-sidebar__logo {
  width: 46px;
  height: 46px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand) 70%, var(--gold));
  color: #fff;
  font-size: 22px;
  font-weight: 800;
}

.admin-sidebar__brand strong,
.admin-sidebar__profile strong {
  font-size: 18px;
}

.admin-sidebar__brand small,
.admin-sidebar__profile small,
.admin-sidebar__summary-item span {
  color: rgba(244, 247, 239, 0.72);
}

.admin-sidebar__profile,
.admin-sidebar__summary {
  padding: 18px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.admin-sidebar__tag {
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(201, 151, 42, 0.16);
  color: #f7d98a;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.admin-sidebar__nav {
  display: grid;
  gap: 10px;
}

.admin-sidebar__link {
  min-height: 50px;
  padding: 0 16px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  color: rgba(244, 247, 239, 0.78);
  border: 1px solid transparent;
}

.admin-sidebar__link--active {
  color: #fff;
  background: rgba(201, 151, 42, 0.14);
  border-color: rgba(201, 151, 42, 0.24);
}

.admin-sidebar__summary {
  display: grid;
  gap: 14px;
}

.admin-sidebar__summary-item strong {
  font-size: 28px;
  color: #fff;
}

.admin-main {
  min-width: 0;
  padding: 28px;
  display: grid;
  gap: 24px;
  align-content: start;
}

.admin-topbar {
  min-height: 88px;
  padding: 22px 24px;
  border-radius: 12px;
  background: rgba(255, 253, 248, 0.92);
  border: 1px solid var(--line);
  box-shadow: var(--shadow);
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.admin-topbar h1 {
  margin: 6px 0 0;
  font-size: 28px;
}

.admin-topbar__eyebrow {
  color: var(--gold);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: none;
}

.admin-topbar__actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.admin-topbar__ghost,
.admin-topbar__primary {
  min-height: 46px;
  padding: 0 18px;
  border-radius: 8px;
  font-weight: 600;
}

.admin-topbar__ghost {
  border: 1px solid var(--line);
  background: var(--surface-strong);
  color: var(--text);
}

.admin-topbar__primary {
  border: none;
  background: linear-gradient(135deg, var(--brand-deep), var(--brand));
  color: #fff;
}

.admin-content {
  min-width: 0;
}

@media (max-width: 1080px) {
  .admin-shell {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .admin-sidebar__nav,
  .admin-sidebar__summary {
    grid-column: 1 / -1;
  }
}

@media (max-width: 768px) {
  .admin-main,
  .admin-sidebar {
    padding: 18px;
  }

  .admin-sidebar {
    grid-template-columns: 1fr;
  }

  .admin-topbar {
    padding: 18px;
    flex-direction: column;
    align-items: flex-start;
  }

  .admin-topbar__actions {
    width: 100%;
  }

  .admin-topbar__ghost,
  .admin-topbar__primary {
    flex: 1;
    justify-content: center;
    display: inline-flex;
    align-items: center;
  }
}
</style>
