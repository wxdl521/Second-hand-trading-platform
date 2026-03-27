import type { CarbonRecord, CarbonSummary } from '@/types'
import { apiClient, getAppStore } from '@/api'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface BackendCarbonSummary {
  balance: number
  level: string
  monthCarbonSavedKg: number
}

interface BackendCarbonRecord {
  id: number
  title: string
  points: number
  type: '收入' | '支出'
  bizDate: string
  description: string
}

const mapRecord = (payload: BackendCarbonRecord): CarbonRecord => ({
  id: `carbon-${payload.id}`,
  title: payload.title,
  points: Number(payload.points),
  type: payload.type === '支出' ? '支出' : '收入',
  date: payload.bizDate,
  description: payload.description
})

const mapSummary = (payload: BackendCarbonSummary): CarbonSummary => ({
  balance: Number(payload.balance ?? 0),
  level: payload.level || 'Lv.1 绿色新手',
  monthCarbonSavedKg: Number(payload.monthCarbonSavedKg ?? 0)
})

export const getCarbonSummary = async (): Promise<CarbonSummary> => {
  const store = getAppStore()
  const response = await apiClient.get<ApiResponse<BackendCarbonSummary>>('/carbon/account')
  const summary = mapSummary(response.data.data)
  store.setCarbonSummary(summary)
  return summary
}

export const getCarbonBalance = async (): Promise<number> =>
  getCarbonSummary().then((summary) => summary.balance)

export const listCarbonRecords = async (): Promise<CarbonRecord[]> => {
  const store = getAppStore()
  const [summaryResponse, recordsResponse] = await Promise.all([
    apiClient.get<ApiResponse<BackendCarbonSummary>>('/carbon/account'),
    apiClient.get<ApiResponse<BackendCarbonRecord[]>>('/carbon/records')
  ])
  const summary = mapSummary(summaryResponse.data.data)
  const records = recordsResponse.data.data.map(mapRecord)
  store.setCarbonSummary(summary)
  store.setCarbonRecords(records)
  return records
}
