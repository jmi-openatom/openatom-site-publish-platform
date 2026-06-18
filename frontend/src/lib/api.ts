import axios from 'axios'
import type { ApiEnvelope } from '@/types'

export const api = axios.create({
  baseURL: '/api',
  timeout: 20000,
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('site_publish_token')
  if (token) {
    config.headers.satoken = token
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('site_publish_token')
      localStorage.removeItem('site_publish_user')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

export function errorMessage(error: unknown) {
  if (axios.isAxiosError<ApiEnvelope<unknown>>(error)) {
    return error.response?.data?.message || error.message
  }
  return error instanceof Error ? error.message : '操作失败，请稍后重试'
}

