import type {
  AdminCategoryItem,
  AdminDatasetSnapshot,
  AdminGoodsItem,
  AdminOrderItem,
  AdminUserItem
} from '@admin/types'
import { categories as seedCategoryNames } from '@shared/data/mock'
import { resolveAssetUrl } from '@shared/utils/assets'
import type { GoodsItem, UserProfile } from '@shared/types'

const ADMIN_DATASET_KEY = 'syxs-admin-dataset-v1'
const pendingReviewGoodsIds = new Set(['goods-007', 'goods-011'])
const rejectedGoodsIds = new Set(['goods-004'])
const pendingKycPhones = new Set(['13800000001'])
const bannedPhones = new Set<string>([])

const categoryDescriptionMap: Record<string, string> = {
  奢品箱包: '聚焦高价值箱包、皮具与经典保值款商品的后台运营。',
  珠宝腕表: '统一管理珠宝首饰、腕表库存、估价与成交节奏。',
  数码设备: '覆盖相机、电脑与高端数码产品的循环交易品类。',
  艺术收藏: '用于管理收藏级艺术周边、配饰与稀缺性商品。',
  家居好物: '维护设计家具、家居摆件和生活方式商品的分类运营。',
  未分类: '承接待整理或暂未归类的商品，方便管理员后续调整。'
}

const clone = <T>(value: T): T => JSON.parse(JSON.stringify(value))
const dedupe = (items: string[]) =>
  Array.from(new Set(items.map((item) => item.trim()).filter(Boolean)))

const hashNumber = (input: string) =>
  input.split('').reduce((sum, current) => sum + current.charCodeAt(0), 0)

const createCategoryCover = (goods: AdminGoodsItem[], categoryName: string) =>
  goods.find((item) => item.category === categoryName)?.heroImage ?? resolveAssetUrl(undefined, 'goods')

const resolveAuditStatus = (item: GoodsItem) => {
  if (pendingReviewGoodsIds.has(item.id)) return '待审核'
  if (rejectedGoodsIds.has(item.id)) return '已驳回'
  return '审核通过'
}

export const normalizeAdminGoods = (
  item: AdminGoodsItem | GoodsItem,
  index = 0
): AdminGoodsItem => {
  const seed = hashNumber(`${item.id}-${item.title}-${index}`)
  const auditStatus =
    'auditStatus' in item ? item.auditStatus : resolveAuditStatus(item)
  const status = item.status

  return {
    ...item,
    heroImage: item.heroImage || resolveAssetUrl(undefined, 'goods'),
    gallery: item.gallery?.length ? item.gallery : [item.heroImage || resolveAssetUrl(undefined, 'goods')],
    auditStatus,
    reviewNote:
      'reviewNote' in item
        ? item.reviewNote
        : auditStatus === '已驳回'
          ? '附件与细节图不足，请补充后重新提交。'
          : auditStatus === '待审核'
            ? '待运营审核后上架。'
            : '审核通过，允许正常流转。',
    transferType:
      'transferType' in item ? item.transferType : (['自卖', '寄卖', '极速回收'] as const)[seed % 3],
    viewCount: 'viewCount' in item ? item.viewCount : 180 + (seed % 600),
    favorCount: 'favorCount' in item ? item.favorCount : 8 + (seed % 60),
    mockCertified: 'mockCertified' in item ? item.mockCertified : auditStatus === '审核通过',
    createdAt: item.createdAt || '2026-03-24',
    status:
      status === '草稿' ||
      status === '待审核' ||
      status === '在售中' ||
      status === '已预订' ||
      status === '已售出' ||
      status === '已下架'
        ? status
        : '在售中'
  }
}

const createDateFromPhone = (phone: string, fallbackDay: number) => {
  const suffix = Number(phone.slice(-2))
  const day = Number.isNaN(suffix) ? fallbackDay : Math.min(28, Math.max(1, suffix % 28 || fallbackDay))
  return `2026-03-${String(day).padStart(2, '0')} 10:00`
}

export const normalizeAdminUser = (
  item: AdminUserItem | UserProfile,
  index = 0
): AdminUserItem => {
  const phone = item.phone
  const registerAt = 'registerAt' in item ? item.registerAt : createDateFromPhone(phone, index + 8)
  const pendingKyc = pendingKycPhones.has(phone)
  const accountStatus = 'accountStatus' in item ? item.accountStatus : bannedPhones.has(phone) ? '已封禁' : '正常'

  return {
    ...item,
    avatar: item.avatar || resolveAssetUrl(undefined, 'avatar'),
    accountStatus,
    kycReviewStatus:
      'kycReviewStatus' in item
        ? item.kycReviewStatus
        : pendingKyc
          ? '待审核'
          : item.kycLevel === 'L1'
            ? '待审核'
            : '已通过',
    registerAt,
    lastActiveAt: 'lastActiveAt' in item ? item.lastActiveAt : registerAt,
    likedGoodsIds: item.likedGoodsIds ?? []
  }
}

export const createCategoryItem = (
  name: string,
  goods: AdminGoodsItem[],
  fallback?: Partial<AdminCategoryItem>
): AdminCategoryItem => {
  const categoryGoods = goods.filter((item) => item.category === name)
  const brands = dedupe([
    ...(fallback?.featuredBrands ?? []),
    ...categoryGoods.map((item) => item.brand)
  ]).slice(0, 4)

  return {
    id: fallback?.id ?? `category-${Date.now()}-${Math.random().toString(36).slice(2, 6)}`,
    name,
    description:
      fallback?.description ||
      categoryDescriptionMap[name] ||
      `用于管理 ${name} 品类商品的上架、状态与交易情况。`,
    featuredBrands: brands,
    coverImage: fallback?.coverImage || createCategoryCover(goods, name)
  }
}

export const mergeCategories = (
  goods: AdminGoodsItem[],
  categories: AdminCategoryItem[] = []
): AdminCategoryItem[] => {
  const nameSet = new Set<string>([...seedCategoryNames, ...goods.map((item) => item.category)])
  categories.forEach((item) => nameSet.add(item.name))

  return Array.from(nameSet)
    .filter(Boolean)
    .map((name) => {
      const fallback = categories.find((item) => item.name === name)
      return createCategoryItem(name, goods, fallback)
    })
    .sort((left, right) => left.name.localeCompare(right.name, 'zh-CN'))
}

export const createDatasetSnapshot = (payload: {
  goods: AdminGoodsItem[] | GoodsItem[]
  orders: AdminOrderItem[]
  users: AdminUserItem[] | UserProfile[]
  categories?: AdminCategoryItem[]
}): AdminDatasetSnapshot => {
  const goods = clone(payload.goods).map((item: AdminGoodsItem | GoodsItem, index: number) =>
    normalizeAdminGoods(item, index)
  )
  const orders = clone(payload.orders)
  const users = clone(payload.users).map((item: AdminUserItem | UserProfile, index: number) =>
    normalizeAdminUser(item, index)
  )
  const categories = mergeCategories(goods, payload.categories)

  return { goods, orders, users, categories }
}

export const readAdminDataset = (): AdminDatasetSnapshot | null => {
  if (typeof window === 'undefined') return null

  const raw = window.localStorage.getItem(ADMIN_DATASET_KEY)
  if (!raw) return null

  try {
    const parsed = JSON.parse(raw) as Partial<AdminDatasetSnapshot>
    return createDatasetSnapshot({
      goods: parsed.goods ?? [],
      orders: parsed.orders ?? [],
      users: parsed.users ?? [],
      categories: parsed.categories ?? []
    })
  } catch (error) {
    console.warn('Failed to parse admin dataset cache.', error)
    return null
  }
}

export const writeAdminDataset = (snapshot: AdminDatasetSnapshot) => {
  if (typeof window === 'undefined') return

  window.localStorage.setItem(ADMIN_DATASET_KEY, JSON.stringify(snapshot))
}
