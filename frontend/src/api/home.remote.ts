import { apiClient } from '@/api'
import { resolveAssetUrl } from '@/utils/assets'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface HomeBannerPayload {
  id: string
  goodsId: number
  imageUrl: string
  title: string
  subtitle: string
}

interface HomeCategoryCardPayload {
  name: string
  count: number
  sample: string
}

interface HomeLandingPayload {
  banners: HomeBannerPayload[]
  categories: HomeCategoryCardPayload[]
  goodsCount: number
  orderCount: number
  carbonSavedKg: number
  todayCarbonKg: number
}

export interface HomeBanner {
  id: string
  goodsId: string
  image: string
  title: string
  subtitle: string
}

export interface HomeCategoryCard {
  name: string
  count: number
  sample: string
}

export interface HomeLandingData {
  banners: HomeBanner[]
  categories: HomeCategoryCard[]
  stats: {
    goodsCount: number
    orderCount: number
    carbonSavedKg: number
    todayCarbonKg: number
  }
}

const defaultCategorySample = '查看该分类好物'

const mapBanner = (payload: HomeBannerPayload): HomeBanner => ({
  id: payload.id,
  goodsId: String(payload.goodsId),
  image: resolveAssetUrl(payload.imageUrl, 'goods'),
  title: payload.title,
  subtitle: payload.subtitle
})

export const getHomeLanding = async (): Promise<HomeLandingData> => {
  const response = await apiClient.get<ApiResponse<HomeLandingPayload>>('/home/landing')

  return {
    banners: response.data.data.banners.map(mapBanner),
    categories: response.data.data.categories.map((item) => ({
      name: item.name,
      count: Number(item.count) || 0,
      sample: item.sample || defaultCategorySample
    })),
    stats: {
      goodsCount: Number(response.data.data.goodsCount) || 0,
      orderCount: Number(response.data.data.orderCount) || 0,
      carbonSavedKg: Number(response.data.data.carbonSavedKg) || 0,
      todayCarbonKg: Number(response.data.data.todayCarbonKg) || 0
    }
  }
}
