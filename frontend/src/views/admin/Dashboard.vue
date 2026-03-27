<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()

const metrics = computed(() => [
  {
    label: '在售商品',
    value: store.goods.filter((item) => item.status === '在售中').length,
    hint: '实时追踪当前商品池'
  },
  {
    label: '待处理订单',
    value: store.orders.filter((item) => item.status === '待付款' || item.status === '待发货').length,
    hint: '优先跟进发货和支付'
  },
  {
    label: '累计减碳',
    value: `${store.goods.reduce((sum, item) => sum + item.carbonSavedKg, 0)} kg`,
    hint: '平台循环贡献总量'
  },
  {
    label: '用户总数',
    value: store.accounts.length,
    hint: '含买家、卖家与管理员'
  }
])

const latestGoods = computed(() => store.goods.slice(0, 6))
const latestOrders = computed(() => store.orders.slice(0, 5))
</script>

<template>
  <section class="admin-page">
    <div class="admin-page__head">
      <div>
        <span class="admin-page__eyebrow">Overview</span>
        <h2>运营总览</h2>
      </div>
      <span class="admin-page__note">独立后台 · PC 运营工作台</span>
    </div>

    <div class="admin-metric-grid">
      <article v-for="item in metrics" :key="item.label" class="admin-metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </div>

    <div class="admin-dashboard-grid">
      <section class="admin-panel">
        <div class="admin-panel__head">
          <h3>最新商品</h3>
          <RouterLink to="/admin/goods">进入商品管理</RouterLink>
        </div>

        <div class="admin-list">
          <div v-for="item in latestGoods" :key="item.id" class="admin-list__row">
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.brand }} · {{ item.category }} · {{ item.condition }}</p>
            </div>
            <div class="admin-list__meta">
              <span>¥{{ item.price.toLocaleString() }}</span>
              <span class="admin-badge">{{ item.status }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="admin-panel">
        <div class="admin-panel__head">
          <h3>待跟进订单</h3>
          <RouterLink to="/admin/orders">进入订单中心</RouterLink>
        </div>

        <div class="admin-list">
          <div v-for="item in latestOrders" :key="item.id" class="admin-list__row">
            <div>
              <strong>{{ item.id }}</strong>
              <p>{{ item.buyerName }}</p>
            </div>
            <div class="admin-list__meta">
              <span>¥{{ item.amount.toLocaleString() }}</span>
              <span class="admin-badge admin-badge--soft">{{ item.status }}</span>
            </div>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.admin-page {
  display: grid;
  gap: 20px;
}

.admin-page__head,
.admin-panel__head,
.admin-list__row,
.admin-list__meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.admin-page__eyebrow {
  display: inline-block;
  margin-bottom: 8px;
  color: var(--gold);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: none;
}

.admin-page__head h2,
.admin-panel__head h3 {
  margin: 0;
}

.admin-page__head h2 {
  font-size: 28px;
}

.admin-page__note {
  color: var(--muted);
  font-weight: 600;
}

.admin-metric-grid,
.admin-dashboard-grid {
  display: grid;
  gap: 16px;
}

.admin-metric-grid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-dashboard-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.admin-metric-card,
.admin-panel {
  border-radius: 12px;
  border: 1px solid var(--line);
  background: rgba(255, 253, 248, 0.92);
  box-shadow: var(--shadow);
}

.admin-metric-card {
  padding: 22px;
  display: grid;
  gap: 8px;
}

.admin-metric-card span,
.admin-metric-card small,
.admin-list__row p {
  color: var(--muted);
}

.admin-metric-card strong {
  font-size: 32px;
  color: var(--brand);
}

.admin-panel {
  padding: 22px;
  display: grid;
  gap: 14px;
}

.admin-panel__head a {
  color: var(--brand);
  font-weight: 600;
}

.admin-list {
  display: grid;
}

.admin-list__row {
  padding: 14px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.14);
}

.admin-list__row:last-child {
  border-bottom: none;
}

.admin-list__row strong,
.admin-list__row p {
  margin: 0;
}

.admin-list__meta {
  flex-wrap: wrap;
}

.admin-badge {
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--gold-soft);
  color: #8a6416;
  font-size: 13px;
  font-weight: 700;
}

.admin-badge--soft {
  background: var(--brand-soft);
  color: var(--brand);
}

@media (max-width: 1080px) {
  .admin-metric-grid,
  .admin-dashboard-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .admin-page__head,
  .admin-panel__head,
  .admin-list__row {
    flex-direction: column;
    align-items: flex-start;
  }

  .admin-metric-grid,
  .admin-dashboard-grid {
    grid-template-columns: 1fr;
  }
}
</style>
