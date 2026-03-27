<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getOrderDetail } from '@/api/order'
import { useAppStore } from '@/stores/app'

const route = useRoute()
const store = useAppStore()

const order = computed(() => store.orders.find((item) => item.id === String(route.params.id)))
const goods = computed(() => store.goods.find((item) => item.id === order.value?.goodsId))

watch(
  () => route.params.id,
  () => {
    void getOrderDetail(String(route.params.id))
  },
  { immediate: true }
)
</script>

<template>
  <section v-if="order" class="section-block">
    <div class="section-head">
      <div>
        <span class="eyebrow">订单详情</span>
        <h1>{{ goods?.title || order.id }}</h1>
      </div>
      <span class="badge">{{ order.status }}</span>
    </div>

    <div class="two-column">
      <article class="panel">
        <h2>订单信息</h2>
        <div class="summary-grid">
          <div><strong>订单号</strong><span>{{ order.id }}</span></div>
          <div><strong>金额</strong><span>¥{{ order.amount.toLocaleString() }}</span></div>
          <div><strong>买家</strong><span>{{ order.buyerName }}</span></div>
          <div><strong>下单时间</strong><span>{{ order.createdAt }}</span></div>
        </div>
      </article>

      <article class="panel" v-if="goods">
        <h2>商品信息</h2>
        <div class="summary-grid">
          <div><strong>标题</strong><span>{{ goods.title }}</span></div>
          <div><strong>品牌</strong><span>{{ goods.brand }}</span></div>
          <div><strong>分类</strong><span>{{ goods.category }}</span></div>
          <div><strong>减碳贡献</strong><span>{{ goods.carbonSavedKg }} kg</span></div>
        </div>
      </article>
    </div>

    <article class="panel">
      <h2>履约时间轴</h2>
      <ul class="timeline">
        <li v-for="node in order.timeline" :key="`${order.id}-${node.time}`">
          <strong>{{ node.label }}</strong>
          <span>{{ node.time }}</span>
        </li>
      </ul>
    </article>
  </section>

  <section v-else class="empty-card">
    <span class="eyebrow">订单详情</span>
    <h1>订单不存在</h1>
    <RouterLink class="primary-btn" to="/order/list">返回订单列表</RouterLink>
  </section>
</template>
