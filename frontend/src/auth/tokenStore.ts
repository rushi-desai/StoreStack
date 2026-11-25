import type { AuthUser, StoredSession } from '../types/api'

const SESSION_KEY = 'shopease.session'

function isAuthUser(value: unknown): value is AuthUser {
  if (typeof value !== 'object' || value === null) return false
  if (!('id' in value) || !('email' in value) || !('role' in value)) return false
  return typeof value.id === 'number'
    && typeof value.email === 'string'
    && (value.role === 'USER' || value.role === 'ADMIN')
}

function isStoredSession(value: unknown): value is StoredSession {
  if (typeof value !== 'object' || value === null) return false
  if (!('accessToken' in value) || !('refreshToken' in value) || !('user' in value)) return false
  return typeof value.accessToken === 'string'
    && typeof value.refreshToken === 'string'
    && isAuthUser(value.user)
}

export function readSession(): StoredSession | null {
  const rawSession = window.localStorage.getItem(SESSION_KEY)
  if (!rawSession) return null

  try {
    const session: unknown = JSON.parse(rawSession)
    if (isStoredSession(session)) return session
  } catch {
    clearSession()
  }

  clearSession()
  return null
}

export function saveSession(accessToken: string, refreshToken: string, user: AuthUser): void {
  const session: StoredSession = { accessToken, refreshToken, user }
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session))
}

export function updateSessionTokens(accessToken: string, refreshToken: string): StoredSession | null {
  const current = readSession()
  if (!current) return null
  const session = { ...current, accessToken, refreshToken }
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session))
  return session
}

export function clearSession(): void {
  window.localStorage.removeItem(SESSION_KEY)
}
