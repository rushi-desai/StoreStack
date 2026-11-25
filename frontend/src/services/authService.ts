import type { AuthResponse, RegisterRequest } from '../types/api'
import { apiClient } from './http'

export async function login(email: string, password: string): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>('/auth/login', { email, password })
  return data
}

export async function register(request: RegisterRequest): Promise<string> {
  const { data } = await apiClient.post<string>('/auth/register', request)
  return data
}

export async function logoutRequest(): Promise<void> {
  await apiClient.post('/auth/logout')
}
