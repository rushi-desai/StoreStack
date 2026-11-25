import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { AuthProvider, useAuth } from './AuthContext'
import { login as loginRequest, logoutRequest } from '../services/authService'

vi.mock('../services/authService', () => ({
  login: vi.fn(),
  logoutRequest: vi.fn(),
  register: vi.fn(),
}))

function AuthControls() {
  const { user, login, logout } = useAuth()
  return (
    <div>
      <span>{user ? `${user.role}:${user.email}` : 'Signed out'}</span>
      <button onClick={() => void login('person@example.com', 'Password123')}>Sign in</button>
      <button onClick={() => void logout()}>Sign out</button>
    </div>
  )
}

describe('AuthProvider', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.mocked(loginRequest).mockResolvedValue({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      tokenType: 'Bearer',
      expiresIn: 900000,
      role: 'USER',
      email: 'person@example.com',
      userId: 7,
    })
    vi.mocked(logoutRequest).mockResolvedValue()
  })

  it('persists the authenticated user and clears the session on logout', async () => {
    render(<AuthProvider><AuthControls /></AuthProvider>)

    fireEvent.click(screen.getByRole('button', { name: 'Sign in' }))
    expect(await screen.findByText('USER:person@example.com')).toBeInTheDocument()
    expect(JSON.parse(window.localStorage.getItem('shopease.session') ?? '{}')).toMatchObject({
      accessToken: 'access-token',
      refreshToken: 'refresh-token',
      user: { id: 7, email: 'person@example.com', role: 'USER' },
    })

    fireEvent.click(screen.getByRole('button', { name: 'Sign out' }))
    await waitFor(() => expect(screen.getByText('Signed out')).toBeInTheDocument())
    expect(logoutRequest).toHaveBeenCalledOnce()
    expect(window.localStorage.getItem('shopease.session')).toBeNull()
  })
})
