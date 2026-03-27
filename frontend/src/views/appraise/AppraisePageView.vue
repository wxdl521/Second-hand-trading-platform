<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  createAppraiseAppointment,
  listAppraiseAppointments,
  normalizeAppraiseMode
} from '@/api/appraise'
import { useAppStore } from '@/stores/app'
import type { AppraiseAppointment, AppraiseMode } from '@/types'

const route = useRoute()
const store = useAppStore()

const timeSlots = ['10:00', '14:00', '16:00', '19:30'] as const

const formatDateInput = (date: Date) => {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const today = formatDateInput(new Date())

const modeCards: Array<{
  mode: AppraiseMode
  title: string
  price: string
  eta: string
  desc: string
}> = [
  {
    mode: 'AI 快速鉴定',
    title: 'AI 快速鉴定',
    price: '￥39',
    eta: '约 30 分钟',
    desc: '适合先做真伪和成色初筛，完成后可继续去发布闲置或升级人工复核。'
  },
  {
    mode: '视频连线鉴定',
    title: '视频连线鉴定',
    price: '￥199',
    eta: '当天可约',
    desc: '支持连线查看五金、走时、编号与细节，适合高客单价商品快速成交。'
  },
  {
    mode: '线下到店鉴定',
    title: '线下到店鉴定',
    price: '￥299',
    eta: '预约到店',
    desc: '适合珠宝腕表与高价奢品，现场完成复检并生成平台记录。'
  }
]

const form = reactive({
  goodsTitle: '',
  mode: '视频连线鉴定' as AppraiseMode,
  date: today,
  timeSlot: '14:00' as (typeof timeSlots)[number],
  note: ''
})

const submitting = ref(false)
const lastSubmitted = ref<AppraiseAppointment | null>(null)

const normalizeRouteDate = (value: string) =>
  /^\d{4}-\d{2}-\d{2}$/.test(value) && value >= today ? value : today

const applyRoutePreset = () => {
  const title =
    typeof route.query.title === 'string'
      ? route.query.title.trim()
      : typeof route.query.goodsTitle === 'string'
        ? route.query.goodsTitle.trim()
        : ''
  const mode = typeof route.query.mode === 'string' ? normalizeAppraiseMode(route.query.mode) : null
  const date = typeof route.query.date === 'string' ? normalizeRouteDate(route.query.date) : null
  const time =
    typeof route.query.time === 'string' &&
    timeSlots.includes(route.query.time as (typeof timeSlots)[number])
      ? (route.query.time as (typeof timeSlots)[number])
      : null
  const note = typeof route.query.note === 'string' ? route.query.note.trim() : ''

  if (title) {
    form.goodsTitle = title
  }
  if (mode) {
    form.mode = mode
  }
  if (date) {
    form.date = date
  }
  if (time) {
    form.timeSlot = time
  }
  if (note) {
    form.note = note
  }
}

watch(
  () => route.query,
  () => {
    applyRoutePreset()
  },
  { immediate: true }
)

onMounted(async () => {
  await listAppraiseAppointments()
})

const activeMode = computed(() => modeCards.find((item) => item.mode === form.mode) ?? modeCards[0])
const bookingTime = computed(() => `${form.date} ${form.timeSlot}`)

const selectionTip = computed(() => {
  if (form.mode === 'AI 快速鉴定') {
    return 'AI 档期提交后通常会直接锁定，适合估价完成后立即补做快审。'
  }
  if (form.mode === '视频连线鉴定') {
    return '视频时段单场最多 6 单，若当前时段较满会自动进入待确认。'
  }
  return '到店鉴定单时段容量更紧张，建议至少提前一天预约。'
})

const appointmentStats = computed(() => [
  { label: '累计预约', value: String(store.appointments.length) },
  { label: '待确认', value: String(store.appointments.filter((item) => item.status === '待确认').length) },
  { label: '已预约', value: String(store.appointments.filter((item) => item.status === '已预约').length) }
])

const isPastBookingTime = () => {
  const date = new Date(`${form.date}T${form.timeSlot}:00`)
  return Number.isNaN(date.getTime()) || date.getTime() < Date.now()
}

const onSubmit = async () => {
  if (!form.goodsTitle.trim()) {
    alert('请先填写待鉴定商品名称。')
    return
  }
  if (isPastBookingTime()) {
    alert('预约时间不能早于当前时间，请重新选择。')
    return
  }

  submitting.value = true

  try {
    const appointment = await createAppraiseAppointment({
      goodsTitle: form.goodsTitle.trim(),
      mode: form.mode,
      bookingTime: bookingTime.value,
      note: form.note.trim()
    })
    lastSubmitted.value = appointment
    form.goodsTitle = ''
    form.note = ''

    alert(
      appointment.status === '已预约'
        ? `预约已提交，档期已锁定：${appointment.id}`
        : `预约已提交，当前时段较满，平台会尽快确认：${appointment.id}`
    )
  } catch (error) {
    alert((error as Error).message)
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <section class="section-block appraise-page">
    <div class="section-head">
      <div>
        <span class="eyebrow">鉴定预约</span>
        <h1>AI / 视频 / 到店三种模式，覆盖高价值商品鉴定需求</h1>
        <p>已补齐模式选择、预约时间、状态反馈和最近记录，并支持从估价页、商品页带着标题直接续上流程。</p>
      </div>
      <RouterLink class="ghost-btn" to="/estimate">先去估价</RouterLink>
    </div>

    <section class="panel appraise-mode-panel">
      <div class="appraise-mode-grid">
        <button
          v-for="item in modeCards"
          :key="item.mode"
          class="appraise-mode-card"
          :class="{ 'appraise-mode-card--active': form.mode === item.mode }"
          type="button"
          @click="form.mode = item.mode"
        >
          <div class="appraise-mode-card__top">
            <strong>{{ item.title }}</strong>
            <span>{{ item.price }}</span>
          </div>
          <small>{{ item.eta }}</small>
          <p>{{ item.desc }}</p>
        </button>
      </div>
    </section>

    <div class="appraise-layout">
      <section class="panel">
        <div class="section-head section-head--compact">
          <div>
            <span class="eyebrow">新建预约</span>
            <h2>{{ activeMode.title }}</h2>
            <p>{{ activeMode.desc }}</p>
          </div>
        </div>

        <div class="appraise-summary-card">
          <span>当前选择</span>
          <strong>{{ activeMode.title }}</strong>
          <p>{{ bookingTime }} · {{ activeMode.price }}</p>
          <small>{{ selectionTip }}</small>
        </div>

        <form class="form-grid form-grid--2" @submit.prevent="onSubmit">
          <label class="form-grid__full">
            <span>商品名称</span>
            <input v-model.trim="form.goodsTitle" placeholder="请输入待鉴定商品名称" />
          </label>
          <label>
            <span>预约日期</span>
            <input v-model="form.date" :min="today" type="date" />
          </label>
          <label>
            <span>预约时间</span>
            <select v-model="form.timeSlot">
              <option v-for="item in timeSlots" :key="item" :value="item">{{ item }}</option>
            </select>
          </label>
          <label class="form-grid__full">
            <span>补充说明</span>
            <textarea
              v-model.trim="form.note"
              rows="5"
              placeholder="可填写成色、附件、编码、保卡、发票或你最关心的细节"
            />
          </label>
          <button class="primary-btn primary-btn--full" type="submit" :disabled="submitting">
            {{ submitting ? '提交预约中...' : '提交预约' }}
          </button>
        </form>

        <div
          v-if="lastSubmitted"
          class="appraise-feedback"
          :class="{ 'appraise-feedback--pending': lastSubmitted.status === '待确认' }"
        >
          <strong>{{ lastSubmitted.status === '已预约' ? '预约已锁定' : '预约待确认' }}</strong>
          <p>
            {{
              lastSubmitted.status === '已预约'
                ? '平台已为你预留该档期，可按预约时间完成鉴定。'
                : '该时段当前较满，平台会在空出名额后优先与你确认。'
            }}
          </p>
          <small>预约编号：{{ lastSubmitted.id }}</small>
        </div>
      </section>

      <section class="panel appraise-side-panel">
        <div class="appraise-stats">
          <article v-for="item in appointmentStats" :key="item.label" class="appraise-stat">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </article>
        </div>

        <div class="appraise-service">
          <strong>排期说明</strong>
          <ul>
            <li>AI 快速鉴定优先进入快审队列，适合估价后立刻补做真伪初筛。</li>
            <li>视频连线鉴定支持放大查看五金、走时、编号与磨损细节。</li>
            <li>线下到店鉴定适合珠宝腕表与高价奢品，现场可完成更完整复检。</li>
          </ul>
        </div>
      </section>
    </div>

    <section class="panel">
      <div class="section-head section-head--compact">
        <div>
          <span class="eyebrow">预约记录</span>
          <h2>最近的鉴定安排</h2>
        </div>
      </div>

      <div v-if="store.appointments.length" class="appraise-records">
        <article v-for="item in store.appointments" :key="item.id" class="appraise-record">
          <div class="appraise-record__content">
            <strong>{{ item.goodsTitle }}</strong>
            <p>{{ item.mode }} · {{ item.date }}</p>
            <small>{{ item.note || '暂无补充说明' }}</small>
          </div>
          <span class="badge" :class="item.status === '已预约' ? 'badge--success' : 'badge--pending'">
            {{ item.status }}
          </span>
        </article>
      </div>

      <div v-else class="empty-card">
        <h3>还没有预约记录</h3>
        <p>先创建一笔鉴定预约，平台会把排期和状态更新集中展示在这里。</p>
      </div>
    </section>
  </section>
</template>

<style scoped>
.appraise-page {
  gap: 20px;
}

.appraise-mode-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.appraise-mode-card {
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: #fff;
  border-radius: 22px;
  padding: 20px;
  text-align: left;
  display: grid;
  gap: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.appraise-mode-card--active {
  border-color: rgba(46, 158, 91, 0.34);
  background: linear-gradient(180deg, rgba(232, 245, 236, 0.96), #fff);
  box-shadow: 0 16px 28px rgba(27, 107, 58, 0.12);
}

.appraise-mode-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.appraise-mode-card__top strong {
  color: #0f172a;
  font-size: 16px;
}

.appraise-mode-card__top span {
  color: #2e9e5b;
  font-weight: 700;
}

.appraise-mode-card small,
.appraise-mode-card p {
  color: #64748b;
}

.appraise-mode-card p {
  margin: 0;
  line-height: 1.75;
}

.appraise-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.08fr) minmax(300px, 0.92fr);
  gap: 18px;
}

.appraise-summary-card,
.appraise-side-panel,
.appraise-stats,
.appraise-service,
.appraise-feedback {
  display: grid;
  gap: 14px;
}

.appraise-summary-card {
  margin-bottom: 18px;
  padding: 18px 20px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(232, 245, 236, 0.92), #fff);
}

.appraise-stats {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.appraise-stat {
  padding: 18px;
  border-radius: 20px;
  background: rgba(248, 250, 252, 0.96);
  display: grid;
  gap: 8px;
}

.appraise-summary-card span,
.appraise-feedback small,
.appraise-stat span {
  color: #64748b;
  font-size: 13px;
}

.appraise-summary-card strong,
.appraise-feedback strong,
.appraise-service strong,
.appraise-stat strong {
  color: #0f172a;
}

.appraise-stat strong {
  font-size: 22px;
}

.appraise-summary-card p,
.appraise-feedback p {
  margin: 0;
  color: #475569;
  line-height: 1.8;
}

.appraise-feedback {
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(232, 245, 236, 0.9);
}

.appraise-feedback--pending {
  background: rgba(255, 247, 237, 0.95);
}

.appraise-service ul {
  margin: 0;
  padding-left: 18px;
  color: #475569;
  line-height: 1.8;
}

.appraise-records {
  display: grid;
  gap: 14px;
}

.appraise-record {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.16);
}

.appraise-record:last-child {
  padding-bottom: 0;
  border-bottom: none;
}

.appraise-record__content {
  display: grid;
  gap: 6px;
}

.appraise-record strong {
  color: #0f172a;
  font-size: 16px;
}

.appraise-record p,
.appraise-record small {
  margin: 0;
  color: #64748b;
}

.badge--success {
  background: rgba(46, 158, 91, 0.14);
  color: #1b6b3a;
}

.badge--pending {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

@media (max-width: 1199px) {
  .appraise-mode-grid,
  .appraise-layout,
  .appraise-stats {
    grid-template-columns: 1fr;
  }
}
</style>
