import { apiClient, getAppStore } from '@/api'
import type { AppraiseAppointment, AppraiseMode } from '@/types'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface BackendAppraise {
  id: number | string
  goodsTitle: string
  mode: string
  bookingTime: string
  note: string
  status: string
}

interface CreateAppraisePayload {
  goodsTitle: string
  mode: AppraiseMode
  bookingTime: string
  note: string
}

export const normalizeAppraiseMode = (mode: string): AppraiseMode => {
  if (mode.includes('AI') || mode.toLowerCase().includes('estimate')) {
    return 'AI 快速鉴定'
  }
  if (mode.includes('视频') || mode.includes('瑙嗛')) {
    return '视频连线鉴定'
  }
  if (mode.includes('线下') || mode.includes('到店') || mode.includes('绾夸笅')) {
    return '线下到店鉴定'
  }
  return '视频连线鉴定'
}

export const normalizeAppraiseStatus = (status: string): AppraiseAppointment['status'] => {
  if (status === 'CONFIRMED' || status.includes('已预约') || status.includes('宸查')) {
    return '已预约'
  }
  return '待确认'
}

const mapAppointment = (payload: BackendAppraise): AppraiseAppointment => ({
  id: `appraise-${payload.id}`,
  goodsTitle: payload.goodsTitle,
  mode: normalizeAppraiseMode(payload.mode),
  date: payload.bookingTime,
  note: payload.note,
  status: normalizeAppraiseStatus(payload.status)
})

export const listAppraiseAppointments = async (): Promise<AppraiseAppointment[]> => {
  const response = await apiClient.get<ApiResponse<BackendAppraise[]>>('/appraise/list')
  const appointments = response.data.data.map(mapAppointment)
  getAppStore().setAppointments(appointments)
  return appointments
}

export const createAppraiseAppointment = async (
  payload: CreateAppraisePayload
): Promise<AppraiseAppointment> => {
  const response = await apiClient.post<ApiResponse<BackendAppraise>>('/appraise/create', payload)
  const appointment = mapAppointment(response.data.data)
  getAppStore().setAppointments([appointment, ...getAppStore().appointments])
  return appointment
}
