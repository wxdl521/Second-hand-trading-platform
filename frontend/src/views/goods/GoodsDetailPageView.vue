<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getLatestGoodsEstimate } from '@/api/ai'
import { createOrder } from '@/api/order'
import { deleteOwnedGoods, getGoodsDetail, listGoods, publishOwnedGoods, toggleFavoriteGoods } from '@/api/goods'
import GoodsCarousel from '@/components/goods/GoodsCarousel.vue'
import GoodsCard from '@/components/goods/GoodsCard.vue'
import { ElSkeleton, ElSkeletonItem } from 'element-plus'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const router = useRouter()
const store = useAppStore()
const latestEstimate = ref<Awaited<ReturnType<typeof getLatestGoodsEstimate>>>(null)
const detailLoading = ref(true)
const detailError = ref('')

const goods = computed(() => store.goods.find((item) => item.id === route.params.id))
const displayAiPrice = computed(() => latestEstimate.value?.price ?? goods.value?.aiPrice ?? 0)
const displayEstimateRange = computed(() =>
  latestEstimate.value ? `¥${latestEstimate.value.low.toLocaleString()} - ¥${latestEstimate.value.high.toLocaleString()}` : '待生成最新估价'
)
const recommendGoods = computed(() =>
  store.goods.filter((item) => item.id !== goods.value?.id).slice(0, 4)
)
const appraiseLink = computed(() =>
  goods.value
    ? {
        path: '/appraise',
        query: {
          title: goods.value.title,
          mode: '视频连线鉴定'
        }
      }
    : '/appraise'
)
const isOwner = computed(() => Boolean(goods.value && store.currentUser?.name === goods.value.sellerName))
const canBuy = computed(() => Boolean(goods.value && goods.value.status === '在售中' && !isOwner.value))
const canSubmitForReview = computed(() => Boolean(goods.value && isOwner.value && ['草稿', '已下架'].includes(goods.value.status)))
const canOffShelf = computed(() => Boolean(goods.value && isOwner.value && !['已下架', '已售出'].includes(goods.value.status)))
const statusHint = computed(() => {
  if (!goods.value) return ''
  if (isOwner.value) {
    if (goods.value.reviewNote) {
      return goods.value.reviewNote
    }
    if (goods.value.status === '草稿') {
      return '当前还是草稿状态，补充资料后可以直接提交审核。'
    }
    if (goods.value.status === '待审核') {
      return '商品已进入审核队列，等待平台复核后会上架。'
    }
    if (goods.value.status === '已下架') {
      return '商品当前已下架，可以继续编辑后重新提交审核。'
    }
    return '你正在以卖家身份查看该商品。'
  }

  if (goods.value.status !== '在售中') {
    return '该商品当前不在公开售卖状态，暂时不能下单。'
  }

  return '商品支持下单、收藏和鉴定预约。'
})

const loadGoods = async () => {
  detailLoading.value = true
  detailError.value = ''
  latestEstimate.value = null

  try {
    await getGoodsDetail(String(route.params.id))
    if (!store.goods.length) {
      await listGoods()
    }
  } catch (error) {
    detailError.value = (error as Error).message || '商品详情加载失败，请稍后重试。'
    detailLoading.value = false
    return
  }

  try {
    latestEstimate.value = await getLatestGoodsEstimate(String(route.params.id))
  } catch {
    latestEstimate.value = null
  } finally {
    detailLoading.value = false
  }
}

watch(
  () => route.params.id,
  () => {
    void loadGoods()
  },
  { immediate: true }
)

const onRetry = () => {
  void loadGoods()
}

const onBuy = async () => {
  if (!goods.value) return
  try {
    const order = await createOrder(goods.value.id)
    alert(`已创建订单：${order.id}`)
    router.push('/order/list')
  } catch (error) {
    alert((error as Error).message)
    router.push(`/login?redirect=/goods/${route.params.id}`)
  }
}

const onFavorite = async () => {
  if (!goods.value) return
  try {
    await toggleFavoriteGoods(goods.value.id)
  } catch (error) {
    alert((error as Error).message)
    router.push(`/login?redirect=/goods/${route.params.id}`)
  }
}

const onOwnerPublish = async () => {
  if (!goods.value) return

  try {
    const updated = await publishOwnedGoods(goods.value.id)
    alert(updated.status === '待审核' ? '商品已提交审核。' : '商品状态已更新。')
  } catch (error) {
    alert((error as Error).message)
  }
}

const onOwnerOffShelf = async () => {
  if (!goods.value) return

  try {
    await deleteOwnedGoods(goods.value.id)
    alert('商品已下架。')
  } catch (error) {
    alert((error as Error).message)
  }
}
</script>

<template>
  <section v-if="detailLoading" class="detail-page detail-page--loading">
    <div class="detail-page__main">
      <section class="panel detail-skeleton-card detail-skeleton-card--media">
        <ElSkeleton animated>
          <template #template>
            <ElSkeletonItem variant="image" class="detail-skeleton-media" />
          </template>
        </ElSkeleton>
      </section>

      <section class="panel detail-page__panel detail-skeleton-card">
        <ElSkeleton animated>
          <template #template>
            <div class="detail-skeleton-stack">
              <div class="detail-skeleton-badges">
                <ElSkeletonItem variant="button" style="width: 120px; height: 34px; border-radius: 999px" />
                <ElSkeletonItem variant="button" style="width: 96px; height: 34px; border-radius: 999px" />
                <ElSkeletonItem variant="button" style="width: 88px; height: 34px; border-radius: 999px" />
              </div>
              <ElSkeletonItem variant="text" style="width: 48%; height: 34px" />
              <ElSkeletonItem variant="text" style="width: 100%; height: 18px" />
              <ElSkeletonItem variant="text" style="width: 74%; height: 18px" />
              <div class="detail-skeleton-grid">
                <ElSkeletonItem variant="text" style="width: 100%; height: 92px" />
                <ElSkeletonItem variant="text" style="width: 100%; height: 92px" />
                <ElSkeletonItem variant="text" style="width: 100%; height: 92px" />
                <ElSkeletonItem variant="text" style="width: 100%; height: 92px" />
              </div>
              <div class="detail-skeleton-section">
                <ElSkeletonItem variant="text" style="width: 36%; height: 18px" />
                <ElSkeletonItem variant="text" style="width: 100%; height: 120px" />
              </div>
              <div class="detail-skeleton-grid detail-skeleton-grid--triple">
                <ElSkeletonItem variant="text" style="width: 100%; height: 112px" />
                <ElSkeletonItem variant="text" style="width: 100%; height: 112px" />
                <ElSkeletonItem variant="text" style="width: 100%; height: 112px" />
              </div>
            </div>
          </template>
        </ElSkeleton>
      </section>
    </div>

    <div class="detail-page__extra">
      <section class="panel detail-skeleton-card">
        <ElSkeleton animated>
          <template #template>
            <div class="detail-skeleton-section">
              <ElSkeletonItem variant="text" style="width: 28%; height: 18px" />
              <ElSkeletonItem variant="text" style="width: 100%; height: 144px" />
            </div>
          </template>
        </ElSkeleton>
      </section>

      <section class="panel panel--soft detail-skeleton-card">
        <ElSkeleton animated>
          <template #template>
            <div class="detail-skeleton-section">
              <ElSkeletonItem variant="text" style="width: 32%; height: 18px" />
              <ElSkeletonItem variant="text" style="width: 100%; height: 96px" />
            </div>
          </template>
        </ElSkeleton>
      </section>
    </div>

    <section class="section-block">
      <div class="goods-grid goods-grid--compact">
        <article v-for="index in 4" :key="`detail-skeleton-recommend-${index}`" class="panel detail-skeleton-recommend">
          <ElSkeleton animated>
            <template #template>
              <div class="detail-skeleton-section">
                <ElSkeletonItem variant="image" class="detail-skeleton-recommend__image" />
                <ElSkeletonItem variant="text" style="width: 42%; height: 14px" />
                <ElSkeletonItem variant="text" style="width: 76%; height: 22px" />
                <ElSkeletonItem variant="text" style="width: 100%; height: 14px" />
              </div>
            </template>
          </ElSkeleton>
        </article>
      </div>
    </section>
  </section>

  <section v-else-if="goods" class="detail-page">
    <div class="detail-page__main">
      <GoodsCarousel :images="goods.gallery.length ? goods.gallery : [goods.heroImage]" :title="goods.title" />

      <section class="panel detail-page__panel">
        <div class="detail-page__badges">
          <span class="badge">AI 估价 ¥{{ displayAiPrice.toLocaleString() }}</span>
          <span v-if="goods.mockCertified" class="detail-cert-badge">平台认证</span>
          <span class="goods-card__chip goods-card__chip--soft">{{ goods.category }}</span>
          <span class="goods-card__chip goods-card__chip--soft">{{ goods.brand }}</span>
        </div>

        <h1>{{ goods.title }}</h1>
        <p class="lead">{{ goods.story }}</p>

        <div class="detail-price">
          <div>
            <strong>¥{{ goods.price.toLocaleString() }}</strong>
            <small>原价 ¥{{ goods.originalPrice.toLocaleString() }}</small>
          </div>
          <span class="badge">{{ goods.carbonSavedKg }}kg 减碳</span>
        </div>

        <div class="detail-info-grid">
          <article>
            <span>成色</span>
            <strong>{{ goods.condition }}</strong>
          </article>
          <article>
            <span>卖家</span>
            <strong>{{ goods.sellerName }} / {{ goods.sellerLevel }}</strong>
          </article>
          <article>
            <span>城市</span>
            <strong>{{ goods.city }}</strong>
          </article>
          <article>
            <span>状态</span>
            <strong>{{ goods.status }}</strong>
          </article>
        </div>

        <div class="panel panel--soft detail-review-card">
          <div class="detail-review-card__head">
            <div>
              <span class="eyebrow">{{ isOwner ? '卖家视角' : '交易提示' }}</span>
              <h3>{{ goods.auditStatus ?? goods.status }}</h3>
            </div>
            <span class="badge">{{ goods.createdAt }}</span>
          </div>
          <p>{{ statusHint }}</p>
        </div>

        <div v-if="goods.mockCertified" class="panel panel--soft detail-cert-panel">
          <strong>平台认证商品</strong>
          <p>当前商品已带平台认证标识，支持在详情页直接展示，让买家更容易识别已通过鉴定/审核的高信任商品。</p>
        </div>

        <div class="panel panel--soft detail-estimate-card">
          <div class="detail-estimate-card__head">
            <div>
              <span class="eyebrow">最新估价</span>
              <h3>AI 最新建议区间</h3>
            </div>
            <span class="badge">{{ latestEstimate?.confidence ?? 0 }}% 置信度</span>
          </div>
          <div class="detail-estimate-card__grid">
            <article>
              <span>预估中位价</span>
              <strong>¥{{ displayAiPrice.toLocaleString() }}</strong>
            </article>
            <article>
              <span>建议区间</span>
              <strong>{{ displayEstimateRange }}</strong>
            </article>
            <article>
              <span>减碳贡献</span>
              <strong>{{ latestEstimate?.carbonSavedKg ?? goods.carbonSavedKg }} kgCO₂e</strong>
            </article>
          </div>
          <p>
            {{
              latestEstimate?.summary ||
              '当前还没有最新估价记录，系统会先展示商品基础 AI 建议价。'
            }}
          </p>
        </div>

        <div class="goods-card__tags detail-tags">
          <span v-for="tag in goods.tags" :key="tag">{{ tag }}</span>
        </div>

        <div class="panel panel--soft detail-service">
          <h3>服务保障</h3>
          <ul class="list">
            <li>支持 AI 估价参考与人工鉴定预约。</li>
            <li>高价值商品支持模拟支付与订单履约追踪。</li>
            <li>平台展示循环减碳贡献，强化绿色交易价值。</li>
          </ul>
        </div>
      </section>
    </div>

    <div class="detail-page__extra">
      <section class="panel">
        <div class="section-head section-head--compact">
          <div>
            <span class="eyebrow">商品详情</span>
            <h2>成色与描述</h2>
          </div>
        </div>
        <p class="detail-page__desc">{{ goods.description }}</p>
      </section>

      <section class="panel panel--soft">
        <div class="section-head section-head--compact">
          <div>
            <span class="eyebrow">卖家信息</span>
            <h2>交易建议</h2>
          </div>
        </div>
        <div class="detail-seller">
          <div>
            <strong>{{ goods.sellerName }}</strong>
            <span>{{ goods.city }} · {{ goods.sellerLevel }} 实名等级</span>
          </div>
          <div class="action-row detail-seller__actions">
            <RouterLink v-if="isOwner" class="ghost-btn" :to="`/goods/publish?goodsId=${goods.id}`">编辑商品</RouterLink>
            <button v-if="canSubmitForReview" class="primary-btn" type="button" @click="onOwnerPublish">提交审核</button>
            <button v-if="canOffShelf" class="ghost-btn" type="button" @click="onOwnerOffShelf">下架商品</button>
            <RouterLink v-if="isOwner" class="ghost-btn" to="/goods/manage">我的商品</RouterLink>
            <RouterLink v-else class="ghost-btn" :to="appraiseLink">预约鉴定</RouterLink>
          </div>
        </div>
      </section>
    </div>

    <section v-if="recommendGoods.length" class="section-block">
      <div class="section-head">
        <div>
          <span class="eyebrow">相似推荐</span>
          <h2>你可能还喜欢</h2>
        </div>
      </div>

      <div class="goods-grid goods-grid--compact">
        <GoodsCard v-for="item in recommendGoods" :key="item.id" :item="item" />
      </div>
    </section>

    <div class="detail-floating-bar">
      <div>
        <strong>¥{{ goods.price.toLocaleString() }}</strong>
        <span>AI 估价 ¥{{ displayAiPrice.toLocaleString() }}</span>
      </div>
      <div class="detail-floating-bar__actions">
        <template v-if="isOwner">
          <RouterLink class="ghost-btn" :to="`/goods/publish?goodsId=${goods.id}`">继续编辑</RouterLink>
          <button v-if="canSubmitForReview" class="primary-btn" type="button" @click="onOwnerPublish">提交审核</button>
          <button v-else-if="canOffShelf" class="ghost-btn" type="button" @click="onOwnerOffShelf">下架商品</button>
          <RouterLink class="ghost-btn" to="/goods/manage">查看全部商品</RouterLink>
        </template>
        <template v-else>
          <button class="ghost-btn" type="button" @click="onFavorite">收藏</button>
          <button class="primary-btn" type="button" :disabled="!canBuy" @click="onBuy">
            {{ canBuy ? '立即下单' : '暂不可购买' }}
          </button>
        </template>
      </div>
    </div>
  </section>

  <section v-else-if="detailError" class="empty-card">
    <h1>商品详情加载失败</h1>
    <p>{{ detailError }}</p>
    <div class="action-row">
      <button class="primary-btn" type="button" @click="onRetry">重新加载</button>
      <RouterLink class="ghost-btn" to="/goods">返回列表</RouterLink>
    </div>
  </section>

  <section v-else class="empty-card">
    <h1>商品不存在</h1>
    <RouterLink class="primary-btn" to="/goods">返回列表</RouterLink>
  </section>
</template>

<style scoped>
.detail-page {
  display: grid;
  gap: 20px;
  padding-bottom: 90px;
}

.detail-page--loading {
  pointer-events: none;
}

.detail-page__main,
.detail-page__extra {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 22px;
  align-items: start;
}

.detail-page__panel,
.detail-page__extra > .panel {
  padding: 26px;
}

.detail-skeleton-card {
  display: grid;
  gap: 18px;
}

.detail-skeleton-card--media {
  padding: 18px;
}

.detail-skeleton-media {
  width: 100%;
  min-height: 420px;
  border-radius: 32px;
}

.detail-skeleton-stack,
.detail-skeleton-section {
  display: grid;
  gap: 14px;
}

.detail-skeleton-badges {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.detail-skeleton-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.detail-skeleton-grid--triple {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.detail-skeleton-recommend {
  padding: 16px;
}

.detail-skeleton-recommend__image {
  width: 100%;
  min-height: 220px;
  border-radius: 24px;
}

.detail-page__badges {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.detail-cert-badge {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  border: 1px solid rgba(22, 101, 52, 0.2);
  background: rgba(220, 252, 231, 0.9);
  color: #166534;
  font-size: 13px;
  font-weight: 700;
}

.detail-page__panel h1,
.detail-page__desc,
.detail-service h3 {
  margin: 0;
}

.detail-price {
  margin: 20px 0;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.detail-price strong {
  display: block;
  font-size: 38px;
  line-height: 1;
}

.detail-price small {
  color: var(--muted);
}

.detail-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 16px;
}

.detail-info-grid article {
  padding: 16px 18px;
  border: 1px solid var(--line);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.82);
}

.detail-info-grid span,
.detail-seller span {
  display: block;
  color: var(--muted);
}

.detail-info-grid strong,
.detail-seller strong {
  display: block;
  margin-top: 8px;
}

.detail-tags {
  margin-bottom: 18px;
}

.detail-service {
  display: grid;
  gap: 12px;
}

.detail-review-card {
  display: grid;
  gap: 12px;
  margin-bottom: 18px;
}

.detail-cert-panel {
  display: grid;
  gap: 8px;
  margin-bottom: 18px;
}

.detail-cert-panel strong,
.detail-cert-panel p {
  margin: 0;
}

.detail-cert-panel p {
  color: var(--muted);
  line-height: 1.8;
}

.detail-estimate-card {
  display: grid;
  gap: 14px;
  margin-bottom: 18px;
}

.detail-review-card__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.detail-review-card__head h3,
.detail-estimate-card__head h3,
.detail-review-card p {
  margin: 0;
}

.detail-estimate-card__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.detail-estimate-card__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.detail-estimate-card__grid article {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid var(--line);
}

.detail-estimate-card__grid span,
.detail-estimate-card p {
  color: var(--muted);
}

.detail-estimate-card__grid strong {
  display: block;
  margin-top: 8px;
}

.detail-estimate-card p {
  margin: 0;
  line-height: 1.8;
}

.detail-review-card p {
  color: var(--muted);
  line-height: 1.8;
}

.detail-page__desc {
  color: var(--muted);
  line-height: 1.9;
}

.detail-seller {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.detail-seller__actions {
  flex-wrap: wrap;
  justify-content: flex-end;
}

.detail-floating-bar {
  position: fixed;
  left: 50%;
  bottom: 90px;
  transform: translateX(-50%);
  z-index: 30;
  width: min(1240px, calc(100% - 36px));
  padding: 16px 18px;
  border-radius: 24px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: var(--shadow);
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.detail-floating-bar strong,
.detail-floating-bar span {
  display: block;
}

.detail-floating-bar strong {
  font-size: 24px;
}

.detail-floating-bar span {
  color: var(--muted);
}

.detail-floating-bar__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

@media (max-width: 1199px) {
  .detail-page__main,
  .detail-page__extra {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .detail-info-grid {
    grid-template-columns: 1fr;
  }

  .detail-skeleton-grid,
  .detail-estimate-card__grid {
    grid-template-columns: 1fr;
  }

  .detail-price,
  .detail-review-card__head,
  .detail-estimate-card__head,
  .detail-seller,
  .detail-floating-bar {
    flex-direction: column;
    align-items: flex-start;
  }

  .detail-floating-bar {
    bottom: 84px;
  }

  .detail-floating-bar__actions {
    width: 100%;
  }

  .detail-floating-bar__actions .ghost-btn,
  .detail-floating-bar__actions .primary-btn {
    flex: 1;
    justify-content: center;
  }
}
</style>
