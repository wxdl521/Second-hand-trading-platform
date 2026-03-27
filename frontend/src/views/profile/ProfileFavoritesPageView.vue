<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listFavoriteGoods } from '@/api/goods'
import GoodsCard from '@/components/goods/GoodsCard.vue'
import type { GoodsItem } from '@/types'

const favorites = ref<GoodsItem[]>([])
const loading = ref(false)

const loadFavorites = async () => {
  loading.value = true

  try {
    favorites.value = await listFavoriteGoods()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadFavorites()
})

const favoriteStats = computed(() => [
  { label: '收藏商品', value: String(favorites.value.length) },
  {
    label: '在售中',
    value: String(favorites.value.filter((item) => item.status === '在售中').length)
  },
  {
    label: '待审核/下架',
    value: String(favorites.value.filter((item) => ['待审核', '已下架'].includes(item.status)).length)
  }
])
</script>

<template>
  <section class="profile-page profile-page--compact">
    <div class="profile-topbar">
      <div>
        <span class="eyebrow">我的收藏</span>
        <h1>收藏夹</h1>
        <p class="lead">集中查看你关注的商品，方便继续比较、下单或观察审核状态。</p>
      </div>
      <div class="action-row">
        <RouterLink class="ghost-btn" to="/goods">继续逛商品</RouterLink>
        <RouterLink class="profile-settings-btn profile-settings-btn--plain" to="/profile">返回主页</RouterLink>
      </div>
    </div>

    <section class="panel panel--soft">
      <div class="profile-stats">
        <article v-for="item in favoriteStats" :key="item.label" class="profile-stat-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="section-head section-head--compact">
        <div>
          <span class="eyebrow">收藏列表</span>
          <h2>你关注的商品都在这里</h2>
        </div>
      </div>

      <div v-if="loading" class="empty-card">
        <h3>正在同步收藏</h3>
        <p>请稍候，系统正在拉取你最新的收藏列表。</p>
      </div>

      <div v-else-if="favorites.length" class="goods-grid goods-grid--compact">
        <GoodsCard v-for="item in favorites" :key="item.id" :item="item" />
      </div>

      <div v-else class="empty-card">
        <h3>还没有收藏商品</h3>
        <p>去逛逛精选商品，把喜欢的好物先收藏起来。</p>
        <RouterLink class="primary-btn" to="/goods">去挑选商品</RouterLink>
      </div>
    </section>
  </section>
</template>
