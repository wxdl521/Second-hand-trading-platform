<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listMessages, markMessageRead, type UserMessageItem } from '@/api/message'

type MessageFilter = 'all' | 'unread' | 'read'

const messages = ref<UserMessageItem[]>([])
const loading = ref(false)
const activeFilter = ref<MessageFilter>('all')

const messageFilters: Array<{ key: MessageFilter; label: string }> = [
  { key: 'all', label: '全部消息' },
  { key: 'unread', label: '未读消息' },
  { key: 'read', label: '已读消息' }
]

const loadMessages = async () => {
  loading.value = true

  try {
    messages.value = await listMessages()
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadMessages()
})

const unreadCount = computed(() => messages.value.filter((item) => !item.isRead).length)

const visibleMessages = computed(() => {
  if (activeFilter.value === 'unread') {
    return messages.value.filter((item) => !item.isRead)
  }

  if (activeFilter.value === 'read') {
    return messages.value.filter((item) => item.isRead)
  }

  return messages.value
})

const messageStats = computed(() => [
  { label: '全部通知', value: String(messages.value.length) },
  { label: '未读消息', value: String(unreadCount.value) },
  { label: '已读消息', value: String(messages.value.length - unreadCount.value) }
])

const resolveTypeLabel = (type: string) => {
  switch (type) {
    case 'ORDER':
      return '订单'
    case 'GOODS':
      return '商品'
    case 'KYC':
      return '认证'
    case 'REFUND':
      return '退款'
    default:
      return '系统'
  }
}

const onReadMessage = async (id: string) => {
  const updated = await markMessageRead(id)
  messages.value = messages.value.map((item) => (item.id === updated.id ? updated : item))
}

const onReadAll = async () => {
  const unreadItems = messages.value.filter((item) => !item.isRead)

  if (!unreadItems.length) {
    return
  }

  const updatedItems = await Promise.all(unreadItems.map((item) => markMessageRead(item.id)))
  const updatedMap = new Map(updatedItems.map((item) => [item.id, item]))
  messages.value = messages.value.map((item) => updatedMap.get(item.id) ?? item)
}
</script>

<template>
  <section class="profile-page profile-page--compact">
    <div class="profile-topbar">
      <div>
        <span class="eyebrow">消息中心</span>
        <h1>消息通知</h1>
        <p class="lead">订单流转、商品审核、实名认证和退款进度都会集中展示在这里。</p>
      </div>
      <div class="action-row">
        <button class="ghost-btn" type="button" :disabled="!unreadCount" @click="onReadAll">全部已读</button>
        <RouterLink class="profile-settings-btn profile-settings-btn--plain" to="/profile">返回主页</RouterLink>
      </div>
    </div>

    <section class="panel panel--soft">
      <div class="profile-stats">
        <article v-for="item in messageStats" :key="item.label" class="profile-stat-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </article>
      </div>
    </section>

    <section class="panel">
      <div class="section-head section-head--compact">
        <div>
          <span class="eyebrow">筛选</span>
          <h2>按状态查看消息</h2>
        </div>
        <span class="badge">{{ unreadCount }} 条未读</span>
      </div>

      <div class="chip-row message-filters">
        <button
          v-for="filter in messageFilters"
          :key="filter.key"
          class="chip"
          :class="{ 'chip--active': activeFilter === filter.key }"
          type="button"
          @click="activeFilter = filter.key"
        >
          {{ filter.label }}
        </button>
      </div>

      <div v-if="loading" class="empty-card">
        <h3>正在同步消息</h3>
        <p>请稍候，系统正在拉取最新动态。</p>
      </div>

      <div v-else-if="visibleMessages.length" class="message-list-page">
        <article
          v-for="message in visibleMessages"
          :key="message.id"
          class="message-list-card"
          :class="{ 'message-list-card--read': message.isRead }"
        >
          <div class="message-list-card__meta">
            <div class="message-list-card__header">
              <span class="message-list-card__type">{{ resolveTypeLabel(message.type) }}</span>
              <strong>{{ message.title }}</strong>
            </div>
            <span>{{ message.createdAt }}</span>
          </div>
          <p>{{ message.content }}</p>
          <div class="action-row">
            <button v-if="!message.isRead" class="ghost-btn" type="button" @click="onReadMessage(message.id)">
              标记已读
            </button>
          </div>
        </article>
      </div>

      <div v-else class="empty-card">
        <h3>{{ activeFilter === 'unread' ? '没有未读消息' : activeFilter === 'read' ? '还没有已读消息' : '暂时没有消息' }}</h3>
        <p>新的订单、审核和账户进度出现后，这里会自动更新。</p>
      </div>
    </section>
  </section>
</template>

<style scoped>
.message-filters {
  margin-bottom: 20px;
}

.message-list-page {
  display: grid;
  gap: 16px;
}

.message-list-card {
  display: grid;
  gap: 12px;
  padding: 20px;
  border-radius: 20px;
  background: linear-gradient(145deg, rgba(248, 250, 252, 0.96), rgba(240, 249, 244, 0.98));
  border: 1px solid rgba(46, 158, 91, 0.12);
}

.message-list-card--read {
  opacity: 0.74;
}

.message-list-card__meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.message-list-card__header {
  display: grid;
  gap: 8px;
}

.message-list-card__type {
  width: fit-content;
  padding: 5px 10px;
  border-radius: 999px;
  background: rgba(22, 163, 74, 0.12);
  color: #166534;
  font-size: 12px;
  font-weight: 700;
}

.message-list-card__meta strong {
  color: #0f172a;
  font-size: 18px;
}

.message-list-card__meta span {
  color: #64748b;
  font-size: 13px;
}

.message-list-card p {
  margin: 0;
  color: #475569;
  line-height: 1.8;
}

@media (max-width: 767px) {
  .message-list-card__meta {
    flex-direction: column;
  }
}
</style>
