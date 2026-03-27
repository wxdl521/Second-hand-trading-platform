import { apiClient } from '@/api'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
  timestamp?: number
}

interface UploadPayload {
  filename: string
  url: string
}

export const uploadFile = async (file: File): Promise<UploadPayload> => {
  const formData = new FormData()
  formData.append('file', file)
  const response = await apiClient.post<ApiResponse<UploadPayload>>('/file/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  return response.data.data
}
