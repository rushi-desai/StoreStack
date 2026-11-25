import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useState } from 'react'
import { useAuth } from '../auth/AuthContext'
import { ErrorNotice } from './Feedback'

export function AppShell() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  const [logoutError, setLogoutError] = useState('')

  async function handleLogout() {
    setLogoutError('')
    try {
      await logout()
    } catch (error) {
      setLogoutError(error instanceof Error ? error.message : 'Sign out could not reach the server.')
    } finally {
      navigate('/')
    }
  }

  return (
    <div className="app-frame">
      <header className="site-header">
        <div className="header-inner">
          <Link className="brand" to="/" aria-label="StoreStack home">
            <span className="brand-mark">S</span>
            <span>storestack<span className="brand-period">.</span></span>
          </Link>
          <nav className="main-nav" aria-label="Main navigation">
            <NavLink to="/" end>Discover</NavLink>
            {user?.role === 'USER' ? <NavLink to="/cart">My cart</NavLink> : null}
            {user?.role === 'USER' ? <NavLink to="/orders">Orders</NavLink> : null}
            {user?.role === 'ADMIN' ? <NavLink to="/admin">Admin</NavLink> : null}
          </nav>
          <div className="header-account">
            {user ? (
              <>
                <span className="account-email">{user.email}</span>
                <button className="button button--quiet button--small" onClick={handleLogout} type="button">
                  Sign out
                </button>
              </>
            ) : (
              <>
                <Link className="header-login" to="/login">Sign in</Link>
                <Link className="button button--dark button--small" to="/register">Create account</Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main className="main-content">
        {logoutError ? <ErrorNotice>{logoutError}</ErrorNotice> : null}
        <Outlet />
      </main>
      <footer className="site-footer">
        <span className="brand brand--footer">storestack<span className="brand-period">.</span></span>
        <span>Thoughtful finds, made simple.</span>
      </footer>
    </div>
  )
}
