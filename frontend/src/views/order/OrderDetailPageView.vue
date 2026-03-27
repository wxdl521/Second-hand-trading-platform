<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getGoodsDetail, listGoods } from '@/api/goods'
import { confirmOrder, getOrderDetail, payOrder, refundOrder, shipOrder } from '@/api/order'
import {
  isBuyerOrder,
  isSellerOrder,
  resolveOrderAddressRealtime,
  resolveOrderLogisticsRealtime,
  resolveOrderServicePhoneRealtime,
  resolveOrderStageTextRealtime,
  resolveOrderTrackingNoRealtime
} from '@/features/orderCenter'
import { useAppStore } from '@/stores/app'
import { applyImageFallback, resolveAssetUrl } from '@/utils/assets'

const route = useRoute()
const router = useRouter()
const store = useAppStore()
const detailLoading = ref(true)
const detailError = ref('')

const loadOrderContext = async () => {
  detailLoading.value = true
  detailError.value = ''

  try {
    const currentOrder = await getOrderDetail(String(route.params.id))

    if (!currentOrder) {
      detailError.value = '订单不存在或当前账号无权访问。'
      return
    }

    if (!store.goods.find((item) => item.id === currentOrder.goodsId)) {
      try {
        await getGoodsDetail(currentOrder.goodsId)
      } catch {
        await listGoods()
      }
    }
  } catch (error) {
    detailError.value = (error as Error).message || '订单详情加载失败，请稍后重试。'
  } finally {
    detailLoading.value = false
  }
}

onMounted(async () => {
  await loadOrderContext()
})

const order = computed(() => {
  const rawId = String(route.params.id)
  return store.orders.find((item) => item.id === rawId || item.id === `order-${rawId}`)
})

const goods = computed(() => store.goods.find((item) => item.id === order.value?.goodsId))
const image = computed(() => resolveAssetUrl(goods.value?.heroImage, 'goods'))
const logistics = computed(() => (order.value ? resolveOrderLogisticsRealtime(order.value) : []))
const stageText = computed(() => (order.value ? resolveOrderStageTextRealtime(order.value) : '处理中'))
const trackingNo = computed(() => (order.value ? resolveOrderTrackingNoRealtime(order.value) : '-'))
const isBuyer = computed(() => Boolean(order.value && isBuyerOrder(order.value, store.currentUser?.name)))
const isSeller = computed(() =>
  Boolean(order.value && isSellerOrder(order.value, store.currentUser?.name, goods.value?.sellerName))
)

const orderAmount = computed(() => order.value?.amount ?? 0)
const carbonSaved = computed(() => goods.value?.carbonSavedKg ?? 0)

const orderSummary = computed(() => [
  { label: '订单编号', value: order.value?.id ?? '-' },
  { label: '当前状态', value: order.value?.status ?? '-' },
  { label: '成交金额', value: `¥${orderAmount.value.toLocaleString()}` },
  { label: '下单时间', value: order.value?.createdAt ?? '-' }
])

const tradeSummary = computed(() => [
  { label: '买家', value: order.value?.buyerName ?? '-' },
  { label: '卖家', value: goods.value?.sellerName ?? '-' },
  { label: '成色', value: goods.value?.condition ?? '-' },
  { label: 'AI 估价', value: goods.value ? `¥${goods.value.aiPrice.toLocaleString()}` : '-' }
])

const onImageError = (event: Event) => applyImageFallback(event, 'goods')

const onPay = async () => {
  if (!order.value) return

  try {
    await payOrder(order.value.id)
    alert('支付成功，订单已转入待发货。')
  } catch (error) {
    alert((error as Error).message)
  }
}

const onShip = async () => {
  if (!order.value) return

  try {
    await shipOrder(order.value.id)
    alert('发货成功，物流信息已更新。')
  } catch (error) {
    alert((error as Error).message)
  }
}

const onConfirm = async () => {
  if (!order.value) return

  try {
    await confirmOrder(order.value.id)
    alert('确认收货成功，积分和碳贡献已同步。')
  } catch (error) {
    alert((error as Error).message)
  }
}

const onRefund = async () => {
  if (!order.value) return

  try {
    await refundOrder(order.value.id)
    alert('退款已完成，商品已恢复在售状态。')
  } catch (error) {
    alert((error as Error).message)
  }
}
</script>

<template>
  <section v-if="detailLoading" class="empty-card">
    <span class="eyebrow">订单详情</span>
    <h1>订单加载中...</h1>
    <p>正在同步订单与商品信息，请稍候。</p>
  </section>

  <section v-else-if="order && goods" class="section-block order-detail-page">
    <div class="section-head">
      <div>
        <span class="eyebrow">订单详情</span>
        <h1>{{ goods.title }}</h1>
        <p>展示完整订单信息、支付动作、发货节点和物流追踪，和 PRD 详情页保持一致。</p>
      </div>
      <RouterLink class="ghost-btn" to="/order/list">返回订单中心</RouterLink>
    </div>

    <section class="panel order-detail-hero">
      <div class="order-detail-hero__media">
        <img :src="image" :alt="goods.title" loading="lazy" @error="onImageError" />
      </div>

      <div class="order-detail-hero__body">
        <div class="order-detail-hero__top">
          <div>
            <span class="eyebrow">{{ order.id }}</span>
            <h2>{{ goods.title }}</h2>
            <p>{{ goods.story }}</p>
          </div>
          <span class="badge">{{ order.status }}</span>
        </div>

        <div class="order-detail-price">
          <strong>¥{{ order.amount.toLocaleString() }}</strong>
          <span>AI 建议价 ¥{{ goods.aiPrice.toLocaleString() }}</span>
        </div>

        <div class="order-detail-tags">
          <span>{{ goods.category }}</span>
          <span>{{ goods.brand }}</span>
          <span>{{ goods.condition }}</span>
          <span>减碳 {{ carbonSaved }}kg</span>
        </div>

        <div class="action-row order-detail-actions">
          <button
            v-if="['待付款', '待发货', '运输中'].includes(order.status) && isBuyer"
            class="ghost-btn"
            type="button"
            @click="onRefund"
          >
            申请退款
          </button>
          <button v-if="order.status === '待付款' && isBuyer" class="primary-btn" type="button" @click="onPay">
            立即支付
          </button>
          <button v-if="order.status === '待发货' && isSeller" class="primary-btn" type="button" @click="onShip">
            卖家发货
          </button>
          <button v-if="order.status === '运输中' && isBuyer" class="primary-btn" type="button" @click="onConfirm">
            确认收货
          </button>
          <button class="ghost-btn" type="button" @click="router.push('/carbon')">查看碳账户</button>
        </div>
      </div>
    </section>

    <div class="order-detail-grid">
      <section class="panel">
        <div class="section-head section-head--compact">
          <div>
            <span class="eyebrow">订单概览</span>
            <h2>完整订单信息</h2>
          </div>
        </div>
        <div class="summary-grid">
          <div v-for="item in orderSummary" :key="item.label">
            <strong>{{ item.label }}</strong>
            <span>{{ item.value }}</span>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="section-head section-head--compact">
          <div>
            <span class="eyebrow">交易双方</span>
            <h2>买卖家信息</h2>
          </div>
        </div>
        <div class="summary-grid">
          <div v-for="item in tradeSummary" :key="item.label">
            <strong>{{ item.label }}</strong>
            <span>{{ item.value }}</span>
          </div>
        </div>
      </section>
    </div>

    <section class="panel order-logistics-card">
      <div class="section-head section-head--compact">
        <div>
          <span class="eyebrow">物流追踪</span>
          <h2>{{ stageText }}</h2>
        </div>
      </div>

      <div class="order-logistics-card__meta">
        <div>
          <strong>{{ order.status === '已退款' ? '退款状态' : '物流单号' }}</strong>
          <span>{{ order.status === '待付款' ? '支付后生成' : order.status === '已退款' ? '原路退回' : trackingNo }}</span>
        </div>
        <div>
          <strong>承运服务</strong>
          <span>{{ order.shippingCompany || (order.status === '待付款' ? '平台担保交易' : order.status === '已退款' ? '平台退款通道' : '顺丰速运') }}</span>
        </div>
        <div>
          <strong>收货地址</strong>
          <span>{{ resolveOrderAddressRealtime(order) }}</span>
        </div>
        <div>
          <strong>客服电话</strong>
          <span>{{ resolveOrderServicePhoneRealtime(order) }}</span>
        </div>
      </div>

      <ul class="order-logistics-card__timeline">
        <li v-for="node in logistics" :key="`${node.label}-${node.time}`">
          <div class="order-logistics-card__dot" />
          <div>
            <strong>{{ node.label }}</strong>
            <span>{{ node.time }}</span>
            <p>{{ node.detail }}</p>
          </div>
        </li>
      </ul>
    </section>

    <section class="panel">
      <div class="section-head section-head--compact">
        <div>
          <span class="eyebrow">时间轴</span>
          <h2>订单流转记录</h2>
        </div>
      </div>
      <ul class="order-flow-timeline">
        <li v-for="node in order.timeline" :key="`${order.id}-${node.label}-${node.time}`">
          <strong>{{ node.label }}</strong>
          <span>{{ node.time }}</span>
        </li>
      </ul>
    </section>
  </section>

  <section v-else class="empty-card">
    <span class="eyebrow">订单详情</span>
    <h1>订单不存在或已失效</h1>
    <p>可以返回订单中心重新查看，或继续浏览首页商品。</p>
    <div class="action-row">
      <RouterLink class="primary-btn" to="/order/list">返回订单中心</RouterLink>
      <RouterLink class="ghost-btn" to="/goods">去逛商品</RouterLink>
    </div>
  </section>
</template>

<style scoped>
.order-detail-page {
  gap: 20px;
}

.order-detail-hero {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 24px;
}

.order-detail-hero__media {
  min-height: 320px;
  border-radius: 24px;
  overflow: hidden;
  background: #f8fafc;
}

.order-detail-hero__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.order-detail-hero__body {
  display: grid;
  gap: 20px;
}

.order-detail-hero__top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.order-detail-hero__top h2 {
  margin: 6px 0 10px;
  font-size: 28px;
  line-height: 1.32;
}

.order-detail-hero__top p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.order-detail-price {
  display: grid;
  gap: 8px;
}

.order-detail-price strong {
  color: #0f172a;
  font-size: 34px;
}

.order-detail-price span {
  color: #2e9e5b;
  font-size: 15px;
  font-weight: 700;
}

.order-detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.order-detail-tags span {
  padding: 9px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
  color: #1e293b;
  background: rgba(232, 245, 236, 0.95);
}

.order-detail-actions {
  justify-content: flex-start;
}

.order-detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.order-logistics-card {
  display: grid;
  gap: 18px;
}

.order-logistics-card__meta {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(248, 250, 252, 0.96);
}

.order-logistics-card__meta div {
  display: grid;
  gap: 6px;
}

.order-logistics-card__meta strong {
  color: #0f172a;
  font-size: 13px;
}

.order-logistics-card__meta span {
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.order-logistics-card__timeline,
.order-flow-timeline {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 14px;
}

.order-logistics-card__timeline li {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 12px;
}

.order-logistics-card__dot {
  width: 12px;
  height: 12px;
  margin-top: 6px;
  border-radius: 50%;
  background: linear-gradient(135deg, #1b6b3a, #2e9e5b);
  box-shadow: 0 0 0 6px rgba(46, 158, 91, 0.1);
}

.order-logistics-card__timeline strong,
.order-flow-timeline strong {
  color: #0f172a;
  font-size: 14px;
}

.order-logistics-card__timeline span,
.order-flow-timeline span {
  color: #64748b;
  font-size: 13px;
}

.order-logistics-card__timeline p {
  margin: 6px 0 0;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.order-flow-timeline li {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 14px;
  border-bottom: 1px dashed rgba(148, 163, 184, 0.24);
}

.order-flow-timeline li:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

@media (max-width: 1199px) {
  .order-detail-hero,
  .order-detail-grid,
  .order-logistics-card__meta {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .order-detail-hero__media {
    min-height: 220px;
  }

  .order-detail-hero__top,
  .order-flow-timeline li {
    flex-direction: column;
  }
}
</style>
