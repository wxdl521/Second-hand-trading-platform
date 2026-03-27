<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useSwipe } from '@vueuse/core'
import { listGoods } from '@/api/goods'
import { getHomeLanding, type HomeBanner, type HomeCategoryCard } from '@/api/home.remote'
import GoodsCard from '@/components/goods/GoodsCard.vue'
import { ElSkeleton, ElSkeletonItem } from 'element-plus'
import type { GoodsItem } from '@/types'

const banners = ref<HomeBanner[]>([])
const categoryCards = ref<HomeCategoryCard[]>([])
const featuredGoods = ref<GoodsItem[]>([])
const landingStats = ref({
  goodsCount: 0,
  orderCount: 0,
  carbonSavedKg: 0,
  todayCarbonKg: 0
})
const landingLoading = ref(true)
const goodsLoading = ref(true)
const displayIndex = ref(0)
const isSliding = ref(true)
const carouselViewport = ref<HTMLElement | null>(null)
const homeRoot = ref<HTMLElement | null>(null)
const refreshing = ref(false)
const liveCarbon = ref(0)

let bannerTimer: ReturnType<typeof window.setInterval> | null = null
let carbonTimer: ReturnType<typeof window.setInterval> | null = null

const displayBanners = computed(() => {
  if (banners.value.length <= 1) return banners.value
  return [banners.value[banners.value.length - 1], ...banners.value, banners.value[0]]
})

const activeBanner = computed(() => {
  if (banners.value.length <= 1) return 0
  if (displayIndex.value === 0) return banners.value.length - 1
  if (displayIndex.value === banners.value.length + 1) return 0
  return displayIndex.value - 1
})

const homeStats = computed(() => [
  { label: '在售商品', value: String(landingStats.value.goodsCount), hint: '首页聚合接口已切到后端' },
  { label: '成交订单', value: String(landingStats.value.orderCount), hint: '履约链路状态可直接回填首页' },
  { label: '累计减碳', value: `${liveCarbon.value}kg`, hint: '实时滚动展示循环贡献' }
])

const stopBannerTimer = () => {
  if (bannerTimer) {
    window.clearInterval(bannerTimer)
    bannerTimer = null
  }
}

const resetBannerTimer = () => {
  stopBannerTimer()
  if (banners.value.length <= 1) return
  bannerTimer = window.setInterval(() => {
    goToNextBanner()
  }, 3600)
}

const goToBanner = (index: number) => {
  isSliding.value = true
  displayIndex.value = banners.value.length > 1 ? index + 1 : index
  resetBannerTimer()
}

const goToNextBanner = () => {
  if (!banners.value.length) return
  isSliding.value = true
  displayIndex.value += 1
}

const goToPrevBanner = () => {
  if (!banners.value.length) return
  isSliding.value = true
  displayIndex.value -= 1
}

useSwipe(carouselViewport, {
  passive: true,
  threshold: 40,
  onSwipeEnd: (_, direction) => {
    if (direction === 'left') {
      goToNextBanner()
      resetBannerTimer()
      return
    }

    if (direction === 'right') {
      goToPrevBanner()
      resetBannerTimer()
    }
  }
})

useSwipe(homeRoot, {
  passive: true,
  threshold: 72,
  onSwipeEnd: async (_, direction) => {
    if (direction !== 'down' || refreshing.value || window.scrollY > 12) {
      return
    }

    await refreshHome()
  }
})

const onTransitionEnd = async () => {
  if (banners.value.length <= 1) return

  if (displayIndex.value === banners.value.length + 1) {
    isSliding.value = false
    displayIndex.value = 1
    await nextTick()
    requestAnimationFrame(() => {
      isSliding.value = true
    })
  }

  if (displayIndex.value === 0) {
    isSliding.value = false
    displayIndex.value = banners.value.length
    await nextTick()
    requestAnimationFrame(() => {
      isSliding.value = true
    })
  }
}

const syncCarbonCounter = () => {
  const target = landingStats.value.carbonSavedKg
  if (!liveCarbon.value) {
    liveCarbon.value = Math.max(0, target - 28)
  }

  if (liveCarbon.value < target) {
    liveCarbon.value += Math.min(4, target - liveCarbon.value)
    return
  }

  liveCarbon.value += 1
}

const resetCarbonTimer = () => {
  if (carbonTimer) {
    window.clearInterval(carbonTimer)
  }

  carbonTimer = window.setInterval(syncCarbonCounter, 1800)
}

const loadHomeLanding = async () => {
  landingLoading.value = true
  try {
    const landing = await getHomeLanding()
    banners.value = landing.banners
    categoryCards.value = landing.categories
    landingStats.value = landing.stats
    liveCarbon.value = Math.max(0, landing.stats.carbonSavedKg - 28)
  } finally {
    landingLoading.value = false
  }
}

const loadFeaturedGoods = async () => {
  goodsLoading.value = true
  try {
    featuredGoods.value = (await listGoods()).slice(0, 8)
  } finally {
    goodsLoading.value = false
  }
}

const refreshHome = async (silent = false) => {
  if (refreshing.value) return

  refreshing.value = true
  try {
    await Promise.all([loadHomeLanding(), loadFeaturedGoods()])
  } catch (error) {
    if (!silent) {
      alert((error as Error).message)
    }
  } finally {
    refreshing.value = false
  }
}

watch(
  () => banners.value.length,
  (count) => {
    displayIndex.value = count > 1 ? 1 : 0
    isSliding.value = true
    resetBannerTimer()
  },
  { immediate: true }
)

onMounted(async () => {
  await refreshHome(true)
  resetCarbonTimer()
})

onBeforeUnmount(() => {
  stopBannerTimer()
  if (carbonTimer) {
    window.clearInterval(carbonTimer)
    carbonTimer = null
  }
})

const carouselStyle = computed(() => ({
  transform: `translateX(-${displayIndex.value * 100}%)`,
  transition: isSliding.value ? 'transform 420ms ease' : 'none'
}))
</script>

<template>
  <section ref="homeRoot" class="home-page">
    <div class="home-refresh" :class="{ 'home-refresh--active': refreshing }">
      {{ refreshing ? '首页刷新中...' : '下拉可刷新首页内容' }}
    </div>

    <section class="home-layout">
      <aside class="home-aside">
      <section class="panel home-category-panel">
        <div class="section-head section-head--compact">
          <div>
            <span class="eyebrow">品类导航</span>
            <h2>热门分类</h2>
          </div>
        </div>

        <div v-if="landingLoading" class="home-skeleton-list">
          <ElSkeleton v-for="index in 4" :key="`category-skeleton-${index}`" animated>
            <template #template>
              <div class="home-category-item home-category-item--skeleton">
                <div class="home-skeleton-copy">
                  <ElSkeletonItem variant="text" style="width: 120px; height: 18px" />
                  <ElSkeletonItem variant="text" style="width: 160px; height: 14px" />
                </div>
                <ElSkeletonItem variant="button" style="width: 64px; height: 34px; border-radius: 999px" />
              </div>
            </template>
          </ElSkeleton>
        </div>

        <template v-else>
          <RouterLink
            v-for="item in categoryCards"
            :key="item.name"
            class="home-category-item"
            :to="{ name: 'goods-list', query: { category: item.name } }"
          >
            <div>
              <strong>{{ item.name }}</strong>
              <p>{{ item.sample }}</p>
            </div>
            <span>{{ item.count }} 件</span>
          </RouterLink>
        </template>
      </section>

      <section class="panel home-carbon-panel">
        <template v-if="landingLoading">
          <ElSkeleton animated>
            <template #template>
              <div class="home-carbon-panel__skeleton">
                <ElSkeletonItem variant="text" style="width: 72px; height: 14px" />
                <ElSkeletonItem variant="text" style="width: 128px; height: 48px" />
                <ElSkeletonItem variant="text" style="width: 100%; height: 18px" />
                <div class="home-carbon-panel__meta">
                  <ElSkeletonItem variant="button" style="width: 120px; height: 34px; border-radius: 999px" />
                  <ElSkeletonItem variant="button" style="width: 120px; height: 34px; border-radius: 999px" />
                </div>
              </div>
            </template>
          </ElSkeleton>
        </template>
        <template v-else>
          <span class="eyebrow">绿色贡献</span>
          <strong>{{ liveCarbon }}</strong>
          <p>平台累计循环减碳贡献实时滚动展示，强化“绿色交易”心智。</p>
          <div class="home-carbon-panel__meta">
            <span>今日新增 {{ landingStats.todayCarbonKg }}kg</span>
            <span>精选分类 {{ categoryCards.length }} 个</span>
          </div>
        </template>
      </section>
      </aside>

      <div class="home-main">
        <section class="home-carousel" @mouseenter="stopBannerTimer" @mouseleave="resetBannerTimer">
        <div v-if="landingLoading" class="panel panel--soft home-carousel__empty">
          <ElSkeleton animated class="home-carousel__skeleton">
            <template #template>
              <ElSkeletonItem variant="image" class="home-carousel__skeleton-image" />
              <div class="home-carousel__skeleton-content">
                <ElSkeletonItem variant="text" style="width: 48%; height: 26px" />
                <ElSkeletonItem variant="text" style="width: 72%; height: 16px" />
              </div>
            </template>
          </ElSkeleton>
        </div>
        <div
          v-else-if="banners.length"
          ref="carouselViewport"
          class="home-carousel__viewport"
        >
          <div class="home-carousel__slides" :style="carouselStyle" @transitionend="onTransitionEnd">
            <RouterLink
              v-for="(item, index) in displayBanners"
              :key="`${item.id}-${index}`"
              class="home-carousel__slide"
              :to="`/goods/${item.goodsId}`"
            >
              <img :src="item.image" :alt="item.title" />
              <div class="home-carousel__content">
                <strong>{{ item.title }}</strong>
                <p>{{ item.subtitle }}</p>
              </div>
            </RouterLink>
          </div>

          <div class="home-carousel__dots">
            <button
              v-for="(item, index) in banners"
              :key="item.id"
              class="home-carousel__dot"
              :class="{ 'home-carousel__dot--active': activeBanner === index }"
              type="button"
              :aria-label="`切换到第 ${index + 1} 张轮播图`"
              @click.prevent="goToBanner(index)"
            ></button>
          </div>
        </div>
        <div v-else class="panel panel--soft home-carousel__empty">
          <strong>首页内容准备中</strong>
          <p>后端首页聚合接口已接入，这里会展示平台精选轮播内容。</p>
        </div>
        </section>

        <section v-if="landingLoading" class="home-stats">
        <article v-for="index in 3" :key="`stat-skeleton-${index}`" class="metric-card home-stat-card">
          <ElSkeleton animated>
            <template #template>
              <div class="home-stat-card__skeleton">
                <ElSkeletonItem variant="text" style="width: 84px; height: 14px" />
                <ElSkeletonItem variant="text" style="width: 112px; height: 32px" />
                <ElSkeletonItem variant="text" style="width: 100%; height: 14px" />
              </div>
            </template>
          </ElSkeleton>
        </article>
        </section>

        <section v-else class="home-stats">
        <article v-for="item in homeStats" :key="item.label" class="metric-card home-stat-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
          <small>{{ item.hint }}</small>
        </article>
        </section>

        <section class="section-block home-goods">
        <div class="section-head">
          <div>
            <span class="eyebrow">精选商品</span>
            <h2>推荐好物</h2>
          </div>
          <RouterLink class="ghost-btn" to="/goods">查看全部</RouterLink>
        </div>

        <div v-if="goodsLoading" class="goods-grid home-goods__grid">
          <article v-for="index in 4" :key="`goods-skeleton-${index}`" class="panel home-goods__skeleton-card">
            <ElSkeleton animated>
              <template #template>
                <div class="home-goods__skeleton-media">
                  <ElSkeletonItem variant="image" class="home-goods__skeleton-image" />
                </div>
                <div class="home-goods__skeleton-copy">
                  <ElSkeletonItem variant="text" style="width: 42%; height: 14px" />
                  <ElSkeletonItem variant="text" style="width: 72%; height: 22px" />
                  <ElSkeletonItem variant="text" style="width: 100%; height: 14px" />
                  <ElSkeletonItem variant="text" style="width: 36%; height: 24px" />
                </div>
              </template>
            </ElSkeleton>
          </article>
        </div>

        <div v-else class="goods-grid home-goods__grid">
          <GoodsCard v-for="item in featuredGoods" :key="item.id" :item="item" />
        </div>
        </section>
      </div>
    </section>
  </section>
</template>

<style scoped>
.home-page {
  display: grid;
  gap: 12px;
  touch-action: pan-y;
}

.home-refresh {
  min-height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.86);
  color: var(--muted);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  justify-self: center;
  font-size: 13px;
  transition: all 180ms ease;
}

.home-refresh--active {
  border-color: rgba(27, 107, 58, 0.18);
  background: var(--brand-soft);
  color: var(--brand);
}

.home-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.home-aside,
.home-main {
  display: grid;
  gap: 20px;
}

.home-category-panel,
.home-carbon-panel {
  padding: 22px;
}

.home-skeleton-list {
  display: grid;
}

.home-category-item {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: center;
  padding: 16px 0;
  border-top: 1px solid rgba(148, 163, 184, 0.12);
}

.home-category-item--skeleton {
  padding-right: 0;
}

.home-skeleton-copy {
  display: grid;
  gap: 10px;
}

.home-category-item:first-of-type {
  border-top: none;
  padding-top: 0;
}

.home-category-item:last-of-type {
  padding-bottom: 0;
}

.home-category-item strong,
.home-category-item p,
.home-carbon-panel p,
.home-carbon-panel span {
  margin: 0;
}

.home-category-item strong {
  display: block;
  margin-bottom: 6px;
}

.home-category-item p {
  color: var(--muted);
  line-height: 1.6;
}

.home-category-item > span {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.home-carbon-panel {
  background:
    radial-gradient(circle at top right, rgba(201, 151, 42, 0.22), transparent 32%),
    linear-gradient(135deg, rgba(20, 81, 43, 0.96), rgba(27, 107, 58, 0.94));
  color: #fff;
}

.home-carbon-panel .eyebrow,
.home-carbon-panel p,
.home-carbon-panel__meta span {
  color: rgba(255, 255, 255, 0.88);
}

.home-carbon-panel strong {
  display: block;
  margin: 8px 0 12px;
  font-size: 46px;
  line-height: 1;
}

.home-carbon-panel p {
  line-height: 1.8;
}

.home-carbon-panel__skeleton {
  display: grid;
  gap: 14px;
}

.home-carbon-panel__meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.home-carbon-panel__meta span {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.16);
  font-size: 13px;
}

.home-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.home-stat-card {
  min-height: 148px;
}

.home-stat-card__skeleton {
  display: grid;
  gap: 14px;
}

.home-carousel__viewport {
  touch-action: pan-y;
}

.home-carousel__slide {
  position: relative;
}

.home-carousel__skeleton {
  width: 100%;
  display: grid;
  gap: 16px;
}

.home-carousel__skeleton-image {
  width: 100%;
  min-height: 340px;
  border-radius: 32px;
}

.home-carousel__skeleton-content {
  display: grid;
  gap: 10px;
  width: min(440px, 100%);
}

.home-carousel__content {
  position: absolute;
  inset: auto 24px 24px 24px;
  display: grid;
  gap: 8px;
  padding: 20px 22px;
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(15, 23, 42, 0.12), rgba(15, 23, 42, 0.62));
  color: #fff;
}

.home-carousel__content strong,
.home-carousel__content p {
  margin: 0;
}

.home-carousel__content strong {
  font-size: 24px;
}

.home-carousel__content p {
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.7;
}

.home-carousel__empty {
  min-height: 340px;
  display: grid;
  place-content: center;
  gap: 10px;
  text-align: center;
}

.home-goods__skeleton-card {
  padding: 16px;
}

.home-goods__skeleton-media {
  margin-bottom: 14px;
}

.home-goods__skeleton-image {
  width: 100%;
  min-height: 200px;
  border-radius: 24px;
}

.home-goods__skeleton-copy {
  display: grid;
  gap: 12px;
}

@media (max-width: 1199px) {
  .home-layout {
    grid-template-columns: 1fr;
  }

  .home-aside {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .home-aside,
  .home-stats {
    grid-template-columns: 1fr;
  }

  .home-carousel__content {
    inset: auto 16px 16px 16px;
    padding: 16px 18px;
  }

  .home-carousel__content strong {
    font-size: 20px;
  }
}
</style>
