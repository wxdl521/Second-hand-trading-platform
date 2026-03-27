import type { GoodsItem } from '@/types'
import { apiClient, getAppStore } from '@/api'
import { resolveAssetUrl } from '@/utils/assets'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

interface GoodsListPayload {
  id: number
  title: string
  category: string
  brand: string
  conditionLevel?: string
  coverUrl: string
  salePrice: number
  aiPrice: number
  carbonSavedKg: number
  story?: string
  sellerName?: string
  sellerLevel?: 'L1' | 'L2' | 'L3'
  city?: string
  status: string
  auditStatus?: string
  reviewNote?: string
  favorCount?: number
  mockCertified?: boolean
  createdAt?: string
}

interface GoodsDetailPayload {
  id: number
  title: string
  category: string
  brand: string
  conditionLevel: string
  salePrice: number
  originalPrice: number
  aiPrice: number
  description: string
  story: string
  sellerName: string
  sellerLevel: 'L1' | 'L2' | 'L3'
  city: string
  status: string
  auditStatus?: string
  reviewNote?: string
  favorCount?: number
  mockCertified?: boolean
  tags: string[]
  gallery: string[]
}

interface GoodsCategoryPayload {
  name: string
  count: number
}

interface LegacyDemoGoods {
  title: string
  category: string
  condition: string
  originalPrice: number
  city: string
  sellerName: string
  sellerLevel: 'L1' | 'L2' | 'L3'
  story: string
  description: string
  tags: string[]
  heroImage: string
  gallery: string[]
}

export interface GoodsListQuery {
  keyword?: string
  category?: string
  maxPrice?: number
  sort?: 'latest' | 'priceAsc' | 'priceDesc'
}

export interface GoodsCategoryStat {
  name: string
  count: number
}

export interface GoodsMutationPayload {
  title: string
  category: string
  brand: string
  condition: string
  price: number
  description: string
  story: string
  tags: string[]
  imageUrls?: string[]
}

const ALL_CATEGORY = '全部'

const legacyDemoGoodsByKey: Record<string, LegacyDemoGoods> = {
  chanel: {
    title: '香奈儿 Classic Flap 中号链条包',
    category: '奢品箱包',
    condition: '95 新',
    originalPrice: 58999,
    city: '上海',
    sellerName: '苏黎',
    sellerLevel: 'L3',
    story: '陪伴过两次晚宴，保养得很好，五金成色优秀。',
    description: '附原盒、防尘袋、购入小票，支持视频验货与线下复核。',
    tags: ['经典款', '全套附件', '保值款'],
    heroImage: '/uploads/demo-chanel.svg',
    gallery: ['/uploads/demo-chanel.svg', '/uploads/demo-chanel-detail.svg']
  },
  rolex: {
    title: '劳力士 Datejust 36 蓝盘腕表',
    category: '珠宝腕表',
    condition: '98 新',
    originalPrice: 75999,
    city: '深圳',
    sellerName: '周衡',
    sellerLevel: 'L3',
    story: '2024 年购入，仅日常通勤佩戴，走时稳定。',
    description: '支持平台寄售与鉴定预约，附保卡和原装表节。',
    tags: ['蓝盘', '保卡齐全', '支持寄售'],
    heroImage: '/uploads/demo-rolex.svg',
    gallery: ['/uploads/demo-rolex.svg', '/uploads/demo-rolex-detail.svg']
  },
  leica: {
    title: '徕卡 Q3 全画幅相机套装',
    category: '数码设备',
    condition: '92 新',
    originalPrice: 46888,
    city: '北京',
    sellerName: '江沅',
    sellerLevel: 'L2',
    story: '主要用于旅行拍摄，快门次数很低。',
    description: '带手柄与备用电池，平台 AI 估价波动较小。',
    tags: ['低快门', '摄影好物', '配件齐'],
    heroImage: '/uploads/demo-leica.svg',
    gallery: ['/uploads/demo-leica.svg', '/uploads/demo-leica-detail.svg']
  }
}

const isBrokenText = (value?: string | null) => Boolean(value && value.includes('?'))

const findLegacyDemoGoods = (payload: {
  brand: string
  title?: string
  coverUrl?: string
  gallery?: string[]
}) => {
  const candidate = [payload.coverUrl, ...(payload.gallery ?? []), payload.title].filter(Boolean).join(' ')

  if (payload.brand === 'Chanel' && /Classic Flap|demo-chanel/i.test(candidate)) {
    return legacyDemoGoodsByKey.chanel
  }
  if (payload.brand === 'Rolex' && /Datejust|demo-rolex/i.test(candidate)) {
    return legacyDemoGoodsByKey.rolex
  }
  if (payload.brand === 'Leica' && /\bQ3\b|demo-leica/i.test(candidate)) {
    return legacyDemoGoodsByKey.leica
  }

  return null
}

const normalizeGallery = (images: string[]) => images.map((image) => resolveAssetUrl(image, 'goods'))
const normalizeGoodsCreatedAt = (value?: string) =>
  value ? String(value).replace('T', ' ').slice(0, 10) : new Date().toISOString().slice(0, 10)

const toGoodsMutationPayload = (payload: GoodsMutationPayload) => ({
  title: payload.title,
  category: payload.category,
  brand: payload.brand,
  conditionLevel: payload.condition,
  salePrice: payload.price,
  story: payload.story,
  tags: payload.tags.join(','),
  description: payload.description,
  imageUrls: payload.imageUrls ?? []
})

const toBackendGoodsStatusParam = (status?: GoodsItem['status'] | '全部') => {
  if (!status || status === '全部') {
    return undefined
  }

  if (status === '草稿') return 'DRAFT'
  if (status === '待审核') return 'PENDING_APPRAISAL'
  if (status === '已下架') return 'OFF_SHELF'
  if (status === '已预订') return 'RESERVED'
  if (status === '已售出') return 'SOLD'
  return 'ON_SALE'
}

const mapGoodsStatus = (status: string): GoodsItem['status'] => {
  if (status === 'RESERVED') return '已预订'
  if (status === 'SOLD') return '已售出'
  return '在售中'
}

const mapBackendGoodsStatus = (status: string): GoodsItem['status'] => {
  if (status === 'DRAFT') return '草稿' as GoodsItem['status']
  if (status === 'PENDING_APPRAISAL') return '待审核' as GoodsItem['status']
  if (status === 'OFF_SHELF') return '已下架' as GoodsItem['status']
  if (status === 'RESERVED') return '已预订' as GoodsItem['status']
  if (status === 'SOLD') return '已售出' as GoodsItem['status']
  return '在售中' as GoodsItem['status']
}

const sortGoods = (goods: GoodsItem[], sortMode: GoodsListQuery['sort'] = 'latest') =>
  [...goods].sort((left, right) => {
    if (sortMode === 'priceAsc') return left.price - right.price
    if (sortMode === 'priceDesc') return right.price - left.price
    return right.createdAt.localeCompare(left.createdAt)
  })

const mergeCatalogs = (...catalogGroups: GoodsItem[][]) => {
  const catalog = new Map<string, GoodsItem>()
  const titleKeys = new Set<string>()

  catalogGroups.flat().forEach((item) => {
    const key = `${item.brand}-${item.title}`
    if (!catalog.has(item.id) && !titleKeys.has(key)) {
      catalog.set(item.id, item)
      titleKeys.add(key)
    }
  })

  return Array.from(catalog.values())
}

const mapListGoods = (payload: GoodsListPayload, current?: GoodsItem): GoodsItem => {
  const legacy = findLegacyDemoGoods(payload)

  return {
    id: String(payload.id),
    title: isBrokenText(payload.title) ? legacy?.title ?? current?.title ?? payload.title : payload.title,
    category: isBrokenText(payload.category)
      ? legacy?.category ?? current?.category ?? payload.category
      : payload.category,
    brand: payload.brand,
    condition: isBrokenText(payload.conditionLevel)
      ? current?.condition ?? legacy?.condition ?? '95 新'
      : payload.conditionLevel ?? current?.condition ?? legacy?.condition ?? '95 新',
    price: Number(payload.salePrice),
    originalPrice: current?.originalPrice ?? legacy?.originalPrice ?? Math.round(Number(payload.salePrice) * 1.6),
    carbonSavedKg: payload.carbonSavedKg,
    aiPrice: Number(payload.aiPrice),
    city: isBrokenText(payload.city) ? current?.city ?? legacy?.city ?? '上海' : payload.city ?? current?.city ?? legacy?.city ?? '上海',
    sellerName: isBrokenText(payload.sellerName)
      ? current?.sellerName ?? legacy?.sellerName ?? '平台卖家'
      : payload.sellerName ?? current?.sellerName ?? legacy?.sellerName ?? '平台卖家',
    sellerLevel: payload.sellerLevel ?? current?.sellerLevel ?? legacy?.sellerLevel ?? 'L2',
    story: isBrokenText(payload.story) ? legacy?.story ?? current?.story ?? '' : payload.story || current?.story || '',
    description: current?.description ?? legacy?.description ?? '',
    status: mapBackendGoodsStatus(payload.status),
    auditStatus: payload.auditStatus ?? current?.auditStatus,
    reviewNote: payload.reviewNote ?? current?.reviewNote,
    favorCount: Number(payload.favorCount ?? current?.favorCount ?? 0),
    mockCertified: Boolean(payload.mockCertified ?? current?.mockCertified ?? false),
    tags: current?.tags ?? legacy?.tags ?? [],
    heroImage: resolveAssetUrl(legacy?.heroImage ?? payload.coverUrl, 'goods'),
    gallery: normalizeGallery(current?.gallery ?? legacy?.gallery ?? [payload.coverUrl]),
    createdAt: payload.createdAt ? normalizeGoodsCreatedAt(payload.createdAt) : current?.createdAt ?? new Date().toISOString().slice(0, 10)
  }
}

const mapDetailGoods = (payload: GoodsDetailPayload, current?: GoodsItem): GoodsItem => {
  const legacy = findLegacyDemoGoods({
    brand: payload.brand,
    title: payload.title,
    gallery: payload.gallery
  })
  const gallery = payload.gallery.length ? payload.gallery : legacy?.gallery ?? current?.gallery ?? []

  return {
    id: String(payload.id),
    title: isBrokenText(payload.title) ? legacy?.title ?? current?.title ?? payload.title : payload.title,
    category: isBrokenText(payload.category)
      ? legacy?.category ?? current?.category ?? payload.category
      : payload.category,
    brand: payload.brand,
    condition: isBrokenText(payload.conditionLevel)
      ? legacy?.condition ?? current?.condition ?? payload.conditionLevel
      : payload.conditionLevel,
    price: Number(payload.salePrice),
    originalPrice: Number(payload.originalPrice) || legacy?.originalPrice || current?.originalPrice || 0,
    carbonSavedKg: current?.carbonSavedKg ?? Math.max(1, Math.round(Number(payload.salePrice) / 500)),
    aiPrice: Number(payload.aiPrice),
    city: isBrokenText(payload.city) ? legacy?.city ?? current?.city ?? payload.city : payload.city,
    sellerName: isBrokenText(payload.sellerName)
      ? legacy?.sellerName ?? current?.sellerName ?? payload.sellerName
      : payload.sellerName,
    sellerLevel: payload.sellerLevel,
    story: isBrokenText(payload.story) ? legacy?.story ?? current?.story ?? payload.story : payload.story,
    description: isBrokenText(payload.description)
      ? legacy?.description ?? current?.description ?? payload.description
      : payload.description,
    status: mapBackendGoodsStatus(payload.status),
    favorCount: Number(payload.favorCount ?? current?.favorCount ?? 0),
    auditStatus: payload.auditStatus ?? current?.auditStatus,
    reviewNote: payload.reviewNote ?? current?.reviewNote,
    mockCertified: Boolean(payload.mockCertified ?? current?.mockCertified ?? false),
    tags:
      payload.tags.length && !payload.tags.some((tag) => isBrokenText(tag))
        ? payload.tags
        : legacy?.tags ?? current?.tags ?? [],
    heroImage: resolveAssetUrl(gallery[0] || legacy?.heroImage || current?.heroImage || '', 'goods'),
    gallery: normalizeGallery(gallery),
    createdAt: current?.createdAt ?? new Date().toISOString().slice(0, 10)
  }
}

const normalizeGoodsQuery = (query: GoodsListQuery = {}) => {
  const keyword = query.keyword?.trim() || undefined
  const category =
    query.category && query.category.trim() && query.category.trim() !== ALL_CATEGORY
      ? query.category.trim()
      : undefined
  const maxPrice =
    typeof query.maxPrice === 'number' && Number.isFinite(query.maxPrice)
      ? Math.max(0, query.maxPrice)
      : undefined
  const sort = query.sort ?? 'latest'

  return {
    keyword,
    category,
    maxPrice,
    sort,
    hasScopedQuery: Boolean(keyword || category || typeof maxPrice === 'number')
  }
}

const buildGoodsParams = (query: ReturnType<typeof normalizeGoodsQuery>) => ({
  ...(query.keyword ? { q: query.keyword } : {}),
  ...(query.category ? { category: query.category } : {}),
  ...(typeof query.maxPrice === 'number' ? { maxPrice: query.maxPrice } : {}),
  ...(query.sort !== 'latest' ? { sort: query.sort } : {})
})

const applyLocalGoodsQuery = (goods: GoodsItem[], query: GoodsListQuery = {}) => {
  const normalizedQuery = normalizeGoodsQuery(query)
  const keyword = normalizedQuery.keyword?.toLowerCase()

  return sortGoods(
    goods.filter((item) => {
      const matchesKeyword =
        !keyword ||
        [item.title, item.brand, item.category, item.story, item.description]
          .join(' ')
          .toLowerCase()
          .includes(keyword)
      const matchesCategory = !normalizedQuery.category || item.category === normalizedQuery.category
      const matchesPrice =
        typeof normalizedQuery.maxPrice !== 'number' || item.price <= normalizedQuery.maxPrice
      return matchesKeyword && matchesCategory && matchesPrice
    }),
    normalizedQuery.sort
  )
}

export const listGoods = async (query: GoodsListQuery = {}): Promise<GoodsItem[]> => {
  const store = getAppStore()
  const normalizedQuery = normalizeGoodsQuery(query)
  const response = await apiClient.get<ApiResponse<GoodsListPayload[]>>('/goods', {
    params: buildGoodsParams(normalizedQuery)
  })
  const goods = sortGoods(
    response.data.data.map((item) => mapListGoods(item, store.goods.find((goodsItem) => goodsItem.id === String(item.id)))),
    normalizedQuery.sort
  )

  if (!normalizedQuery.hasScopedQuery) {
    store.setGoods(goods)
  }

  return goods
}

export const listGoodsCategories = async (): Promise<GoodsCategoryStat[]> => {
  const response = await apiClient.get<ApiResponse<GoodsCategoryPayload[]>>('/goods/categories')
  return response.data.data
    .filter((item) => item.name)
    .map((item) => ({
      name: item.name,
      count: Number(item.count) || 0
    }))
}

export const getGoodsDetail = async (id: string): Promise<GoodsItem | undefined> => {
  const store = getAppStore()
  const response = await apiClient.get<ApiResponse<GoodsDetailPayload>>(`/goods/${id}`)
  const goods = mapDetailGoods(response.data.data, store.goods.find((item) => item.id === id))
  store.upsertGoods(goods)
  return goods
}

export const listMyGoods = async (status?: GoodsItem['status'] | '全部'): Promise<GoodsItem[]> => {
  const store = getAppStore()
  const response = await apiClient.get<ApiResponse<GoodsListPayload[]>>('/goods/my', {
    params: toBackendGoodsStatusParam(status) ? { status: toBackendGoodsStatusParam(status) } : {}
  })
  const goods = response.data.data.map((item) =>
    mapListGoods(item, store.goods.find((goodsItem) => goodsItem.id === String(item.id)))
  )
  goods.forEach((item) => store.upsertGoods(item))
  return goods
}

export const saveGoodsDraft = async (payload: GoodsMutationPayload): Promise<GoodsItem> => {
  const store = getAppStore()
  const response = await apiClient.post<ApiResponse<GoodsDetailPayload>>('/goods', toGoodsMutationPayload(payload))
  const goods = mapDetailGoods(response.data.data)
  store.upsertGoods(goods)
  return goods
}

export const updateOwnedGoods = async (id: string, payload: GoodsMutationPayload): Promise<GoodsItem> => {
  const store = getAppStore()
  const response = await apiClient.put<ApiResponse<GoodsDetailPayload>>(`/goods/${id}`, toGoodsMutationPayload(payload))
  const goods = mapDetailGoods(response.data.data, store.goods.find((item) => item.id === id))
  store.upsertGoods(goods)
  return goods
}

export const publishOwnedGoods = async (id: string): Promise<GoodsItem> => {
  const store = getAppStore()
  const response = await apiClient.post<ApiResponse<GoodsDetailPayload>>(`/goods/${id}/publish`)
  const goods = mapDetailGoods(response.data.data, store.goods.find((item) => item.id === id))
  store.upsertGoods(goods)
  return goods
}

export const deleteOwnedGoods = async (id: string): Promise<void> => {
  const store = getAppStore()
  await apiClient.delete(`/goods/${id}`)
  const current = store.goods.find((item) => item.id === id)
  if (current) {
    store.upsertGoods({
      ...current,
      status: '已下架',
      reviewNote: '商品已下架。'
    })
  }
}

export const publishGoods = async (payload: GoodsMutationPayload): Promise<GoodsItem> => {
  const draft = await saveGoodsDraft(payload)
  return publishOwnedGoods(draft.id)
}

export const toggleFavoriteGoods = async (goodsId: string): Promise<void> => {
  const store = getAppStore()
  const liked = store.currentUser?.likedGoodsIds.includes(goodsId)
  if (liked) {
    await apiClient.delete(`/goods/${goodsId}/favorite`)
  } else {
    await apiClient.post(`/goods/${goodsId}/favorite`)
  }
  store.toggleFavorite(goodsId)
}

export const listFavoriteGoods = async (): Promise<GoodsItem[]> => {
  const store = getAppStore()
  const response = await apiClient.get<ApiResponse<GoodsListPayload[]>>('/goods/my/favorites')
  const goods = response.data.data.map((item) =>
    mapListGoods(item, store.goods.find((goodsItem) => goodsItem.id === String(item.id)))
  )
  goods.forEach((item) => store.upsertGoods(item))
  return goods
}
