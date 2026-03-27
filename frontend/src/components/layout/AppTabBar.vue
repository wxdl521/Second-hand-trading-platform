<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { listMessages, onMessagesChanged } from '@/api/message'
import { useAppStore } from '@/stores/app'

const store = useAppStore()
const route = useRoute()
const unreadMessageCount = ref(0)

const publishTarget = computed(() =>
  store.isLoggedIn ? '/goods/publish' : '/login?redirect=/goods/publish'
)
const messageTarget = computed(() =>
  store.isLoggedIn ? '/profile/messages' : '/login?redirect=/profile/messages'
)
const profileTarget = computed(() => (store.isLoggedIn ? '/profile' : '/login'))

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
</script>

<template>
  <nav class="tabbar">
    <template v-if="false">
    <RouterLink class="tabbar__item" to="/">首页</RouterLink>
    <RouterLink class="tabbar__item" to="/goods">商品</RouterLink>
    <RouterLink class="tabbar__publish" to="/goods/publish" aria-label="发布闲置">+</RouterLink>
    <RouterLink class="tabbar__item" to="/estimate">估价</RouterLink>
    <RouterLink class="tabbar__item tabbar__item--with-badge" to="/profile">
      <span>我的</span>
      <span v-if="store.isLoggedIn && unreadMessageCount" class="tabbar__badge">{{ unreadMessageCount }}</span>
    </RouterLink>
    </template>
    <RouterLink class="tabbar__item" to="/">首页</RouterLink>
    <RouterLink class="tabbar__item" to="/goods">商品</RouterLink>
    <RouterLink class="tabbar__publish" :to="publishTarget" aria-label="发布闲置">+</RouterLink>
    <RouterLink class="tabbar__item tabbar__item--with-badge" :to="messageTarget">
      <span>消息</span>
      <span v-if="store.isLoggedIn && unreadMessageCount" class="tabbar__badge">{{ unreadMessageCount }}</span>
    </RouterLink>
    <RouterLink class="tabbar__item" :to="profileTarget">我的</RouterLink>
  </nav>
</template>
