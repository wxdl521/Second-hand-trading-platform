<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import { applyImageFallback, resolveAssetUrl } from '@/utils/assets'

const store = useAppStore()
const goods = computed(() => store.goods)
const goodsSummary = computed(() => [
  { label: '全部商品', value: goods.value.length },
  { label: '在售中', value: goods.value.filter((item) => item.status === '在售中').length },
  { label: '已预订', value: goods.value.filter((item) => item.status === '已预订').length },
  { label: '已售出', value: goods.value.filter((item) => item.status === '已售出').length }
])
const onImageError = (event: Event) => applyImageFallback(event, 'goods')
</script>

<template>
  <section class="admin-page">
    <div class="admin-page__head">
      <div>
        <span class="admin-page__eyebrow">Goods Review</span>
        <h2>商品管理</h2>
      </div>
      <span class="admin-page__count">{{ goods.length }} 件</span>
    </div>

    <div class="admin-summary-grid">
      <article v-for="item in goodsSummary" :key="item.label" class="admin-summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <section class="admin-panel">
      <div class="goods-admin-list">
        <article v-for="item in goods" :key="item.id" class="goods-admin-card">
          <img
            :src="resolveAssetUrl(item.heroImage, 'goods')"
            :alt="item.title"
            class="goods-admin-card__image"
            @error="onImageError"
          />
          <div class="goods-admin-card__content">
            <div class="goods-admin-card__title-row">
              <div>
                <h3>{{ item.title }}</h3>
                <p>{{ item.brand }} · {{ item.category }} · {{ item.condition }}</p>
              </div>
              <span class="goods-admin-card__badge">{{ item.status }}</span>
            </div>

            <div class="goods-admin-card__meta">
              <div><strong>售价</strong><span>¥{{ item.price.toLocaleString() }}</span></div>
              <div><strong>AI 估价</strong><span>¥{{ item.aiPrice.toLocaleString() }}</span></div>
              <div><strong>卖家</strong><span>{{ item.sellerName }}</span></div>
              <div><strong>减碳</strong><span>{{ item.carbonSavedKg }} kg</span></div>
            </div>

            <div class="goods-admin-card__actions">
              <RouterLink :to="`/goods/${item.id}`">查看前台详情</RouterLink>
              <span>{{ item.createdAt }}</span>
            </div>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.admin-page {
  display: grid;
  gap: 20px;
}

.admin-page__head,
.goods-admin-card__title-row,
.goods-admin-card__actions {
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
  gap: 8px;
}

.admin-summary-card span,
.goods-admin-card__content p,
.goods-admin-card__meta strong,
.goods-admin-card__actions span {
  color: var(--muted);
}

.admin-summary-card strong {
  font-size: 30px;
  color: var(--brand);
}

.admin-panel {
  padding: 22px;
}

.goods-admin-list {
  display: grid;
  gap: 16px;
}

.goods-admin-card {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  border-radius: 12px;
  background: rgba(247, 250, 244, 0.96);
  border: 1px solid var(--line);
}

.goods-admin-card__image {
  width: 100%;
  height: 180px;
  object-fit: cover;
  border-radius: 18px;
}

.goods-admin-card__content {
  display: grid;
  gap: 14px;
  min-width: 0;
}

.goods-admin-card__content h3,
.goods-admin-card__content p {
  margin: 0;
}

.goods-admin-card__badge {
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--gold-soft);
  color: #8a6416;
  font-size: 13px;
  font-weight: 700;
}

.goods-admin-card__meta {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.goods-admin-card__meta div {
  display: grid;
  gap: 4px;
}

.goods-admin-card__meta span,
.goods-admin-card__actions a {
  color: var(--text);
  font-weight: 600;
}

.goods-admin-card__actions a {
  color: var(--brand);
}

@media (max-width: 1080px) {
  .admin-summary-grid,
  .goods-admin-card__meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .admin-page__head,
  .goods-admin-card__title-row,
  .goods-admin-card__actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .admin-summary-grid,
  .goods-admin-card,
  .goods-admin-card__meta {
    grid-template-columns: 1fr;
  }

  .goods-admin-card {
    padding: 14px;
  }
}
</style>
