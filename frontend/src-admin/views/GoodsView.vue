<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import AdminDialog from '@admin/components/AdminDialog.vue'
import { useAdminDataStore } from '@admin/stores/data'
import type { AdminGoodsAuditStatus, AdminGoodsDraft, AdminTransferType } from '@admin/types'
import type { KycLevel, SellerGoodsStatus } from '@shared/types'

const dataStore = useAdminDataStore()
const loading = ref(false)
const keyword = ref('')
const auditFilter = ref<'全部' | AdminGoodsAuditStatus>('全部')
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const tagInput = ref('')
const selectedIds = ref<string[]>([])

const statusOptions: SellerGoodsStatus[] = ['草稿', '待审核', '在售中', '已预订', '已售出', '已下架']
const auditStatusOptions: AdminGoodsAuditStatus[] = ['待审核', '审核通过', '已驳回']
const sellerLevelOptions: KycLevel[] = ['L1', 'L2', 'L3']
const transferTypeOptions: AdminTransferType[] = ['自卖', '寄卖', '极速回收']

const formatCurrency = (value: number) => `¥${value.toLocaleString('zh-CN')}`
const previewText = (text: string) => (text.length > 42 ? `${text.slice(0, 42)}...` : text)

const createEmptyGoodsForm = (): AdminGoodsDraft => ({
  title: '',
  category: dataStore.categories[0]?.name ?? '奢品箱包',
  brand: '',
  condition: '95 新',
  price: 0,
  originalPrice: 0,
  carbonSavedKg: 0,
  aiPrice: 0,
  city: '上海',
  sellerName: '平台卖家',
  sellerLevel: 'L2',
  story: '',
  description: '',
  status: '待审核',
  tags: [],
  heroImage: '',
  gallery: [],
  createdAt: new Date().toISOString().slice(0, 10),
  auditStatus: '待审核',
  reviewNote: '新发布商品，待运营审核。',
  transferType: '自卖',
  viewCount: 0,
  favorCount: 0,
  mockCertified: false
})

const form = reactive<AdminGoodsDraft>(createEmptyGoodsForm())

watch(
  () => form.auditStatus,
  (value) => {
    if (value === '待审核') {
      form.status = '待审核'
      return
    }

    if (value === '已驳回') {
      form.status = '草稿'
      return
    }

    if (value === '审核通过' && ['待审核', '草稿'].includes(form.status)) {
      form.status = '在售中'
    }
  },
  { immediate: true }
)

const resetForm = () => {
  Object.assign(form, createEmptyGoodsForm())
  tagInput.value = ''
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

const metrics = computed(() => ({
  total: dataStore.goods.length,
  pending: dataStore.goods.filter((item) => item.auditStatus === '待审核').length,
  approved: dataStore.goods.filter((item) => item.auditStatus === '审核通过').length,
  rejected: dataStore.goods.filter((item) => item.auditStatus === '已驳回').length,
  certified: dataStore.goods.filter((item) => item.mockCertified).length
}))

const filteredGoods = computed(() => {
  const query = keyword.value.trim().toLowerCase()

  return dataStore.goods.filter((item) => {
    const matchesFilter = auditFilter.value === '全部' || item.auditStatus === auditFilter.value
    const matchesQuery =
      !query ||
      [item.title, item.brand, item.category, item.sellerName, item.city, item.reviewNote]
        .some((field) => field.toLowerCase().includes(query))

    return matchesFilter && matchesQuery
  })
})

watch(
  () => dataStore.goods.map((item) => item.id),
  (ids) => {
    const validIds = new Set(ids)
    selectedIds.value = selectedIds.value.filter((id) => validIds.has(id))
  },
  { immediate: true }
)

const visibleIds = computed(() => filteredGoods.value.map((item) => item.id))
const visibleSelectedCount = computed(() =>
  visibleIds.value.filter((id) => selectedIds.value.includes(id)).length
)
const selectedCount = computed(() => selectedIds.value.length)

const allVisibleSelected = computed({
  get: () => visibleIds.value.length > 0 && visibleSelectedCount.value === visibleIds.value.length,
  set: (checked: boolean) => {
    const next = new Set(selectedIds.value)

    if (checked) {
      visibleIds.value.forEach((id) => next.add(id))
    } else {
      visibleIds.value.forEach((id) => next.delete(id))
    }

    selectedIds.value = Array.from(next)
  }
})

const isSelectionIndeterminate = computed(
  () => visibleSelectedCount.value > 0 && !allVisibleSelected.value
)

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (id: string) => {
  const current = dataStore.goods.find((item) => item.id === id)
  if (!current) return

  editingId.value = id
  Object.assign(form, JSON.parse(JSON.stringify(current)))
  tagInput.value = current.tags.join('，')
  dialogVisible.value = true
}

const closeDialog = () => {
  dialogVisible.value = false
  resetForm()
}

const auditStatusClass = (status: AdminGoodsAuditStatus) => {
  if (status === '审核通过') return 'admin-badge admin-badge--success'
  if (status === '待审核') return 'admin-badge admin-badge--warning'
  return 'admin-badge admin-badge--neutral'
}

const goodsStatusClass = (status: string) => {
  if (status === '草稿') return 'admin-badge admin-badge--neutral'
  if (status === '待审核') return 'admin-badge admin-badge--warning'
  if (status === '在售中') return 'admin-badge admin-badge--primary'
  if (status === '已预订') return 'admin-badge admin-badge--warning'
  return 'admin-badge admin-badge--neutral'
}

const submitForm = async () => {
  if (!form.title.trim() || !form.category.trim() || !form.brand.trim()) {
    alert('请完整填写商品标题、分类和品牌。')
    return
  }

  const heroImage = form.heroImage.trim()
  const tags = tagInput.value
    .split(/[，,]/)
    .map((item) => item.trim())
    .filter(Boolean)

  const payload: AdminGoodsDraft = {
    ...form,
    title: form.title.trim(),
    category: form.category.trim(),
    brand: form.brand.trim(),
    city: form.city.trim(),
    sellerName: form.sellerName.trim(),
    story: form.story.trim(),
    description: form.description.trim(),
    heroImage,
    gallery: heroImage ? [heroImage] : [],
    tags,
    price: Number(form.price),
    originalPrice: Number(form.originalPrice),
    carbonSavedKg: Number(form.carbonSavedKg),
    aiPrice: Number(form.aiPrice),
    viewCount: Number(form.viewCount),
    favorCount: Number(form.favorCount)
  }

  if (editingId.value) {
    await dataStore.updateGoods(editingId.value, payload)
  } else {
    await dataStore.addGoods(payload)
  }

  closeDialog()
}

const approveGoods = async (id: string) => {
  await dataStore.approveGoods(id)
}

const clearSelection = () => {
  selectedIds.value = []
}

const toggleSelection = (id: string, checked: string | number | boolean) => {
  if (checked) {
    selectedIds.value = Array.from(new Set([...selectedIds.value, id]))
    return
  }

  selectedIds.value = selectedIds.value.filter((item) => item !== id)
}

const approveSelected = async () => {
  if (!selectedIds.value.length) {
    alert('请先勾选要通过审核的商品。')
    return
  }

  await dataStore.approveGoodsBatch(selectedIds.value)
  clearSelection()
}

const rejectGoods = async (id: string) => {
  const reason = window.prompt('请输入驳回原因', '图片或附件不完整，请补充后重新提交。')
  if (!reason) return
  await dataStore.rejectGoods(id, reason)
}

const rejectSelected = async () => {
  if (!selectedIds.value.length) {
    alert('请先勾选要驳回的商品。')
    return
  }

  const reason = window.prompt('请输入批量驳回原因', '图片或附件不完整，请补充后重新提交。')
  if (!reason) return

  await dataStore.rejectGoodsBatch(selectedIds.value, reason)
  clearSelection()
}

const approveAllPending = async () => {
  if (!metrics.value.pending) {
    alert('当前没有待审核商品。')
    return
  }

  if (!confirm(`确认批量通过 ${metrics.value.pending} 件待审核商品吗？`)) {
    return
  }

  await dataStore.approveAllPendingGoods()
}

const removeGoods = async (id: string) => {
  const current = dataStore.goods.find((item) => item.id === id)
  if (!current) return

  if (!confirm(`确定删除商品“${current.title}”吗？相关订单也会一起移除。`)) {
    return
  }

  await dataStore.deleteGoods(id)
  selectedIds.value = selectedIds.value.filter((item) => item !== id)
}
</script>

<template>
  <section class="admin-page">
    <section class="admin-page__hero">
      <div class="admin-page__hero-text">
        <span class="admin-page__eyebrow">Goods Review</span>
        <h2>商品审核</h2>
        <p>对齐 PRD 的后台商品审核队列，支持待审核筛选、通过/驳回、批量通过与基础商品资料维护。</p>
        <div class="admin-page__meta">
          <span class="admin-pill">待审核 {{ metrics.pending }}</span>
          <span class="admin-pill">审核通过 {{ metrics.approved }}</span>
          <span class="admin-pill">已驳回 {{ metrics.rejected }}</span>
        </div>
      </div>

      <div class="goods-spotlight">
        <span class="goods-spotlight__tag">审核中心</span>
        <strong>{{ metrics.pending }}</strong>
        <p>件商品待处理</p>
        <span>已认证商品 {{ metrics.certified }} 件</span>
      </div>
    </section>

    <div class="admin-metric-grid goods-review-metrics">
      <article class="admin-metric-card">
        <span>商品总数</span>
        <strong>{{ metrics.total }}</strong>
        <small>后台全部商品池</small>
      </article>
      <article class="admin-metric-card">
        <span>待审核</span>
        <strong>{{ metrics.pending }}</strong>
        <small>优先处理新发布商品</small>
      </article>
      <article class="admin-metric-card">
        <span>已驳回</span>
        <strong>{{ metrics.rejected }}</strong>
        <small>需要卖家补充资料</small>
      </article>
      <article class="admin-metric-card">
        <span>已认证</span>
        <strong>{{ metrics.certified }}</strong>
        <small>模拟链上认证展示</small>
      </article>
    </div>

    <section class="admin-section-card">
      <div class="admin-section__head">
        <div>
          <h3 class="admin-section__title">审核队列</h3>
          <p class="admin-section__desc">支持搜索、增删改查，以及 PRD 要求的商品审核通过/驳回与批量处理。</p>
        </div>
      </div>

      <div class="admin-toolbar">
        <div class="admin-toolbar__group">
          <input v-model="keyword" class="admin-search" placeholder="搜索商品标题、品牌、卖家或审核意见" />
          <select v-model="auditFilter" class="admin-select goods-toolbar__select">
            <option value="全部">全部审核状态</option>
            <option v-for="item in auditStatusOptions" :key="item" :value="item">{{ item }}</option>
          </select>
        </div>
        <div class="admin-toolbar__group">
          <span class="admin-pill">已选择 {{ selectedCount }} 件</span>
          <button class="admin-button" type="button" @click="approveSelected">批量通过</button>
          <button class="admin-button admin-button--danger" type="button" @click="rejectSelected">批量驳回</button>
          <button class="admin-button" type="button" @click="approveAllPending">通过全部待审核</button>
          <button class="admin-button admin-button--primary" type="button" @click="openCreate">新增商品</button>
        </div>
      </div>

      <div v-if="filteredGoods.length" class="goods-review-table">
        <div class="goods-review-table__head">
          <label class="goods-review-table__checkbox">
            <el-checkbox v-model="allVisibleSelected" :indeterminate="isSelectionIndeterminate" />
            <span>全选</span>
          </label>
          <span>商品信息</span>
          <span>卖家 / 流转方式</span>
          <span>挂牌价</span>
          <span>审核状态</span>
          <span>当前状态</span>
          <span>审核意见</span>
          <span>操作</span>
        </div>

        <article
          v-for="item in filteredGoods"
          :key="item.id"
          class="goods-review-table__row"
          :class="{ 'goods-review-table__row--selected': selectedIds.includes(item.id) }"
        >
          <div class="goods-review-table__checkbox goods-review-table__checkbox--cell">
            <el-checkbox
              :model-value="selectedIds.includes(item.id)"
              @change="(checked) => toggleSelection(item.id, checked)"
            />
          </div>

          <div class="goods-review-table__product">
            <img :src="item.heroImage" :alt="item.title" class="goods-review-table__image" />
            <div>
              <strong>{{ item.title }}</strong>
              <p>{{ item.brand }} · {{ item.category }} · {{ item.condition }}</p>
              <small>浏览 {{ item.viewCount }} · 收藏 {{ item.favorCount }}</small>
            </div>
          </div>

          <div class="goods-review-table__meta">
            <strong>{{ item.sellerName }}</strong>
            <span>{{ item.city }} · {{ item.transferType }}</span>
          </div>

          <div class="goods-review-table__meta goods-review-table__meta--price">
            <strong>{{ formatCurrency(item.price) }}</strong>
            <span>AI {{ formatCurrency(item.aiPrice) }}</span>
          </div>

          <span :class="auditStatusClass(item.auditStatus)">{{ item.auditStatus }}</span>
          <span :class="goodsStatusClass(item.status)">{{ item.status }}</span>

          <div class="goods-review-table__meta">
            <strong>{{ previewText(item.reviewNote) }}</strong>
            <span>{{ item.mockCertified ? '已认证展示' : '未认证' }}</span>
          </div>

          <div class="admin-actions">
            <button class="admin-action-button" type="button" @click="openEdit(item.id)">编辑</button>
            <button v-if="item.auditStatus !== '审核通过'" class="admin-action-button" type="button" @click="approveGoods(item.id)">通过</button>
            <button v-if="item.auditStatus !== '已驳回'" class="admin-action-button" type="button" @click="rejectGoods(item.id)">驳回</button>
            <button class="admin-action-button admin-action-button--danger" type="button" @click="removeGoods(item.id)">删除</button>
          </div>
        </article>
      </div>
      <p v-else class="admin-empty">暂无符合条件的商品</p>
    </section>

    <AdminDialog
      v-model="dialogVisible"
      :title="editingId ? '编辑商品' : '新增商品'"
      description="保存后会写入管理员数据集；新建商品默认进入待审核状态。"
      width="960px"
    >
      <form class="admin-form" @submit.prevent="submitForm">
        <div class="admin-form-grid">
          <div class="admin-field">
            <span>商品标题</span>
            <input v-model="form.title" class="admin-input" placeholder="请输入商品标题" />
          </div>
          <div class="admin-field">
            <span>商品分类</span>
            <select v-model="form.category" class="admin-select">
              <option v-for="item in dataStore.categories" :key="item.id" :value="item.name">{{ item.name }}</option>
            </select>
          </div>
          <div class="admin-field">
            <span>品牌</span>
            <input v-model="form.brand" class="admin-input" placeholder="请输入品牌" />
          </div>
          <div class="admin-field">
            <span>流转方式</span>
            <select v-model="form.transferType" class="admin-select">
              <option v-for="item in transferTypeOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="admin-field">
            <span>成色</span>
            <input v-model="form.condition" class="admin-input" placeholder="如 95 新" />
          </div>
          <div class="admin-field">
            <span>商品状态</span>
            <select v-model="form.status" class="admin-select">
              <option v-for="item in statusOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="admin-field">
            <span>审核状态</span>
            <select v-model="form.auditStatus" class="admin-select">
              <option v-for="item in auditStatusOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="admin-field">
            <span>卖家等级</span>
            <select v-model="form.sellerLevel" class="admin-select">
              <option v-for="item in sellerLevelOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="admin-field">
            <span>售价</span>
            <input v-model.number="form.price" class="admin-input" type="number" min="0" />
          </div>
          <div class="admin-field">
            <span>原价</span>
            <input v-model.number="form.originalPrice" class="admin-input" type="number" min="0" />
          </div>
          <div class="admin-field">
            <span>AI 估价</span>
            <input v-model.number="form.aiPrice" class="admin-input" type="number" min="0" />
          </div>
          <div class="admin-field">
            <span>减碳值(kg)</span>
            <input v-model.number="form.carbonSavedKg" class="admin-input" type="number" min="0" />
          </div>
          <div class="admin-field">
            <span>卖家名称</span>
            <input v-model="form.sellerName" class="admin-input" placeholder="请输入卖家名称" />
          </div>
          <div class="admin-field">
            <span>城市</span>
            <input v-model="form.city" class="admin-input" placeholder="请输入城市" />
          </div>
          <div class="admin-field">
            <span>浏览量</span>
            <input v-model.number="form.viewCount" class="admin-input" type="number" min="0" />
          </div>
          <div class="admin-field">
            <span>收藏量</span>
            <input v-model.number="form.favorCount" class="admin-input" type="number" min="0" />
          </div>
          <div class="admin-field admin-field--full">
            <span>商品标签</span>
            <input v-model="tagInput" class="admin-input" placeholder="多个标签请用中文逗号分隔" />
          </div>
          <div class="admin-field admin-field--full">
            <span>封面图片地址</span>
            <input v-model="form.heroImage" class="admin-input" placeholder="可填写图片 URL，不填则使用默认图" />
          </div>
          <div class="admin-field admin-field--full">
            <span>审核意见</span>
            <textarea v-model="form.reviewNote" class="admin-textarea" placeholder="请输入审核意见"></textarea>
          </div>
          <div class="admin-field admin-field--full">
            <span>商品故事</span>
            <textarea v-model="form.story" class="admin-textarea" placeholder="请输入商品故事"></textarea>
          </div>
          <div class="admin-field admin-field--full">
            <span>商品描述</span>
            <textarea v-model="form.description" class="admin-textarea" placeholder="请输入商品描述"></textarea>
          </div>
        </div>
      </form>

      <template #footer>
        <button class="admin-button" type="button" @click="closeDialog">取消</button>
        <button class="admin-button admin-button--primary" type="button" @click="submitForm">保存商品</button>
      </template>
    </AdminDialog>
  </section>
</template>

<style scoped>
.goods-spotlight {
  width: 320px;
  padding: 24px;
  border-radius: 28px;
  background: linear-gradient(160deg, #0f172a 0%, #1d4ed8 58%, #38bdf8 100%);
  color: #fff;
  display: grid;
  gap: 10px;
}

.goods-spotlight__tag {
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

.goods-spotlight strong {
  font-size: 36px;
  line-height: 1.1;
}

.goods-spotlight p,
.goods-spotlight span {
  margin: 0;
  color: rgba(255, 255, 255, 0.84);
}

.goods-toolbar__select {
  min-width: 180px;
}

.goods-review-metrics {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.goods-review-table {
  display: grid;
}

.goods-review-table__head,
.goods-review-table__row {
  display: grid;
  grid-template-columns: 78px 2.2fr 1.1fr 0.85fr 0.8fr 0.8fr 1.15fr 1fr;
  gap: 18px;
  align-items: center;
}

.goods-review-table__head {
  min-height: 54px;
  padding: 0 12px 12px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.goods-review-table__row {
  min-height: 140px;
  padding: 18px 12px;
  border-top: 1px solid rgba(226, 232, 240, 0.95);
}

.goods-review-table__row--selected {
  background: rgba(239, 246, 255, 0.68);
}

.goods-review-table__checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #334155;
  font-weight: 700;
}

.goods-review-table__checkbox--cell {
  justify-content: center;
}

.goods-review-table__product {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.goods-review-table__image {
  width: 96px;
  height: 96px;
  border-radius: 24px;
  object-fit: cover;
}

.goods-review-table__product strong,
.goods-review-table__product p,
.goods-review-table__product small,
.goods-review-table__meta strong,
.goods-review-table__meta span {
  display: block;
  margin: 0;
}

.goods-review-table__product strong,
.goods-review-table__meta strong {
  color: #0f172a;
}

.goods-review-table__product p,
.goods-review-table__product small,
.goods-review-table__meta span {
  margin-top: 6px;
  color: #64748b;
  line-height: 1.6;
}

.goods-review-table__meta--price strong {
  font-size: 18px;
}
</style>
