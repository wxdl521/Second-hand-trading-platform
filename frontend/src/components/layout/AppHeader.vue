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
  <header class="header mobile-header">
    <div class="header__inner">
      <RouterLink class="brand" to="/">
        <span class="brand__dot">S</span>
        <div class="mobile-header__brand-copy">
          <strong>尚有新生</strong>
          <small>绿色循环交易</small>
        </div>
        <div>
          <strong>尚有新生</strong>
          <small>AI 循环交易平台</small>
        </div>
      </RouterLink>

      <nav class="header__nav">
        <RouterLink to="/goods">全部商品</RouterLink>
        <RouterLink to="/appraise">鉴定预约</RouterLink>
        <RouterLink to="/carbon">碳账户</RouterLink>
      </nav>

      <div class="header__actions">
        <button class="header__search-toggle" type="button" aria-label="搜索商品" @click="onToggleSearch">
          <span aria-hidden="true">⌕</span>
        </button>
        <RouterLink class="mobile-header__message" :to="messageTarget" aria-label="查看消息">
          <span class="mobile-header__message-text">消息</span>
          <span v-if="store.isLoggedIn && unreadMessageCount" class="header__profile-badge">
            {{ unreadMessageCount }}
          </span>
        </RouterLink>
        <RouterLink v-if="!store.isLoggedIn" class="primary-btn" to="/login">登录</RouterLink>
      </div>
    </div>

    <div v-if="searchOpen" class="header__searchbar">
      <div class="search-box header__searchbox">
        <input v-model="keyword" placeholder="搜索品牌、品类、关键字" @keyup.enter="onSearch" />
        <button type="button" @click="onSearch">搜索</button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.mobile-header .header__inner {
  width: calc(100% - 24px);
  margin: 0 auto;
  min-height: 64px;
  padding: 10px 0;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.mobile-header .brand {
  min-width: 0;
}

.mobile-header .brand > div:not(.mobile-header__brand-copy) {
  display: none;
}

.mobile-header__brand-copy strong {
  display: block;
  font-size: 16px;
}

.mobile-header__brand-copy small {
  display: block;
  font-size: 12px;
}

.mobile-header .header__nav,
.mobile-header .header__actions > .primary-btn {
  display: none;
}

.mobile-header .header__actions {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: flex-end;
}

.mobile-header__message {
  position: relative;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: var(--surface-strong);
  border: 1px solid var(--line);
  box-shadow: var(--shadow);
  display: grid;
  place-items: center;
  color: var(--text);
}

.mobile-header__message-text {
  font-size: 12px;
  font-weight: 700;
}

.mobile-header .header__searchbar {
  width: calc(100% - 24px);
  padding-bottom: 8px;
}
</style>
