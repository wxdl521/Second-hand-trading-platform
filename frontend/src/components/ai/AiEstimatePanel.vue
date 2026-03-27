<script setup lang="ts">
import PriceGauge from '@/components/ai/PriceGauge.vue'
import type { EstimateResult } from '@/types'

defineProps<{
  result: EstimateResult | null
}>()
</script>

<template>
  <div class="panel">
    <h2>估价结果</h2>
    <template v-if="result">
      <div class="price-panel price-panel--large">
        <div>
          <strong>¥{{ result.price.toLocaleString() }}</strong>
          <small>建议成交区间 ¥{{ result.low.toLocaleString() }} - ¥{{ result.high.toLocaleString() }}</small>
        </div>
        <span class="badge">{{ result.carbonSavedKg }}kg 减碳</span>
      </div>
      <PriceGauge :confidence="result.confidence" />
      <p>{{ result.summary }}</p>
      <div class="summary-grid">
        <div><strong>推荐动作</strong><span>补充附件与细节图</span></div>
        <div><strong>鉴定建议</strong><span>可继续发起视频连线鉴定</span></div>
      </div>
      <ul class="list">
        <li v-for="tip in result.tips" :key="tip">{{ tip }}</li>
      </ul>
    </template>
    <template v-else>
      <div class="empty-inline">
        <p>填写左侧信息后，系统会返回最新的真实估价结果。</p>
      </div>
    </template>
  </div>
</template>
