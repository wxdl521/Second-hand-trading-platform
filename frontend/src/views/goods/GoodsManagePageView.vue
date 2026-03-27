<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { deleteOwnedGoods, listMyGoods, publishOwnedGoods } from '@/api/goods'
import type { GoodsItem } from '@/types'
import { applyImageFallback, resolveAssetUrl } from '@/utils/assets'

type GoodsFilter = '全部' | GoodsItem['status']

const goodsList = ref<GoodsItem[]>([])
const loading = ref(false)
const activeFilter = ref<GoodsFilter>('全部')

const filterTabs: GoodsFilter[] = ['全部', '草稿', '待审核', '在售中', '已下架', '已预订', '已售出']

const loadGoods = async () => {
  loading.value = true

  try {
    goodsList.value = await listMyGoods()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadGoods()
})

const visibleGoods = computed(() =>
  activeFilter.value === '全部' ? goodsList.value : goodsList.value.filter((item) => item.status === activeFilter.value)
)

const goodsStats = computed(() => [
  { label: '全部商品', value: String(goodsList.value.length) },
  { label: '草稿待补充', value: String(goodsList.value.filter((item) => item.status === '草稿').length) },
  { label: '待审核', value: String(goodsList.value.filter((item) => item.status === '待审核').length) },
  { label: '在售中', value: String(goodsList.value.filter((item) => item.status === '在售中').length) }
])

const replaceGoods = (nextItem: GoodsItem) => {
  goodsList.value = goodsList.value.map((item) => (item.id === nextItem.id ? nextItem : item))
}

const onImageError = (event: Event) => applyImageFallback(event, 'goods')

const onPublish = async (item: GoodsItem) => {
  try {
    const nextItem = await publishOwnedGoods(item.id)
    replaceGoods(nextItem)
    alert('商品已提交审核。')
  } catch (error) {
    alert((error as Error).message)
  }
}

const onOffShelf = async (item: GoodsItem) => {
  try {
    await deleteOwnedGoods(item.id)
    replaceGoods({ ...item, status: '已下架', reviewNote: '商品已下架。' })
    alert('商品已下架。')
  } catch (error) {
    alert((error as Error).message)
  }
}

const canPublish = (status: GoodsItem['status']) => ['草稿', '已下架'].includes(status)
const canOffShelf = (status: GoodsItem['status']) => !['已下架', '已售出'].includes(status)
</script>

<template>
  <section class="section-block goods-manage-page">
    <div class="section-head">
      <div>
        <span class="eyebrow">我的商品</span>
        <h1>管理草稿、送审和在售状态</h1>
        <p>这里集中展示你的卖家商品，支持继续编辑、提交审核和下架处理。</p>
      </div>
      <RouterLink class="primary-btn" to="/goods/publish">发布新商品</RouterLink>
    </div>

    <section class="panel panel--soft">
      <div class="profile-stats">
        <article v-for="item in goodsStats" :key="item.label" class="profile-stat-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="section-head section-head--compact">
        <div>
          <span class="eyebrow">状态筛选</span>
          <h2>按状态查看商品</h2>
        </div>
      </div>

      <div class="chip-row goods-manage-filters">
        <button
          v-for="status in filterTabs"
          :key="status"
          class="chip"
          :class="{ 'chip--active': activeFilter === status }"
          type="button"
          @click="activeFilter = status"
        >
          {{ status }}
        </button>
      </div>

      <div v-if="loading" class="empty-card">
        <h3>正在同步商品</h3>
        <p>请稍候，平台正在拉取你的最新商品状态。</p>
      </div>

      <div v-else-if="visibleGoods.length" class="goods-manage-list">
        <article v-for="item in visibleGoods" :key="item.id" class="panel goods-manage-card">
          <img :src="resolveAssetUrl(item.heroImage, 'goods')" :alt="item.title" @error="onImageError" />

          <div class="goods-manage-card__body">
            <div class="goods-manage-card__top">
              <div>
                <span class="eyebrow">{{ item.category }}</span>
                <h2>{{ item.title }}</h2>
              </div>
              <span class="badge">{{ item.status }}</span>
            </div>

            <div class="goods-manage-card__meta">
              <span>品牌：{{ item.brand }}</span>
              <span>成色：{{ item.condition }}</span>
              <span>售价：¥{{ item.price.toLocaleString() }}</span>
              <span>创建：{{ item.createdAt }}</span>
              <span>审核：{{ item.auditStatus ?? item.status }}</span>
              <span>收藏：{{ item.favorCount ?? 0 }}</span>
            </div>

            <p class="goods-manage-card__story">{{ item.story || '继续完善商品资料，让审核和成交更顺畅。' }}</p>
            <p v-if="item.reviewNote" class="goods-manage-card__note">{{ item.reviewNote }}</p>

            <div class="action-row goods-manage-card__actions">
              <RouterLink class="ghost-btn" :to="`/goods/${item.id}`">查看详情</RouterLink>
              <RouterLink v-if="item.status !== '已售出'" class="ghost-btn" :to="`/goods/publish?goodsId=${item.id}`">继续编辑</RouterLink>
              <button v-if="canPublish(item.status)" class="primary-btn" type="button" @click="onPublish(item)">提交审核</button>
              <button v-if="canOffShelf(item.status)" class="ghost-btn" type="button" @click="onOffShelf(item)">下架商品</button>
            </div>
          </div>
        </article>
      </div>

      <div v-else class="empty-card">
        <h3>{{ activeFilter === '全部' ? '还没有发布商品' : `暂无${activeFilter}商品` }}</h3>
        <p>可以先发布一个商品，或者切换其他状态查看历史记录。</p>
        <RouterLink class="primary-btn" to="/goods/publish">去发布商品</RouterLink>
      </div>
    </section>
  </section>
</template>

<style scoped>
.goods-manage-page {
  gap: 20px;
}

.goods-manage-filters {
  margin-bottom: 20px;
}

.goods-manage-list {
  display: grid;
  gap: 16px;
}

.goods-manage-card {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 18px;
  align-items: stretch;
}

.goods-manage-card img {
  width: 100%;
  height: 100%;
  min-height: 180px;
  border-radius: 20px;
  object-fit: cover;
}

.goods-manage-card__body {
  display: grid;
  gap: 14px;
}

.goods-manage-card__top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.goods-manage-card__top h2,
.goods-manage-card__story {
  margin: 0;
}

.goods-manage-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  color: #64748b;
}

.goods-manage-card__story {
  color: #475569;
  line-height: 1.7;
}

.goods-manage-card__note {
  margin: 0;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 244, 229, 0.95);
  color: #9a3412;
  line-height: 1.7;
}

.goods-manage-card__actions {
  flex-wrap: wrap;
}

@media (max-width: 767px) {
  .goods-manage-card {
    grid-template-columns: 1fr;
  }
}
</style>
