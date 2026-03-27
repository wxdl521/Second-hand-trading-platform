import type { useAppStore } from '@/stores/app'
import type { OrderItem } from '@/types'

type AppStore = ReturnType<typeof useAppStore>

export type OrderPanelKey = 'buy' | 'sell'
export type OrderFilterKey = '全部' | '待付款' | '待发货' | '运输中' | '已完成' | '已退款'

export interface LogisticsNode {
  label: string
  time: string
  detail: string
}

const SHIPPING_COMPANY = '顺丰速运'
const SERVICE_HOTLINE = '400-820-2026'
const SUPPORT_ADDRESS = '上海市徐汇区绿色循环交付中心'

const formatNow = () =>
  new Date()
    .toLocaleString('zh-CN', { hour12: false })
    .replace(/\//g, '-')

const formatDate = () => new Date().toISOString().slice(0, 10)

const orderDigits = (orderId: string) => orderId.replace(/\D/g, '').slice(-6).padStart(6, '0')

const appendTimeline = (order: OrderItem, label: string) => {
  if (order.timeline.at(-1)?.label === label) {
    return order.timeline
  }

  return [...order.timeline, { label, time: formatNow() }]
}

export const resolveOrderTrackingNo = (order: OrderItem) => `SF${orderDigits(order.id)}886`

export const resolveOrderServicePhone = () => SERVICE_HOTLINE

export const resolveOrderAddress = () => SUPPORT_ADDRESS

export const resolveOrderLogistics = (order: OrderItem): LogisticsNode[] => {
  const baseTime = order.timeline.at(-1)?.time ?? order.createdAt

  if (order.status === '待付款') {
    return [
      {
        label: '等待支付',
        time: baseTime,
        detail: '订单已创建，支付成功后将自动进入发货队列。'
      }
    ]
  }

  if (order.status === '待发货') {
    return [
      {
        label: '平台验货完成',
        time: baseTime,
        detail: '平台已锁定库存，卖家正在准备发货。'
      },
      {
        label: '等待揽收',
        time: baseTime,
        detail: `${SHIPPING_COMPANY} 将在 24 小时内上门揽收。`
      }
    ]
  }

  if (order.status === '运输中') {
    return [
      {
        label: '商家已发货',
        time: order.timeline.at(-1)?.time ?? baseTime,
        detail: `包裹已交由 ${SHIPPING_COMPANY} 承运。`
      },
      {
        label: '干线运输中',
        time: baseTime,
        detail: '包裹正在前往收货城市分拨中心。'
      },
      {
        label: '配送准备中',
        time: baseTime,
        detail: '预计 24 小时内送达，请留意签收。'
      }
    ]
  }

  if (order.status === '已退款') {
    return [
      {
        label: '退款已提交',
        time: order.timeline.at(-1)?.time ?? baseTime,
        detail: '平台已接收退款申请，订单状态正在同步更新。'
      },
      {
        label: '退款已完成',
        time: order.timeline.at(-1)?.time ?? baseTime,
        detail: '款项已原路退回，商品重新回到在售状态。'
      }
    ]
  }

  return [
    {
      label: '订单完成',
      time: order.timeline.at(-1)?.time ?? baseTime,
      detail: '已确认收货，绿色积分已成功入账。'
    },
    {
      label: '服务闭环',
      time: order.timeline.at(-1)?.time ?? baseTime,
      detail: '支持继续下载证书、查看碳账户与复购推荐。'
    }
  ]
}

export const resolveOrderStageText = (order: OrderItem) =>
  resolveOrderLogistics(order).at(-1)?.label ?? order.timeline.at(-1)?.label ?? '处理中'

export const isBuyerOrder = (order: OrderItem, currentUserName?: string | null) =>
  Boolean(currentUserName) && order.buyerName === currentUserName

export const isSellerOrder = (
  order: OrderItem,
  currentUserName?: string | null,
  goodsSellerName?: string | null
) => Boolean(currentUserName) && goodsSellerName === currentUserName && order.buyerName !== currentUserName

export const shipOrderLocal = (store: AppStore, orderId: string) => {
  const target = store.orders.find((item) => item.id === orderId)

  if (!target) {
    throw new Error('订单不存在')
  }

  if (target.status !== '待发货') {
    return target
  }

  const updatedOrder: OrderItem = {
    ...target,
    status: '运输中',
    timeline: appendTimeline(target, '卖家已发货')
  }

  store.setOrders(store.orders.map((item) => (item.id === orderId ? updatedOrder : item)))
  return updatedOrder
}

export const confirmOrderLocal = (store: AppStore, orderId: string) => {
  const target = store.orders.find((item) => item.id === orderId)

  if (!target) {
    throw new Error('订单不存在')
  }

  if (target.status === '已完成') {
    return target
  }

  if (target.status !== '运输中') {
    throw new Error('当前订单还不能确认收货')
  }

  const goods = store.goods.find((item) => item.id === target.goodsId)
  const carbonSavedKg = goods?.carbonSavedKg ?? Math.max(1, Number((target.amount * 0.0008).toFixed(2)))
  const points = Math.max(30, Math.round(target.amount * 0.05))

  const updatedOrder: OrderItem = {
    ...target,
    status: '已完成',
    timeline: appendTimeline(target, '买家确认收货')
  }

  store.setOrders(store.orders.map((item) => (item.id === orderId ? updatedOrder : item)))
  store.setCarbonRecords([
    {
      id: `carbon-${Date.now()}`,
      title: '确认收货获得绿色积分',
      points,
      type: '收入',
      date: formatDate(),
      description: `订单 ${target.id} 完成交付，累计减碳 ${carbonSavedKg}kg。`
    },
    ...store.carbonRecords
  ])
  store.applyCarbonDelta(points)

  return updatedOrder
}

const resolveRuntimeNodeTime = (primary?: string, fallback?: string) => primary ?? fallback ?? formatNow()

const resolveRuntimeCarrierName = (order: OrderItem) => order.shippingCompany?.trim() || SHIPPING_COMPANY

export const resolveOrderTrackingNoRealtime = (order: OrderItem) =>
  order.trackingNo?.trim() || `SF${orderDigits(order.id)}886`

export const resolveOrderServicePhoneRealtime = (order: OrderItem) =>
  order.servicePhone?.trim() || SERVICE_HOTLINE

export const resolveOrderAddressRealtime = (order: OrderItem) =>
  order.deliveryAddress?.trim() || SUPPORT_ADDRESS

export const resolveOrderLogisticsRealtime = (order: OrderItem): LogisticsNode[] => {
  const baseTime = order.timeline.at(-1)?.time ?? order.createdAt
  const paidAt = resolveRuntimeNodeTime(order.paidAt, order.createdAt)
  const shippedAt = resolveRuntimeNodeTime(order.shippedAt, paidAt)
  const completedAt = resolveRuntimeNodeTime(order.completedAt, shippedAt)
  const refundedAt = resolveRuntimeNodeTime(order.refundedAt, baseTime)
  const carrierName = resolveRuntimeCarrierName(order)
  const trackingNo = resolveOrderTrackingNoRealtime(order)

  if (order.status === '待付款') {
    return [
      {
        label: '等待支付',
        time: order.createdAt,
        detail: '订单已创建，支付成功后将自动进入发货队列。'
      }
    ]
  }

  if (order.status === '待发货') {
    return [
      {
        label: '支付完成',
        time: paidAt,
        detail: '买家已完成支付，平台正在同步锁定库存与履约信息。'
      },
      {
        label: '等待揽收',
        time: paidAt,
        detail: `${carrierName} 已进入待揽收队列，客服与履约电话 ${resolveOrderServicePhoneRealtime(order)}。`
      }
    ]
  }

  if (order.status === '运输中') {
    return [
      {
        label: '支付完成',
        time: paidAt,
        detail: '订单已完成支付，卖家开始准备发货。'
      },
      {
        label: '卖家已发货',
        time: shippedAt,
        detail: `包裹已交由 ${carrierName} 承运，物流单号 ${trackingNo}。`
      },
      {
        label: '干线运输中',
        time: shippedAt,
        detail: `包裹正发往 ${resolveOrderAddressRealtime(order)} 对应收货区域，请留意签收通知。`
      }
    ]
  }

  if (order.status === '已退款') {
    return [
      {
        label: '退款已提交',
        time: refundedAt,
        detail: '平台已接收退款申请，订单状态与商品库存正在同步回滚。'
      },
      {
        label: '退款已完成',
        time: refundedAt,
        detail: '款项已原路退回，商品重新回到可售状态。'
      }
    ]
  }

  return [
    {
      label: '卖家已发货',
      time: shippedAt,
      detail: `包裹通过 ${carrierName} 完成配送，物流单号 ${trackingNo}。`
    },
    {
      label: '订单完成',
      time: completedAt,
      detail: '买家已确认收货，绿色积分与碳减排贡献已同步入账。'
    }
  ]
}

export const resolveOrderStageTextRealtime = (order: OrderItem) =>
  resolveOrderLogisticsRealtime(order).at(-1)?.label ?? order.timeline.at(-1)?.label ?? '处理中'
