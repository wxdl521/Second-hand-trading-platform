<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { applyImageFallback, resolveAssetUrl } from '@/utils/assets'

const props = defineProps<{
  images: string[]
  title: string
}>()

const active = ref(0)
const normalizedImages = computed(() =>
  (props.images.length ? props.images : [resolveAssetUrl(undefined, 'goods')]).map((image) =>
    resolveAssetUrl(image, 'goods')
  )
)

watch(
  () => props.images,
  () => {
    active.value = 0
  }
)

const onImageError = (event: Event) => applyImageFallback(event, 'goods')
</script>

<template>
  <div class="detail-gallery">
    <img :src="normalizedImages[active] || normalizedImages[0]" :alt="title" @error="onImageError" />
    <div class="thumb-row">
      <button
        v-for="(image, index) in normalizedImages"
        :key="image"
        class="thumb"
        :class="{ 'thumb--active': active === index }"
        type="button"
        @click="active = index"
      >
        <img :src="image" :alt="`${title}-${index}`" @error="onImageError" />
      </button>
    </div>
  </div>
</template>
