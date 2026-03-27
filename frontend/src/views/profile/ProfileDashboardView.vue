<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getProfile } from '@/api/auth'
import { listFavoriteGoods, listMyGoods } from '@/api/goods'
import { listMessages, markMessageRead, type UserMessageItem } from '@/api/message'
import GoodsCard from '@/components/goods/GoodsCard.vue'
import { useAppStore } from '@/stores/app'
import { applyImageFallback, resolveAssetUrl } from '@/utils/assets'
import type { GoodsItem } from '@/types'

const store = useAppStore()
const messages = ref<UserMessageItem[]>([])
const myGoods = ref<GoodsItem[]>([])

onMounted(async () => {
  await getProfile()

  if (!store.currentUser) {
    return
  }

  const [favoriteResult, messageResult, goodsResult] = await Promise.allSettled([
    listFavoriteGoods(),
    listMessages(),
    listMyGoods()
  ])

  if (messageResult.status === 'fulfilled') {
    messages.value = messageResult.value
  }

  if (goodsResult.status === 'fulfilled') {
    myGoods.value = goodsResult.value
  }

  if (favoriteResult.status === 'rejected') {
    console.warn('Failed to load favorite goods.', favoriteResult.reason)
  }
})

const avatarUrl = computed(() => resolveAssetUrl(store.currentUser?.avatar, 'avatar'))

const userSummary = computed(() => [
  { label: 'KYC 等级', value: store.currentUser?.kycLevel ?? '-' },
  { label: '所在城市', value: store.currentUser?.city || '未设置' },
  { label: '碳积分', value: String(store.carbonBalance) },
  { label: '收藏数', value: String(store.favoriteGoods.length) }
])

const profileTags = computed(() => [
  store.currentUser?.city || '未设置城市',
  `KYC ${store.currentUser?.kycLevel ?? '-'}`,
  `${store.carbonBalance} 碳积分`
])

const unreadMessageCount = computed(() => messages.value.filter((item) => !item.isRead).length)
const sellerSummary = computed(() => [
  { label: '我的商品', value: String(myGoods.value.length) },
  { label: '草稿待补充', value: String(myGoods.value.filter((item) => item.status === '草稿').length) },
  { label: '待审核', value: String(myGoods.value.filter((item) => item.status === '待审核').length) },
  { label: '在售中', value: String(myGoods.value.filter((item) => item.status === '在售中').length) }
])
const sellerActions = computed(() => [
  { title: '管理商品', desc: '查看草稿、待审核和在售状态', to: '/goods/manage' },
  { title: '发布闲置', desc: '继续发布新的高价值商品', to: '/goods/publish' },
  { title: '订单中心', desc: '查看交易与物流进度', to: '/order/list' },
  { title: '消息通知', desc: `${unreadMessageCount.value} 条未读消息`, to: '/profile/messages' }
])

const onAvatarError = (event: Event) => applyImageFallback(event, 'avatar')

const onReadMessage = async (id: string) => {
  const updated = await markMessageRead(id)
  messages.value = messages.value.map((item) => (item.id === updated.id ? updated : item))
}
</script>

<template>
  <section v-if="store.currentUser" class="profile-page profile-page--compact">
    <div class="profile-topbar">
      <div>
        <span class="eyebrow">我的主页</span>
        <h1>{{ store.currentUser.name }}</h1>
      </div>
      <RouterLink class="profile-settings-btn" to="/profile/settings">进入设置</RouterLink>
    </div>

    <div class="profile-layout">
      <section class="panel profile-hero-card">
        <div class="profile-hero-card__main">
          <img
            :src="avatarUrl"
            :alt="store.currentUser.name"
            class="profile-avatar profile-avatar--xl"
            @error="onAvatarError"
          />
          <div class="profile-hero-card__body">
            <span class="profile-badge">KYC {{ store.currentUser.kycLevel }}</span>
            <div>
              <h2>{{ store.currentUser.name }}</h2>
              <p>{{ store.currentUser.bio || '介绍一下你的偏好和生活方式，让主页更有温度。' }}</p>
            </div>
            <div class="chip-row">
              <span v-for="tag in profileTags" :key="tag" class="profile-tag">{{ tag }}</span>
            </div>
          </div>
        </div>
      </section>

      <section class="panel panel--soft">
        <div class="section-head section-head--compact">
          <div>
            <span class="eyebrow">账户概览</span>
            <h2>个人信息一览</h2>
          </div>
        </div>
        <div class="profile-stats">
          <article v-for="item in userSummary" :key="item.label" class="profile-stat-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>
      </section>
    </div>

    <section class="panel">
      <div class="section-head section-head--compact">
        <div>
          <span class="eyebrow">卖家工作台</span>
          <h2>商品与审核进度</h2>
          <p>从这里继续处理草稿、待审核商品和订单进度。</p>
        </div>
        <RouterLink class="ghost-btn" to="/goods/manage">查看全部商品</RouterLink>
      </div>

      <div class="profile-stats">
        <article v-for="item in sellerSummary" :key="item.label" class="profile-stat-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>

      <div class="profile-action-strip">
        <RouterLink v-for="item in sellerActions" :key="item.title" class="profile-action-chip" :to="item.to">
          <strong>{{ item.title }}</strong>
          <span>{{ item.desc }}</span>
        </RouterLink>
      </div>
    </section>

    <section class="panel">
      <div class="section-head section-head--compact">
        <div>
          <span class="eyebrow">消息通知</span>
          <h2>最近动态与审核进度</h2>
          <p>订单流转、商品审核和实名认证结果都会在这里同步。</p>
        </div>
        <div class="profile-message-actions">
          <span class="badge">{{ unreadMessageCount }} 条未读</span>
          <RouterLink class="ghost-btn" to="/profile/messages">查看全部</RouterLink>
        </div>
      </div>

      <div v-if="messages.length" class="profile-message-list">
        <article
          v-for="message in messages.slice(0, 4)"
          :key="message.id"
          class="profile-message-card"
          :class="{ 'profile-message-card--read': message.isRead }"
        >
          <div class="profile-message-card__meta">
            <strong>{{ message.title }}</strong>
            <span>{{ message.createdAt }}</span>
          </div>
          <p>{{ message.content }}</p>
          <button v-if="!message.isRead" class="ghost-btn" type="button" @click="onReadMessage(message.id)">
            标记已读
          </button>
        </article>
      </div>

      <div v-else class="empty-card">
        <h3>暂时没有新消息</h3>
        <p>等订单、审核或实名认证有进度时，这里会第一时间更新。</p>
      </div>
    </section>

    <section class="panel">
      <div class="section-head section-head--compact">
        <div>
          <span class="eyebrow">我的收藏</span>
          <h2>关注中的高价值好物</h2>
          <p>同步展示你最近收藏的商品，方便继续比较与下单。</p>
        </div>
        <div class="profile-message-actions">
          <RouterLink class="ghost-btn" to="/profile/favorites">查看全部</RouterLink>
          <RouterLink class="ghost-btn" to="/goods">继续逛逛</RouterLink>
        </div>
      </div>

      <div v-if="store.favoriteGoods.length" class="goods-grid goods-grid--compact">
        <GoodsCard v-for="item in store.favoriteGoods.slice(0, 4)" :key="item.id" :item="item" />
      </div>

      <div v-else class="empty-card">
        <h3>还没有收藏商品</h3>
        <p>去逛逛精选商品，把喜欢的好物先收藏起来。</p>
        <RouterLink class="primary-btn" to="/goods">去挑选商品</RouterLink>
      </div>
    </section>
  </section>
</template>

<style scoped>
.profile-message-list {
  display: grid;
  gap: 14px;
}

.profile-message-actions {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.profile-message-card {
  display: grid;
  gap: 10px;
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba(248, 250, 252, 0.94);
  border: 1px solid rgba(46, 158, 91, 0.12);
}

.profile-message-card--read {
  opacity: 0.72;
}

.profile-message-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.profile-message-card__meta strong {
  color: #0f172a;
}

.profile-message-card__meta span {
  color: #64748b;
  font-size: 13px;
}

.profile-message-card p {
  margin: 0;
  color: #475569;
  line-height: 1.7;
}

@media (max-width: 767px) {
  .profile-message-actions {
    width: 100%;
    justify-content: space-between;
  }

  .profile-message-card__meta {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
