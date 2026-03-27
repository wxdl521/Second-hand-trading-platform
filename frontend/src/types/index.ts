export type GoodsStatus = '在售中' | '已预订' | '已售出'

export type SellerGoodsStatus = '草稿' | '待审核' | '在售中' | '已预订' | '已售出' | '已下架'

export type OrderStatus = '待付款' | '待发货' | '运输中' | '已完成' | '已退款'

export type AppraiseMode = 'AI 快速鉴定' | '视频连线鉴定' | '线下到店鉴定'
export type AppraiseStatus = '待确认' | '已预约'

export type KycLevel = 'L1' | 'L2' | 'L3'
export type UserRole = 'USER' | 'SELLER' | 'APPRAISER' | 'ADMIN'

export interface UserProfile {
  id: string
  name: string
  phone: string
  password: string
  avatar: string
  city: string
  bio: string
  role: UserRole
  kycLevel: KycLevel
  carbonPoints: number
  likedGoodsIds: string[]
}

export interface GoodsItem {
  id: string
  title: string
  category: string
  brand: string
  condition: string
  price: number
  originalPrice: number
  carbonSavedKg: number
  aiPrice: number
  city: string
  sellerName: string
  sellerLevel: KycLevel
  story: string
  description: string
  status: SellerGoodsStatus
  tags: string[]
  heroImage: string
  gallery: string[]
  createdAt: string
  favorCount?: number
  auditStatus?: string
  reviewNote?: string
  mockCertified?: boolean
}

export interface OrderTimeline {
  label: string
  time: string
}

export interface OrderItem {
  id: string
  goodsId: string
  amount: number
  buyerName: string
  status: OrderStatus
  createdAt: string
  paidAt?: string
  shippedAt?: string
  completedAt?: string
  refundedAt?: string
  shippingCompany?: string
  trackingNo?: string
  servicePhone?: string
  deliveryAddress?: string
  timeline: OrderTimeline[]
}

export interface CarbonSummary {
  balance: number
  level: string
  monthCarbonSavedKg: number
}

export interface CarbonRecord {
  id: string
  title: string
  points: number
  type: '收入' | '支出'
  date: string
  description: string
}

export interface AppraiseAppointment {
  id: string
  goodsTitle: string
  mode: AppraiseMode
  date: string
  note: string
  status: AppraiseStatus
}

export interface EstimateInput {
  title: string
  category: string
  brand: string
  condition: string
  yearsUsed: number
  rarity: number
  description?: string
  imageUrls?: string[]
}

export interface EstimateResult {
  price: number
  low: number
  high: number
  confidence: number
  carbonSavedKg: number
  summary: string
  tips: string[]
}

