import axios, {
  type AxiosError,
  type InternalAxiosRequestConfig,
} from 'axios'
import { clearSession, readSession, updateSessionTokens } from '../auth/tokenStore'
import type { TokenRefreshResponse } from '../types/api'

const API_BASE_URL = (import.meta.env.VITE_API_URL || 'http://localhost:8080/api').replace(/\/+$/, '')

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

apiClient.interceptors.request.use((config) => {
  const session = readSession()
  if (session?.accessToken) {
    config.headers.set('Authorization', `Bearer ${session.accessToken}`)
  }
  return config
})

interface RetriableRequestConfig extends InternalAxiosRequestConfig {
  _retriedAfterRefresh?: boolean
}

let refreshRequest: Promise<string> | null = null

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetriableRequestConfig | undefined
    const requestUrl = originalRequest?.url ?? ''
    const isAuthRequest = ['/auth/login', '/auth/register', '/auth/refresh-token']
      .some((path) => requestUrl.includes(path))

    if (
      error.response?.status !== 401
      || !originalRequest
      || originalRequest._retriedAfterRefresh
      || isAuthRequest
    ) {
      return Promise.reject(error)
    }

    const session = readSession()
    if (!session?.refreshToken) {
      clearSession()
      window.dispatchEvent(new Event('shopease:session-expired'))
      return Promise.reject(error)
    }

    originalRequest._retriedAfterRefresh = true
    if (!refreshRequest) {
      refreshRequest = axios.post<TokenRefreshResponse>(
        `${API_BASE_URL}/auth/refresh-token`,
        { refreshToken: session.refreshToken },
        { headers: { 'Content-Type': 'application/json' } },
      ).then(({ data }) => {
        const updated = updateSessionTokens(data.accessToken, data.refreshToken)
        if (!updated) throw new Error('Your session has expired. Please sign in again.')
        return data.accessToken
      })
    }

    try {
      const accessToken = await refreshRequest
      originalRequest.headers.set('Authorization', `Bearer ${accessToken}`)
      return await apiClient(originalRequest)
    } catch (refreshError) {
      clearSession()
      window.dispatchEvent(new Event('shopease:session-expired'))
      return Promise.reject(refreshError)
    } finally {
      refreshRequest = null
    }
  },
)
