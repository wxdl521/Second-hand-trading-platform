<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { estimateGoods, getLatestGoodsEstimate } from '@/api/ai'
import { createAppraiseAppointment } from '@/api/appraise'
import { uploadFile } from '@/api/file'
import {
  getGoodsDetail,
  listGoodsCategories,
  publishGoods,
  publishOwnedGoods,
  saveGoodsDraft,
  updateOwnedGoods
} from '@/api/goods'
import { useAppStore } from '@/stores/app'
import type { AppraiseAppointment, AppraiseMode, EstimateResult, GoodsItem } from '@/types'

const store = useAppStore()
const route = useRoute()
const router = useRouter()
const currentStep = ref(0)
const uploading = ref(false)
const pricingLoading = ref(false)
const submitting = ref(false)
const imageUrls = ref<string[]>([])
const categories = ref<string[]>([])
const appraisalMode = ref<'AI 快速估价' | '视频连线鉴定' | '到店复核'>('AI 快速估价')
const editingGoodsStatus = ref<GoodsItem['status']>('草稿')
const latestEstimate = ref<Awaited<ReturnType<typeof getLatestGoodsEstimate>>>(null)
const draftEstimate = ref<EstimateResult | null>(null)

const appraisalTimeSlots = ['10:00', '14:00', '16:00', '19:30'] as const

const formatDateInput = (date: Date) => {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const today = formatDateInput(new Date())

const form = reactive({
  title: '',
  category: '',
  brand: '',
  condition: '95 新',
  price: 9800,
  description: '',
  story: '',
  tags: '成色优秀,支持鉴定,附件齐全'
})

const appointmentForm = reactive({
  date: today,
  timeSlot: '14:00' as (typeof appraisalTimeSlots)[number],
  note: ''
})

const steps = ['上传图片', '填写信息', '定价建议', '鉴定服务', '确认发布']
const goodsId = computed(() => (typeof route.query.goodsId === 'string' ? route.query.goodsId : ''))
const isEditing = computed(() => Boolean(goodsId.value))
const publishActionLabel = computed(() =>
  isEditing.value && ['草稿', '已下架'].includes(editingGoodsStatus.value) ? '更新并提交审核' : isEditing.value ? '保存修改' : '确认发布'
)
const canSaveDraft = computed(() => Boolean(form.title && form.brand && form.category))
const estimateResult = computed<EstimateResult | null>(() => draftEstimate.value ?? latestEstimate.value ?? store.lastEstimate ?? null)
const canGenerateEstimate = computed(() => Boolean(form.title && form.brand && form.category && imageUrls.value.length))
const requiresManualAppointment = computed(() => appraisalMode.value !== 'AI 快速估价')
const appointmentMode = computed<AppraiseMode | null>(() => {
  if (appraisalMode.value === '视频连线鉴定') {
    return '视频连线鉴定'
  }
  if (appraisalMode.value === '到店复核') {
    return '线下到店鉴定'
  }
  return null
})
const appointmentBookingTime = computed(() => `${appointmentForm.date} ${appointmentForm.timeSlot}`)
const appointmentSummary = computed(() => {
  if (!requiresManualAppointment.value) {
    return '当前选择 AI 快速估价，正式发布后不会额外创建人工鉴定预约。'
  }

  const note = appointmentForm.note.trim()
  return note
    ? `${appraisalMode.value} · ${appointmentBookingTime.value} · ${note}`
    : `${appraisalMode.value} · ${appointmentBookingTime.value}`
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

  if (goodsId.value) {
    const goods = await getGoodsDetail(goodsId.value)

    if (!goods) {
      alert('未找到要编辑的商品。')
      router.replace('/goods/manage')
      return
    }

    latestEstimate.value = await getLatestGoodsEstimate(goodsId.value)
    editingGoodsStatus.value = goods.status
    form.title = goods.title
    form.category = goods.category
    form.brand = goods.brand
    form.condition = goods.condition
    form.price = goods.price
    form.description = goods.description
    form.story = goods.story
    form.tags = goods.tags.join(',')
    imageUrls.value = [...goods.gallery]
    if (!categories.value.includes(goods.category)) {
      categories.value = [goods.category, ...categories.value]
    }
  }
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

watch(
  [() => form.title, () => form.category, () => form.brand, () => form.condition, () => form.description, () => imageUrls.value.join('|')],
  () => {
    draftEstimate.value = null
  }
)

const priceSuggestion = computed(() => {
  const estimateSource = estimateResult.value

  if (estimateSource) {
    return {
      low: estimateSource.low,
      high: estimateSource.high,
      median: estimateSource.price,
      confidence: estimateSource.confidence,
      carbonSavedKg: estimateSource.carbonSavedKg,
      summary: estimateSource.summary,
      tips: estimateSource.tips,
      label: `AI 建议区间 ￥${estimateSource.low.toLocaleString()} ~ ￥${estimateSource.high.toLocaleString()}`
    }
  }

  const low = Math.round(Math.max(3000, form.price * 0.95))
  const high = Math.round(form.price * 1.08)
  return {
    low,
    high,
    median: Math.round((low + high) / 2),
    confidence: 0,
    carbonSavedKg: 0,
    summary: '这一阶段会基于商品信息、图片和成色生成 AI 定价建议，可作为正式发布前的参考区间。',
    tips: ['上传更多细节图可提高估价准确性。', '补充附件、保卡和购入时间，能帮助模型判断成色与稀缺度。'],
    label: `建议上架区间 ￥${low.toLocaleString()} ~ ￥${high.toLocaleString()}`
  }
})

const canNext = computed(() => {
  if (currentStep.value === 0) return imageUrls.value.length > 0
  if (currentStep.value === 1) return Boolean(form.title && form.brand && form.description && form.category)
  if (currentStep.value === 3 && requiresManualAppointment.value) {
    return Boolean(appointmentForm.date && appointmentForm.timeSlot)
  }
  return true
})

const resolveEstimateYearsUsed = () => {
  if (form.condition === '99 新') return 0
  if (form.condition === '95 新') return 1
  if (form.condition === '90 新') return 2
  return 4
}

const resolveEstimateRarity = () => {
  const rawText = `${form.title} ${form.story} ${form.description} ${form.tags}`
  if (/(限量|联名|绝版|编号|收藏|稀缺)/.test(rawText)) {
    return 5
  }
  if (/(经典|热门|保值|专柜|全套)/.test(rawText)) {
    return 4
  }
  if (form.condition === '99 新') {
    return 4
  }
  if (form.condition === '95 新') {
    return 3
  }
  return 2
}

const isPastAppointmentTime = () => {
  if (!requiresManualAppointment.value) {
    return false
  }

  const bookingDate = new Date(`${appointmentForm.date}T${appointmentForm.timeSlot}:00`)
  return Number.isNaN(bookingDate.getTime()) || bookingDate.getTime() < Date.now()
}

const onGenerateEstimate = async () => {
  if (!canGenerateEstimate.value) {
    alert('请先补全标题、品牌、分类，并至少上传 1 张图片后再生成 AI 定价建议。')
    return
  }

  pricingLoading.value = true

  try {
    draftEstimate.value = await estimateGoods({
      title: form.title,
      category: form.category,
      brand: form.brand,
      condition: form.condition,
      description: [form.story, form.description].filter(Boolean).join('；'),
      imageUrls: imageUrls.value,
      yearsUsed: resolveEstimateYearsUsed(),
      rarity: resolveEstimateRarity()
    })
  } catch (error) {
    alert((error as Error).message)
  } finally {
    pricingLoading.value = false
  }
}

const onApplyEstimatePrice = () => {
  if (!estimateResult.value) {
    alert('请先生成 AI 定价建议。')
    return
  }

  form.price = estimateResult.value.price
}

const onNext = () => {
  if (!canNext.value) {
    alert('请先补全当前步骤内容。')
    return
  }
  currentStep.value = Math.min(steps.length - 1, currentStep.value + 1)
}

const onPrev = () => {
  currentStep.value = Math.max(0, currentStep.value - 1)
}

const onSelectFiles = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  if (!files.length) return

  const nextFiles = files.slice(0, Math.max(0, 6 - imageUrls.value.length))
  if (!nextFiles.length) {
    alert('最多上传 6 张图片。')
    input.value = ''
    return
  }

  uploading.value = true
  try {
    const uploaded = await Promise.all(nextFiles.map((file) => uploadFile(file)))
    imageUrls.value = [...imageUrls.value, ...uploaded.map((item) => item.url)]
  } catch (error) {
    alert((error as Error).message)
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const removeImage = (index: number) => {
  imageUrls.value = imageUrls.value.filter((_, current) => current !== index)
}

const buildPayload = () => ({
  title: form.title,
  category: form.category,
  brand: form.brand,
  condition: form.condition,
  price: form.price,
  description: form.description,
  story: form.story,
  tags: form.tags
    .split(/[,，]/)
    .map((entry) => entry.trim())
    .filter(Boolean),
  imageUrls: imageUrls.value
})

const onSaveDraft = async () => {
  if (!canSaveDraft.value) {
    alert('请先填写标题、品牌和分类后再保存草稿。')
    return
  }

  submitting.value = true
  try {
    if (isEditing.value) {
      await updateOwnedGoods(goodsId.value, buildPayload())
    } else {
      await saveGoodsDraft(buildPayload())
    }
    alert('草稿已保存。')
    router.push('/goods/manage')
  } catch (error) {
    alert((error as Error).message)
  } finally {
    submitting.value = false
  }
}

const createPublishAppointment = async (goodsTitle: string): Promise<AppraiseAppointment | null> => {
  if (!requiresManualAppointment.value || !appointmentMode.value) {
    return null
  }

  return createAppraiseAppointment({
    goodsTitle,
    mode: appointmentMode.value,
    bookingTime: appointmentBookingTime.value,
    note: appointmentForm.note.trim()
  })
}

const onSubmit = async () => {
  if (!form.title || !form.brand || !form.description || !form.category) {
    alert('请先完善商品信息。')
    return
  }

  if (!imageUrls.value.length) {
    alert('请至少上传 1 张商品图片。')
    return
  }

  if (isPastAppointmentTime()) {
    alert('鉴定预约时间不能早于当前时间，请重新选择。')
    return
  }

  submitting.value = true
  try {
    const item = isEditing.value
      ? await updateOwnedGoods(goodsId.value, buildPayload())
      : await publishGoods(buildPayload())
    const finalItem =
      isEditing.value && ['草稿', '已下架'].includes(editingGoodsStatus.value)
        ? await publishOwnedGoods(item.id)
        : item

    let appointment: AppraiseAppointment | null = null

    if (requiresManualAppointment.value) {
      try {
        appointment = await createPublishAppointment(finalItem.title)
      } catch (error) {
        const message = error instanceof Error ? error.message : '未知错误'
        alert(`商品已提交成功，但鉴定预约创建失败：${message}`)
        editingGoodsStatus.value = finalItem.status
        router.push(`/goods/${finalItem.id}`)
        return
      }
    }

    const submitMessage = isEditing.value
      ? ['草稿', '已下架'].includes(editingGoodsStatus.value)
        ? '商品已更新并重新提交审核。'
        : '商品信息已更新。'
      : '发布成功，商品已进入审核流程。'

    alert(
      appointment
        ? `${submitMessage} 已同步创建${appraisalMode.value}预约（${appointment.id}）。`
        : submitMessage
    )
    editingGoodsStatus.value = finalItem.status
    router.push(`/goods/${finalItem.id}`)
  } catch (error) {
    alert((error as Error).message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="publish-page">
    <div class="section-head">
      <div>
        <span class="eyebrow">五步发布向导</span>
        <h1>{{ isEditing ? '编辑我的商品' : '把你的高价值闲置交给平台' }}</h1>
        <p>{{ isEditing ? '支持继续编辑草稿、重新送审和更新在售商品信息。' : '按图片、信息、定价、鉴定、确认流程完成发布。' }}</p>
      </div>
      <RouterLink class="ghost-btn" to="/goods/manage">我的商品</RouterLink>
    </div>

    <div class="publish-steps">
      <div
        v-for="(item, index) in steps"
        :key="item"
        class="publish-step"
        :class="{ 'publish-step--active': index === currentStep }"
      >
        <span>{{ index + 1 }}</span>
        <strong>{{ item }}</strong>
      </div>
    </div>

    <section class="panel publish-panel">
      <template v-if="currentStep === 0">
        <div class="publish-panel__head">
          <div>
            <h2>上传商品图片</h2>
            <p>支持 JPEG / PNG / WEBP，最多 6 张，首图将作为商品主图。</p>
          </div>
          <label class="upload-trigger">
            <input class="upload-input" type="file" accept="image/*" multiple @change="onSelectFiles" />
            <span>{{ uploading ? '上传中...' : '选择图片' }}</span>
          </label>
        </div>

        <div v-if="imageUrls.length" class="preview-grid">
          <div v-for="(url, index) in imageUrls" :key="`${url}-${index}`" class="preview-card">
            <img :src="url" :alt="`商品图片 ${index + 1}`" />
            <button type="button" class="preview-card__remove" @click="removeImage(index)">移除</button>
          </div>
        </div>

        <div v-else class="upload-grid">
          <div class="upload-placeholder">主图</div>
          <div class="upload-placeholder">细节图</div>
          <div class="upload-placeholder">附件图</div>
        </div>
      </template>

      <template v-else-if="currentStep === 1">
        <h2>填写商品信息</h2>
        <div class="form-grid form-grid--2">
          <label>
            <span>商品标题</span>
            <input v-model="form.title" placeholder="例如：Chanel CF 中号链条包" />
          </label>
          <label>
            <span>品牌</span>
            <input v-model="form.brand" placeholder="请输入品牌" />
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
          <label class="form-grid__full">
            <span>商品亮点</span>
            <textarea v-model="form.story" rows="3" placeholder="一句话描述核心卖点" />
          </label>
          <label class="form-grid__full">
            <span>详细描述</span>
            <textarea v-model="form.description" rows="5" placeholder="填写成色、附件、使用历史等信息" />
          </label>
          <label class="form-grid__full">
            <span>标签</span>
            <input v-model="form.tags" placeholder="多个标签用中文或英文逗号分隔" />
          </label>
        </div>
      </template>

      <template v-else-if="currentStep === 2">
        <h2>定价建议</h2>
        <div class="publish-pricing">
          <label>
            <span>期望售价</span>
            <input v-model.number="form.price" min="1000" step="100" type="number" />
          </label>
          <div class="panel panel--soft publish-pricing__panel">
            <strong>{{ priceSuggestion.label }}</strong>
            <p v-if="false">
              {{
                latestEstimate
                  ? latestEstimate.summary
                  : store.lastEstimate
                    ? store.lastEstimate.summary
                    : '这一阶段对应 AI 定价建议，可作为正式发布前的参考区间。'
              }}
            </p>
            <div v-if="false" class="publish-pricing__meta">
              <span>置信度 {{ latestEstimate?.confidence ?? store.lastEstimate?.confidence ?? 0 }}%</span>
              <span>减碳 {{ latestEstimate?.carbonSavedKg ?? store.lastEstimate?.carbonSavedKg ?? 0 }} kgCO₂e</span>
            </div>
            <p>{{ priceSuggestion.summary }}</p>
            <div class="publish-pricing__meta">
              <span>建议中位价 ￥{{ priceSuggestion.median.toLocaleString() }}</span>
              <span>置信度 {{ priceSuggestion.confidence }}%</span>
              <span>减碳 {{ priceSuggestion.carbonSavedKg }} kgCO2e</span>
            </div>
            <div class="publish-pricing__actions">
              <button class="primary-btn" type="button" @click="onGenerateEstimate" :disabled="pricingLoading">
                {{ pricingLoading ? 'AI 估价中...' : '生成 AI 定价建议' }}
              </button>
              <button class="ghost-btn" type="button" @click="onApplyEstimatePrice" :disabled="!estimateResult">
                采用中位价
              </button>
            </div>
            <ul class="publish-pricing__tips">
              <li v-for="tip in priceSuggestion.tips" :key="tip">{{ tip }}</li>
            </ul>
          </div>
        </div>
      </template>

      <template v-else-if="currentStep === 3">
        <h2>鉴定服务</h2>
        <div class="feature-grid publish-services">
          <button
            v-for="item in ['AI 快速估价', '视频连线鉴定', '到店复核']"
            :key="item"
            class="panel panel--soft publish-service"
            :class="{ 'publish-service--active': appraisalMode === item }"
            type="button"
            @click="appraisalMode = item as typeof appraisalMode.value"
          >
            <h3>{{ item }}</h3>
            <p>
              {{
                item === 'AI 快速估价'
                  ? '适合先获得价格参考，最快 1 分钟内完成。'
                  : item === '视频连线鉴定'
                    ? '适合箱包、腕表等高客单商品。'
                    : '适合收藏级商品或需要附件复核的交易场景。'
              }}
            </p>
          </button>
        </div>
        <div class="panel panel--soft publish-appraisal-panel">
          <div class="publish-appraisal-panel__summary">
            <strong>{{ requiresManualAppointment ? '人工鉴定预约信息' : '当前无需额外预约' }}</strong>
            <p>{{ appointmentSummary }}</p>
          </div>

          <div v-if="requiresManualAppointment" class="form-grid form-grid--2">
            <label>
              <span>预约日期</span>
              <input v-model="appointmentForm.date" :min="today" type="date" />
            </label>
            <label>
              <span>预约时段</span>
              <select v-model="appointmentForm.timeSlot">
                <option v-for="item in appraisalTimeSlots" :key="item" :value="item">{{ item }}</option>
              </select>
            </label>
            <label class="form-grid__full">
              <span>备注说明</span>
              <textarea
                v-model.trim="appointmentForm.note"
                rows="4"
                placeholder="可填写附件、保卡、希望重点查看的细节，发布成功后会同步带入鉴定预约。"
              />
            </label>
          </div>
        </div>
      </template>

      <template v-else>
        <h2>确认发布</h2>
        <div class="summary-grid publish-summary">
          <div><strong>标题</strong><span>{{ form.title || '未填写' }}</span></div>
          <div><strong>品牌</strong><span>{{ form.brand || '未填写' }}</span></div>
          <div><strong>分类</strong><span>{{ form.category || '未填写' }}</span></div>
          <div><strong>成色</strong><span>{{ form.condition }}</span></div>
          <div><strong>售价</strong><span>￥{{ form.price.toLocaleString() }}</span></div>
          <div><strong>图片数</strong><span>{{ imageUrls.length }} 张</span></div>
          <div><strong>鉴定方式</strong><span>{{ appraisalMode }}</span></div>
          <div><strong>AI 定价</strong><span>{{ estimateResult ? `￥${estimateResult.low.toLocaleString()} ~ ￥${estimateResult.high.toLocaleString()}` : '尚未生成' }}</span></div>
          <div><strong>预约信息</strong><span>{{ requiresManualAppointment ? appointmentBookingTime : '无需额外预约' }}</span></div>
          <div><strong>标签</strong><span>{{ form.tags }}</span></div>
        </div>
        <div class="panel panel--soft publish-summary__note">
          <strong>发布后动作</strong>
          <p>
            {{
              requiresManualAppointment
                ? '商品提交成功后，会自动创建对应的人工鉴定预约，方便审核和成交前复核同步推进。'
                : '商品提交成功后，将沿用当前 AI 定价建议进入审核流程，你也可以稍后再补做人工鉴定。'
            }}
          </p>
        </div>
      </template>

      <div class="action-row publish-actions">
        <button class="ghost-btn" type="button" @click="onPrev" :disabled="currentStep === 0 || submitting">上一步</button>
        <button class="ghost-btn" type="button" @click="onSaveDraft" :disabled="!canSaveDraft || submitting">保存草稿</button>
        <button v-if="currentStep < steps.length - 1" class="primary-btn" type="button" @click="onNext">下一步</button>
        <button v-else class="primary-btn" type="button" @click="onSubmit" :disabled="submitting">{{ publishActionLabel }}</button>
      </div>
    </section>
  </section>
</template>

<style scoped>
.publish-page {
  display: grid;
  gap: 18px;
}

.publish-page p,
.publish-panel__head h2,
.publish-panel__head p {
  margin: 0;
}

.publish-steps {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.publish-step {
  min-height: 88px;
  padding: 16px 18px;
  border-radius: 24px;
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.86);
  display: grid;
  gap: 6px;
}

.publish-step span {
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
}

.publish-step--active {
  background: var(--brand-soft);
  border-color: rgba(27, 107, 58, 0.2);
}

.publish-panel {
  display: grid;
  gap: 22px;
}

.publish-panel__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.upload-input {
  display: none;
}

.upload-trigger {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 132px;
  padding: 12px 18px;
  border-radius: 999px;
  background: var(--brand-soft);
  color: var(--brand);
  font-weight: 600;
  cursor: pointer;
}

.preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
}

.preview-card {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.preview-card img {
  width: 100%;
  height: 160px;
  object-fit: cover;
  border-radius: 18px;
}

.preview-card__remove {
  border: none;
  background: transparent;
  color: #cc4b37;
  cursor: pointer;
}

.publish-pricing {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 18px;
  align-items: start;
}

.publish-pricing__panel {
  min-height: 132px;
  display: grid;
  gap: 14px;
}

.publish-pricing__meta {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  color: var(--muted);
}

.publish-pricing__actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.publish-pricing__tips {
  margin: 0;
  padding-left: 18px;
  color: var(--muted);
  display: grid;
  gap: 8px;
}

.publish-services {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.publish-service {
  text-align: left;
}

.publish-service--active {
  border-color: rgba(27, 107, 58, 0.2);
  background: var(--brand-soft);
}

.publish-appraisal-panel {
  display: grid;
  gap: 16px;
}

.publish-appraisal-panel__summary {
  display: grid;
  gap: 8px;
}

.publish-summary {
  row-gap: 18px;
}

.publish-summary__note {
  display: grid;
  gap: 8px;
}

.publish-actions {
  justify-content: space-between;
}

@media (max-width: 992px) {
  .publish-steps,
  .publish-services,
  .publish-pricing {
    grid-template-columns: 1fr;
  }
}
</style>
