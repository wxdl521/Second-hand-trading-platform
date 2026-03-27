<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { listMessages, onMessagesChanged } from '@/api/message'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const router = useRouter()
const route = useRoute()

const searchOpen = ref(false)
const keyword = ref(typeof route.query.q === 'string' ? route.query.q : '')
const unreadMessageCount = ref(0)

const navItems = [
  { label: '首页', to: '/' },
  { label: '全部商品', to: '/goods' },
  { label: 'AI 估价', to: '/estimate' },
  { label: '发布闲置', to: '/goods/publish' }
]

watch(
  () => route.query.q,
  (value) => {
    keyword.value = typeof value === 'string' ? value : ''
  }
)

const refreshUnreadMessages = async () => {
  if (!store.isLoggedIn) {
    unreadMessageCount.value = 0
    return
  }

  try {
    const messages = await listMessages()
    unreadMessageCount.value = messages.filter((item) => !item.isRead).length
  } catch {
    unreadMessageCount.value = 0
  }
}

watch(
  [() => store.isLoggedIn, () => route.fullPath],
  () => {
    void refreshUnreadMessages()
  },
  { immediate: true }
)

const removeMessageListener = onMessagesChanged(() => {
  void refreshUnreadMessages()
})

onBeforeUnmount(() => {
  removeMessageListener()
})

const profileLabel = computed(() => (store.isLoggedIn ? '我的' : '登录'))
const profileTarget = computed(() => (store.isLoggedIn ? '/profile' : '/login'))

const messageTarget = computed(() =>
  store.isLoggedIn ? '/profile/messages' : '/login?redirect=/profile/messages'
)

const onToggleSearch = () => {
  searchOpen.value = !searchOpen.value
}

const onSearch = () => {
  searchOpen.value = false
  router.push({ name: 'goods-list', query: keyword.value.trim() ? { q: keyword.value.trim() } : {} })
}
</script>

<template>
  <header class="header">
    <div class="header__inner">
      <RouterLink class="brand" to="/">
        <span class="brand__dot">S</span>
        <div>
          <strong>尚有新生</strong>
          <small>AI 循环交易平台</small>
        </div>
      </RouterLink>

      <nav class="header__nav">
        <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">{{ item.label }}</RouterLink>
      </nav>

      <div class="header__actions">
        <button class="header__search-toggle" type="button" aria-label="搜索商品" @click="onToggleSearch">
          <span aria-hidden="true">⌕</span>
        </button>
        <RouterLink class="ghost-btn header__profile-btn" :to="messageTarget">
          <span v-if="store.isLoggedIn && unreadMessageCount" class="header__profile-badge">{{ unreadMessageCount }}</span>
          <span>消息</span>
        </RouterLink>
        <RouterLink class="ghost-btn header__profile-btn" :to="profileTarget">
          <span>{{ profileLabel }}</span>
        </RouterLink>
      </div>
    </div>

    <div v-if="searchOpen" class="header__searchbar">
      <div class="search-box header__searchbox">
        <input v-model="keyword" placeholder="搜索品牌、品类、商品关键词" @keyup.enter="onSearch" />
        <button type="button" @click="onSearch">搜索</button>
      </div>
    </div>
  </header>
</template>
