<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useBreakpoints, useIntersectionObserver } from '@vueuse/core'
import { useRoute, useRouter } from 'vue-router'
import { listGoods, listGoodsCategories, type GoodsCategoryStat } from '@/api/goods'
import GoodsCard from '@/components/goods/GoodsCard.vue'
import { ElDrawer, ElSkeleton, ElSkeletonItem } from 'element-plus'
import type { GoodsItem } from '@/types'

const route = useRoute()
const router = useRouter()

const ALL_CATEGORY = '全部'
const breakpoints = useBreakpoints({ mobile: 0, tablet: 768, desktop: 1200 })

const activeCategory = computed(() =>
  typeof route.query.category === 'string' ? route.query.category : ALL_CATEGORY
)
const sortMode = ref<'latest' | 'priceAsc' | 'priceDesc'>('latest')
const goods = ref<GoodsItem[]>([])
const categoryStats = ref<GoodsCategoryStat[]>([])
const loading = ref(false)
const priceCap = ref(0)
const filtersOpen = ref(false)
const visibleCount = ref(8)
const loadMoreTrigger = ref<HTMLElement | null>(null)

const keywordText = computed(() => (typeof route.query.q === 'string' ? route.query.q : '').trim())
const isMobile = breakpoints.smaller('desktop')
const categoryOptions = computed(() => [
  {
    name: ALL_CATEGORY,
    count: categoryStats.value.reduce((total, item) => total + item.count, 0)
  },
  ...categoryStats.value
])
const maxPrice = computed(() => Math.max(...goods.value.map((item) => item.price), 10000))
const initialVisibleCount = computed(() => (isMobile.value ? 6 : 8))

const loadCatalog = async () => {
  loading.value = true
  try {
    goods.value = await listGoods({
      keyword: keywordText.value || undefined,
      category: activeCategory.value === ALL_CATEGORY ? undefined : activeCategory.value
    })
  } finally {
    loading.value = false
  }
}

const loadCategoryStats = async () => {
  categoryStats.value = await listGoodsCategories()
}

onMounted(async () => {
  await Promise.all([loadCatalog(), loadCategoryStats()])
})

watch([() => route.query.q, () => route.query.category], async () => {
  await loadCatalog()
})

watch(
  maxPrice,
  (value) => {
    if (!priceCap.value || priceCap.value > value) {
      priceCap.value = value
    }
  },
  { immediate: true }
)

const updateCategory = (value: string) => {
  router.replace({
    query: {
      ...(keywordText.value ? { q: keywordText.value } : {}),
      ...(value !== ALL_CATEGORY ? { category: value } : {})
    }
  })
}

const filteredGoods = computed(() => {
  return goods.value.filter((item) => item.price <= priceCap.value).sort((left, right) => {
    if (sortMode.value === 'priceAsc') return left.price - right.price
    if (sortMode.value === 'priceDesc') return right.price - left.price
    return right.createdAt.localeCompare(left.createdAt)
  })
})

const visibleGoods = computed(() => filteredGoods.value.slice(0, visibleCount.value))
const canLoadMore = computed(() => visibleCount.value < filteredGoods.value.length)

const activeCategoryName = computed(() =>
  activeCategory.value === ALL_CATEGORY ? '全部商品' : activeCategory.value
)
const categorySummary = (category: string) =>
  categoryOptions.value.find((item) => item.name === category)?.count ?? 0

const loadMoreGoods = () => {
  if (!canLoadMore.value) {
    return
  }

  visibleCount.value = Math.min(
    filteredGoods.value.length,
    visibleCount.value + (isMobile.value ? 4 : 6)
  )
}

watch(
  [filteredGoods, isMobile],
  () => {
    visibleCount.value = Math.min(filteredGoods.value.length || initialVisibleCount.value, initialVisibleCount.value)
  },
  { immediate: true }
)

watch(isMobile, (value) => {
  if (!value) {
    filtersOpen.value = false
  }
})

useIntersectionObserver(
  loadMoreTrigger,
  ([entry]) => {
    if (entry?.isIntersecting && !loading.value) {
      loadMoreGoods()
    }
  },
  {
    rootMargin: '180px 0px'
  }
)
</script>

<template>
  <section class="catalog-page">
    <aside v-if="!isMobile" class="panel catalog-sidebar">
      <div class="section-head section-head--compact">
        <div>
          <span class="eyebrow">筛选条件</span>
          <h2>商品筛选</h2>
        </div>
      </div>

      <div class="catalog-sidebar__section">
        <span class="catalog-sidebar__title">分类</span>
        <button
          v-for="item in categoryOptions"
          :key="item.name"
          class="catalog-sidebar__option"
          :class="{ 'catalog-sidebar__option--active': activeCategory === item.name }"
          type="button"
          @click="updateCategory(item.name)"
        >
          <span>{{ item.name }}</span>
          <small>{{ categorySummary(item.name) }}</small>
        </button>
      </div>

      <div class="catalog-sidebar__section">
        <span class="catalog-sidebar__title">价格上限</span>
        <div class="catalog-range">
          <input v-model="priceCap" type="range" min="1000" :max="maxPrice" step="500" />
          <strong>¥{{ priceCap.toLocaleString() }}</strong>
        </div>
      </div>

      <div class="catalog-sidebar__section">
        <span class="catalog-sidebar__title">排序</span>
        <select v-model="sortMode" class="catalog-sort">
          <option value="latest">最新上架</option>
          <option value="priceAsc">价格从低到高</option>
          <option value="priceDesc">价格从高到低</option>
        </select>
      </div>
    </aside>

    <ElDrawer v-model="filtersOpen" title="筛选与排序" size="88%" direction="btt">
      <div class="catalog-drawer">
        <div class="catalog-sidebar__section">
          <span class="catalog-sidebar__title">分类</span>
          <button
            v-for="item in categoryOptions"
            :key="`drawer-${item.name}`"
            class="catalog-sidebar__option"
            :class="{ 'catalog-sidebar__option--active': activeCategory === item.name }"
            type="button"
            @click="updateCategory(item.name); filtersOpen = false"
          >
            <span>{{ item.name }}</span>
            <small>{{ categorySummary(item.name) }}</small>
          </button>
        </div>

        <div class="catalog-sidebar__section">
          <span class="catalog-sidebar__title">价格上限</span>
          <div class="catalog-range">
            <input v-model="priceCap" type="range" min="1000" :max="maxPrice" step="500" />
            <strong>¥{{ priceCap.toLocaleString() }}</strong>
          </div>
        </div>

        <div class="catalog-sidebar__section">
          <span class="catalog-sidebar__title">排序</span>
          <select v-model="sortMode" class="catalog-sort">
            <option value="latest">最新上架</option>
            <option value="priceAsc">价格从低到高</option>
            <option value="priceDesc">价格从高到低</option>
          </select>
        </div>
      </div>
    </ElDrawer>

    <div class="catalog-main">
      <section class="panel catalog-summary">
        <div>
          <span class="eyebrow">商品列表</span>
          <h1>{{ activeCategoryName }}</h1>
          <p v-if="keywordText">搜索词：“{{ keywordText }}” · 共找到 {{ filteredGoods.length }} 件商品</p>
          <p v-else>支持分类筛选、价格范围筛选和排序浏览。</p>
        </div>
        <div class="catalog-summary__meta">
          <span class="catalog-summary__pill">结果 {{ filteredGoods.length }} 件</span>
          <span class="catalog-summary__pill">价格上限 ¥{{ priceCap.toLocaleString() }}</span>
        </div>
      </section>

      <div v-if="isMobile" class="catalog-mobile-actions">
        <button class="ghost-btn" type="button" @click="filtersOpen = true">筛选与排序</button>
        <span class="catalog-summary__pill">已显示 {{ visibleGoods.length }} / {{ filteredGoods.length }}</span>
      </div>

      <div v-if="loading" class="goods-grid catalog-grid">
        <article v-for="index in 4" :key="`catalog-skeleton-${index}`" class="panel catalog-skeleton-card">
          <ElSkeleton animated>
            <template #template>
              <ElSkeletonItem variant="image" class="catalog-skeleton-card__image" />
              <div class="catalog-skeleton-card__copy">
                <ElSkeletonItem variant="text" style="width: 42%; height: 14px" />
                <ElSkeletonItem variant="text" style="width: 74%; height: 22px" />
                <ElSkeletonItem variant="text" style="width: 100%; height: 14px" />
                <ElSkeletonItem variant="text" style="width: 36%; height: 24px" />
              </div>
            </template>
          </ElSkeleton>
        </article>
      </div>

      <div v-else-if="visibleGoods.length" class="goods-grid catalog-grid">
        <GoodsCard v-for="item in visibleGoods" :key="item.id" :item="item" />
      </div>

      <div v-if="!loading && visibleGoods.length" class="catalog-loadmore">
        <div v-if="canLoadMore" ref="loadMoreTrigger" class="catalog-loadmore__trigger"></div>
        <span>{{ canLoadMore ? '继续下滑加载更多商品' : '已经到底了' }}</span>
      </div>

      <section v-else-if="!loading" class="empty-card">
        <h2>没有找到符合条件的商品</h2>
        <p>可以放宽价格范围，或切换分类继续浏览。</p>
      </section>
    </div>
  </section>
</template>

<style scoped>
.catalog-page {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 22px;
  align-items: start;
}

.catalog-sidebar,
.catalog-summary {
  padding: 24px;
}

.catalog-sidebar {
  position: sticky;
  top: 96px;
  display: grid;
  gap: 20px;
}

.catalog-drawer {
  display: grid;
  gap: 20px;
}

.catalog-sidebar__section {
  display: grid;
  gap: 12px;
}

.catalog-sidebar__title {
  font-size: 14px;
  font-weight: 700;
  color: var(--brand);
}

.catalog-sidebar__option {
  width: 100%;
  min-height: 48px;
  padding: 0 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid var(--line);
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  color: var(--text);
}

.catalog-sidebar__option small {
  color: var(--muted);
}

.catalog-sidebar__option--active {
  background: var(--brand-soft);
  border-color: rgba(27, 107, 58, 0.18);
  color: var(--brand);
}

.catalog-range {
  display: grid;
  gap: 10px;
}

.catalog-range strong {
  font-size: 22px;
}

.catalog-sort {
  width: 100%;
}

.catalog-main {
  display: grid;
  gap: 20px;
}

.catalog-mobile-actions {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}

.catalog-summary {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: end;
}

.catalog-summary h1,
.catalog-summary p {
  margin: 0;
}

.catalog-summary p {
  margin-top: 8px;
  color: var(--muted);
}

.catalog-summary__meta {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.catalog-summary__pill {
  min-height: 38px;
  padding: 0 14px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
}

.catalog-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  touch-action: pan-y;
}

.catalog-skeleton-card {
  padding: 16px;
}

.catalog-skeleton-card__image {
  width: 100%;
  min-height: 220px;
  border-radius: 24px;
}

.catalog-skeleton-card__copy {
  display: grid;
  gap: 12px;
  margin-top: 14px;
}

.catalog-loadmore {
  display: grid;
  justify-items: center;
  gap: 8px;
  color: var(--muted);
}

.catalog-loadmore__trigger {
  width: 100%;
  height: 1px;
}

@media (max-width: 1199px) {
  .catalog-page {
    grid-template-columns: 1fr;
  }

  .catalog-sidebar {
    position: static;
  }
}

@media (max-width: 768px) {
  .catalog-summary {
    flex-direction: column;
    align-items: flex-start;
  }

  .catalog-grid {
    grid-template-columns: 1fr;
  }
}
</style>
