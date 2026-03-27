import type { GoodsItem, OrderItem, UserProfile } from '@shared/types'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

export interface AdminSessionSnapshot {
  profile: UserProfile | null
  accessToken: string | null
  refreshToken: string | null
}

export type AdminGoodsAuditStatus = '待审核' | '审核通过' | '已驳回'
export type AdminUserAccountStatus = '正常' | '已封禁'
export type AdminKycReviewStatus = '待审核' | '已通过' | '已驳回'
export type AdminTransferType = '自卖' | '寄卖' | '极速回收'

export interface AdminGoodsItem extends GoodsItem {
  auditStatus: AdminGoodsAuditStatus
  reviewNote: string
  transferType: AdminTransferType
  viewCount: number
  favorCount: number
  mockCertified: boolean
}

export type AdminOrderItem = OrderItem

export interface AdminUserItem extends UserProfile {
  accountStatus: AdminUserAccountStatus
  kycReviewStatus: AdminKycReviewStatus
  registerAt: string
  lastActiveAt: string
}

export interface AdminCategoryItem {
  id: string
  name: string
  description: string
  featuredBrands: string[]
  coverImage: string
}

export interface AdminDashboardChartItem {
  label: string
  value: number
}

export interface AdminDashboardTrendPoint {
  label: string
  gmv: number
  orderCount: number
}

export interface AdminDashboardData {
  totalGmv: number
  newUsersCount: number
  totalCarbonSaved: number
  pendingGoodsCount: number
  pendingKycCount: number
  completedOrdersCount: number
  auditApprovalRate: number
  kycPassRate: number
  orderCompletionRate: number
  recentGmv: AdminDashboardTrendPoint[]
  orderFunnel: AdminDashboardChartItem[]
  goodsAudit: AdminDashboardChartItem[]
  userStatus: AdminDashboardChartItem[]
  orderStatus: AdminDashboardChartItem[]
  pendingGoods: AdminGoodsItem[]
  pendingUsers: AdminUserItem[]
}

export interface AdminDatasetSnapshot {
  goods: AdminGoodsItem[]
  orders: AdminOrderItem[]
  users: AdminUserItem[]
  categories: AdminCategoryItem[]
}

export interface AdminGoodsDraft extends Omit<AdminGoodsItem, 'id'> {}

export interface AdminOrderDraft extends Omit<AdminOrderItem, 'id' | 'timeline'> {}

export interface AdminUserDraft extends Omit<AdminUserItem, 'id'> {}

export interface AdminCategoryDraft extends Omit<AdminCategoryItem, 'id'> {}
