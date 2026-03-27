<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { estimateGoods, uploadEstimateImage } from '@/api/ai'
import { listGoodsCategories } from '@/api/goods'
import { useAppStore } from '@/stores/app'
import { applyImageFallback, resolveAssetUrl } from '@/utils/assets'

const store = useAppStore()

const loading = ref(false)
const uploading = ref(false)
const uploadedImages = ref<string[]>([])
const categories = ref<string[]>([])

const form = reactive({
  title: '',
  category: '',
  brand: '',
  condition: '95 新',
  yearsUsed: 1,
  rarity: 3,
  description: ''
})

const loadCategories = async () => {
  const stats = await listGoodsCategories()
  categories.value = stats.map((item) => item.name)
  if (!categories.value.length) {
    categories.value = ['未分类']
  }
}

onMounted(async () => {
  await loadCategories()
})

watch(
  categories,
  (value) => {
    if (!form.category && value.length) {
      form.category = value[0]
    }
  },
  { immediate: true }
)

const estimateCards = computed(() => {
  const result = store.lastEstimate

  if (!result) {
    return [
      { label: '预估中位价', value: '--' },
      { label: '建议区间', value: '--' },
      { label: '减碳贡献', value: '--' }
    ]
  }

  return [
    { label: '预估中位价', value: `￥${result.price.toLocaleString()}` },
    { label: '建议区间', value: `￥${result.low.toLocaleString()} - ￥${result.high.toLocaleString()}` },
    { label: '减碳贡献', value: `${result.carbonSavedKg} kgCO₂e` }
  ]
})

const appraiseLink = computed(() => ({
  path: '/appraise',
  query: {
    mode: '视频连线鉴定',
    ...(form.title.trim() ? { title: form.title.trim() } : {})
  }
}))

const estimateBreakdown = computed(() => {
  const result = store.lastEstimate

  if (!result) {
    return []
  }

  const basePrice = Math.max(result.price, 1)

  return [
    { label: '成色影响', value: Math.min(100, 55 + form.rarity * 4) },
    { label: '品牌溢价', value: Math.min(100, 48 + Math.min(form.brand.length, 10) * 4) },
    { label: '流通热度', value: Math.min(100, Math.round((result.high / basePrice) * 52)) }
  ]
})

const onImageError = (event: Event) => applyImageFallback(event, 'goods')

const onSelectFiles = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = Array.from(target.files ?? []).slice(0, 4)

  if (!files.length) return

  uploading.value = true

  try {
    const payloads = await Promise.all(files.map((file) => uploadEstimateImage(file)))
    uploadedImages.value = payloads.map((item) => item.url)
  } catch (error) {
    alert((error as Error).message)
  } finally {
    uploading.value = false
    target.value = ''
  }
}

const onEstimate = async () => {
  if (!form.title || !form.brand || !form.category) {
    alert('请先填写商品名称、品牌和分类。')
    return
  }

  loading.value = true

  try {
    await estimateGoods({
      title: form.title,
      category: form.category,
      brand: form.brand,
      condition: form.condition,
      description: form.description,
      imageUrls: uploadedImages.value,
      yearsUsed: form.yearsUsed,
      rarity: form.rarity
    })
  } catch (error) {
    alert((error as Error).message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="section-block estimate-page">
    <div class="section-head">
      <div>
        <span class="eyebrow">AI 智能估价</span>
        <h1>拖拽上传图片，生成真实估价结果</h1>
        <p>上传图片后会创建真实估价任务，并返回区间价格、置信度和分项分析。</p>
      </div>
      <RouterLink class="ghost-btn" to="/goods/publish">去发布闲置</RouterLink>
    </div>

    <div class="estimate-layout">
      <section class="panel estimate-form-panel">
        <label class="upload-dropzone estimate-dropzone">
          <input type="file" accept="image/*" multiple @change="onSelectFiles" />
          <strong>{{ uploading ? '图片上传中...' : '上传商品图片' }}</strong>
          <span>支持 JPG / PNG / WEBP，最多 4 张，首张图会作为估价主图参考。</span>
        </label>

        <div v-if="uploadedImages.length" class="estimate-gallery">
          <img
            v-for="(image, index) in uploadedImages"
            :key="`${image}-${index}`"
            :src="resolveAssetUrl(image, 'goods')"
            :alt="`估价图片 ${index + 1}`"
            loading="lazy"
            @error="onImageError"
          />
        </div>

        <form class="form-grid form-grid--2" @submit.prevent="onEstimate">
          <label>
            <span>商品名称</span>
            <input v-model.trim="form.title" placeholder="例如：Rolex Datejust 蓝盘腕表" />
          </label>
          <label>
            <span>品牌</span>
            <input v-model.trim="form.brand" placeholder="请输入品牌" />
          </label>
          <label>
            <span>分类</span>
            <select v-model="form.category">
              <option v-for="item in categories" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label>
            <span>成色</span>
            <select v-model="form.condition">
              <option>99 新</option>
              <option>95 新</option>
              <option>90 新</option>
              <option>85 新</option>
            </select>
          </label>
          <label>
            <span>使用年限</span>
            <input v-model.number="form.yearsUsed" min="0" max="10" type="number" />
          </label>
          <label>
            <span>稀缺度</span>
            <input v-model.number="form.rarity" min="1" max="5" type="range" />
          </label>
          <label class="form-grid__full">
            <span>补充说明</span>
            <textarea v-model.trim="form.description" rows="5" placeholder="可填写附件、购入年份、保卡发票等补充信息" />
          </label>
          <button class="primary-btn primary-btn--full" type="submit" :disabled="loading">
            {{ loading ? 'AI 正在分析商品，大约需要 30 秒...' : '开始估价' }}
          </button>
        </form>
      </section>

      <section class="panel estimate-result-panel">
        <div class="estimate-result-panel__top">
          <span class="eyebrow">估价结果</span>
          <h2>{{ store.lastEstimate ? '本次估价已完成' : '等待开始估价' }}</h2>
          <p>
            {{
              store.lastEstimate
                ? store.lastEstimate.summary
                : '上传图片并填写商品信息后，右侧会展示估价区间、置信度与分项分析。'
            }}
          </p>
        </div>

        <div class="estimate-card-grid">
          <article v-for="item in estimateCards" :key="item.label" class="estimate-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>

        <div class="estimate-confidence">
          <div class="estimate-confidence__header">
            <strong>估价置信度</strong>
            <span>{{ store.lastEstimate?.confidence ?? 0 }}%</span>
          </div>
          <div class="estimate-confidence__track">
            <div class="estimate-confidence__value" :style="{ width: `${store.lastEstimate?.confidence ?? 0}%` }" />
          </div>
        </div>

        <div v-if="estimateBreakdown.length" class="estimate-breakdown">
          <article v-for="item in estimateBreakdown" :key="item.label" class="estimate-breakdown__item">
            <div class="estimate-breakdown__row">
              <strong>{{ item.label }}</strong>
              <span>{{ item.value }}%</span>
            </div>
            <div class="estimate-breakdown__track">
              <div class="estimate-breakdown__value" :style="{ width: `${item.value}%` }" />
            </div>
          </article>
        </div>

        <div v-if="store.lastEstimate?.tips?.length" class="estimate-tips">
          <strong>优化建议</strong>
          <ul>
            <li v-for="tip in store.lastEstimate.tips" :key="tip">{{ tip }}</li>
          </ul>
        </div>

        <div class="action-row estimate-actions">
          <RouterLink class="primary-btn" to="/goods/publish">去发布闲置</RouterLink>
          <RouterLink class="ghost-btn" :to="appraiseLink">预约鉴定</RouterLink>
        </div>
      </section>
    </div>
  </section>
</template>

<style scoped>
.estimate-page {
  gap: 20px;
}

.estimate-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(360px, 0.88fr);
  gap: 18px;
}

.estimate-form-panel,
.estimate-result-panel {
  display: grid;
  gap: 18px;
}

.estimate-dropzone {
  min-height: 150px;
}

.estimate-gallery {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.estimate-gallery img {
  width: 100%;
  height: 110px;
  display: block;
  object-fit: cover;
  border-radius: 18px;
  background: #f8fafc;
}

.estimate-result-panel__top h2 {
  margin: 6px 0 10px;
  font-size: 28px;
  color: #0f172a;
}

.estimate-result-panel__top p {
  margin: 0;
  color: #64748b;
  line-height: 1.8;
}

.estimate-card-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.estimate-card {
  display: grid;
  gap: 8px;
  padding: 18px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(232, 245, 236, 0.92), #fff);
}

.estimate-card span {
  color: #64748b;
  font-size: 13px;
}

.estimate-card strong {
  color: #0f172a;
  font-size: 22px;
  line-height: 1.4;
}

.estimate-confidence,
.estimate-tips,
.estimate-breakdown {
  display: grid;
  gap: 12px;
}

.estimate-confidence__header,
.estimate-breakdown__row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.estimate-confidence__header strong,
.estimate-breakdown__row strong,
.estimate-tips strong {
  color: #0f172a;
  font-size: 14px;
}

.estimate-confidence__header span,
.estimate-breakdown__row span {
  color: #2e9e5b;
  font-weight: 700;
}

.estimate-confidence__track,
.estimate-breakdown__track {
  width: 100%;
  height: 12px;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.16);
  overflow: hidden;
}

.estimate-confidence__value,
.estimate-breakdown__value {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(135deg, #1b6b3a, #2e9e5b);
}

.estimate-tips ul {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}

.estimate-actions {
  justify-content: flex-start;
}

@media (max-width: 1199px) {
  .estimate-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 767px) {
  .estimate-gallery,
  .estimate-card-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
