<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { toggleFavoriteGoods } from '@/api/goods'
import { useAppStore } from '@/stores/app'
import type { GoodsItem } from '@/types'
import { applyImageFallback, resolveAssetUrl } from '@/utils/assets'

const props = defineProps<{ item: GoodsItem }>()

const router = useRouter()
const store = useAppStore()

const isLiked = computed(() => store.currentUser?.likedGoodsIds.includes(props.item.id))
const heroImage = computed(() => resolveAssetUrl(props.item.heroImage, 'goods'))

const onToggleFavorite = async () => {
  try {
    await toggleFavoriteGoods(props.item.id)
  } catch (error) {
    alert((error as Error).message)
    router.push('/login')
  }
}

const onImageError = (event: Event) => applyImageFallback(event, 'goods')
</script>

<template>
  <article class="goods-card">
    <button class="goods-card__fav" type="button" @click="onToggleFavorite">
      {{ isLiked ? '♥' : '♡' }}
    </button>
    <RouterLink class="goods-card__link" :to="`/goods/${item.id}`">
      <div class="goods-card__media">
        <img :src="heroImage" :alt="item.title" loading="lazy" @error="onImageError" />
      </div>
      <div class="goods-card__content">
        <div class="goods-card__topline">
          <span class="goods-card__chip">{{ item.category }}</span>
          <span class="goods-card__chip goods-card__chip--soft">{{ item.condition }}</span>
          <span v-if="item.mockCertified" class="goods-card__verified">平台认证</span>
        </div>
        <h3>{{ item.title }}</h3>
        <p>{{ item.story }}</p>
        <div class="goods-card__info">
          <span>{{ item.brand }}</span>
          <span>{{ item.city }}</span>
        </div>
        <div class="goods-card__footer">
          <div class="goods-card__price">
            <strong>¥{{ item.price.toLocaleString() }}</strong>
            <small>¥{{ item.originalPrice.toLocaleString() }}</small>
          </div>
          <span class="badge">{{ item.carbonSavedKg }}kg 减碳</span>
        </div>
      </div>
    </RouterLink>
  </article>
</template>

<style scoped>
.goods-card__verified {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  background: rgba(220, 252, 231, 0.92);
  border: 1px solid rgba(22, 101, 52, 0.16);
  color: #166534;
  font-size: 12px;
  font-weight: 700;
}
</style>
