<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import { getAdminDashboard } from '@admin/api/admin'
import EChartPanel from '@admin/components/EChartPanel.vue'
import { useAdminDataStore } from '@admin/stores/data'
import type { AdminDashboardChartItem, AdminDashboardData } from '@admin/types'

const dataStore = useAdminDataStore()
const loading = ref(false)
const dashboard = ref<AdminDashboardData | null>(null)
const loadError = ref('')

const formatNumber = (value: number) => value.toLocaleString('zh-CN')
const formatCurrency = (value: number) => `¥${value.toLocaleString('zh-CN')}`

onMounted(async () => {
  loading.value = true
  loadError.value = ''

  try {
    dashboard.value = await getAdminDashboard()
  } catch (error) {
    console.warn('Admin dashboard request failed.', error)
    dashboard.value = null
  }

  try {
    if (!dataStore.goods.length || !dataStore.orders.length || !dataStore.users.length) {
      await dataStore.fetchAll()
    }
  } catch (error) {
    console.warn('Admin base data request failed.', error)
    loadError.value = dashboard.value
      ? '基础运营数据同步失败，部分明细可能延迟更新。'
      : '后台数据同步失败，请检查服务状态后重试。'
  } finally {
    if (!loadError.value && !dashboard.value && dataStore.initialized) {
      loadError.value = '看板聚合接口暂时不可用，当前展示基础运营数据。'
    }
    loading.value = false
  }
})

const parseDate = (value: string) => new Date(value.replace(' ', 'T'))
const todayLabel = computed(() =>
  new Date().toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
)
const syncStatusText = computed(() => {
  if (loading.value) return '正在同步后台数据'
  if (loadError.value) return '后台数据同步异常'
  if (dashboard.value) return '后台聚合数据已同步'
  if (dataStore.initialized) return '基础运营数据已同步'
  return '等待同步后台数据'
})

const percent = (value: number) => Math.max(0, Math.min(100, Math.round(value)))
const chartValue = (items: AdminDashboardChartItem[] | undefined, label: string) =>
  Number(items?.find((item) => item.label === label)?.value ?? 0)

const totalGmv = computed(() => dashboard.value?.totalGmv ?? dataStore.orders.reduce((sum, item) => sum + item.amount, 0))
const totalCarbonSaved = computed(() => dashboard.value?.totalCarbonSaved ?? dataStore.goods.reduce((sum, item) => sum + item.carbonSavedKg, 0))
const pendingGoodsCount = computed(() => dashboard.value?.pendingGoodsCount ?? dataStore.goods.filter((item) => item.auditStatus === '待审核').length)
const approvedGoodsCount = computed(() => dashboard.value ? chartValue(dashboard.value.goodsAudit, '审核通过') : dataStore.goods.filter((item) => item.auditStatus === '审核通过').length)
const rejectedGoodsCount = computed(() => dashboard.value ? chartValue(dashboard.value.goodsAudit, '已驳回') : dataStore.goods.filter((item) => item.auditStatus === '已驳回').length)
const pendingKycCount = computed(() => dashboard.value?.pendingKycCount ?? dataStore.users.filter((item) => item.kycReviewStatus === '待审核').length)
const approvedKycCount = computed(() => dataStore.users.filter((item) => item.kycReviewStatus === '已通过').length)
const rejectedKycCount = computed(() => dataStore.users.filter((item) => item.kycReviewStatus === '已驳回').length)
const completedOrdersCount = computed(() => dashboard.value?.completedOrdersCount ?? dataStore.orders.filter((item) => item.status === '已完成').length)
const paidOrdersCount = computed(() => dataStore.orders.filter((item) => item.status !== '待付款').length)
const newUsersCount = computed(() => {
  if (dashboard.value) return dashboard.value.newUsersCount
  const now = Date.now()
  const sevenDays = 7 * 24 * 60 * 60 * 1000
  return dataStore.users.filter((item) => {
    const timestamp = parseDate(item.registerAt).getTime()
    return Number.isFinite(timestamp) && now - timestamp <= sevenDays
  }).length
})

const metrics = computed(() => [
  {
    label: '成交 GMV',
    value: formatCurrency(totalGmv.value),
    hint: '后台交易总金额汇总'
  },
  {
    label: '新增用户数',
    value: formatNumber(newUsersCount.value),
    hint: '近 7 日注册用户'
  },
  {
    label: '碳贡献汇总',
    value: `${formatNumber(totalCarbonSaved.value)} kg`,
    hint: '平台累计减碳贡献'
  },
  {
    label: '待审核商品',
    value: formatNumber(pendingGoodsCount.value),
    hint: '待人工审核上架'
  }
])

const auditApprovalRate = computed(() => {
  if (dashboard.value) return dashboard.value.auditApprovalRate
  const reviewedCount = approvedGoodsCount.value + rejectedGoodsCount.value
  return reviewedCount ? percent((approvedGoodsCount.value / reviewedCount) * 100) : 0
})

const kycPassRate = computed(() => {
  if (dashboard.value) return dashboard.value.kycPassRate
  const reviewedCount = approvedKycCount.value + rejectedKycCount.value
  return reviewedCount ? percent((approvedKycCount.value / reviewedCount) * 100) : 0
})

const orderCompletionRate = computed(() => {
  if (dashboard.value) return dashboard.value.orderCompletionRate
  return dataStore.orders.length ? percent((completedOrdersCount.value / dataStore.orders.length) * 100) : 0
})

const rateCards = computed(() => [
  {
    label: '商品审核通过率',
    value: auditApprovalRate.value,
    hint: `已通过 ${approvedGoodsCount.value} 件，已驳回 ${rejectedGoodsCount.value} 件`,
    color: '#2563eb'
  },
  {
    label: 'KYC 通过率',
    value: kycPassRate.value,
    hint: `已通过 ${approvedKycCount.value} 位，待审 ${pendingKycCount.value} 位`,
    color: '#14b8a6'
  },
  {
    label: '订单完成率',
    value: orderCompletionRate.value,
    hint: `已完成 ${completedOrdersCount.value} 单 / 总订单 ${dataStore.orders.length} 单`,
    color: '#f59e0b'
  }
])

const recentDays = computed(() => {
  if (dashboard.value?.recentGmv?.length) {
    return dashboard.value.recentGmv.map((item, index) => ({
      key: `${item.label}-${index}`,
      label: item.label,
      gmv: item.gmv,
      count: item.orderCount
    }))
  }

  const days = Array.from({ length: 7 }, (_, index) => {
    const date = new Date()
    date.setDate(date.getDate() - (6 - index))
    return date
  })

  return days.map((date) => {
    const key = date.toISOString().slice(0, 10)
    const dayOrders = dataStore.orders.filter((item) => item.createdAt.startsWith(key))
    return {
      key,
      label: `${date.getMonth() + 1}/${date.getDate()}`,
      gmv: dayOrders.reduce((sum, item) => sum + item.amount, 0),
      count: dayOrders.length
    }
  })
})

const funnelItems = computed(() => {
  if (dashboard.value?.orderFunnel?.length) {
    const items = dashboard.value.orderFunnel
    const base = Math.max(items[0]?.value ?? 0, 1)
    const colors = ['#2563eb', '#0ea5e9', '#14b8a6', '#f59e0b', '#10b981']

    return items.map((item, index) => ({
      label: item.label,
      value: item.value,
      rate: percent((item.value / base) * 100),
      color: colors[index] ?? '#2563eb'
    }))
  }

  const viewCount = dataStore.goods.reduce((sum, item) => sum + item.viewCount, 0)
  const favoriteCount = dataStore.goods.reduce((sum, item) => sum + item.favorCount, 0)
  const orderCount = dataStore.orders.length
  const paidCount = paidOrdersCount.value
  const doneCount = completedOrdersCount.value
  const base = Math.max(viewCount, 1)

  return [
    { label: '商品浏览', value: viewCount, rate: percent((viewCount / base) * 100), color: '#2563eb' },
    { label: '商品收藏', value: favoriteCount, rate: percent((favoriteCount / base) * 100), color: '#0ea5e9' },
    { label: '提交订单', value: orderCount, rate: percent((orderCount / base) * 100), color: '#14b8a6' },
    { label: '完成支付', value: paidCount, rate: percent((paidCount / base) * 100), color: '#f59e0b' },
    { label: '交易完成', value: doneCount, rate: percent((doneCount / base) * 100), color: '#10b981' }
  ]
})

const goodsReviewSegments = computed(() =>
  dashboard.value?.goodsAudit?.length
    ? [
        { label: '待审核', value: chartValue(dashboard.value.goodsAudit, '待审核'), color: '#f59e0b' },
        { label: '审核通过', value: chartValue(dashboard.value.goodsAudit, '审核通过'), color: '#10b981' },
        { label: '已驳回', value: chartValue(dashboard.value.goodsAudit, '已驳回'), color: '#64748b' }
      ]
    : [
        { label: '待审核', value: pendingGoodsCount.value, color: '#f59e0b' },
        { label: '审核通过', value: approvedGoodsCount.value, color: '#10b981' },
        { label: '已驳回', value: rejectedGoodsCount.value, color: '#64748b' }
      ]
)

const userStatusSegments = computed(() =>
  dashboard.value?.userStatus?.length
    ? [
        { label: '正常账户', value: chartValue(dashboard.value.userStatus, '正常账户'), color: '#2563eb' },
        { label: '已封禁', value: chartValue(dashboard.value.userStatus, '已封禁'), color: '#475569' },
        { label: '待 KYC 审核', value: chartValue(dashboard.value.userStatus, '待 KYC 审核'), color: '#f59e0b' }
      ]
    : [
        { label: '正常账户', value: dataStore.users.filter((item) => item.accountStatus === '正常').length, color: '#2563eb' },
        { label: '已封禁', value: dataStore.users.filter((item) => item.accountStatus === '已封禁').length, color: '#475569' },
        { label: '待 KYC 审核', value: pendingKycCount.value, color: '#f59e0b' }
      ]
)

const orderStatusSegments = computed(() =>
  dashboard.value?.orderStatus?.length
    ? [
        { label: '待付款', value: chartValue(dashboard.value.orderStatus, '待付款'), color: '#94a3b8' },
        { label: '待发货', value: chartValue(dashboard.value.orderStatus, '待发货'), color: '#2563eb' },
        { label: '运输中', value: chartValue(dashboard.value.orderStatus, '运输中'), color: '#14b8a6' },
        { label: '已完成', value: chartValue(dashboard.value.orderStatus, '已完成'), color: '#10b981' }
      ]
    : [
        { label: '待付款', value: dataStore.orders.filter((item) => item.status === '待付款').length, color: '#94a3b8' },
        { label: '待发货', value: dataStore.orders.filter((item) => item.status === '待发货').length, color: '#2563eb' },
        { label: '运输中', value: dataStore.orders.filter((item) => item.status === '运输中').length, color: '#14b8a6' },
        { label: '已完成', value: completedOrdersCount.value, color: '#10b981' }
      ]
)

const pieChartOption = (items: { label: string; value: number; color: string }[], title: string): EChartsOption => ({
  color: items.map((item) => item.color),
  tooltip: {
    trigger: 'item',
    formatter: '{b}<br/>{c} ({d}%)'
  },
  legend: {
    bottom: 0,
    left: 'center',
    itemWidth: 12,
    textStyle: {
      color: '#475569',
      fontSize: 12
    }
  },
  series: [
    {
      name: title,
      type: 'pie',
      radius: ['54%', '76%'],
      center: ['50%', '44%'],
      label: {
        color: '#475569',
        formatter: '{b}\n{d}%'
      },
      labelLine: {
        length: 12,
        length2: 10
      },
      data: items.map((item) => ({
        name: item.label,
        value: item.value
      }))
    }
  ]
})

const gmvTrendOption = computed<EChartsOption>(() => ({
  color: ['#93c5fd', '#2563eb'],
  tooltip: {
    trigger: 'axis'
  },
  grid: {
    top: 24,
    left: 20,
    right: 20,
    bottom: 20,
    containLabel: true
  },
  legend: {
    top: 0,
    right: 0,
    textStyle: {
      color: '#475569'
    }
  },
  xAxis: {
    type: 'category',
    data: recentDays.value.map((item) => item.label),
    axisLine: {
      lineStyle: { color: '#dbe7fb' }
    },
    axisLabel: {
      color: '#64748b'
    }
  },
  yAxis: [
    {
      type: 'value',
      axisLabel: {
        color: '#64748b',
        formatter: (value: number) => `¥${value}`
      },
      splitLine: {
        lineStyle: { color: '#eef4ff' }
      }
    },
    {
      type: 'value',
      axisLabel: {
        color: '#94a3b8'
      },
      splitLine: { show: false }
    }
  ],
  series: [
    {
      name: '成交 GMV',
      type: 'bar',
      barWidth: 22,
      itemStyle: {
        borderRadius: [10, 10, 0, 0]
      },
      data: recentDays.value.map((item) => item.gmv)
    },
    {
      name: '订单数',
      type: 'line',
      yAxisIndex: 1,
      smooth: true,
      symbolSize: 8,
      lineStyle: {
        width: 3
      },
      data: recentDays.value.map((item) => item.count)
    }
  ]
}))

const funnelOption = computed<EChartsOption>(() => ({
  grid: {
    top: 16,
    left: 20,
    right: 28,
    bottom: 8,
    containLabel: true
  },
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: (params: Array<{ dataIndex: number }>) => {
      const current = funnelItems.value[params[0]?.dataIndex ?? 0]
      return `${current.label}<br/>数量：${formatNumber(current.value)}<br/>转化：${current.rate}%`
    }
  },
  xAxis: {
    type: 'value',
    axisLabel: {
      color: '#64748b'
    },
    splitLine: {
      lineStyle: { color: '#eef4ff' }
    }
  },
  yAxis: {
    type: 'category',
    data: funnelItems.value.map((item) => item.label),
    axisLabel: {
      color: '#475569'
    },
    axisTick: { show: false },
    axisLine: { show: false }
  },
  series: [
    {
      type: 'bar',
      barWidth: 18,
      data: funnelItems.value.map((item) => ({
        value: item.value,
        itemStyle: { color: item.color, borderRadius: 999 }
      })),
      label: {
        show: true,
        position: 'right',
        color: '#475569',
        formatter: ({ dataIndex }: { dataIndex: number }) => `${funnelItems.value[dataIndex]?.rate ?? 0}%`
      }
    }
  ]
}))

const goodsReviewOption = computed(() => pieChartOption(goodsReviewSegments.value, '商品审核分布'))
const userStatusOption = computed(() => pieChartOption(userStatusSegments.value, '用户状态分布'))
const orderStatusOption = computed(() => pieChartOption(orderStatusSegments.value, '订单状态分布'))

const pendingGoods = computed(() => dashboard.value?.pendingGoods ?? dataStore.goods.filter((item) => item.auditStatus === '待审核').slice(0, 5))
const pendingUsers = computed(() => dashboard.value?.pendingUsers ?? dataStore.users.filter((item) => item.kycReviewStatus === '待审核').slice(0, 5))
</script>

<template>
  <section class="admin-page dashboard-page">
    <section class="admin-page__hero">
      <div class="admin-page__hero-text">
        <span class="admin-page__eyebrow">Overview</span>
        <h2>数据看板</h2>
        <p>继续按 PRD 推进后台能力，把 GMV 趋势、新增用户、碳贡献、商品审核和订单漏斗统一聚合到后台桌面看板。</p>
        <div class="admin-page__meta">
          <span class="admin-pill">{{ todayLabel }}</span>
          <span class="admin-pill">{{ syncStatusText }}</span>
          <span class="admin-pill">待 KYC 审核 {{ pendingKycCount }}</span>
        </div>
        <p v-if="loadError" class="dashboard-alert">{{ loadError }}</p>
      </div>

      <div class="dashboard-spotlight">
        <span class="dashboard-spotlight__tag">今日重点</span>
        <strong>{{ pendingGoodsCount }}</strong>
        <p>件商品待审核</p>
        <div class="dashboard-spotlight__meta">
          <span>KYC 待审核 {{ pendingKycCount }}</span>
          <span>已完成订单 {{ completedOrdersCount }}</span>
          <span>近 7 日新增 {{ newUsersCount }}</span>
        </div>
      </div>
    </section>

    <div class="admin-metric-grid">
      <article v-for="item in metrics" :key="item.label" class="admin-metric-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.value }}</strong>
        <small>{{ item.hint }}</small>
      </article>
    </div>

    <div class="dashboard-rate-grid">
      <article v-for="item in rateCards" :key="item.label" class="dashboard-rate-card">
        <div class="dashboard-rate-card__top">
          <div>
            <strong>{{ item.label }}</strong>
            <p>{{ item.hint }}</p>
          </div>
          <span>{{ item.value }}%</span>
        </div>
        <el-progress :percentage="item.value" :show-text="false" :stroke-width="10" :color="item.color" />
      </article>
    </div>

    <div class="dashboard-panel-grid">
      <EChartPanel
        title="GMV 趋势图"
        subtitle="最近 7 天成交金额与订单数走势，更贴近 PRD 里的后台数据图表。"
        :option="gmvTrendOption"
        height="340px"
      />
      <EChartPanel
        title="订单漏斗"
        subtitle="按浏览、收藏、下单、支付、完成路径查看转化。"
        :option="funnelOption"
        height="340px"
      />
    </div>

    <div class="dashboard-chart-grid">
      <EChartPanel
        title="商品审核分布"
        subtitle="待审核、审核通过和已驳回商品结构。"
        :option="goodsReviewOption"
        height="320px"
      />
      <EChartPanel
        title="用户状态分布"
        subtitle="查看正常账户、封禁账户和待 KYC 审核用户。"
        :option="userStatusOption"
        height="320px"
      />
      <EChartPanel
        title="订单状态分布"
        subtitle="观察待付款、待发货、运输中和已完成订单占比。"
        :option="orderStatusOption"
        height="320px"
      />
    </div>

    <div class="dashboard-bottom-grid">
      <section class="admin-section-card">
        <div class="admin-section__head">
          <div>
            <h3 class="admin-section__title">待审核商品</h3>
            <p class="admin-section__desc">优先处理新发布的待审核商品。</p>
          </div>
          <RouterLink class="admin-link" to="/goods">进入商品审核</RouterLink>
        </div>

        <div v-if="pendingGoods.length" class="dashboard-list">
          <article v-for="item in pendingGoods" :key="item.id" class="dashboard-list__item">
            <img :src="item.heroImage" :alt="item.title" class="dashboard-list__thumb" />
            <div class="dashboard-list__content">
              <strong>{{ item.title }}</strong>
              <p>{{ item.brand }} · {{ item.category }} · {{ item.sellerName }}</p>
            </div>
            <div class="dashboard-list__aside">
              <span>{{ formatCurrency(item.price) }}</span>
              <span class="admin-badge admin-badge--warning">{{ item.auditStatus }}</span>
            </div>
          </article>
        </div>
        <p v-else class="admin-empty">当前没有待审核商品</p>
      </section>

      <section class="admin-section-card">
        <div class="admin-section__head">
          <div>
            <h3 class="admin-section__title">待审核用户</h3>
            <p class="admin-section__desc">重点关注待 KYC 审核和风控处理中的用户。</p>
          </div>
          <RouterLink class="admin-link" to="/users">进入用户管理</RouterLink>
        </div>

        <div v-if="pendingUsers.length" class="dashboard-list">
          <article v-for="item in pendingUsers" :key="item.id" class="dashboard-list__item dashboard-list__item--order">
            <div class="dashboard-list__content">
              <strong>{{ item.name }}</strong>
              <p>{{ item.phone }} · {{ item.city }} · 注册于 {{ item.registerAt }}</p>
            </div>
            <div class="dashboard-list__aside">
              <span>{{ item.role }}</span>
              <span class="admin-badge admin-badge--warning">{{ item.kycReviewStatus }}</span>
            </div>
          </article>
        </div>
        <p v-else class="admin-empty">当前没有待 KYC 审核用户</p>
      </section>
    </div>
  </section>
</template>

<style scoped>
.dashboard-page,
.dashboard-panel-grid,
.dashboard-rate-grid,
.dashboard-chart-grid,
.dashboard-bottom-grid,
.dashboard-list {
  display: grid;
  gap: 24px;
}

.dashboard-rate-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.dashboard-alert {
  margin: 0;
  padding: 12px 16px;
  border-radius: 18px;
  background: rgba(245, 158, 11, 0.12);
  color: #92400e;
  line-height: 1.6;
}

.dashboard-rate-card {
  padding: 22px 24px;
  border-radius: 28px;
  border: 1px solid rgba(219, 228, 241, 0.92);
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.05);
  display: grid;
  gap: 16px;
}

.dashboard-rate-card__top {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.dashboard-rate-card__top strong,
.dashboard-rate-card__top p,
.dashboard-rate-card__top span {
  margin: 0;
}

.dashboard-rate-card__top strong {
  font-size: 16px;
  color: #0f172a;
}

.dashboard-rate-card__top p {
  margin-top: 8px;
  color: #64748b;
  line-height: 1.6;
}

.dashboard-rate-card__top span {
  font-size: 28px;
  font-weight: 800;
  color: #0f172a;
}

.dashboard-spotlight {
  width: 320px;
  padding: 24px;
  border-radius: 28px;
  background: linear-gradient(160deg, #1d4ed8 0%, #2563eb 52%, #14b8a6 100%);
  color: #fff;
  box-shadow: 0 24px 44px rgba(37, 99, 235, 0.22);
  display: grid;
  align-content: start;
  gap: 12px;
}

.dashboard-spotlight__tag {
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

.dashboard-spotlight strong {
  font-size: 54px;
  line-height: 1;
}

.dashboard-spotlight p,
.dashboard-spotlight__meta {
  margin: 0;
  color: rgba(255, 255, 255, 0.88);
}

.dashboard-spotlight__meta {
  display: grid;
  gap: 8px;
  font-size: 13px;
}

.dashboard-panel-grid,
.dashboard-bottom-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.dashboard-chart-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.dashboard-list__item {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr) auto;
  gap: 16px;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid rgba(226, 232, 240, 0.9);
}

.dashboard-list__item:last-child {
  border-bottom: none;
}

.dashboard-list__item--order {
  grid-template-columns: minmax(0, 1fr) auto;
}

.dashboard-list__thumb {
  width: 84px;
  height: 84px;
  border-radius: 22px;
  object-fit: cover;
}

.dashboard-list__content,
.dashboard-list__aside {
  display: grid;
  gap: 6px;
}

.dashboard-list__content strong,
.dashboard-list__content p,
.dashboard-list__aside span {
  margin: 0;
}

.dashboard-list__content strong {
  font-size: 16px;
  color: #0f172a;
}

.dashboard-list__content p {
  color: #64748b;
  line-height: 1.6;
}

.dashboard-list__aside {
  justify-items: end;
}
</style>
