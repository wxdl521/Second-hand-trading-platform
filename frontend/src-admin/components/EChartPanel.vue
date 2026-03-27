<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useResizeObserver } from '@vueuse/core'
import * as echarts from 'echarts/core'
import type { ECharts, EChartsOption } from 'echarts'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([BarChart, LineChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = withDefaults(defineProps<{
  title: string
  subtitle?: string
  option: EChartsOption
  height?: string
}>(), {
  subtitle: '',
  height: '320px'
})

const chartRef = ref<HTMLDivElement | null>(null)
let instance: ECharts | null = null

const renderChart = async () => {
  await nextTick()
  if (!chartRef.value) return

  if (!instance) {
    instance = echarts.init(chartRef.value)
  }

  instance.setOption(props.option, true)
  instance.resize()
}

const resizeChart = () => {
  instance?.resize()
}

watch(() => props.option, renderChart, { deep: true })

useResizeObserver(chartRef, () => {
  resizeChart()
})

onMounted(() => {
  renderChart()
  if (typeof window !== 'undefined') {
    window.addEventListener('resize', resizeChart)
  }
})

onBeforeUnmount(() => {
  if (typeof window !== 'undefined') {
    window.removeEventListener('resize', resizeChart)
  }
  instance?.dispose()
  instance = null
})
</script>

<template>
  <section class="admin-section-card chart-panel">
    <div class="admin-section__head chart-panel__head">
      <div>
        <h3 class="admin-section__title">{{ title }}</h3>
        <p v-if="subtitle" class="admin-section__desc">{{ subtitle }}</p>
      </div>
      <slot name="extra" />
    </div>

    <div ref="chartRef" class="chart-panel__canvas" :style="{ height }"></div>
  </section>
</template>

<style scoped>
.chart-panel {
  display: grid;
  gap: 12px;
}

.chart-panel__head {
  margin-bottom: 4px;
}

.chart-panel__canvas {
  width: 100%;
}
</style>
