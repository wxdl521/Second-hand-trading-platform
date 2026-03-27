import { computed } from 'vue'
import { defineStore } from 'pinia'
import * as orderApi from '@/api/order'
import { useAppStore } from '@/stores/app'

export const useCartStore = defineStore('cart', () => {
  const appStore = useAppStore()
  const orders = computed(() => appStore.orders)

  const createOrder = (goodsId: string) => orderApi.createOrder(goodsId)
  const payOrder = (orderId: string) => orderApi.payOrder(orderId)

  return {
    orders,
    createOrder,
    payOrder
  }
})
