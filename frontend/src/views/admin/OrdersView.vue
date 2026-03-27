<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'

const store = useAppStore()

const orders = computed(() => store.orders)
const statusSummary = computed(() => [
  { label: '待付款', value: orders.value.filter((item) => item.status === '待付款').length },
  { label: '待发货', value: orders.value.filter((item) => item.status === '待发货').length },
  { label: '运输中', value: orders.value.filter((item) => item.status === '运输中').length },
  { label: '已完成', value: orders.value.filter((item) => item.status === '已完成').length }
])
</script>

<template>
  <section class="admin-page">
    <div class="admin-page__head">
      <div>
        <span class="admin-page__eyebrow">Order Center</span>
        <h2>订单中心</h2>
      </div>
      <span class="admin-page__count">共 {{ orders.length }} 笔订单</span>
    </div>

    <div class="admin-summary-grid">
      <article v-for="item in statusSummary" :key="item.label" class="admin-summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <section class="admin-panel">
      <div class="admin-table">
        <div class="admin-table__head">
          <span>订单号</span>
          <span>买家</span>
          <span>金额</span>
          <span>状态</span>
          <span>创建时间</span>
        </div>

        <div v-for="item in orders" :key="item.id" class="admin-table__row">
          <strong>{{ item.id }}</strong>
          <span>{{ item.buyerName }}</span>
          <span>¥{{ item.amount.toLocaleString() }}</span>
          <span class="admin-status">{{ item.status }}</span>
          <span>{{ item.createdAt }}</span>
        </div>
      </div>
    </section>
  </section>
</template>

<style scoped>
.admin-page {
  display: grid;
  gap: 20px;
}

.admin-page__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
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

.admin-page__head h2 {
  margin: 0;
  font-size: 28px;
}

.admin-page__count {
  color: var(--muted);
  font-weight: 600;
}

.admin-summary-grid {
  display: grid;
  gap: 16px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.admin-summary-card,
.admin-panel {
  border-radius: 12px;
  border: 1px solid var(--line);
  background: rgba(255, 253, 248, 0.92);
  box-shadow: var(--shadow);
}

.admin-summary-card {
  padding: 22px;
  display: grid;
  gap: 10px;
}

.admin-summary-card span,
.admin-table__head {
  color: var(--muted);
}

.admin-summary-card strong {
  font-size: 30px;
  color: var(--brand);
}

.admin-panel {
  padding: 10px 0;
}

.admin-table {
  display: grid;
}

.admin-table__head,
.admin-table__row {
  padding: 0 22px;
  min-height: 64px;
  display: grid;
  grid-template-columns: 1.25fr 0.9fr 0.8fr 0.8fr 1fr;
  gap: 16px;
  align-items: center;
}

.admin-table__row {
  border-top: 1px solid rgba(148, 163, 184, 0.14);
}

.admin-status {
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 1080px) {
  .admin-summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .admin-page__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .admin-summary-grid,
  .admin-table__head,
  .admin-table__row {
    grid-template-columns: 1fr;
  }

  .admin-table__head {
    display: none;
  }

  .admin-table__row {
    padding: 18px 22px;
    gap: 10px;
  }
}
</style>
