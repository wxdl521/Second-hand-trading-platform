<script setup lang="ts">
import PointsCard from '@/components/carbon/PointsCard.vue'

defineProps<{
  balance: number
  progress: number
  level: string
  monthCarbonSavedKg: number
}>()
</script>

<template>
  <div class="panel carbon-hero">
    <div class="carbon-ring" :style="{ '--progress': `${progress}%` }">
      <div>
        <strong>{{ balance }}</strong>
        <span>当前积分</span>
      </div>
    </div>
    <div class="hero-grid">
      <PointsCard title="账户等级" :value="level" hint="完成更多循环交易可升级权益" />
      <PointsCard
        :title="'本月减碳'"
        :value="`${Number(monthCarbonSavedKg.toFixed(2))} kg`"
        hint="交易与估价行为持续累积贡献"
      />
    </div>
  </div>
</template>
