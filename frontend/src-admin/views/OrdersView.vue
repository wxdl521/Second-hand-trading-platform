<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminDialog from '@admin/components/AdminDialog.vue'
import { useAdminDataStore } from '@admin/stores/data'
import type { AdminOrderDraft } from '@admin/types'
import type { OrderStatus } from '@shared/types'

const dataStore = useAdminDataStore()
const loading = ref(false)
const keyword = ref('')
const statusFilter = ref<'全部' | OrderStatus>('全部')
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)

const statusOptions: OrderStatus[] = ['待付款', '待发货', '运输中', '已完成']

const toInputDateTime = (value?: string) => {
  if (!value) return new Date().toISOString().slice(0, 16)
  if (value.includes('T')) return value.slice(0, 16)
  if (value.includes(' ')) return value.replace(' ', 'T').slice(0, 16)
  return value
}

const formatOrderTime = (value: string) => (value ? value.replace('T', ' ') : value)
const formatCurrency = (value: number) => `¥${value.toLocaleString('zh-CN')}`

const createEmptyOrderForm = (): AdminOrderDraft => ({
  goodsId: dataStore.goods[0]?.id ?? '',
  amount: dataStore.goods[0]?.price ?? 0,
  buyerName: '',
  status: '待付款',
  createdAt: toInputDateTime()
})

const form = reactive<AdminOrderDraft>(createEmptyOrderForm())

const resetForm = () => {
  Object.assign(form, createEmptyOrderForm())
  editingId.value = null
}

const loadData = async () => {
  loading.value = true
  try {
    await dataStore.fetchAll()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const goodsMap = computed(() => {
  const map = new Map<string, (typeof dataStore.goods)[number]>()
  dataStore.goods.forEach((item) => map.set(item.id, item))
  return map
})

const metrics = computed(() => ({
  total: dataStore.orders.length,
  pendingPayment: dataStore.orders.filter((item) => item.status === '待付款').length,
  pendingShipment: dataStore.orders.filter((item) => item.status === '待发货').length,
  shipping: dataStore.orders.filter((item) => item.status === '运输中').length,
  completed: dataStore.orders.filter((item) => item.status === '已完成').length,
  totalGmv: dataStore.orders.reduce((sum, item) => sum + item.amount, 0),
  completedGmv: dataStore.orders
    .filter((item) => item.status === '已完成')
    .reduce((sum, item) => sum + item.amount, 0)
}))

const filteredOrders = computed(() => {
  const query = keyword.value.trim().toLowerCase()

  return dataStore.orders.filter((item) => {
    const goods = goodsMap.value.get(item.goodsId)
    const matchesStatus = statusFilter.value === '全部' || item.status === statusFilter.value
    const matchesQuery =
      !query ||
      [item.id, item.buyerName, item.status, goods?.title ?? '', goods?.category ?? '', goods?.brand ?? ''].some(
        (field) => field.toLowerCase().includes(query)
      )

    return matchesStatus && matchesQuery
  })
})

const latestOrder = computed(() => filteredOrders.value[0] ?? dataStore.orders[0] ?? null)
const goodsInfo = (goodsId: string) => goodsMap.value.get(goodsId)
const goodsTitle = (goodsId: string) => goodsInfo(goodsId)?.title ?? `商品 ${goodsId}`
const goodsSubline = (goodsId: string) => {
  const goods = goodsInfo(goodsId)
  if (!goods) return `商品编号 ${goodsId}`
  return `${goods.category} · ${goods.brand}`
}

const statusClass = (status: string) => {
  if (status === '已完成') return 'admin-badge admin-badge--success'
  if (status === '待付款' || status === '待发货') return 'admin-badge admin-badge--warning'
  return 'admin-badge admin-badge--primary'
}

const syncAmountFromGoods = () => {
  const goods = dataStore.goods.find((item) => item.id === form.goodsId)
  if (goods) {
    form.amount = goods.price
  }
}

const openCreate = () => {
  resetForm()
  syncAmountFromGoods()
  dialogVisible.value = true
}

const openEdit = (id: string) => {
  const current = dataStore.orders.find((item) => item.id === id)
  if (!current) return

  editingId.value = id
  Object.assign(form, {
    goodsId: current.goodsId,
    amount: current.amount,
    buyerName: current.buyerName,
    status: current.status,
    createdAt: toInputDateTime(current.createdAt)
  })
  dialogVisible.value = true
}

const closeDialog = () => {
  dialogVisible.value = false
  resetForm()
}

const submitForm = async () => {
  if (!form.goodsId || !form.buyerName.trim()) {
    alert('请先选择商品并填写买家姓名。')
    return
  }

  const payload: AdminOrderDraft = {
    goodsId: form.goodsId,
    amount: Number(form.amount),
    buyerName: form.buyerName.trim(),
    status: form.status,
    createdAt: formatOrderTime(form.createdAt)
  }

  if (editingId.value) {
    await dataStore.updateOrder(editingId.value, payload)
  } else {
    await dataStore.addOrder(payload)
  }

  closeDialog()
}

const updateStatus = async (id: string, status: OrderStatus) => {
  const current = dataStore.orders.find((item) => item.id === id)
  if (!current || current.status === status) return

  await dataStore.updateOrder(id, {
    goodsId: current.goodsId,
    amount: current.amount,
    buyerName: current.buyerName,
    status,
    createdAt: current.createdAt
  })
}

const onStatusChange = (id: string, event: Event) => {
  const target = event.target as HTMLSelectElement | null
  if (!target) return
  void updateStatus(id, target.value as OrderStatus)
}

const removeOrder = async (id: string) => {
  const current = dataStore.orders.find((item) => item.id === id)
  if (!current) return

  if (!confirm(`确定删除订单“${current.id}”吗？`)) {
    return
  }

  await dataStore.deleteOrder(id)
}
</script>

<template>
  <section class="admin-page">
    <section class="admin-page__hero">
      <div class="admin-page__hero-text">
        <span class="admin-page__eyebrow">Orders</span>
        <h2>订单管理</h2>
        <p>对齐后台交易履约场景，支持订单搜索、状态筛选、编辑删除与履约进度维护。</p>
        <div class="admin-page__meta">
          <span class="admin-pill">{{ loading ? '正在加载订单数据' : `共 ${metrics.total} 笔订单` }}</span>
          <span class="admin-pill">待付款 {{ metrics.pendingPayment }}</span>
          <span class="admin-pill">待发货 {{ metrics.pendingShipment }}</span>
          <span class="admin-pill">运输中 {{ metrics.shipping }}</span>
        </div>
      </div>

      <div class="orders-spotlight">
        <span class="orders-spotlight__tag">履约概览</span>
        <strong>{{ metrics.completed }}</strong>
        <p>笔订单已完成</p>
        <span>{{ formatCurrency(metrics.completedGmv) }} 已成交</span>
        <small v-if="latestOrder">最新订单 {{ latestOrder.id }} · {{ latestOrder.status }}</small>
      </div>
    </section>

    <div class="admin-metric-grid">
      <article class="admin-metric-card">
        <span>订单总量</span>
        <strong>{{ metrics.total }}</strong>
        <small>后台全部交易订单</small>
      </article>
      <article class="admin-metric-card">
        <span>待付款</span>
        <strong>{{ metrics.pendingPayment }}</strong>
        <small>需跟进支付转化</small>
      </article>
      <article class="admin-metric-card">
        <span>待发货 / 运输中</span>
        <strong>{{ metrics.pendingShipment + metrics.shipping }}</strong>
        <small>履约链路中的订单</small>
      </article>
      <article class="admin-metric-card">
        <span>订单 GMV</span>
        <strong>{{ formatCurrency(metrics.totalGmv) }}</strong>
        <small>累计订单金额汇总</small>
      </article>
    </div>

    <section class="admin-section-card">
      <div class="admin-section__head">
        <div>
          <h3 class="admin-section__title">订单列表</h3>
          <p class="admin-section__desc">支持搜索、状态筛选、履约流转与订单 CRUD。</p>
        </div>
      </div>

      <div class="admin-toolbar">
        <div class="admin-toolbar__group">
          <input v-model="keyword" class="admin-search" placeholder="搜索订单号、买家、商品标题、分类或品牌" />
          <select v-model="statusFilter" class="admin-select orders-filter">
            <option value="全部">全部状态</option>
            <option v-for="item in statusOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </div>
        <div class="admin-toolbar__group">
          <span class="admin-pill">已完成 {{ metrics.completed }} 笔</span>
          <button class="admin-button admin-button--primary" type="button" @click="openCreate">新增订单</button>
        </div>
      </div>

      <div v-if="filteredOrders.length" class="orders-table">
        <div class="orders-table__head">
          <span>订单信息</span>
          <span>关联商品</span>
          <span>买家</span>
          <span>金额</span>
          <span>履约状态</span>
          <span>时间轴</span>
          <span>操作</span>
        </div>

        <article v-for="item in filteredOrders" :key="item.id" class="orders-table__row">
          <div class="orders-table__cell">
            <strong>{{ item.id }}</strong>
            <span>{{ item.createdAt }}</span>
          </div>

          <div class="orders-product">
            <img
              :src="goodsInfo(item.goodsId)?.heroImage"
              :alt="goodsTitle(item.goodsId)"
              class="orders-product__cover"
            />
            <div class="orders-product__body">
              <strong>{{ goodsTitle(item.goodsId) }}</strong>
              <span>{{ goodsSubline(item.goodsId) }}</span>
            </div>
          </div>

          <div class="orders-table__cell">
            <strong>{{ item.buyerName }}</strong>
            <span>关联商品 {{ item.goodsId }}</span>
          </div>

          <div class="orders-table__cell orders-table__cell--price">
            <strong>{{ formatCurrency(item.amount) }}</strong>
            <span>订单金额</span>
          </div>

          <div class="orders-status">
            <span :class="statusClass(item.status)">{{ item.status }}</span>
            <select
              class="admin-select orders-status__select"
              :value="item.status"
              @change="onStatusChange(item.id, $event)"
            >
              <option v-for="status in statusOptions" :key="status" :value="status">{{ status }}</option>
            </select>
          </div>

          <div class="orders-timeline">
            <span v-for="node in item.timeline" :key="`${item.id}-${node.label}`" class="orders-timeline__node">
              {{ node.label }}
            </span>
          </div>

          <div class="admin-actions">
            <button class="admin-action-button" type="button" @click="openEdit(item.id)">编辑</button>
            <button class="admin-action-button admin-action-button--danger" type="button" @click="removeOrder(item.id)">
              删除
            </button>
          </div>
        </article>
      </div>
      <p v-else class="admin-empty">暂无符合条件的订单</p>
    </section>

    <AdminDialog
      v-model="dialogVisible"
      :title="editingId ? '编辑订单' : '新增订单'"
      description="保存后会立即更新后台订单列表。"
      width="860px"
    >
      <form class="admin-form" @submit.prevent="submitForm">
        <div class="admin-form-grid">
          <div class="admin-field">
            <span>关联商品</span>
            <select v-model="form.goodsId" class="admin-select" @change="syncAmountFromGoods">
              <option v-for="item in dataStore.goods" :key="item.id" :value="item.id">{{ item.title }}</option>
            </select>
          </div>
          <div class="admin-field">
            <span>买家姓名</span>
            <input v-model="form.buyerName" class="admin-input" placeholder="请输入买家姓名" />
          </div>
          <div class="admin-field">
            <span>订单金额</span>
            <input v-model.number="form.amount" class="admin-input" type="number" min="0" />
          </div>
          <div class="admin-field">
            <span>订单状态</span>
            <select v-model="form.status" class="admin-select">
              <option v-for="item in statusOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="admin-field admin-field--full">
            <span>创建时间</span>
            <input v-model="form.createdAt" class="admin-input" type="datetime-local" />
          </div>
        </div>
      </form>

      <template #footer>
        <button class="admin-button" type="button" @click="closeDialog">取消</button>
        <button class="admin-button admin-button--primary" type="button" @click="submitForm">保存订单</button>
      </template>
    </AdminDialog>
  </section>
</template>

<style scoped>
.orders-spotlight {
  width: 320px;
  padding: 24px;
  border-radius: 28px;
  background: linear-gradient(160deg, #1d4ed8 0%, #3b82f6 58%, #14b8a6 100%);
  color: #fff;
  display: grid;
  gap: 10px;
}

.orders-spotlight__tag {
  width: fit-content;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.16);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.orders-spotlight strong {
  font-size: 40px;
  line-height: 1.1;
}

.orders-spotlight p,
.orders-spotlight span,
.orders-spotlight small {
  margin: 0;
  color: rgba(255, 255, 255, 0.84);
}

.orders-spotlight small {
  font-size: 13px;
}

.orders-filter {
  width: 180px;
}

.orders-table {
  display: grid;
}

.orders-table__head,
.orders-table__row {
  display: grid;
  grid-template-columns: 1fr 1.5fr 0.9fr 0.8fr 1.05fr 1.3fr 0.8fr;
  gap: 18px;
  align-items: center;
}

.orders-table__head {
  min-height: 54px;
  padding: 0 12px 12px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.orders-table__row {
  min-height: 124px;
  padding: 18px 12px;
  border-top: 1px solid rgba(226, 232, 240, 0.95);
}

.orders-table__cell {
  display: grid;
  gap: 6px;
}

.orders-table__cell strong,
.orders-table__cell span {
  display: block;
  margin: 0;
}

.orders-table__cell strong {
  color: #0f172a;
}

.orders-table__cell span {
  color: #64748b;
  line-height: 1.6;
}

.orders-table__cell--price strong {
  font-size: 18px;
}

.orders-product {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr);
  gap: 14px;
  align-items: center;
}

.orders-product__cover {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  object-fit: cover;
  background: #eff6ff;
  border: 1px solid rgba(191, 219, 254, 0.7);
}

.orders-product__body {
  display: grid;
  gap: 6px;
}

.orders-product__body strong,
.orders-product__body span {
  margin: 0;
}

.orders-product__body span {
  color: #64748b;
  line-height: 1.6;
}

.orders-status {
  display: grid;
  gap: 10px;
}

.orders-status__select {
  min-height: 40px;
  padding-right: 36px;
}

.orders-timeline {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.orders-timeline__node {
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  background: #f8fbff;
  border: 1px solid rgba(191, 219, 254, 0.86);
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
}
</style>
