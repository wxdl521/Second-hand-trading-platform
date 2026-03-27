import type { OrderItem, OrderStatus } from '@/types'
import { apiClient, getAppStore } from '@/api'
import { listCarbonRecords } from '@/api/carbon'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface BackendOrder {
  id: number
  goodsId: number
  buyerName: string
  buyerPhone?: string
  shippingCompany?: string
  trackingNo?: string
  servicePhone?: string
  deliveryAddress?: string
  amount: number
  status: 'PENDING_PAYMENT' | 'PENDING_SHIPMENT' | 'IN_TRANSIT' | 'COMPLETED' | 'REFUNDED'
  createdAt?: string
  paidAt?: string
  shippedAt?: string
  completedAt?: string
  refundedAt?: string
}

const ORDER_STATUS_PENDING_PAYMENT = '\u5f85\u4ed8\u6b3e' as OrderStatus
const ORDER_STATUS_PENDING_SHIPMENT = '\u5f85\u53d1\u8d27' as OrderStatus
const ORDER_STATUS_IN_TRANSIT = '\u8fd0\u8f93\u4e2d' as OrderStatus
const ORDER_STATUS_COMPLETED = '\u5df2\u5b8c\u6210' as OrderStatus
const ORDER_STATUS_REFUNDED = '已退款' as OrderStatus
const GOODS_STATUS_RESERVED = '\u5df2\u9884\u8ba2' as const
const GOODS_STATUS_SOLD = '\u5df2\u552e\u51fa' as const
const GOODS_STATUS_ON_SALE = '在售中' as const

const statusMap: Record<BackendOrder['status'], OrderStatus> = {
  PENDING_PAYMENT: ORDER_STATUS_PENDING_PAYMENT,
  PENDING_SHIPMENT: ORDER_STATUS_PENDING_SHIPMENT,
  IN_TRANSIT: ORDER_STATUS_IN_TRANSIT,
  COMPLETED: ORDER_STATUS_COMPLETED,
  REFUNDED: ORDER_STATUS_REFUNDED
}

const normalizeDateTime = (value?: string) =>
  value ? String(value).replace('T', ' ').slice(0, 19) : new Date().toLocaleString('zh-CN', { hour12: false })

const makeTimeline = (payload: BackendOrder, status: OrderStatus, createdAt: string) => {
  const items = [{ label: '\u8ba2\u5355\u521b\u5efa', time: createdAt }]

  if (status !== ORDER_STATUS_PENDING_PAYMENT) {
    items.push({ label: '\u5b8c\u6210\u652f\u4ed8', time: payload.paidAt ? normalizeDateTime(payload.paidAt) : createdAt })
  }

  if (status === ORDER_STATUS_IN_TRANSIT || status === ORDER_STATUS_COMPLETED) {
    items.push({ label: '\u5356\u5bb6\u53d1\u8d27', time: payload.shippedAt ? normalizeDateTime(payload.shippedAt) : createdAt })
  }

  if (status === ORDER_STATUS_COMPLETED) {
    items.push({ label: '\u8ba2\u5355\u5b8c\u6210', time: payload.completedAt ? normalizeDateTime(payload.completedAt) : createdAt })
  }

  if (status === ORDER_STATUS_REFUNDED) {
    items.push({ label: '订单退款', time: payload.refundedAt ? normalizeDateTime(payload.refundedAt) : createdAt })
  }

  return items
}

const syncGoodsStatus = (
  goodsId: string,
  nextStatus: typeof GOODS_STATUS_RESERVED | typeof GOODS_STATUS_SOLD | typeof GOODS_STATUS_ON_SALE
) => {
  const store = getAppStore()
  const goods = store.goods.find((item) => item.id === goodsId)
  if (!goods) return
  store.upsertGoods({ ...goods, status: nextStatus })
}

const mapOrder = (payload: BackendOrder): OrderItem => {
  const createdAt = normalizeDateTime(payload.createdAt)
  const status = statusMap[payload.status] ?? ORDER_STATUS_PENDING_PAYMENT

  return {
    id: `order-${payload.id}`,
    goodsId: String(payload.goodsId),
    amount: Number(payload.amount),
    buyerName: payload.buyerName,
    status,
    createdAt,
    paidAt: payload.paidAt ? normalizeDateTime(payload.paidAt) : undefined,
    shippedAt: payload.shippedAt ? normalizeDateTime(payload.shippedAt) : undefined,
    completedAt: payload.completedAt ? normalizeDateTime(payload.completedAt) : undefined,
    refundedAt: payload.refundedAt ? normalizeDateTime(payload.refundedAt) : undefined,
    shippingCompany: payload.shippingCompany,
    trackingNo: payload.trackingNo,
    servicePhone: payload.servicePhone,
    deliveryAddress: payload.deliveryAddress,
    timeline: makeTimeline(payload, status, createdAt)
  }
}

const mergeOrders = (...groups: OrderItem[][]) => {
  const merged = new Map<string, OrderItem>()
  groups.flat().forEach((order) => {
    merged.set(order.id, order)
  })
  return Array.from(merged.values()).sort((left, right) => right.createdAt.localeCompare(left.createdAt))
}

export const getOrderDetail = async (id: string): Promise<OrderItem | undefined> => {
  const store = getAppStore()
  const numericId = id.replace('order-', '')
  const response = await apiClient.get<ApiResponse<BackendOrder>>(`/order/${numericId}`)
  const order = mapOrder(response.data.data)
  store.upsertOrder(order)
  return order
}

export const listOrders = async (): Promise<OrderItem[]> => {
  const store = getAppStore()
  const [buyResponse, sellResponse] = await Promise.all([
    apiClient.get<ApiResponse<BackendOrder[]>>('/order/my/buy'),
    apiClient.get<ApiResponse<BackendOrder[]>>('/order/my/sell')
  ])
  const orders = mergeOrders(buyResponse.data.data.map(mapOrder), sellResponse.data.data.map(mapOrder))
  store.setOrders(orders)
  return orders
}

export const listBuyOrders = async (): Promise<OrderItem[]> => {
  const response = await apiClient.get<ApiResponse<BackendOrder[]>>('/order/my/buy')
  return response.data.data.map(mapOrder)
}

export const listSellOrders = async (): Promise<OrderItem[]> => {
  const response = await apiClient.get<ApiResponse<BackendOrder[]>>('/order/my/sell')
  return response.data.data.map(mapOrder)
}

export const createOrder = async (goodsId: string): Promise<OrderItem> => {
  const store = getAppStore()
  const response = await apiClient.post<ApiResponse<BackendOrder>>('/order/create', { goodsId: Number(goodsId) })
  const order = mapOrder(response.data.data)
  store.upsertOrder(order)
  syncGoodsStatus(order.goodsId, GOODS_STATUS_RESERVED)
  return order
}

export const payOrder = async (orderId: string): Promise<void> => {
  const store = getAppStore()
  const numericId = orderId.replace('order-', '')
  const response = await apiClient.post<ApiResponse<BackendOrder>>(`/order/pay/${numericId}`)
  const order = mapOrder(response.data.data)
  store.upsertOrder(order)
  syncGoodsStatus(order.goodsId, GOODS_STATUS_RESERVED)
}

export const shipOrder = async (orderId: string): Promise<void> => {
  const store = getAppStore()
  const numericId = orderId.replace('order-', '')
  const response = await apiClient.post<ApiResponse<BackendOrder>>(`/order/ship/${numericId}`)
  const order = mapOrder(response.data.data)
  store.upsertOrder(order)
  syncGoodsStatus(order.goodsId, GOODS_STATUS_RESERVED)
}

export const confirmOrder = async (orderId: string): Promise<void> => {
  const store = getAppStore()
  const numericId = orderId.replace('order-', '')
  const response = await apiClient.post<ApiResponse<BackendOrder>>(`/order/confirm/${numericId}`)
  const order = mapOrder(response.data.data)
  store.upsertOrder(order)
  syncGoodsStatus(order.goodsId, GOODS_STATUS_SOLD)
  await listCarbonRecords()
}

export const refundOrder = async (orderId: string): Promise<void> => {
  const store = getAppStore()
  const numericId = orderId.replace('order-', '')
  const response = await apiClient.post<ApiResponse<BackendOrder>>(`/order/${numericId}/refund`)
  const order = mapOrder(response.data.data)
  store.upsertOrder(order)
  syncGoodsStatus(order.goodsId, GOODS_STATUS_ON_SALE)
}
