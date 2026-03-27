<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import { applyImageFallback, resolveAssetUrl } from '@/utils/assets'

const store = useAppStore()
const users = computed(() => store.accounts)
const userSummary = computed(() => [
  { label: '全部用户', value: users.value.length },
  { label: '管理员', value: users.value.filter((item) => item.role === 'ADMIN').length },
  { label: '卖家', value: users.value.filter((item) => item.role === 'SELLER').length },
  { label: '普通用户', value: users.value.filter((item) => item.role === 'USER').length }
])
const onImageError = (event: Event) => applyImageFallback(event, 'avatar')
</script>

<template>
  <section class="admin-page">
    <div class="admin-page__head">
      <div>
        <span class="admin-page__eyebrow">User Center</span>
        <h2>用户管理</h2>
      </div>
      <span class="admin-page__count">{{ users.length }} 位用户</span>
    </div>

    <div class="admin-summary-grid">
      <article v-for="item in userSummary" :key="item.label" class="admin-summary-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <section class="admin-panel">
      <div class="user-admin-list">
        <article v-for="user in users" :key="user.id" class="user-admin-card">
          <img
            :src="resolveAssetUrl(user.avatar, 'avatar')"
            :alt="user.name"
            class="user-admin-card__avatar"
            @error="onImageError"
          />
          <div class="user-admin-card__content">
            <div class="user-admin-card__title-row">
              <div>
                <h3>{{ user.name }}</h3>
                <p>{{ user.phone }}</p>
              </div>
              <div class="user-admin-card__badges">
                <span class="user-admin-card__badge user-admin-card__badge--role">{{ user.role }}</span>
                <span class="user-admin-card__badge">{{ user.kycLevel }}</span>
              </div>
            </div>

            <div class="user-admin-card__meta">
              <div><strong>城市</strong><span>{{ user.city }}</span></div>
              <div><strong>碳积分</strong><span>{{ user.carbonPoints }}</span></div>
              <div><strong>收藏数</strong><span>{{ user.likedGoodsIds.length }}</span></div>
            </div>

            <p class="user-admin-card__bio">{{ user.bio }}</p>
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
.user-admin-card__title-row {
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
  gap: 16px;
}

.admin-summary-card span,
.user-admin-card__content p,
.user-admin-card__meta strong {
  color: var(--muted);
}

.admin-summary-card strong {
  font-size: 30px;
  color: var(--brand);
}

.admin-panel {
  padding: 22px;
}

.user-admin-list {
  display: grid;
  gap: 16px;
}

.user-admin-card {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr);
  gap: 18px;
  padding: 18px;
  border-radius: 12px;
  background: rgba(247, 250, 244, 0.96);
  border: 1px solid var(--line);
}

.user-admin-card__avatar {
  width: 84px;
  height: 84px;
  border-radius: 24px;
  object-fit: cover;
}

.user-admin-card__content {
  display: grid;
  gap: 14px;
}

.user-admin-card__content h3,
.user-admin-card__content p {
  margin: 0;
}

.user-admin-card__badges {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.user-admin-card__badge {
  width: fit-content;
  padding: 6px 10px;
  border-radius: 999px;
  background: var(--gold-soft);
  color: #8a6416;
  font-size: 13px;
  font-weight: 700;
}

.user-admin-card__badge--role {
  background: var(--brand-soft);
  color: var(--brand);
}

.user-admin-card__meta {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.user-admin-card__meta div {
  display: grid;
  gap: 4px;
}

.user-admin-card__bio {
  color: var(--muted);
  line-height: 1.7;
}

@media (max-width: 1080px) {
  .admin-summary-grid,
  .user-admin-card__meta {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .admin-page__head,
  .user-admin-card__title-row {
    flex-direction: column;
    align-items: flex-start;
  }

  .admin-summary-grid,
  .user-admin-card {
    grid-template-columns: 1fr;
  }

  .user-admin-card__meta {
    grid-template-columns: 1fr;
  }
}
</style>
