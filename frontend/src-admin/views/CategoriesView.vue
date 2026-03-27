<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import AdminDialog from '@admin/components/AdminDialog.vue'
import { useAdminDataStore } from '@admin/stores/data'
import type { AdminCategoryDraft } from '@admin/types'

const dataStore = useAdminDataStore()
const loading = ref(false)
const keyword = ref('')
const usageFilter = ref<'全部' | '使用中' | '空分类'>('全部')
const dialogVisible = ref(false)
const editingId = ref<string | null>(null)
const brandsInput = ref('')

const formatCurrency = (value: number) => `¥${value.toLocaleString('zh-CN')}`

const createEmptyCategoryForm = (): AdminCategoryDraft => ({
  name: '',
  description: '',
  featuredBrands: [],
  coverImage: ''
})

const form = reactive<AdminCategoryDraft>(createEmptyCategoryForm())

const resetForm = () => {
  Object.assign(form, createEmptyCategoryForm())
  brandsInput.value = ''
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

const categoryStats = computed(() => {
  const map = new Map<
    string,
    { goodsCount: number; onSaleCount: number; soldCount: number; avgPrice: number; latestTitle: string; previewTitles: string[] }
  >()

  dataStore.goods.forEach((item) => {
    const current = map.get(item.category)
    if (!current) {
      map.set(item.category, {
        goodsCount: 1,
        onSaleCount: item.status === '在售中' ? 1 : 0,
        soldCount: item.status === '已售出' ? 1 : 0,
        avgPrice: item.price,
        latestTitle: item.title,
        previewTitles: [item.title]
      })
      return
    }

    current.goodsCount += 1
    current.onSaleCount += item.status === '在售中' ? 1 : 0
    current.soldCount += item.status === '已售出' ? 1 : 0
    current.avgPrice += item.price
    current.latestTitle = item.title
    if (current.previewTitles.length < 3) {
      current.previewTitles.push(item.title)
    }
  })

  return map
})

const allCategoryRows = computed(() =>
  dataStore.categories.map((item) => {
    const stats = categoryStats.value.get(item.name)
    return {
      ...item,
      goodsCount: stats?.goodsCount ?? 0,
      onSaleCount: stats?.onSaleCount ?? 0,
      soldCount: stats?.soldCount ?? 0,
      avgPrice: stats?.goodsCount ? Math.round(stats.avgPrice / stats.goodsCount) : 0,
      latestTitle: stats?.latestTitle ?? '暂无商品',
      previewTitles: stats?.previewTitles ?? [],
      isEmpty: !stats?.goodsCount
    }
  })
)

const categoryRows = computed(() => {
  const query = keyword.value.trim().toLowerCase()

  return allCategoryRows.value.filter((item) => {
    const matchesQuery =
      !query ||
      [item.name, item.description, item.featuredBrands.join(' '), item.previewTitles.join(' ')]
        .some((field) => field.toLowerCase().includes(query))
    const matchesUsage =
      usageFilter.value === '全部' ||
      (usageFilter.value === '使用中' ? item.goodsCount > 0 : item.goodsCount === 0)

    return matchesQuery && matchesUsage
  })
})

const topCategory = computed(() => [...allCategoryRows.value].sort((left, right) => right.goodsCount - left.goodsCount)[0])
const totalCategories = computed(() => dataStore.categories.length)
const totalBrands = computed(() => {
  const brands = new Set<string>()
  dataStore.categories.forEach((item) => item.featuredBrands.forEach((brand) => brands.add(brand)))
  return brands.size
})
const activeCategories = computed(() => allCategoryRows.value.filter((item) => item.goodsCount > 0).length)
const emptyCategories = computed(() => totalCategories.value - activeCategories.value)
const totalOnSale = computed(() =>
  dataStore.goods.filter((item) => item.status === '在售中').length
)
const uncategorizedGoods = computed(() => dataStore.goods.filter((item) => item.category === '未分类').length)

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (id: string) => {
  const current = dataStore.categories.find((item) => item.id === id)
  if (!current) return

  editingId.value = id
  Object.assign(form, JSON.parse(JSON.stringify(current)))
  brandsInput.value = current.featuredBrands.join('，')
  dialogVisible.value = true
}

const closeDialog = () => {
  dialogVisible.value = false
  resetForm()
}

const submitForm = async () => {
  if (!form.name.trim()) {
    alert('请填写分类名称。')
    return
  }

  const payload: AdminCategoryDraft = {
    name: form.name.trim(),
    description: form.description.trim(),
    coverImage: form.coverImage.trim(),
    featuredBrands: brandsInput.value
      .split(/[，,]/)
      .map((item) => item.trim())
      .filter(Boolean)
  }

  try {
    if (editingId.value) {
      await dataStore.updateCategory(editingId.value, payload)
    } else {
      await dataStore.addCategory(payload)
    }
    closeDialog()
  } catch (error) {
    alert((error as Error).message)
  }
}

const removeCategory = async (id: string) => {
  const current = dataStore.categories.find((item) => item.id === id)
  if (!current) return

  if (!confirm(`确定删除分类“${current.name}”吗？该分类下商品会被转移到“未分类”。`)) {
    return
  }

  await dataStore.deleteCategory(id)
}
</script>

<template>
  <section class="admin-page">
    <section class="admin-page__hero">
      <div class="admin-page__hero-text">
        <span class="admin-page__eyebrow">Categories</span>
        <h2>分类管理</h2>
        <p>按后台运营视角维护品类资料，支持搜索筛选、分类 CRUD、品牌标签管理和未分类承接。</p>
        <div class="admin-page__meta">
          <span class="admin-pill">{{ loading ? '正在加载分类数据' : `共 ${totalCategories} 个分类` }}</span>
          <span class="admin-pill">覆盖品牌 {{ totalBrands }}</span>
          <span class="admin-pill">在售商品 {{ totalOnSale }}</span>
          <span class="admin-pill">未分类商品 {{ uncategorizedGoods }}</span>
        </div>
      </div>

      <div class="category-spotlight">
        <span class="category-spotlight__tag">重点分类</span>
        <strong>{{ topCategory?.name ?? '暂无' }}</strong>
        <p>{{ topCategory ? `${topCategory.goodsCount} 件商品 · 平均售价 ${formatCurrency(topCategory.avgPrice)}` : '暂无分类数据' }}</p>
      </div>
    </section>

    <div class="admin-metric-grid">
      <article class="admin-metric-card">
        <span>分类总数</span>
        <strong>{{ totalCategories }}</strong>
        <small>后台可维护全部品类</small>
      </article>
      <article class="admin-metric-card">
        <span>使用中分类</span>
        <strong>{{ activeCategories }}</strong>
        <small>已有商品挂载的分类</small>
      </article>
      <article class="admin-metric-card">
        <span>空分类</span>
        <strong>{{ emptyCategories }}</strong>
        <small>适合补充封面和标签后运营</small>
      </article>
      <article class="admin-metric-card">
        <span>在售商品</span>
        <strong>{{ totalOnSale }}</strong>
        <small>当前在售商品数量</small>
      </article>
    </div>

    <section class="admin-section-card">
      <div class="admin-section__head">
        <div>
          <h3 class="admin-section__title">分类列表</h3>
          <p class="admin-section__desc">支持搜索、使用状态筛选，以及分类资料维护。</p>
        </div>
      </div>

      <div class="admin-toolbar">
        <div class="admin-toolbar__group">
          <input v-model="keyword" class="admin-search" placeholder="搜索分类名称、说明或品牌" />
          <select v-model="usageFilter" class="admin-select category-filter">
            <option value="全部">全部分类</option>
            <option value="使用中">使用中</option>
            <option value="空分类">空分类</option>
          </select>
        </div>
        <div class="admin-toolbar__group">
          <span class="admin-pill">未分类承接 {{ uncategorizedGoods }} 件</span>
          <button class="admin-button admin-button--primary" type="button" @click="openCreate">新增分类</button>
        </div>
      </div>

      <div v-if="categoryRows.length" class="category-list">
        <div class="category-list__head">
          <span>分类信息</span>
          <span>说明与品牌</span>
          <span>商品数据</span>
          <span>关联商品</span>
          <span>操作</span>
        </div>

        <article v-for="item in categoryRows" :key="item.id" class="category-list__row">
          <div class="category-info">
            <img :src="item.coverImage" :alt="item.name" class="category-info__cover" />
            <div class="category-info__body">
              <div class="category-info__title">
                <strong>{{ item.name }}</strong>
                <span :class="item.goodsCount ? 'admin-badge admin-badge--primary' : 'admin-badge admin-badge--neutral'">
                  {{ item.goodsCount ? '使用中' : '空分类' }}
                </span>
              </div>
              <p>{{ item.description || '暂无分类说明' }}</p>
            </div>
          </div>

          <div class="category-summary">
            <p class="category-summary__text">{{ item.featuredBrands.length ? item.featuredBrands.join(' · ') : '暂无品牌标签' }}</p>
            <span class="category-summary__sub">最近商品：{{ item.latestTitle }}</span>
          </div>

          <div class="category-stats">
            <div>
              <span>在售</span>
              <strong>{{ item.onSaleCount }}</strong>
            </div>
            <div>
              <span>已售</span>
              <strong>{{ item.soldCount }}</strong>
            </div>
            <div>
              <span>均价</span>
              <strong>{{ item.avgPrice ? formatCurrency(item.avgPrice) : '暂无' }}</strong>
            </div>
          </div>

          <div class="category-preview">
            <span v-if="item.previewTitles.length === 0" class="category-preview__empty">当前暂无商品</span>
            <span v-for="title in item.previewTitles" :key="title" class="category-preview__item">{{ title }}</span>
          </div>

          <div class="admin-actions">
            <button class="admin-action-button" type="button" @click="openEdit(item.id)">编辑</button>
            <button
              class="admin-action-button admin-action-button--danger"
              type="button"
              :disabled="item.name === '未分类'"
              @click="removeCategory(item.id)"
            >
              删除
            </button>
          </div>
        </article>
      </div>
      <p v-else class="admin-empty">暂无符合条件的分类</p>
    </section>

    <AdminDialog
      v-model="dialogVisible"
      :title="editingId ? '编辑分类' : '新增分类'"
      description="保存后会立即更新后台分类数据。"
      width="820px"
    >
      <form class="admin-form" @submit.prevent="submitForm">
        <div class="admin-form-grid">
          <div class="admin-field">
            <span>分类名称</span>
            <input v-model="form.name" class="admin-input" placeholder="请输入分类名称" />
          </div>
          <div class="admin-field">
            <span>封面图片地址</span>
            <input v-model="form.coverImage" class="admin-input" placeholder="可填写图片 URL，不填则自动补默认图" />
          </div>
          <div class="admin-field admin-field--full">
            <span>分类说明</span>
            <textarea v-model="form.description" class="admin-textarea" placeholder="请输入分类说明"></textarea>
          </div>
          <div class="admin-field admin-field--full">
            <span>品牌标签</span>
            <input v-model="brandsInput" class="admin-input" placeholder="多个品牌请用中文逗号分隔" />
          </div>
        </div>
      </form>

      <template #footer>
        <button class="admin-button" type="button" @click="closeDialog">取消</button>
        <button class="admin-button admin-button--primary" type="button" @click="submitForm">保存分类</button>
      </template>
    </AdminDialog>
  </section>
</template>

<style scoped>
.category-spotlight {
  width: 320px;
  padding: 24px;
  border-radius: 28px;
  background: linear-gradient(160deg, #0f172a 0%, #1d4ed8 62%, #38bdf8 100%);
  color: #fff;
  display: grid;
  gap: 12px;
}

.category-spotlight__tag {
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

.category-spotlight strong {
  font-size: 34px;
  line-height: 1.1;
}

.category-spotlight p {
  margin: 0;
  color: rgba(255, 255, 255, 0.86);
  line-height: 1.7;
}

.category-filter {
  width: 180px;
}

.category-list {
  display: grid;
}

.category-list__head,
.category-list__row {
  display: grid;
  grid-template-columns: 1.35fr 1fr 1fr 0.9fr 0.7fr;
  gap: 18px;
  align-items: center;
}

.category-list__head {
  min-height: 54px;
  padding: 0 12px 12px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.category-list__row {
  min-height: 138px;
  padding: 18px 12px;
  border-top: 1px solid rgba(226, 232, 240, 0.95);
}

.category-info {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
}

.category-info__cover {
  width: 88px;
  height: 88px;
  border-radius: 20px;
  object-fit: cover;
  background: #eff6ff;
  border: 1px solid rgba(191, 219, 254, 0.7);
}

.category-info__body {
  display: grid;
  gap: 8px;
}

.category-info__title {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}

.category-info__body strong,
.category-info__body p,
.category-summary__text,
.category-summary__sub,
.category-stats strong,
.category-stats span {
  margin: 0;
}

.category-info__body strong {
  font-size: 22px;
}

.category-info__body p,
.category-summary__text,
.category-summary__sub,
.category-stats span {
  color: #64748b;
  line-height: 1.7;
}

.category-summary {
  display: grid;
  gap: 10px;
}

.category-summary__sub {
  font-size: 13px;
}

.category-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.category-stats div {
  padding: 14px 16px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  display: grid;
  gap: 8px;
}

.category-stats strong {
  font-size: 20px;
}

.category-preview {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.category-preview__item,
.category-preview__empty {
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 600;
}

.category-preview__item {
  background: #f8fbff;
  border: 1px solid rgba(191, 219, 254, 0.86);
  color: #1d4ed8;
}

.category-preview__empty {
  background: rgba(100, 116, 139, 0.1);
  color: #475569;
}

.admin-action-button:disabled {
  cursor: not-allowed;
  opacity: 0.5;
}
</style>
