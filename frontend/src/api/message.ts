import { apiClient } from '@/api'

const MESSAGE_CHANGED_EVENT = 'syxs:messages-changed'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export interface UserMessageItem {
  id: string
  type: string
  title: string
  content: string
  isRead: boolean
  createdAt: string
}

interface BackendMessagePayload {
  id: number
  type?: string
  title?: string
  content?: string
  isRead?: boolean
  createdAt?: string
}

const normalizeDateTime = (value?: string) =>
  value ? String(value).replace('T', ' ').slice(0, 19) : new Date().toLocaleString('zh-CN', { hour12: false })

const mapMessage = (payload: BackendMessagePayload): UserMessageItem => ({
  id: `message-${payload.id}`,
  type: payload.type ?? 'SYSTEM',
  title: payload.title ?? '系统通知',
  content: payload.content ?? '',
  isRead: Boolean(payload.isRead),
  createdAt: normalizeDateTime(payload.createdAt)
})

const emitMessagesChanged = () => {
  if (typeof window !== 'undefined') {
    window.dispatchEvent(new CustomEvent(MESSAGE_CHANGED_EVENT))
  }
}

export const listMessages = async (): Promise<UserMessageItem[]> => {
  const response = await apiClient.get<ApiResponse<BackendMessagePayload[]>>('/message/list')
  return response.data.data.map(mapMessage)
}

export const markMessageRead = async (id: string): Promise<UserMessageItem> => {
  const numericId = id.replace('message-', '')
  const response = await apiClient.post<ApiResponse<BackendMessagePayload>>(`/message/${numericId}/read`)
  emitMessagesChanged()
  return mapMessage(response.data.data)
}

export const onMessagesChanged = (listener: () => void) => {
  if (typeof window === 'undefined') {
    return () => undefined
  }

  window.addEventListener(MESSAGE_CHANGED_EVENT, listener)
  return () => window.removeEventListener(MESSAGE_CHANGED_EVENT, listener)
}
