<script setup lang="ts">
defineProps<{
  title: string
  value: string | number
  hint: string
}>()
</script>

<template>
  <article class="metric-card">
    <span>{{ title }}</span>
    <strong>{{ value }}</strong>
    <small>{{ hint }}</small>
  </article>
</template>
