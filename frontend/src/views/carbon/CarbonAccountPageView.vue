<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { apiClient } from '@/api'
import { listCarbonRecords } from '@/api/carbon'
import { listOrders } from '@/api/order'
import { useAppStore } from '@/stores/app'

const store = useAppStore()

const downloading = ref(false)
const certificateError = ref('')

const levelRules = [
  { level: 'Lv.1 绿色新手', min: 0, max: 999 },
  { level: 'Lv.2 低碳买家', min: 1000, max: 1999 },
  { level: 'Lv.3 循环达人', min: 2000, max: 3499 },
  { level: 'Lv.4 碳账户先锋', min: 3500, max: 4999 },
  { level: 'Lv.5 绿色合伙人', min: 5000, max: Number.POSITIVE_INFINITY }
]

onMounted(async () => {
  await Promise.all([listCarbonRecords(), listOrders()])
})

const positiveRecords = computed(() => store.carbonRecords.filter((item) => item.type === '收入'))
const totalPoints = computed(() => store.carbonBalance)
const monthCarbonSavedKg = computed(() => store.monthCarbonSavedKg)
const levelLabel = computed(() => store.carbonLevel)

const totalCarbonKg = computed(() => {
  return Number(
    positiveRecords.value
      .reduce((sum, item) => {
        const matched = item.description.match(/(\d+(?:\.\d+)?)kg/i)
        const carbonKg = matched ? Number(matched[1]) : Number((item.points / 15).toFixed(2))
        return sum + carbonKg
      }, 0)
      .toFixed(2)
  )
})

const levelInfo = computed(() => {
  const matched = levelRules.find((item) => totalPoints.value >= item.min && totalPoints.value <= item.max)
  return matched ?? { level: levelLabel.value, min: 0, max: Number.POSITIVE_INFINITY }
})

const nextLevel = computed(() => levelRules.find((item) => item.min > totalPoints.value) ?? null)

const levelProgress = computed(() => {
  if (!nextLevel.value) {
    return 100
  }

  const span = nextLevel.value.min - levelInfo.value.min
  const completed = totalPoints.value - levelInfo.value.min
  return Math.min(100, Math.max(10, Math.round((completed / span) * 100)))
})

const completedOrders = computed(() =>
  store.orders.filter((item) => item.buyerName === store.currentUser?.name && item.status === '已完成')
)

const carbonStats = computed(() => [
  { label: '当前积分', value: `${totalPoints.value}` },
  { label: '累计减碳', value: `${totalCarbonKg.value} kg` },
  { label: '已完成订单', value: `${completedOrders.value.length}` },
  { label: '本月减碳', value: `${Number(monthCarbonSavedKg.value.toFixed(2))} kg` }
])

const recordRows = computed(() =>
  store.carbonRecords.map((item) => {
    const matched = item.description.match(/(\d+(?:\.\d+)?)kg/i)
    const carbonKg = matched ? Number(matched[1]) : Number((item.points / 15).toFixed(2))

    return {
      ...item,
      carbonKg,
      sign: item.type === '收入' ? '+' : '-'
    }
  })
)

const downloadBlob = (blob: Blob, filename: string) => {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  link.click()
  URL.revokeObjectURL(url)
}

const onDownloadCertificate = async () => {
  downloading.value = true
  certificateError.value = ''

  try {
    const response = await apiClient.post('/carbon/certificate', {}, { responseType: 'blob' })
    const contentType = String(response.headers['content-type'] ?? '')

    if (contentType.includes('image')) {
      downloadBlob(response.data, `尚有新生-碳减排证书-${new Date().toISOString().slice(0, 10)}.png`)
      return
    }

    throw new Error('remote-certificate-unavailable')
  } catch {
    certificateError.value = '证书生成失败，请稍后重试。'
    alert(certificateError.value)
  } finally {
    downloading.value = false
  }
}
</script>

<template>
  <section class="section-block carbon-page">
    <div class="section-head">
      <div>
        <span class="eyebrow">绿色碳账户</span>
        <h1>环形总览、积分流水与证书下载统一收口</h1>
        <p>账户总览与流水明细按 PRD 对齐，证书由后端生成并落库存储后再提供下载。</p>
      </div>
      <RouterLink class="ghost-btn" to="/profile">返回我的主页</RouterLink>
    </div>

    <div class="carbon-grid">
      <section class="panel carbon-hero-card">
        <div class="carbon-ring" :style="{ '--carbon-progress': `${levelProgress}%` }">
          <div class="carbon-ring__inner">
            <strong>{{ totalPoints }}</strong>
            <span>当前积分</span>
            <small>{{ levelLabel }}</small>
          </div>
        </div>

        <div class="carbon-hero-card__content">
          <div class="carbon-hero-card__headline">
            <h2>{{ levelLabel }}</h2>
            <p v-if="nextLevel">距离下一等级还差 {{ nextLevel.min - totalPoints }} 积分</p>
            <p v-else>已达到最高等级，继续绿色交易可持续累积影响力。</p>
          </div>

          <div class="carbon-stats">
            <article v-for="item in carbonStats" :key="item.label" class="carbon-stat">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </article>
          </div>
        </div>
      </section>

      <section class="panel carbon-certificate-card">
        <div class="section-head section-head--compact">
          <div>
            <span class="eyebrow">证书下载</span>
            <h2>碳减排凭证</h2>
            <p>适合截图分享、活动留档或个人绿色消费记录保存。</p>
          </div>
        </div>

        <div class="carbon-certificate-card__body">
          <div>
            <strong>证书内容</strong>
            <p>包含用户昵称、账户等级、累计积分、累计减碳量与生成日期。</p>
          </div>
          <div>
            <strong>生成方式</strong>
            <p>证书会调用后端接口生成 PNG，并同步保存在平台证书目录中。</p>
          </div>
          <button class="primary-btn carbon-certificate-card__btn" type="button" :disabled="downloading" @click="onDownloadCertificate">
            {{ downloading ? '证书生成中...' : '下载碳减排证书' }}
          </button>
          <p v-if="certificateError" class="carbon-certificate-card__error">{{ certificateError }}</p>
        </div>
      </section>
    </div>

    <section class="panel">
      <div class="section-head section-head--compact">
        <div>
          <span class="eyebrow">积分流水</span>
          <h2>每一笔绿色行为都有记录</h2>
        </div>
        <RouterLink class="ghost-btn" to="/order/list">查看关联订单</RouterLink>
      </div>

      <div class="carbon-records">
        <article v-for="record in recordRows" :key="record.id" class="carbon-record">
          <div class="carbon-record__main">
            <strong>{{ record.title }}</strong>
            <p>{{ record.description }}</p>
          </div>
          <div class="carbon-record__meta">
            <span class="carbon-record__points" :class="{ 'carbon-record__points--income': record.type === '收入' }">
              {{ record.sign }}{{ record.points }}
            </span>
            <small>{{ record.carbonKg }} kgCO2e</small>
            <small>{{ record.date }}</small>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<style scoped>
.carbon-page {
  gap: 20px;
}

.carbon-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(340px, 0.9fr);
  gap: 18px;
}

.carbon-hero-card {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 24px;
  align-items: center;
}

.carbon-ring {
  --carbon-progress: 64%;
  width: 260px;
  height: 260px;
  border-radius: 50%;
  padding: 18px;
  background:
    radial-gradient(circle at center, #fff 59%, transparent 60%),
    conic-gradient(#1b6b3a 0, #2e9e5b var(--carbon-progress), rgba(148, 163, 184, 0.18) var(--carbon-progress), rgba(148, 163, 184, 0.18) 100%);
  display: grid;
  place-items: center;
}

.carbon-ring__inner {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  display: grid;
  place-items: center;
  background: rgba(255, 255, 255, 0.92);
  text-align: center;
  gap: 6px;
}

.carbon-ring__inner strong {
  font-size: 48px;
  color: #0f172a;
}

.carbon-ring__inner span,
.carbon-ring__inner small {
  color: #64748b;
}

.carbon-hero-card__content,
.carbon-certificate-card__body {
  display: grid;
  gap: 18px;
}

.carbon-hero-card__headline {
  display: grid;
  gap: 8px;
}

.carbon-hero-card__headline h2,
.carbon-hero-card__headline p,
.carbon-stat span,
.carbon-stat strong,
.carbon-record__main strong,
.carbon-record__main p,
.carbon-record__meta small,
.carbon-certificate-card__body strong,
.carbon-certificate-card__body p {
  margin: 0;
}

.carbon-hero-card__headline p,
.carbon-certificate-card__body p,
.carbon-record__main p,
.carbon-record__meta small {
  color: #64748b;
}

.carbon-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.carbon-stat {
  padding: 18px;
  border-radius: 22px;
  background: rgba(248, 250, 252, 0.9);
  border: 1px solid rgba(226, 232, 240, 0.9);
  display: grid;
  gap: 8px;
}

.carbon-stat strong {
  font-size: 22px;
  color: #0f172a;
}

.carbon-certificate-card__btn {
  width: 100%;
}

.carbon-certificate-card__error {
  color: #b91c1c;
  font-size: 14px;
}

.carbon-records {
  display: grid;
  gap: 14px;
}

.carbon-record {
  padding: 18px 20px;
  border-radius: 24px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  display: flex;
  justify-content: space-between;
  gap: 16px;
  background: rgba(255, 255, 255, 0.86);
}

.carbon-record__main,
.carbon-record__meta {
  display: grid;
  gap: 8px;
}

.carbon-record__meta {
  min-width: 160px;
  text-align: right;
}

.carbon-record__points {
  font-size: 22px;
  font-weight: 700;
  color: #dc2626;
}

.carbon-record__points--income {
  color: #15803d;
}

@media (max-width: 1024px) {
  .carbon-grid,
  .carbon-hero-card {
    grid-template-columns: 1fr;
  }

  .carbon-ring {
    margin: 0 auto;
  }
}

@media (max-width: 720px) {
  .carbon-stats {
    grid-template-columns: 1fr;
  }

  .carbon-record {
    flex-direction: column;
  }

  .carbon-record__meta {
    min-width: 0;
    text-align: left;
  }
}
</style>
