<script setup lang="ts">
import { computed, onMounted, ref, watchEffect } from 'vue'
import { useBreakpoints, useSwipe } from '@vueuse/core'
import { RouterLink } from 'vue-router'
import { listGoods } from '@/api/goods'
import { confirmOrder, listOrders, payOrder, refundOrder, shipOrder } from '@/api/order'
import {
  isBuyerOrder,
  isSellerOrder,
  resolveOrderLogisticsRealtime,
  resolveOrderStageTextRealtime,
  resolveOrderTrackingNoRealtime,
  type OrderFilterKey,
  type OrderPanelKey
} from '@/features/orderCenter'
import { useAppStore } from '@/stores/app'
import { applyImageFallback, resolveAssetUrl } from '@/utils/assets'

const store = useAppStore()
const breakpoints = useBreakpoints({ mobile: 0, tablet: 768, desktop: 1200 })
const orderCenterRoot = ref<HTMLElement | null>(null)
const refreshing = ref(false)

const activePanel = ref<OrderPanelKey>('buy')
const activeFilter = ref<OrderFilterKey>('全部')

const filterTabs: OrderFilterKey[] = ['全部', '待付款', '待发货', '运输中', '已完成', '已退款']

const isMobile = breakpoints.smaller('desktop')

const refreshOrders = async () => {
  if (refreshing.value) {
    return
  }

  refreshing.value = true
  try {
    await Promise.all([listOrders(), store.goods.length ? Promise.resolve() : listGoods()])
  } finally {
    refreshing.value = false
  }
}

useSwipe(orderCenterRoot, {
  passive: true,
  threshold: 70,
  onSwipeEnd: (_, direction) => {
    if (direction === 'down' && isMobile.value && window.scrollY <= 12 && !refreshing.value) {
      void refreshOrders()
    }
  }
})

onMounted(async () => {
  await refreshOrders()
})

const currentUserName = computed(() => store.currentUser?.name ?? '')

const orderCards = computed(() =>
  store.orders.map((order) => {
    const goods = store.goods.find((item) => item.id === order.goodsId)

    return {
      ...order,
      goods,
      image: resolveAssetUrl(goods?.heroImage, 'goods'),
      stageText: resolveOrderStageTextRealtime(order),
      trackingNo: resolveOrderTrackingNoRealtime(order),
      logistics: resolveOrderLogisticsRealtime(order),
      isBuy: isBuyerOrder(order, currentUserName.value),
      isSell: isSellerOrder(order, currentUserName.value, goods?.sellerName)
    }
  })
)

const buyOrders = computed(() => orderCards.value.filter((item) => item.isBuy))
const sellOrders = computed(() => orderCards.value.filter((item) => item.isSell))

watchEffect(() => {
  if (activePanel.value === 'buy' && !buyOrders.value.length && sellOrders.value.length) {
    activePanel.value = 'sell'
  }

  if (activePanel.value === 'sell' && !sellOrders.value.length && buyOrders.value.length) {
    activePanel.value = 'buy'
  }
})

const panelOrders = computed(() => (activePanel.value === 'buy' ? buyOrders.value : sellOrders.value))

const visibleOrders = computed(() =>
  panelOrders.value.filter((item) => activeFilter.value === '全部' || item.status === activeFilter.value)
)

const orderStats = computed(() => {
  const source = panelOrders.value

  return [
    { label: activePanel.value === 'buy' ? '我购买的' : '我出售的', value: String(source.length) },
    { label: '待处理', value: String(source.filter((item) => !['已完成', '已退款'].includes(item.status)).length) },
    { label: '运输中', value: String(source.filter((item) => item.status === '运输中').length) },
    {
      label: '累计金额',
      value: `¥${source.reduce((sum, item) => sum + item.amount, 0).toLocaleString()}`
    }
  ]
})

const emptyTitle = computed(() => (activePanel.value === 'buy' ? '还没有购买订单' : '还没有出售订单'))
const emptyDescription = computed(() =>
  activePanel.value === 'buy'
    ? '去首页逛逛精选商品，完成一笔绿色交易后会同步展示在这里。'
    : '发布你的闲置商品后，成交订单会集中显示在这里，方便跟进发货。'
)

const onImageError = (event: Event) => applyImageFallback(event, 'goods')

const onPay = async (orderId: string) => {
  try {
    await payOrder(orderId)
    alert('支付成功，订单已进入待发货状态。')
  } catch (error) {
    alert((error as Error).message)
  }
}

const onShip = async (orderId: string) => {
  try {
    await shipOrder(orderId)
    alert('发货成功，物流轨迹已同步更新。')
  } catch (error) {
    alert((error as Error).message)
  }
}

const onConfirm = async (orderId: string) => {
  try {
    await confirmOrder(orderId)
    alert('确认收货成功，绿色积分已入账。')
  } catch (error) {
    alert((error as Error).message)
  }
}
const onRefund = async (orderId: string) => {
  try {
    await refundOrder(orderId)
    alert('退款已完成，商品已恢复在售状态。')
  } catch (error) {
    alert((error as Error).message)
  }
}
</script>

<template>
  <section ref="orderCenterRoot" class="section-block order-center-page">
    <div class="section-head">
      <div>
        <span class="eyebrow">订单中心</span>
        <h1>状态分类、时间轴与物流追踪都集中在这里</h1>
        <p>严格按 PRD 补齐购买 / 出售视角，支持支付、发货、确认收货三段式流转。</p>
      </div>
      <RouterLink class="ghost-btn" to="/goods">继续逛商品</RouterLink>
    </div>

    <div v-if="isMobile" class="order-center-refresh" :class="{ 'order-center-refresh--active': refreshing }">
      <span>{{ refreshing ? '刷新中...' : '下拉可刷新订单状态' }}</span>
    </div>

    <section class="panel order-center-hero">
      <div class="order-center-toggle">
        <button
          class="order-center-toggle__item"
          :class="{ 'order-center-toggle__item--active': activePanel === 'buy' }"
          type="button"
          @click="activePanel = 'buy'"
        >
          我购买的
        </button>
        <button
          class="order-center-toggle__item"
          :class="{ 'order-center-toggle__item--active': activePanel === 'sell' }"
          type="button"
          @click="activePanel = 'sell'"
        >
          我出售的
        </button>
      </div>

      <div class="order-center-stats">
        <article v-for="item in orderStats" :key="item.label" class="order-center-stat">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>

      <div class="chip-row order-center-filters">
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
    </section>

    <div v-if="visibleOrders.length" class="list-column order-card-list">
      <article v-for="order in visibleOrders" :key="order.id" class="panel order-center-card">
        <div class="order-center-card__media">
          <img :src="order.image" :alt="order.goods?.title || order.id" loading="lazy" @error="onImageError" />
        </div>

        <div class="order-center-card__content">
          <div class="order-center-card__top">
            <div>
              <span class="eyebrow">{{ order.id }}</span>
              <h2>{{ order.goods?.title || '订单商品' }}</h2>
            </div>
            <span class="badge">{{ order.status }}</span>
          </div>

          <div class="order-center-card__meta">
            <span>{{ activePanel === 'buy' ? '卖家' : '买家' }}：{{ activePanel === 'buy' ? order.goods?.sellerName : order.buyerName }}</span>
            <span>金额：¥{{ order.amount.toLocaleString() }}</span>
            <span>下单时间：{{ order.createdAt }}</span>
            <span>当前节点：{{ order.stageText }}</span>
          </div>

          <div class="order-center-card__tracking">
            <div>
              <strong>{{ order.status === '待付款' ? '等待支付' : order.status === '已退款' ? '退款状态' : '物流单号' }}</strong>
              <span>{{ order.status === '待付款' ? '支付后自动生成' : order.status === '已退款' ? '原路退回' : order.trackingNo }}</span>
            </div>
            <div>
              <strong>物流服务</strong>
              <span>{{ order.shippingCompany || (order.status === '待付款' ? '平台担保交易' : order.status === '已退款' ? '平台退款通道' : '顺丰速运') }}</span>
            </div>
            <div>
              <strong>最新轨迹</strong>
              <span>{{ order.logistics.at(-1)?.detail }}</span>
            </div>
          </div>

          <ul class="order-center-card__timeline">
            <li v-for="node in order.timeline" :key="`${order.id}-${node.label}-${node.time}`">
              <strong>{{ node.label }}</strong>
              <span>{{ node.time }}</span>
            </li>
          </ul>

          <div class="action-row order-center-card__actions">
            <RouterLink class="ghost-btn" :to="`/order/${order.id}`">查看详情</RouterLink>
            <button
              v-if="['待付款', '待发货', '运输中'].includes(order.status) && order.isBuy"
              class="ghost-btn"
              type="button"
              @click="onRefund(order.id)"
            >
              申请退款
            </button>
            <button v-if="order.status === '待付款' && order.isBuy" class="primary-btn" type="button" @click="onPay(order.id)">
              立即支付
            </button>
            <button v-if="order.status === '待发货' && order.isSell" class="primary-btn" type="button" @click="onShip(order.id)">
              立即发货
            </button>
            <button v-if="order.status === '运输中' && order.isBuy" class="primary-btn" type="button" @click="onConfirm(order.id)">
              确认收货
            </button>
          </div>
        </div>
      </article>
    </div>

    <section v-else class="empty-card">
      <span class="eyebrow">订单中心</span>
      <h2>{{ emptyTitle }}</h2>
      <p>{{ emptyDescription }}</p>
      <RouterLink class="primary-btn" :to="activePanel === 'buy' ? '/goods' : '/goods/publish'">
        {{ activePanel === 'buy' ? '去逛商品' : '去发布闲置' }}
      </RouterLink>
    </section>
  </section>
</template>

<style scoped>
.order-center-page {
  gap: 20px;
  touch-action: pan-y;
}

.order-center-refresh {
  min-height: 40px;
  padding: 0 14px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: var(--brand-soft);
  color: var(--brand);
  font-size: 13px;
  font-weight: 700;
}

.order-center-refresh--active {
  background: rgba(27, 107, 58, 0.14);
}

.order-center-hero {
  display: grid;
  gap: 20px;
}

.order-center-toggle {
  display: inline-flex;
  width: fit-content;
  padding: 6px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.05);
}

.order-center-toggle__item {
  border: none;
  background: transparent;
  color: #475569;
  font-size: 14px;
  font-weight: 700;
  padding: 12px 20px;
  border-radius: 999px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.order-center-toggle__item--active {
  background: linear-gradient(135deg, #1b6b3a, #2e9e5b);
  color: #fff;
  box-shadow: 0 12px 24px rgba(27, 107, 58, 0.2);
}

.order-center-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.order-center-stat {
  border-radius: 20px;
  padding: 18px 20px;
  background: linear-gradient(180deg, rgba(232, 245, 236, 0.96), #fff);
  border: 1px solid rgba(46, 158, 91, 0.14);
  display: grid;
  gap: 8px;
}

.order-center-stat span {
  font-size: 13px;
  color: #64748b;
}

.order-center-stat strong {
  font-size: 24px;
  color: #0f172a;
}

.order-center-filters {
  margin-top: -4px;
}

.order-card-list {
  gap: 18px;
}

.order-center-card {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 20px;
  align-items: stretch;
}

.order-center-card__media {
  border-radius: 22px;
  overflow: hidden;
  min-height: 210px;
  background: #f8fafc;
}

.order-center-card__media img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.order-center-card__content {
  display: grid;
  gap: 18px;
  min-width: 0;
}

.order-center-card__top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.order-center-card__top h2 {
  margin: 6px 0 0;
  font-size: 22px;
  line-height: 1.35;
}

.order-center-card__meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  color: #475569;
  font-size: 14px;
}

.order-center-card__tracking {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  padding: 16px;
  border-radius: 20px;
  background: rgba(248, 250, 252, 0.95);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.order-center-card__tracking div {
  display: grid;
  gap: 6px;
}

.order-center-card__tracking strong {
  font-size: 13px;
  color: #0f172a;
}

.order-center-card__tracking span {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

.order-center-card__timeline {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 12px;
}

.order-center-card__timeline li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 12px;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.24);
}

.order-center-card__timeline li:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.order-center-card__timeline strong {
  color: #0f172a;
  font-size: 14px;
}

.order-center-card__timeline span {
  color: #64748b;
  font-size: 13px;
  white-space: nowrap;
}

.order-center-card__actions {
  justify-content: flex-start;
}

@media (max-width: 1199px) {
  .order-center-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .order-center-card {
    grid-template-columns: 180px minmax(0, 1fr);
  }

  .order-center-card__tracking {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .order-center-toggle {
    width: 100%;
  }

  .order-center-toggle__item {
    flex: 1;
  }

  .order-center-stats,
  .order-center-card__meta {
    grid-template-columns: 1fr;
  }

  .order-center-card {
    grid-template-columns: 1fr;
  }

  .order-center-card__media {
    min-height: 188px;
  }

  .order-center-card__top,
  .order-center-card__timeline li {
    flex-direction: column;
  }

  .order-center-card__timeline span {
    white-space: normal;
  }
}
</style>
