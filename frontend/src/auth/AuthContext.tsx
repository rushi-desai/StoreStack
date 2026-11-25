import {
  createContext,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { clearSession, readSession, saveSession } from './tokenStore'
import { login as loginRequest, logoutRequest, register as registerRequest } from '../services/authService'
import type { AuthUser, RegisterRequest } from '../types/api'

interface AuthContextValue {
  user: AuthUser | null
  login: (email: string, password: string) => Promise<void>
  register: (request: RegisterRequest) => Promise<string>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(() => readSession()?.user ?? null)

  useEffect(() => {
    const handleExpiredSession = () => setUser(null)
    const handleStorageChange = () => setUser(readSession()?.user ?? null)

    window.addEventListener('shopease:session-expired', handleExpiredSession)
    window.addEventListener('storage', handleStorageChange)
    return () => {
      window.removeEventListener('shopease:session-expired', handleExpiredSession)
      window.removeEventListener('storage', handleStorageChange)
    }
  }, [])

  async function login(email: string, password: string) {
    const response = await loginRequest(email, password)
    const authenticatedUser: AuthUser = {
      id: response.userId,
      email: response.email,
      role: response.role,
    }
    saveSession(response.accessToken, response.refreshToken, authenticatedUser)
    setUser(authenticatedUser)
  }

  async function register(request: RegisterRequest) {
    return registerRequest(request)
  }

  async function logout() {
    try {
      if (readSession()) await logoutRequest()
    } finally {
      clearSession()
      setUser(null)
    }
  }

  const value = useMemo(() => ({ user, login, register, logout }), [user])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth must be used within AuthProvider')
  return context
}
