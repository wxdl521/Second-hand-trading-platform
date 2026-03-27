<script setup lang="ts">
import { computed } from 'vue'

interface SegmentItem {
  label: string
  value: number
  color: string
}

const props = defineProps<{
  title: string
  subtitle: string
  centerLabel?: string
  segments: SegmentItem[]
}>()

const total = computed(() => props.segments.reduce((sum, item) => sum + item.value, 0))

const gradientStyle = computed(() => {
  if (!props.segments.length || total.value <= 0) {
    return {
      background: 'conic-gradient(#dbeafe 0deg 360deg)'
    }
  }

  let start = 0
  const stops = props.segments.map((item) => {
    const slice = (item.value / total.value) * 360
    const end = start + slice
    const segment = `${item.color} ${start}deg ${end}deg`
    start = end
    return segment
  })

  return {
    background: `conic-gradient(${stops.join(', ')})`
  }
})

const displayCenterLabel = computed(() => props.centerLabel ?? `总计 ${total.value}`)
</script>

<template>
  <section class="donut-panel">
    <div class="donut-panel__head">
      <h3>{{ title }}</h3>
      <p>{{ subtitle }}</p>
    </div>

    <div class="donut-panel__body">
      <div class="donut-panel__chart" :style="gradientStyle">
        <div class="donut-panel__inner">
          <strong>{{ total }}</strong>
          <span>{{ displayCenterLabel }}</span>
        </div>
      </div>

      <div class="donut-panel__legend">
        <div v-for="item in segments" :key="item.label" class="donut-panel__legend-item">
          <span class="donut-panel__legend-dot" :style="{ background: item.color }"></span>
          <div>
            <strong>{{ item.label }}</strong>
            <span>{{ item.value }}</span>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.donut-panel {
  padding: 24px 26px;
  border-radius: 28px;
  border: 1px solid rgba(219, 228, 241, 0.9);
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 44px rgba(15, 23, 42, 0.06);
  display: grid;
  gap: 18px;
}

.donut-panel__head h3,
.donut-panel__head p,
.donut-panel__legend-item strong,
.donut-panel__legend-item span {
  margin: 0;
}

.donut-panel__head h3 {
  font-size: 18px;
  font-weight: 800;
  color: #0f172a;
}

.donut-panel__head p {
  margin-top: 8px;
  color: #64748b;
  font-size: 14px;
}

.donut-panel__body {
  display: grid;
  grid-template-columns: minmax(220px, 320px) minmax(0, 1fr);
  gap: 24px;
  align-items: center;
}

.donut-panel__chart {
  width: 220px;
  height: 220px;
  margin: 0 auto;
  border-radius: 50%;
  position: relative;
}

.donut-panel__chart::before {
  content: '';
  position: absolute;
  inset: 18px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: inset 0 0 0 1px rgba(226, 232, 240, 0.9);
}

.donut-panel__inner {
  position: absolute;
  inset: 0;
  z-index: 1;
  display: grid;
  place-content: center;
  text-align: center;
  gap: 4px;
}

.donut-panel__inner strong {
  font-size: 42px;
  line-height: 1;
  color: #0f172a;
}

.donut-panel__inner span {
  color: #64748b;
  font-size: 13px;
}

.donut-panel__legend {
  display: grid;
  gap: 14px;
}

.donut-panel__legend-item {
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 12px;
  align-items: center;
  min-height: 48px;
  padding: 10px 12px;
  border-radius: 18px;
  background: #f8fbff;
}

.donut-panel__legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.donut-panel__legend-item strong {
  display: block;
  font-size: 14px;
  color: #0f172a;
}

.donut-panel__legend-item span {
  color: #64748b;
  font-size: 13px;
}
</style>
