<script setup lang="ts">
const props = defineProps<{
  confidence: number
}>()
</script>

<template>
  <div class="gauge">
    <div class="gauge__track">
      <div class="gauge__value" :style="{ width: `${Math.min(100, Math.max(0, props.confidence))}%` }" />
    </div>
    <span>{{ props.confidence }}% 置信度</span>
  </div>
</template>
