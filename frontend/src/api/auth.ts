import type { UserProfile, UserRole } from '@/types'
import { apiClient, getAppStore } from '@/api'
import { resolveAssetUrl } from '@/utils/assets'

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

interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

interface FavoriteGoodsPayload {
  id: number
}

const legacyUsersByPhone: Record<
  string,
  {
    nickname: string
    city: string
    bio: string
  }
> = {
  '13800000001': {
    nickname: '林知夏',
    city: '上海',
    bio: '关注循环时尚与高质感生活方式。'
  },
  '13800000002': {
    nickname: '宋屿',
    city: '杭州',
    bio: '专注高端数码与奢侈品循环交易。'
  },
  '13800000003': {
    nickname: '程砚',
    city: '北京',
    bio: '负责高价值商品的鉴定与复核。'
  },
  '13800000099': {
    nickname: '平台管理员',
    city: '上海',
    bio: '负责平台审核、用户运营与内容巡检。'
  }
}

const isBrokenText = (value?: string | null) => Boolean(value && value.includes('?'))
const fallbackRoleByPhone: Record<string, UserRole> = {
  '13800000001': 'USER',
  '13800000002': 'SELLER',
  '13800000003': 'APPRAISER',
  '13800000099': 'ADMIN'
}

const toUserProfile = (payload: BackendProfile, current?: UserProfile | null): UserProfile => ({
  id: String(payload.id),
  name: isBrokenText(payload.nickname) ? legacyUsersByPhone[payload.phone]?.nickname ?? payload.nickname : payload.nickname,
  phone: payload.phone,
  password: current?.password ?? '',
  avatar: resolveAssetUrl(payload.avatar || current?.avatar || '', 'avatar'),
  city: isBrokenText(payload.city)
    ? legacyUsersByPhone[payload.phone]?.city ?? current?.city ?? '未设置'
    : payload.city || current?.city || '未设置',
  bio: isBrokenText(payload.bio)
    ? legacyUsersByPhone[payload.phone]?.bio ?? current?.bio ?? ''
    : payload.bio || current?.bio || '',
  role: (payload.role as UserRole | undefined) ?? current?.role ?? fallbackRoleByPhone[payload.phone] ?? 'USER',
  kycLevel: payload.kycLevel,
  carbonPoints: payload.carbonPoints ?? current?.carbonPoints ?? 0,
  likedGoodsIds: current?.likedGoodsIds ?? []
})

const resolveAccessToken = (payload: BackendProfile) => payload.accessToken ?? payload.token ?? null
const resolveRefreshToken = (payload: BackendProfile) => payload.refreshToken ?? null

const hydrateFavoriteGoods = async (profile: UserProfile): Promise<UserProfile> => {
  try {
    const response = await apiClient.get<ApiResponse<FavoriteGoodsPayload[]>>('/goods/my/favorites')
    profile.likedGoodsIds = response.data.data.map((item) => String(item.id))
  } catch (error) {
    console.warn('Failed to load favorite goods from backend.', error)
  }

  return profile
}

export const sendOtp = async (phone: string): Promise<void> => {
  await apiClient.post<ApiResponse<null>>('/auth/otp/send', { phone })
}

export const login = async (phone: string, otpCode: string): Promise<UserProfile | null> => {
  const store = getAppStore()
  const response = await apiClient.post<ApiResponse<BackendProfile>>('/auth/login', { phone, otpCode })
  const profile = await hydrateFavoriteGoods(toUserProfile(response.data.data, store.currentUser))
  profile.password = store.currentUser?.password ?? ''
  store.setSession(profile, resolveAccessToken(response.data.data), resolveRefreshToken(response.data.data))
  return profile
}

export const adminLogin = async (phone: string, password: string): Promise<UserProfile | null> => {
  const store = getAppStore()
  const response = await apiClient.post<ApiResponse<BackendProfile>>('/admin/auth/login', { phone, password })
  const profile = toUserProfile(response.data.data, store.currentUser)
  profile.password = store.currentUser?.password ?? ''

  if (profile.role !== 'ADMIN') {
    store.logout()
    throw new Error('当前账号没有管理员权限')
  }

  store.setSession(profile, resolveAccessToken(response.data.data), resolveRefreshToken(response.data.data))
  return profile
}

export const register = async (
  nickname: string,
  phone: string,
  otpCode: string,
  password: string
): Promise<UserProfile | null> => {
  const store = getAppStore()
  const response = await apiClient.post<ApiResponse<BackendProfile>>('/auth/register', {
    nickname,
    phone,
    otpCode,
    password
  })
  const profile = await hydrateFavoriteGoods(toUserProfile(response.data.data, store.currentUser))
  profile.password = password
  store.setSession(profile, resolveAccessToken(response.data.data), resolveRefreshToken(response.data.data))
  return profile
}

export const logout = async (): Promise<void> => {
  const store = getAppStore()
  await apiClient.post<ApiResponse<null>>('/auth/logout', {
    refreshToken: store.refreshToken
  })
  store.logout()
}

export const getProfile = async (): Promise<UserProfile | null> => {
  const store = getAppStore()
  const response = await apiClient.get<ApiResponse<BackendProfile>>('/user/me')
  const profile = await hydrateFavoriteGoods(toUserProfile(response.data.data, store.currentUser))
  profile.password = store.currentUser?.password ?? ''
  store.setSession(profile, store.authToken, store.refreshToken)
  return profile
}

export const updateProfile = async (payload: {
  name: string
  city: string
  bio: string
  avatar?: string
}): Promise<UserProfile | null> => {
  const store = getAppStore()
  const response = await apiClient.put<ApiResponse<BackendProfile>>('/user/me', {
    nickname: payload.name,
    city: payload.city,
    bio: payload.bio,
    avatar: payload.avatar
  })
  const profile = toUserProfile(response.data.data, store.currentUser)
  profile.password = store.currentUser?.password ?? ''
  store.setSession(profile, store.authToken, store.refreshToken)
  return profile
}

export const changePassword = async (payload: {
  currentPassword: string
  nextPassword: string
}): Promise<void> => {
  await apiClient.post<ApiResponse<null>>('/user/password/change', payload)
  getAppStore().changePassword(payload)
}

export const submitKyc = async (payload: {
  realName: string
  idNumber: string
}): Promise<UserProfile | null> => {
  const store = getAppStore()
  const response = await apiClient.post<ApiResponse<BackendProfile>>('/user/kyc/submit', payload)
  const profile = toUserProfile(response.data.data, store.currentUser)
  profile.password = store.currentUser?.password ?? ''
  store.setSession(profile, store.authToken, store.refreshToken)
  return profile
}
