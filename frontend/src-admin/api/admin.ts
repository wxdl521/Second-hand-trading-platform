import type {
  AdminDashboardChartItem,
  AdminDashboardData,
  AdminCategoryDraft,
  AdminCategoryItem,
  AdminGoodsDraft,
  AdminGoodsItem,
  AdminOrderDraft,
  AdminOrderItem,
  AdminUserDraft,
  AdminUserItem,
  ApiResponse
} from '@admin/types'
import { adminApiClient } from '@admin/api/client'
import { normalizeAdminGoods, normalizeAdminUser } from '@admin/utils/dataset'
import { resolveAssetUrl } from '@shared/utils/assets'
import type { UserProfile } from '@shared/types'

interface BackendProfile {
  id: number
  nickname: string
  phone: string
  city: string
  avatar: string
  kycLevel: 'L1' | 'L2' | 'L3'
  bio?: string
  role?: string
  carbonPoints?: number
  token?: string | null
  accessToken?: string | null
  refreshToken?: string | null
}

interface BackendAdminGoods {
  id: number
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
  sellerLevel: 'L1' | 'L2' | 'L3'
  story?: string
  description?: string
  status: '草稿' | '待审核' | '在售中' | '已预订' | '已售出' | '已下架' | 'DRAFT' | 'PENDING_APPRAISAL' | 'ON_SALE' | 'RESERVED' | 'SOLD' | 'OFF_SHELF'
  tags?: string[]
  heroImage: string
  gallery?: string[]
  createdAt?: string
  auditStatus: '待审核' | '审核通过' | '已驳回'
  reviewNote?: string
  transferType: '自卖' | '寄卖' | '极速回收'
  viewCount?: number
  favorCount?: number
  mockCertified?: boolean
}

interface BackendAdminUser {
  id: number
  name: string
  phone: string
  password?: string
  avatar: string
  city: string
  bio?: string
  role: UserProfile['role']
  kycLevel: 'L1' | 'L2' | 'L3'
  carbonPoints?: number
  accountStatus: '正常' | '已封禁'
  kycReviewStatus: '待审核' | '已通过' | '已驳回'
  registerAt?: string
  lastActiveAt?: string
}

interface BackendOrder {
  id: number
  goodsId: number
  buyerName: string
  amount: number
  status: 'PENDING_PAYMENT' | 'PENDING_SHIPMENT' | 'IN_TRANSIT' | 'COMPLETED'
  createdAt?: string
}

interface BackendDashboardTrendPoint {
  label: string
  gmv: number
  orderCount: number
}

interface BackendDashboard {
  totalGmv: number
  newUsersCount: number
  totalCarbonSaved: number
  pendingGoodsCount: number
  pendingKycCount: number
  completedOrdersCount: number
  auditApprovalRate: number
  kycPassRate: number
  orderCompletionRate: number
  recentGmv: BackendDashboardTrendPoint[]
  orderFunnel: AdminDashboardChartItem[]
  goodsAudit: AdminDashboardChartItem[]
  userStatus: AdminDashboardChartItem[]
  orderStatus: AdminDashboardChartItem[]
  pendingGoods: BackendAdminGoods[]
  pendingUsers: BackendAdminUser[]
}

interface BackendAdminCategory {
  id: number
  name: string
  description?: string
  featuredBrands?: string[]
  coverImage?: string
}

const roleByPhone: Record<string, UserProfile['role']> = {
  '13800000001': 'USER',
  '13800000002': 'SELLER',
  '13800000003': 'APPRAISER',
  '13800000099': 'ADMIN'
}

const toSessionPayload = (payload: BackendProfile) => ({
  profile: toSharedUser(payload),
  accessToken: payload.accessToken ?? payload.token ?? null,
  refreshToken: payload.refreshToken ?? null
})

const mapAdminGoodsStatus = (status?: BackendAdminGoods['status']): AdminGoodsItem['status'] => {
  if (status === 'DRAFT' || status === '草稿') return '草稿'
  if (status === 'PENDING_APPRAISAL' || status === '待审核') return '待审核'
  if (status === 'OFF_SHELF' || status === '已下架') return '已下架'
  if (status === 'RESERVED' || status === '已预订') return '已预订'
  if (status === 'SOLD' || status === '已售出') return '已售出'
  return '在售中'
}

const mapGoods = (payload: BackendAdminGoods): AdminGoodsItem =>
  normalizeAdminGoods({
    id: String(payload.id),
    title: payload.title,
    category: payload.category,
    brand: payload.brand,
    condition: payload.condition || '95 新',
    price: Number(payload.price),
    originalPrice: Number(payload.originalPrice),
    carbonSavedKg: payload.carbonSavedKg,
    aiPrice: Number(payload.aiPrice),
    city: payload.city || '未设置',
    sellerName: payload.sellerName || '平台卖家',
    sellerLevel: payload.sellerLevel || 'L2',
    story: payload.story || '暂无商品说明',
    description: payload.description || payload.story || '暂无商品说明',
    status: mapAdminGoodsStatus(payload.status),
    tags: payload.tags ?? [],
    heroImage: resolveAssetUrl(payload.heroImage, 'goods'),
    gallery: (payload.gallery?.length ? payload.gallery : [payload.heroImage]).map((item) => resolveAssetUrl(item, 'goods')),
    createdAt: payload.createdAt || new Date().toISOString().slice(0, 10),
    auditStatus: payload.auditStatus,
    reviewNote: payload.reviewNote || '审核通过，允许正常流转。',
    transferType: payload.transferType || '自卖',
    viewCount: Number(payload.viewCount ?? 0),
    favorCount: Number(payload.favorCount ?? 0),
    mockCertified: Boolean(payload.mockCertified)
  })

const toSharedUser = (payload: BackendAdminUser | BackendProfile): AdminUserItem =>
  normalizeAdminUser({
    id: String(payload.id),
    name: 'name' in payload ? payload.name : payload.nickname,
    phone: payload.phone,
    password: 'password' in payload ? payload.password || '' : '',
    avatar: resolveAssetUrl(payload.avatar, 'avatar'),
    city: payload.city || '未设置',
    bio: payload.bio || '',
    role: (payload.role as UserProfile['role']) ?? roleByPhone[payload.phone] ?? 'USER',
    kycLevel: payload.kycLevel,
    carbonPoints: payload.carbonPoints ?? 0,
    likedGoodsIds: [],
    accountStatus: 'accountStatus' in payload ? payload.accountStatus : undefined,
    kycReviewStatus: 'kycReviewStatus' in payload ? payload.kycReviewStatus : undefined,
    registerAt: 'registerAt' in payload ? payload.registerAt : undefined,
    lastActiveAt: 'lastActiveAt' in payload ? payload.lastActiveAt : undefined
  })

const toGoodsPayload = (payload: AdminGoodsDraft) => ({
  title: payload.title,
  category: payload.category,
  brand: payload.brand,
  condition: payload.condition,
  price: payload.price,
  originalPrice: payload.originalPrice,
  carbonSavedKg: payload.carbonSavedKg,
  aiPrice: payload.aiPrice,
  city: payload.city,
  sellerName: payload.sellerName,
  sellerLevel: payload.sellerLevel,
  story: payload.story,
  description: payload.description,
  status: payload.status,
  tags: payload.tags,
  heroImage: payload.heroImage,
  gallery: payload.gallery,
  createdAt: payload.createdAt,
  auditStatus: payload.auditStatus,
  reviewNote: payload.reviewNote,
  transferType: payload.transferType,
  viewCount: payload.viewCount,
  favorCount: payload.favorCount,
  mockCertified: payload.mockCertified
})

const toUserPayload = (payload: AdminUserDraft) => ({
  name: payload.name,
  phone: payload.phone,
  password: payload.password,
  avatar: payload.avatar,
  city: payload.city,
  bio: payload.bio,
  role: payload.role,
  kycLevel: payload.kycLevel,
  carbonPoints: payload.carbonPoints,
  accountStatus: payload.accountStatus,
  kycReviewStatus: payload.kycReviewStatus,
  registerAt: payload.registerAt,
  lastActiveAt: payload.lastActiveAt
})

const normalizeOrderId = (id: string | number) => {
  const value = String(id)
  return value.startsWith('order-') ? value.slice(6) : value
}

const mapOrderStatus = (status: BackendOrder['status']): AdminOrderItem['status'] =>
  status === 'PENDING_SHIPMENT'
    ? '待发货'
    : status === 'IN_TRANSIT'
      ? '运输中'
      : status === 'COMPLETED'
        ? '已完成'
        : '待付款'

const buildOrderTimeline = (createdAt: string, status: AdminOrderItem['status']) => {
  const timeline = [{ label: '订单创建', time: createdAt }]

  if (status === '待发货' || status === '运输中' || status === '已完成') {
    timeline.push({ label: '完成支付', time: createdAt })
  }

  if (status === '运输中' || status === '已完成') {
    timeline.push({ label: '卖家发货', time: createdAt })
  }

  if (status === '已完成') {
    timeline.push({ label: '交易完成', time: createdAt })
  }

  return timeline
}

const mapOrder = (payload: BackendOrder): AdminOrderItem => {
  const createdAt = payload.createdAt
    ? String(payload.createdAt).replace('T', ' ').slice(0, 19)
    : new Date().toLocaleString('zh-CN', { hour12: false })

  const status = mapOrderStatus(payload.status)

  return {
    id: normalizeOrderId(payload.id),
    goodsId: String(payload.goodsId),
    amount: Number(payload.amount),
    buyerName: payload.buyerName,
    status,
    createdAt,
    timeline: buildOrderTimeline(createdAt, status)
  }
}

const mapDashboard = (payload: BackendDashboard): AdminDashboardData => ({
  totalGmv: Number(payload.totalGmv ?? 0),
  newUsersCount: Number(payload.newUsersCount ?? 0),
  totalCarbonSaved: Number(payload.totalCarbonSaved ?? 0),
  pendingGoodsCount: Number(payload.pendingGoodsCount ?? 0),
  pendingKycCount: Number(payload.pendingKycCount ?? 0),
  completedOrdersCount: Number(payload.completedOrdersCount ?? 0),
  auditApprovalRate: Number(payload.auditApprovalRate ?? 0),
  kycPassRate: Number(payload.kycPassRate ?? 0),
  orderCompletionRate: Number(payload.orderCompletionRate ?? 0),
  recentGmv: (payload.recentGmv ?? []).map((item) => ({
    label: item.label,
    gmv: Number(item.gmv ?? 0),
    orderCount: Number(item.orderCount ?? 0)
  })),
  orderFunnel: (payload.orderFunnel ?? []).map((item) => ({ label: item.label, value: Number(item.value) })),
  goodsAudit: (payload.goodsAudit ?? []).map((item) => ({ label: item.label, value: Number(item.value) })),
  userStatus: (payload.userStatus ?? []).map((item) => ({ label: item.label, value: Number(item.value) })),
  orderStatus: (payload.orderStatus ?? []).map((item) => ({ label: item.label, value: Number(item.value) })),
  pendingGoods: (payload.pendingGoods ?? []).map(mapGoods),
  pendingUsers: (payload.pendingUsers ?? []).map(toSharedUser)
})

const mapCategory = (payload: BackendAdminCategory): AdminCategoryItem => ({
  id: String(payload.id),
  name: payload.name,
  description: payload.description || '',
  featuredBrands: payload.featuredBrands ?? [],
  coverImage: resolveAssetUrl(payload.coverImage, 'goods')
})

const toOrderPayload = (payload: AdminOrderDraft) => ({
  goodsId: Number(payload.goodsId),
  buyerName: payload.buyerName,
  amount: payload.amount,
  status: payload.status,
  createdAt: payload.createdAt
})

const toCategoryPayload = (payload: AdminCategoryDraft) => ({
  name: payload.name,
  description: payload.description,
  featuredBrands: payload.featuredBrands,
  coverImage: payload.coverImage
})

export const adminLogin = async (phone: string, password: string) => {
  const response = await adminApiClient.post<ApiResponse<BackendProfile>>('/admin/auth/login', {
    phone,
    password
  })
  return toSessionPayload(response.data.data)
}

export const adminLogout = async (refreshToken: string | null) => {
  try {
    await adminApiClient.post('/auth/logout', { refreshToken })
  } catch (error) {
    console.warn('Admin remote logout failed.', error)
  }
}

export const listAdminUsers = async (): Promise<AdminUserItem[]> => {
  const response = await adminApiClient.get<ApiResponse<BackendAdminUser[]>>('/admin/users')
  return response.data.data.map(toSharedUser)
}

export const listAdminGoods = async (): Promise<AdminGoodsItem[]> => {
  const response = await adminApiClient.get<ApiResponse<BackendAdminGoods[]>>('/admin/goods')
  return response.data.data.map(mapGoods)
}

export const listAdminOrders = async (): Promise<AdminOrderItem[]> => {
  const response = await adminApiClient.get<ApiResponse<BackendOrder[]>>('/admin/orders')
  return response.data.data.map(mapOrder)
}

export const getAdminDashboard = async (): Promise<AdminDashboardData> => {
  const response = await adminApiClient.get<ApiResponse<BackendDashboard>>('/admin/dashboard')
  return mapDashboard(response.data.data)
}

export const listAdminCategories = async (): Promise<AdminCategoryItem[]> => {
  const response = await adminApiClient.get<ApiResponse<BackendAdminCategory[]>>('/admin/categories')
  return response.data.data.map(mapCategory)
}

export const createAdminOrder = async (payload: AdminOrderDraft): Promise<AdminOrderItem> => {
  const response = await adminApiClient.post<ApiResponse<BackendOrder>>('/admin/orders', toOrderPayload(payload))
  return mapOrder(response.data.data)
}

export const updateAdminOrder = async (id: string, payload: AdminOrderDraft): Promise<AdminOrderItem> => {
  const response = await adminApiClient.put<ApiResponse<BackendOrder>>(
    `/admin/orders/${normalizeOrderId(id)}`,
    toOrderPayload(payload)
  )
  return mapOrder(response.data.data)
}

export const deleteAdminOrder = async (id: string) => {
  await adminApiClient.delete(`/admin/orders/${normalizeOrderId(id)}`)
}

export const createAdminCategory = async (payload: AdminCategoryDraft): Promise<AdminCategoryItem> => {
  const response = await adminApiClient.post<ApiResponse<BackendAdminCategory>>('/admin/categories', toCategoryPayload(payload))
  return mapCategory(response.data.data)
}

export const updateAdminCategory = async (id: string, payload: AdminCategoryDraft): Promise<AdminCategoryItem> => {
  const response = await adminApiClient.put<ApiResponse<BackendAdminCategory>>(`/admin/categories/${id}`, toCategoryPayload(payload))
  return mapCategory(response.data.data)
}

export const deleteAdminCategory = async (id: string) => {
  await adminApiClient.delete(`/admin/categories/${id}`)
}

export const createAdminGoods = async (payload: AdminGoodsDraft): Promise<AdminGoodsItem> => {
  const response = await adminApiClient.post<ApiResponse<BackendAdminGoods>>('/admin/goods', toGoodsPayload(payload))
  return mapGoods(response.data.data)
}

export const updateAdminGoods = async (id: string, payload: AdminGoodsDraft): Promise<AdminGoodsItem> => {
  const response = await adminApiClient.put<ApiResponse<BackendAdminGoods>>(`/admin/goods/${id}`, toGoodsPayload(payload))
  return mapGoods(response.data.data)
}

export const deleteAdminGoods = async (id: string) => {
  await adminApiClient.delete(`/admin/goods/${id}`)
}

export const approveAdminGoods = async (id: string, reviewNote?: string): Promise<AdminGoodsItem> => {
  const response = await adminApiClient.post<ApiResponse<BackendAdminGoods>>(`/admin/goods/${id}/approve`, { reviewNote })
  return mapGoods(response.data.data)
}

export const rejectAdminGoods = async (id: string, reviewNote: string): Promise<AdminGoodsItem> => {
  const response = await adminApiClient.post<ApiResponse<BackendAdminGoods>>(`/admin/goods/${id}/reject`, { reviewNote })
  return mapGoods(response.data.data)
}

export const createAdminUser = async (payload: AdminUserDraft): Promise<AdminUserItem> => {
  const response = await adminApiClient.post<ApiResponse<BackendAdminUser>>('/admin/users', toUserPayload(payload))
  return toSharedUser(response.data.data)
}

export const updateAdminUser = async (id: string, payload: AdminUserDraft): Promise<AdminUserItem> => {
  const response = await adminApiClient.put<ApiResponse<BackendAdminUser>>(`/admin/users/${id}`, toUserPayload(payload))
  return toSharedUser(response.data.data)
}

export const deleteAdminUser = async (id: string) => {
  await adminApiClient.delete(`/admin/users/${id}`)
}

export const approveAdminUserKyc = async (id: string): Promise<AdminUserItem> => {
  const response = await adminApiClient.post<ApiResponse<BackendAdminUser>>(`/admin/users/${id}/kyc/approve`)
  return toSharedUser(response.data.data)
}

export const rejectAdminUserKyc = async (id: string): Promise<AdminUserItem> => {
  const response = await adminApiClient.post<ApiResponse<BackendAdminUser>>(`/admin/users/${id}/kyc/reject`)
  return toSharedUser(response.data.data)
}

export const banAdminUser = async (id: string): Promise<AdminUserItem> => {
  const response = await adminApiClient.post<ApiResponse<BackendAdminUser>>(`/admin/users/${id}/ban`)
  return toSharedUser(response.data.data)
}

export const unbanAdminUser = async (id: string): Promise<AdminUserItem> => {
  const response = await adminApiClient.post<ApiResponse<BackendAdminUser>>(`/admin/users/${id}/unban`)
  return toSharedUser(response.data.data)
}
