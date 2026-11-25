import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../auth/AuthContext'

export function ProtectedRoute({
  children,
  adminOnly = false,
  userOnly = false,
}: {
  children: ReactNode
  adminOnly?: boolean
  userOnly?: boolean
}) {
  const { user } = useAuth()
  const location = useLocation()

  if (!user) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }
  if (adminOnly && user.role !== 'ADMIN') return <Navigate to="/" replace />
  if (userOnly && user.role !== 'USER') return <Navigate to="/admin" replace />
  return children
}
