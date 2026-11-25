import axios, {
  AxiosError,
  AxiosHeaders,
  type AxiosAdapter,
  type AxiosResponse,
} from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { saveSession } from '../auth/tokenStore'
import type { AuthUser } from '../types/api'
import { apiClient } from './http'

const user: AuthUser = { id: 7, email: 'person@example.com', role: 'USER' }
let savedApiAdapter = apiClient.defaults.adapter
let savedAxiosAdapter = axios.defaults.adapter

function okResponse<T>(config: Parameters<AxiosAdapter>[0], data: T): AxiosResponse<T> {
  return {
    config,
    data,
    headers: new AxiosHeaders(),
    status: 200,
    statusText: 'OK',
  }
}

function unauthorizedResponse(config: Parameters<AxiosAdapter>[0]): AxiosError {
  const response: AxiosResponse = {
    config,
    data: { message: 'Access token expired' },
    headers: new AxiosHeaders(),
    status: 401,
    statusText: 'Unauthorized',
  }
  return new AxiosError('Unauthorized', AxiosError.ERR_BAD_REQUEST, config, undefined, response)
}

describe('API authentication interceptor', () => {
  beforeEach(() => {
    savedApiAdapter = apiClient.defaults.adapter
    savedAxiosAdapter = axios.defaults.adapter
    window.localStorage.clear()
    saveSession('old-access-token', 'refresh-token', user)
  })

  afterEach(() => {
    apiClient.defaults.adapter = savedApiAdapter
    axios.defaults.adapter = savedAxiosAdapter
    window.localStorage.clear()
    vi.restoreAllMocks()
  })

  it('coalesces concurrent 401 responses into one token refresh and retries both requests', async () => {
    let refreshCalls = 0
    const apiAdapter: AxiosAdapter = async (config) => {
      if (config.headers.get('Authorization') === 'Bearer old-access-token') {
        throw unauthorizedResponse(config)
      }
      return okResponse(config, { ok: true })
    }
    const refreshAdapter: AxiosAdapter = async (config) => {
      refreshCalls += 1
      return okResponse(config, {
        accessToken: 'new-access-token',
        refreshToken: 'rotated-refresh-token',
        tokenType: 'Bearer',
        expiresIn: 900000,
      })
    }
    apiClient.defaults.adapter = apiAdapter
    axios.defaults.adapter = refreshAdapter

    const responses = await Promise.all([
      apiClient.get<{ ok: boolean }>('/private/first'),
      apiClient.get<{ ok: boolean }>('/private/second'),
    ])

    expect(responses.map((response) => response.data.ok)).toEqual([true, true])
    expect(refreshCalls).toBe(1)
    expect(window.localStorage.getItem('shopease.session')).toContain('rotated-refresh-token')
  })

  it('clears the stored session and emits an expiry event when refresh fails', async () => {
    const apiAdapter: AxiosAdapter = async (config) => {
      throw unauthorizedResponse(config)
    }
    const refreshAdapter: AxiosAdapter = async (config) => {
      throw new AxiosError('Refresh failed', AxiosError.ERR_BAD_REQUEST, config)
    }
    apiClient.defaults.adapter = apiAdapter
    axios.defaults.adapter = refreshAdapter
    const onExpired = vi.fn()
    window.addEventListener('shopease:session-expired', onExpired)

    await expect(apiClient.get('/private')).rejects.toThrow('Refresh failed')

    expect(window.localStorage.getItem('shopease.session')).toBeNull()
    expect(onExpired).toHaveBeenCalledOnce()
    window.removeEventListener('shopease:session-expired', onExpired)
  })
})
