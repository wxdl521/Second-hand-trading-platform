import type { EstimateInput, EstimateResult } from '@/types'
import { apiClient, getAppStore } from '@/api'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface EstimatePayload {
  estimatePrice: number
  lowPrice: number
  highPrice: number
  confidence: number
  carbonSavedKg: number
  summary: string
  tips?: string[]
  imageCount?: number
}

interface UploadPayload {
  filename: string
  url: string
}

interface EstimateTaskPayload {
  id: number
  goodsId?: number
  status: 'PENDING' | 'PROCESSING' | 'DONE' | 'FAILED'
  estimatePrice?: number
  priceMin?: number
  priceMax?: number
  confidence?: number
  carbonSavedKg?: number
  summary?: string
  provider?: string
  createdAt?: string
  updatedAt?: string
}

export interface GoodsEstimateSnapshot extends EstimateResult {
  id: string
  goodsId?: string
  status: string
  provider: string
  createdAt: string
  updatedAt: string
}

const normalizeDateTime = (value?: string) =>
  value ? String(value).replace('T', ' ').slice(0, 19) : new Date().toLocaleString('zh-CN', { hour12: false })

const toEstimateResult = (task: EstimateTaskPayload): EstimateResult => ({
  price: Number(task.estimatePrice ?? 0),
  low: Number(task.priceMin ?? 0),
  high: Number(task.priceMax ?? 0),
  confidence: Number(task.confidence ?? 0),
  carbonSavedKg: Number(task.carbonSavedKg ?? 0),
  summary: task.summary ?? '',
  tips: [
    '补充正反面与细节近景，可显著提升 AI 置信度。',
    '若附购入凭证与保卡，建议定价上浮 5%~8%。',
    '支持继续发起视频连线鉴定，提升成交效率。'
  ]
})

const toEstimateSnapshot = (task: EstimateTaskPayload): GoodsEstimateSnapshot => ({
  id: `estimate-${task.id}`,
  goodsId: task.goodsId ? String(task.goodsId) : undefined,
  status: task.status,
  provider: task.provider ?? 'AI',
  createdAt: normalizeDateTime(task.createdAt),
  updatedAt: normalizeDateTime(task.updatedAt),
  ...toEstimateResult(task)
})

export const uploadEstimateImage = async (file: File): Promise<UploadPayload> => {
  const formData = new FormData()
  formData.append('file', file)
  const response = await apiClient.post<ApiResponse<UploadPayload>>('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  return response.data.data
}

export const estimateGoods = async (payload: EstimateInput): Promise<EstimateResult> => {
  const store = getAppStore()
  const response = await apiClient.post<ApiResponse<EstimateTaskPayload>>('/ai/estimate/upload', {
    title: payload.title,
    category: payload.category,
    brand: payload.brand,
    condition: payload.condition,
    description: payload.description ?? '',
    imageUrls: payload.imageUrls ?? [],
    yearsUsed: payload.yearsUsed,
    rarity: payload.rarity
  })
  const task = await pollEstimateTask(response.data.data.id)
  const result = toEstimateResult(task)
  store.setLastEstimate(result)
  return result
}

const pollEstimateTask = async (taskId: number, retries = 12): Promise<EstimateTaskPayload> => {
  for (let index = 0; index < retries; index += 1) {
    const response = await apiClient.get<ApiResponse<EstimateTaskPayload>>(`/ai/estimate/${taskId}`)
    if (response.data.data.status === 'DONE') {
      return response.data.data
    }
    if (response.data.data.status === 'FAILED') {
      throw new Error(response.data.data.summary || 'AI estimate failed')
    }
    await new Promise((resolve) => window.setTimeout(resolve, 600))
  }

  throw new Error('AI estimate timed out')
}

export const getLatestGoodsEstimate = async (goodsId: string): Promise<GoodsEstimateSnapshot | null> => {
  try {
    const response = await apiClient.get<ApiResponse<EstimateTaskPayload>>(`/ai/estimate/goods/${goodsId}`)
    if (!response.data.data?.id) {
      return null
    }
    return toEstimateSnapshot(response.data.data)
  } catch {
    return null
  }
}
